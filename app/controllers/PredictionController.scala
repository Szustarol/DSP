package controllers

import akka.NotUsed
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, SourceQueueWithComplete, Unzip, ZipWith}
import akka.stream.{FlowShape, OverflowStrategy}
import com.google.inject.ImplementedBy
import flows.GeodeStorage.geodeData
import flows.{EncoderStage, InferenceStage, PredictionResult}
import models.PatientModel
import org.apache.spark.ml.classification.{GBTClassificationModel, RandomForestClassificationModel}
import org.apache.spark.sql.SparkSession
import play.api.libs.concurrent.CustomExecutionContext
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

@ImplementedBy(classOf[PredictionExecutionContextImpl])
trait PredictionExecutionContext extends ExecutionContext
class PredictionExecutionContextImpl @Inject() (system: ActorSystem)
  extends CustomExecutionContext(system, "prediction-dispatcher")
  with PredictionExecutionContext

@Singleton
class PredictionController @Inject()(val executionContext: PredictionExecutionContext,
                                     val controllerComponents: ControllerComponents,
                                     val cs: CoordinatedShutdown) extends BaseController{

  private val sparkSession = SparkSession
    .builder()
    .master("local")
    .appName("DistributedStroke")
    .config("spark.ui.enabled", value = false)
    .getOrCreate()

  implicit val sys: ActorSystem = ActorSystem("DistributedStroke")


  cs.addTask(CoordinatedShutdown.PhaseServiceUnbind, "free-geode") { () =>
    geodeData.onTermination
  }

  val bufferSize = 10

  val in: Source[(Promise[Boolean], PatientModel), SourceQueueWithComplete[(Promise[Boolean], PatientModel)]] = Source.queue(bufferSize, OverflowStrategy.dropHead)
  val out = Sink.ignore

  val computationGraph: Flow[(Promise[Boolean], PatientModel), Boolean, NotUsed] = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val sourceUnzip = builder.add(Unzip[Promise[Boolean], PatientModel]())
      val dataEncoder = builder.add(
        new EncoderStage(
        )
      )
      val ForestPredictor = builder.add(
        new InferenceStage(RandomForestClassificationModel.load("train_models/saved/rfc_model.orc")).async
      )
      val GBTPredictor = builder.add(
        new InferenceStage(GBTClassificationModel.load("train_models/saved/gbt_model.orc")).async
      )
      val PredictionZip = builder.add(
        ZipWith[PredictionResult, PredictionResult, Boolean](
          (r1, r2) => if (r1.confidence > r2.confidence) r1.positive  else r2.positive
        )
      )
      val fanout = builder.add(
        Broadcast[org.apache.spark.ml.linalg.Vector](2, eagerCancel = true)
      )

      val promiseZip = builder.add(
        ZipWith[Boolean, Promise[Boolean], Boolean](
          (res: Boolean, p: Promise[Boolean]) => {
            p.complete(Success(res))
            res
          }
        )
      )

      val sourceFanout = builder.add(
        Broadcast[PatientModel](2, eagerCancel = true)
      )

      val buffer = builder.add(
        Flow[org.apache.spark.ml.linalg.Vector].buffer(10, OverflowStrategy.backpressure)
      )

      sourceUnzip.out1 ~> sourceFanout.in
      sourceFanout.out(0) ~> dataEncoder ~> buffer.in
      buffer.out ~> fanout.in
      fanout.out(0) ~> ForestPredictor
      fanout.out(1) ~> GBTPredictor

      val geodeFlow = builder.add(geodeData.getFlow)
      sourceFanout.out(1) ~> geodeFlow.in

      geodeFlow.out ~> Sink.ignore

      ForestPredictor.out ~> PredictionZip.in0
      GBTPredictor.out ~> PredictionZip.in1

      PredictionZip.out ~> promiseZip.in0
      sourceUnzip.out0 ~> promiseZip.in1

      FlowShape(sourceUnzip.in, promiseZip.out)
    }
  )

  val predictionDSLQueue = in.via(computationGraph).to(out).run()


  def predict(patient: PatientModel): Future[Boolean] = {
    val promise = Promise[Boolean]()

    val dslData = (promise, patient)

    predictionDSLQueue offer dslData

    promise.future
  }

  def index(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val patientModel = PatientModel.patientForm.bindFromRequest().get

    val future: Future[Boolean] = predict(patientModel)

    future.map {
      prediction => Ok(views.html.prediction(prediction))
    }(executionContext)
  }
}