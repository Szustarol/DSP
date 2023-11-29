package train_models

import scala.collection.mutable

case class PatientModel(
                         val gender: String,
                         val age: Double,
                         val hypertension: Double,
                         val heart_disease: Double,
                         val ever_married: String,
                         val work_type: String,
                         val Residence_type: String,
                         val avg_glucose_level: Double,
                         val bmi: Double,
                         val smoking_status: String
                       ){
  def assemble(): Array[Double] = {
    val dataBuilder = new mutable.ArrayBuilder.ofDouble()
    dataBuilder.addOne(
      gender match {
        case "Female" => 0.0
        case "Male" => 1.0
        case x => {
          println("different: ", x)
          1.0
        }
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
        case "children" => Array(1.0, 0.0, 0.0, 0.0, 0.0)
        case "Govt_job" => Array(0.0, 1.0, 0.0, 0.0, 0.0)
        case "Never_worked" => Array(0.0, 0.0, 1.0, 0.0, 0.0)
        case "Private" => Array(0.0, 0.0, 0.0, 1.0, 0.0)
        case "Self-employed" => Array(0.0, 0.0, 0.0, 0.0, 1.0)
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
        case "formerly smoked" => Array(1.0, 0.0, 0.0)
        case "smokes" => Array(0.0, 1.0, 0.0)
        case _ => Array(0.0, 0.0, 1.0)
      }
    )
    dataBuilder.result()
  }
}