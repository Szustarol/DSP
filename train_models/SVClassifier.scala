package train_models

import org.apache.spark.ml.classification.LinearSVC
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.sql.SparkSession
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions.{when, col}


object SVCTrainer{
  val session = SparkSession
    .builder()
    .appName("Logistic regression trainer")
    .master("local")
    .getOrCreate()

  def train_svc(df: org.apache.spark.sql.DataFrame):Unit = {

    val n_all = df.count().toDouble
    val n_positive = df.filter(df("label") === 1.0).count().toDouble

    val weighted_df = df.withColumn("weights",
      when(col("label") === 1.0, n_all/n_positive)
        .otherwise(1.0)
    )

    val lr = new LinearSVC()
      .setWeightCol("weights")
      .setRegParam(0.1)
      .setMaxIter(100)

    val Array(train, test) = weighted_df.randomSplit(Array(.7, .3))

    val lr_model = lr.fit(train)

    lr_model.save("saved/svc_model.orc")

    val test_pred = lr_model.transform(test)

    val metrics_auprc = new BinaryClassificationEvaluator()
      .setLabelCol("label")
      .setRawPredictionCol("prediction")
      .setMetricName("areaUnderPR")

    val metrics_auroc = new BinaryClassificationEvaluator()
      .setLabelCol("label")
      .setRawPredictionCol("prediction")
      .setMetricName("areaUnderROC")

    println("=== LinearSVC CLASSIFICATION ===")
    println("EVALUATION - AUPRC")
    println(metrics_auprc.evaluate(test_pred))
    println("EVALUATION - AUROC")
    println(metrics_auroc.evaluate(test_pred))
  }
}