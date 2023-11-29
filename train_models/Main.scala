package train_models

import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.sql.SparkSession

object Main{
  def main(args: Array[String]):Unit={

    val session = SparkSession.builder()
      .appName("DistributedStroke")
      .master("local")
      .config("spark.ui.enabled", value = false)
      .getOrCreate()

    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    Logger.getLogger("org.spark-project").setLevel(Level.ERROR)
    println("Loading dataset")
    val dataset = DatasetLoader.prepare_training_set()
    println("Done")

    ForestTrainer.train_forest(dataset)
    GBTTrainer.train_gbt(dataset)

    session.stop()
  }
}
