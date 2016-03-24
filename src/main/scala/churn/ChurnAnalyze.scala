package churn

import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.feature.StandardScaler
/*
object ChurnAnalyze extends App{


val scaler = new StandardScaler(withMean = true, withStd = true).fit(output.map(_.features))
val scaledOutput = output.map { case LabeledPoint(label, features) =>
  LabeledPoint(label, scaler.transform(features))
}

val cv = scaledOutput.randomSplit(Array(0.7, 0.3))
val training = cv(0)
val testing = cv(1)

val model = new LogisticRegressionWithLBFGS().setNumClasses(2).run(training)
// Compute raw scores on the test set.
val predictionAndLabels = testing.map { case LabeledPoint(label, features) =>
  val prediction = model.predict(features)
  (prediction, label)
}


val accuracy = 1.0 * predictionAndLabels.filter(x => x._1 == x._2).count() / testing.count()


// Get evaluation metrics.
val metrics = new BinaryClassificationMetrics(predictionAndLabels)

// Precision by threshold
val precision = metrics.precisionByThreshold
precision.foreach { case (t, p) =>
  println(s"Threshold: $t, Precision: $p")
}

// Recall by threshold
val recall = metrics.recallByThreshold
recall.foreach { case (t, r) =>
  println(s"Threshold: $t, Recall: $r")
}

// Precision-Recall Curve
val PRC = metrics.pr
}
*/