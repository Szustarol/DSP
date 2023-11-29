package train_models

import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.sql.SparkSession
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions.{when, col}


object ForestTrainer{
  val session = SparkSession
    .builder()
    .appName("Logistic regression trainer")
    .master("local")
    .config("spark.ui.enabled", value = false)
    .getOrCreate()

  def train_forest(df: org.apache.spark.sql.DataFrame):Unit = {

    val n_all = df.count().toDouble
    val n_positive = df.filter(df("label") === 1.0).count().toDouble

    val weighted_df = df.withColumn("weights",
      when(col("label") === 1.0, n_all/n_positive)
        .otherwise(1.0)
    )

    val lr = new RandomForestClassifier()
      .setWeightCol("weights")
      .setFeaturesCol("features")
      .setLabelCol("label")
      .setNumTrees(200)
      .setMaxDepth(10 )

    val Array(train, test) = weighted_df.randomSplit(Array(.7, .3))

    val lr_model = lr.fit(train)

    lr_model.save("saved/rfc_model.orc")

    val test_pred = lr_model.transform(test)

    val metrics_auprc = new BinaryClassificationEvaluator()
      .setLabelCol("label")
      .setRawPredictionCol("prediction")
      .setMetricName("areaUnderPR")

    val metrics_auroc = new BinaryClassificationEvaluator()
      .setLabelCol("label")
      .setRawPredictionCol("prediction")
      .setMetricName("areaUnderROC")

    println("=== RANDOM FOREST CLASSIFICATION ===")
    println("EVALUATION - AUPRC")
    println(metrics_auprc.evaluate(test_pred))
    println("EVALUATION - AUROC")
    println(metrics_auroc.evaluate(test_pred))
  }
}