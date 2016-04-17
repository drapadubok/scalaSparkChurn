package churn

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}


object ChurnData extends App {
  // Setup local standalone spark, 4 workers I guess?
  //val conf = new SparkConf().setMaster("local[4]").setAppName("Churner")
  val conf = new SparkConf().setAppName("ChurnData")
  val sc = new SparkContext(conf)
  val sqlContext = new SQLContext(sc)
  import sqlContext.implicits._

  def bool2double(b:Boolean): Double = if (b) 1.0 else 0.0

  def getIdxDropColumns(head: Array[String], columnsToDrop: Array[String]) = {
    columnsToDrop.map(lab => head.indexWhere(_==lab))
  }

  def dropColumns(line: Array[String], indices: Array[Int]) = {
    // To drop columns, zip with index, filter columns that match indices
    line.zipWithIndex.filterNot( x => indices.contains(x._2) ).map(_._1)
  }

  def parseLine(line: Array[String]): LabeledPoint = {
    val label = bool2double(line.last.contains("True"))
    val noLabel = line.init
    // Convert y/n to bool
    val features = noLabel.map {
      case "yes" => 1.0
      case "no" => 0.0
      case other => other.toDouble
    }
    LabeledPoint(label, Vectors.dense(features))
  }

  def parseData(url: String) = {
    val text = sc.textFile(url)
    val rdd = text.map(_.split(","))
    val head = rdd.first

    // Drop columns that contain useless info
    val columnsToDrop = Array("Phone","Area Code","State")
    val idxColumnsToDrop = getIdxDropColumns(head, columnsToDrop)
    val rddColumnsDropped = rdd.map(dropColumns(_, idxColumnsToDrop))

    // Split into header and data
    val header = rddColumnsDropped.first
    val body = rddColumnsDropped.filter(_(0) != header(0))

    // Parse data, keep header just in case, might use it later
    (header, body.map(parseLine))
  }

  def saveToS3(output: RDD[LabeledPoint]): RDD[Array[Double]] = {
    val toSave = output.map { lp =>
      Array(Array(lp.label), lp.features.toArray).flatten
    }
    toSave
    // add functionality to actually save as csv
    /*  val dataset = sqlContext.createDataFrame(
    Seq((0, 18, 1.0, Vectors.dense(0.0, 10.0, 0.5), 1.0))
    ).toDF("id", "hour", "mobile", "userFeatures", "clicked")
    */
  }

  val output = parseData("s3n://dimaspark/churn_data.csv")
  val df = output._2.toDF
  //df.write.format("com.databricks.spark.csv").option("header","true").mode("overwrite").save("s3n://dimaspark/churn_processed_csv")

  sc.stop()
}