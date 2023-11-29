package train_models
import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.linalg._

object DatasetLoader{
  val session = SparkSession.builder()
    .appName("DistributedStroke")
    .master("local")
    .config("spark.ui.enabled", value = false)
    .getOrCreate()

  def prepare_training_set(): org.apache.spark.sql.DataFrame = {
    val training_set_df = session
      .read.option("nullValue", "N/A").option("header", "true")
      .option("inferSchema", "true")
      .format("csv").load("training.csv")
      .drop("id")
      .na.drop()

    val target = training_set_df.select("stroke")
    val train = training_set_df.drop("stroke")
    import session.implicits._
//    val patientEncoder = Encoders.bean(PatientModel.getClass)
    val patients = train.as[PatientModel].collect()
    var vectorLen = -1
    val patientList = for{patient : PatientModel <- patients} yield {
      val assembled = patient.assemble()
      if (vectorLen == -1) vectorLen = assembled.length
      else if(vectorLen != assembled.length){
        println("@@@ Size mismatch!!!!!!!")
        println(assembled)
        println(vectorLen, "vs", assembled.length)
      }
      Vectors.dense(assembled)
    }
    val targetList = target.as[Double].collect()
    val fin_df = session.sparkContext.parallelize(patientList zip targetList).toDF("features", "label")
    fin_df.show()
    fin_df

//    val numeric_features: Array[String] = Array("avg_glucose_level", "bmi", "age")
//    val categorical_features = training_set_df.columns.filter(x => !numeric_features.contains(x) && x != "stroke")
//
//    val numeric_imputer = new Imputer()
//      .setInputCols(numeric_features)
//      .setOutputCols(numeric_features)
//      .setStrategy("mean")
//
//    val numeric_imputer_model = numeric_imputer.fit(training_set_df)
//
//    val final_df = numeric_imputer_model.transform(
//      training_set_df
//    )
//
//    val indexed_categories = categorical_features map {x => s"${x}_idx"}
//
//    val indexer = new StringIndexer()
//      .setInputCols(categorical_features)
//      .setOutputCols(indexed_categories)
//
//    val indexer_model = indexer.fit(final_df)
//
//    val indexed_df = indexer_model.transform(final_df).drop(categorical_features.toIndexedSeq:_*)
//
//    val cat_imputer = new Imputer()
//      .setInputCols(indexed_categories)
//      .setOutputCols(indexed_categories)
//      .setStrategy("mode")
//
//    val cat_imputer_model = cat_imputer.fit(indexed_df)
//
//    val indexed_cat_df = cat_imputer_model.transform(indexed_df)
//
//    val encoded_categoires = categorical_features map {x => s"${x}_enc"}
//
//    val encoder = new OneHotEncoder()
//      .setInputCols(indexed_categories)
//      .setOutputCols(encoded_categoires)
//
//    val encoder_model = encoder.fit(indexed_cat_df)
//
//    val encoded_df = encoder_model.transform(indexed_cat_df).drop(indexed_categories.toIndexedSeq:_*)
//
//    val assembler = new VectorAssembler()
//      .setInputCols(encoded_df.columns.filter(_ != "stroke"))
//      .setOutputCol("features")
//
//    val assembled_df = assembler.transform(encoded_df)
//
//    try{
//      numeric_imputer_model.save("saved/numeric_imputer.orc")
//      cat_imputer_model.save("saved/cat_imputer.orc")
//      indexer_model.save("saved/indexer.orc")
//      encoder_model.save("saved/encoder.orc")
//      assembler.save("saved/assembler.orc")
//    } catch {
//      case _: Throwable => ()
//    }
//

//    return assembled_df.select("features", "stroke").withColumnRenamed("stroke", "label")
  }
}
