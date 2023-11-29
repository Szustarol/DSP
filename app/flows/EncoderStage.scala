package flows

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import models.PatientModel
import org.apache.spark.ml.linalg
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._

final class EncoderStage(
                        ) extends GraphStage[FlowShape[PatientModel, org.apache.spark.ml.linalg.Vector]]{
  val in: Inlet[PatientModel] = Inlet[PatientModel]("EncoderStage.in")
  val out: Outlet[linalg.Vector] = Outlet[org.apache.spark.ml.linalg.Vector]("EncoderStage.out")

  override def shape: FlowShape[PatientModel, org.apache.spark.ml.linalg.Vector] = FlowShape(in, out)

  override def createLogic(attributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    private val schema = List(
      StructField("gender", StringType),
      StructField("age", IntegerType),
      StructField("hypertension", DoubleType),
      StructField("heart_disease", DoubleType),
      StructField("ever_married", StringType),
      StructField("work_type", StringType),
      StructField("Residence_type", StringType),
      StructField("avg_glucose_level", DoubleType),
      StructField("bmi", DoubleType),
      StructField("smoking_status", StringType)
    )

    private val sparkSession = SparkSession
      .builder()
      .master("local")
      .appName("DistributedStroke")
      .config("spark.ui.enabled", value = false)
      .getOrCreate()

    setHandlers(in, out, new InHandler with OutHandler{
      override def onPush(): Unit = {
        val nextElement: PatientModel = grab(in)
        println("Received model")

        val vec = Vectors.dense(nextElement.assemble())
        push(out, vec)
      }

      override def onPull(): Unit = {
        pull(in)
      }
    })
  }
}
