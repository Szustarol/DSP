package models

import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formats._
import play.api.libs.json.{Json, OWrites}

import scala.collection.mutable

case class PatientModel(
                       val gender: String,
                       val age: Int,
                       val hypertension: Int,
                       val heart_disease: Int,
                       val ever_married: String,
                       val work_type: String,
                       val Residence_type: String,
                       val avg_glucose_level: Double,
                       val bmi: Double,
                       val smoking_status: String,
                       var id: Int = -1
                       ){
  val randomOrder: Int = scala.util.Random.nextInt()
  if(id == -1){
    id = PatientModel.nextId
    PatientModel.nextId += 1
  }
  def assemble(): Array[Double] = {
    val dataBuilder = new mutable.ArrayBuilder.ofDouble()
    dataBuilder.addOne(
      gender match {
        case "Female" => 0.0
        case "Male" => 1.0
      }
    )
    dataBuilder.addOne(age.toDouble)
    dataBuilder.addOne(hypertension.toDouble)
    dataBuilder.addOne(heart_disease.toDouble)
    dataBuilder.addOne(ever_married match {
      case "Yes" => 1.0
      case "No" => 0.0
    })
    dataBuilder.addAll(
      work_type match{
        case "children" => Seq(1.0, 0.0, 0.0, 0.0, 0.0)
        case "Govt_job" => Seq(0.0, 1.0, 0.0, 0.0, 0.0)
        case "Never_worked" => Seq(0.0, 0.0, 1.0, 0.0, 0.0)
        case "Private" => Seq(0.0, 0.0, 0.0, 1.0, 0.0)
        case "Self-employed" => Seq(0.0, 0.0, 0.0, 0.0, 1.0)
      }
    )
    dataBuilder.addOne(
      Residence_type match {
        case "Urban" => 1.0
        case _ => 0.0
      }
    )
    dataBuilder.addOne(avg_glucose_level)
    dataBuilder.addOne(bmi)
    dataBuilder.addAll(
      smoking_status match{
        case "formerly smoked" => Seq(1.0, 0.0, 0.0)
        case "smokes" => Seq(0.0, 1.0, 0.0)
        case _ => Seq(0.0, 0.0, 1.0)
      }
    )
    dataBuilder.result()
  }
}


object PatientModel{

  implicit val patientModelWriter: OWrites[PatientModel] = Json.writes[PatientModel]

  var nextId = 0

  val patientForm: Form[PatientModel] = Form(
    mapping(
      "gender" -> text,
      "age" -> number,
      "hypertension" -> number,
      "heart_disease" -> number,
      "ever_married" -> text,
      "work_type" -> text,
      "Residence_type" -> text,
      "avg_glucose_level" -> of(doubleFormat),
      "bmi" -> of(doubleFormat),
      "smoking_status" -> text,
      "id" -> default(number, -1)
    )(PatientModel.apply)(PatientModel.unapply)
  )
}