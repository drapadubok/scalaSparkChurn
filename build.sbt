name := "Churn"

organization := "dmitrysmirnov.eu"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.6", "2.11.7")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.6.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
  "org.apache.spark" %% "spark-mllib" % "1.6.0"
)

initialCommands := "import dmitrysmirnov.eu.churn._"