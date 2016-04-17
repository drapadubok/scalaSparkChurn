package churn


import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SQLContext
import org.apache.spark.ml.feature.{StandardScaler, VectorIndexer, VectorAssembler, VectorSlicer}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.tuning.{ParamGridBuilder, CrossValidator}
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}

object ChurnAnalyze extends App {
  // Setup local standalone spark, 4 workers I guess?
  val conf = new SparkConf().setMaster("local[4]").setAppName("ChurnSQL")
  val sc = new SparkContext(conf)
  val sqlContext = new SQLContext(sc)

  // doesn't work, loads string instead of vector
  val df = sqlContext.read
    .format("com.databricks.spark.csv")
    .option("header", "true") // Use first line of all files as header
    .option("inferSchema", "true") // Automatically infer data types
    .load("s3n://dimaspark/churn_processed_csv")

  // Automagically detect categorical features
  val featureIndexer = new VectorIndexer()
    .setInputCol("features")
    .setOutputCol("indexedFeatures")
    .setMaxCategories(2)
    .fit(df)
  // Report which features were chosen (here correctly 1 and 2)
  // Put to tests, to make sure it produces expected number of categorical columns
  val categoricalFeatures: Set[Int] = featureIndexer.categoryMaps.keys.toSet
  println(s"Chose ${categoricalFeatures.size} categorical features: " +
    categoricalFeatures.mkString(", "))
  // Create new column "indexed" with categorical values transformed to indices
  val indexedData = featureIndexer.transform(df)
  indexedData.show()

  // Z-score the continuous features
  val ZScorer = new StandardScaler()
    .setInputCol("features")
    .setOutputCol("scaledFeatures")
    .setWithStd(true)
    .setWithMean(true)
    .fit(df)
  // Normalize each feature to have unit standard deviation.
  val scaledIndexedData = ZScorer.transform(indexedData)
  scaledIndexedData.show()

  // I would like to better understand the purpose of DataFrames
  // Right now it feels like they are for SQL people to do basic data manipulation
  // They turn any custom processing into a mess.
  // However ML API is neat and I want to use it.
  // Hopefully the more I use DataFrames, the easier it will become.

  // Slice out the categorical features
  val slicerCategorical = new VectorSlicer()
    .setInputCol("indexedFeatures")
    .setOutputCol("categoricalFeatures")
    .setIndices(Array(1,2)) // make sure you know which are categorical
  val df2 = slicerCategorical.transform(scaledIndexedData)

  // Slice out the numerical features
  val slicerNumerical = new VectorSlicer()
    .setInputCol("scaledFeatures")
    .setOutputCol("numericalFeatures")
    // I know that there are 17 features
    // The hardcoded mess below is done to deal with impossibility
    // to get anything from DataFrame, as Rows contain Any
    .setIndices(Array(Array(0), (3 to 16).toArray).flatten)
  val df3 = slicerNumerical.transform(df2)

  // Assemble the dataset together
  val assembler = new VectorAssembler()
    .setInputCols(Array("categoricalFeatures", "numericalFeatures"))
    .setOutputCol("finalFeatures")
  val finalDF = assembler.transform(df3)

  val model = new LogisticRegression()
    .setFeaturesCol("finalFeatures")
    .setLabelCol("label")
    .setMaxIter(10)
    .setRegParam(0.3)
    .setElasticNetParam(0.8)

  //TODO: find how to correctly define input to model, inspect outputs
  val pipeline = new Pipeline().setStages(Array(featureIndexer, ZScorer, slicerCategorical, slicerNumerical, assembler))
  val pipeline = new Pipeline().setStages(Array(model))
  val cv = new CrossValidator()
    .setEstimator(pipeline)
    .setNumFolds(5)
  val m = cv.fit(finalDF)
}



//val accuracy = 1.0 * predictionAndLabels.filter(x => x._1 == x._2).count() / testing.count()







