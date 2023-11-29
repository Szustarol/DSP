package flows

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import org.apache.spark.ml.classification.{ProbabilisticClassificationModel, ProbabilisticClassifier}
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.SparkSession

final case class PredictionResult(
                                 positive: Boolean,
                                 confidence: Double
                                 )

final class InferenceStage[E <: ProbabilisticClassifier[Vector, E, M], M <: ProbabilisticClassificationModel[Vector, M]](
                          model: ProbabilisticClassificationModel[Vector, M]
                          ) extends GraphStage[FlowShape[Vector, PredictionResult]]{
  val in: Inlet[Vector] = Inlet[Vector]("InferenceStage.in")
  val out: Outlet[PredictionResult] = Outlet[PredictionResult]("InferenceStage.out")

  override def shape: FlowShape[Vector, PredictionResult] = FlowShape(in, out)

  override def createLogic(attributes: Attributes): GraphStageLogic = new GraphStageLogic(shape){
    private val sparkSession = SparkSession
      .builder()
      .master("local")
      .appName("DistributedStroke")
      .config("spark.ui.enabled", value = false)
      .getOrCreate()

    private val currentModel = model


    setHandlers(in, out, new InHandler with OutHandler{
      override def onPush(): Unit = {
        val df = grab(in)
        val predictionResult: Double = currentModel.predict(df)
        val predictionProbability: Double = currentModel.predictProbability(df)(predictionResult.toInt)
        println("Result:", predictionResult, "confidence:", predictionProbability)
        push(out, PredictionResult(
          predictionResult != 0.0,
          predictionProbability
        ))
      }

      override def onPull(): Unit = {
        pull(in)
      }
    })


  }
}