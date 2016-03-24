package churn

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.{SparkConf, SparkContext}

object ChurnData extends App {
  // Setup local standalone spark, 4 workers I guess?
  //val conf = new SparkConf().setMaster("local[4]").setAppName("Churner")
  val conf = new SparkConf().setAppName("Churn")
  val sc = new SparkContext(conf)

  def bool2int(b:Boolean): Int = if (b) 1 else 0

  def getDropColumns(head: Array[String], nameToDrop: Array[String]) = {
    // Given column names, get the indices of these columns
    nameToDrop.map(lab => head.indexWhere(_==lab))
  }

  def parseLine(line: Array[String], colsToDrop: Array[Int]) = {
    val label = bool2int(line.last.contains("True"))
    // Drop columns, zip with index, filter columns that match indices in colsToDrop
    val droppedCols = line.zipWithIndex.filterNot( x => colsToDrop.contains(x._2) ).map(_._1)
    // Drop label
    val piecesNoLabel = droppedCols.take(droppedCols.length-1)
    // Convert y/n to bool
    val piecesFin = piecesNoLabel.map {
      case "yes" => 1.0//true
      case "no" => 0.0//false
      case other => other.toDouble
    }
    LabeledPoint(label, Vectors.dense(piecesFin))
  }

  def parseData (url: String) = {
    val text = sc.textFile(url)
    val rdd = text.map(_.split(","))
    val head = rdd.first
    val body = rdd.filter(_(0) != head(0))

    val nameToDrop = Array("Phone","Area Code","State")
    val colsToDrop = getDropColumns(head, nameToDrop)

    body.map(parseLine(_, colsToDrop))
  }

  val output = parseData("s3n://dimaspark/churn_data.csv")
  output.saveAsTextFile("s3n://dimaspark/churn_processed/")

  sc.stop()
}

