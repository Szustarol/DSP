package controllers

import akka.actor.ActorSystem
import akka.stream.alpakka.file.scaladsl.LogRotatorSink
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.{Compression, FileIO, Flow, Keep, Sink}
import akka.util.ByteString
import flows.GeodeStorage
import models.PatientModel
import play.api._
import play.api.libs.json.Json
import play.api.mvc._

import java.io.File
import java.nio.file.{Path, Paths}
import java.time
import javax.inject._
import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf

@Singleton
class DoctorController @Inject()(val controllerComponents: ControllerComponents) extends BaseController{
  implicit val sys: ActorSystem = ActorSystem("DistributedStroke")

  def index() = Action { implicit request: Request[AnyContent] =>
    val nPatients = GeodeStorage.geodeData.geode.query[PatientModel](s"select * from /patients")
      .runWith(Sink.fold(0)((acc, _) => {
        acc + 1
      }))
    val nPatientsResult = Await.result(nPatients, Inf)

    if(nPatientsResult == 0)
      Ok(views.html.doctor(nPatientsResult, null))
    else{
      val patient = GeodeStorage.geodeData.geode.query[PatientModel](s"select * from /patients")
        .via(Flow[PatientModel].reduce((acc, next_acc) => {
          if(acc.randomOrder < next_acc.randomOrder){
            acc
          } else {
            next_acc
          }
        }))
        .runWith(Sink.head)
      val patientResult = Await.result(patient, Inf)
      Ok(views.html.doctor(nPatientsResult, patientResult))
    }
  }

  def predict(patientId: Int, prediction: Int) = Action {
    implicit request: Request[AnyContent] =>
      val output_file_name = patientId.toString + "_" +
        time.LocalDateTime.now().toString.replace("T","_") +
        ".bin.gz"
      val output_dir = new File("output_data")
      if(!output_dir.exists())
        output_dir.mkdirs()

      def output_file(Unit: ByteString) = {
        Some(Paths.get("output_data", output_file_name))
      }


      val triggerFunctionCreator: () => ByteString => Option[Path] = () => output_file

      val jsonByteStream = GeodeStorage.geodeData.geode.query[PatientModel](
        s"select * from /patients p where p.id=$patientId"
      ).via(
        Flow[PatientModel].map(x => {
          x.id = patientId
          ByteString(Json.toJson(x).toString, "utf-8")
        })
      )

      jsonByteStream.via(JsonReader.select(
          "$.id"
      )).runWith(Sink.foreach(x => {
        println("[LOG] Stream is currently processing ID: " + x.utf8String)
      }))

      jsonByteStream.runWith(
        LogRotatorSink.withSinkFactory(
          triggerFunctionCreator,
          (path: Path) => {
            Flow[ByteString]
              .via(Compression.gzip)
              .toMat(FileIO.toPath(path))(Keep.right)
          }
        )
      )

      Redirect(routes.DoctorController.index())
  }
}