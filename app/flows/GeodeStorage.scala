package flows

import akka.actor.ActorSystem
import akka.stream.alpakka.geode.scaladsl.Geode
import akka.stream.alpakka.geode.{AkkaPdxSerializer, GeodeSettings, RegionSettings}
import akka.stream.scaladsl.{Flow, Sink}
import akka.{Done, NotUsed}
import models.PatientModel
import org.apache.geode.pdx.{PdxReader, PdxWriter}

import scala.concurrent.duration.Duration.Inf
import scala.concurrent.{Await, Future, Promise}
import scala.util.Success

object PatientModelPdxSerializer extends AkkaPdxSerializer[PatientModel]{
  override def clazz: Class[PatientModel] = classOf[PatientModel]

  override def toData(o: scala.Any, out: PdxWriter): Boolean = {
    if(o.isInstanceOf[PatientModel]){

      val p = o.asInstanceOf[PatientModel]
      out.writeString("gender", p.gender)
      out.writeInt("age", p.age)
      out.writeInt("hypertension", p.hypertension)
      out.writeInt("heart_disease", p.heart_disease)
      out.writeString("ever_married", p.ever_married)
      out.writeString("work_type", p.work_type)
      out.writeString("Residence_type", p.Residence_type)
      out.writeDouble("avg_glucose_level", p.avg_glucose_level)
      out.writeDouble("bmi", p.bmi)
      out.writeString("smoking_status", p.smoking_status)
      true
    } else false
  }

  override def fromData(clazz: Class[_], in: PdxReader): AnyRef = {
    PatientModel(
      in.readString("gender"),
      in.readInt("age"),
      in.readInt("hypertension"),
      in.readInt("heart_disease"),
      in.readString("ever_married"),
      in.readString("work_type"),
      in.readString("Residence_type"),
      in.readDouble("avg_glucose_level"),
      in.readDouble("bmi"),
      in.readString("smoking_status")
    )
  }
}

object GeodeStorage{
  val geodeData = new GeodeStorage()

  PatientModel.nextId = geodeData.getNextID
}

class GeodeStorage(){
  implicit val sys: ActorSystem = ActorSystem("DistributedStroke")

  val geodeSettings: GeodeSettings = GeodeSettings("127.0.0.1", 10334)
    .withConfiguration(c => c.setPoolIdleTimeout(360*1000))
  val geode = new Geode(geodeSettings)

  val patientRegionSettings: RegionSettings[Int, PatientModel] =
    RegionSettings("patients", (p: PatientModel) => p.id)

  def getFlow: Flow[PatientModel, PatientModel, NotUsed] = geode.flow(patientRegionSettings)

  def onTermination: Future[Done] ={
    val done = Promise[Done]()
    geode.close()
    done.complete(Success(Done)).future
  }

  def getNextID: Int = {
    val IDSource = geode.query[PatientModel](
      "SELECT * FROM /patients ORDER BY id DESC LIMIT 1"
    ).runWith(
      Sink.fold(0){(acc, patient) => patient.id+1}
    )
    Await.result(IDSource, Inf)
  }
}