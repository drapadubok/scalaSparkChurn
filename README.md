# scalaSparkChurn
Testdriving AWS + Spark + Scala with churn dataset, convert to tutorial when done.


# Tools, I use sbt console and intellij, when done will write step-by-step guide on environment setup

https://cwiki.apache.org/confluence/display/SPARK/Useful+Developer+Tools#UsefulDeveloperTools-IDESetup

https://cwiki.apache.org/confluence/display/SPARK/Useful+Developer+Tools

http://hackers-duffers.logdown.com/posts/245018-configuring-spark-with-intellij

To install AWS cli, python required:

pip install awscli

On first use:

aws configure

aws emr create-default-roles


# Some helpful commands

To create EMR cluster with spark and ssh to it:

aws emr create-cluster --name "MyClu" --release-label emr-4.3.0 --log-uri S3_BUCKET --instance-type m3.xlarge --instance-count 3 --applications Name=Spark --use-default-roles --ec2-attributes KeyName=EC2_Key_Name_Not_Path

aws emr ssh --cluster-id "j-xxx" --key-pair-file PATH_TO_EC2_KEYPAIR


To upload files to S3 via CLI:

time aws s3 cp --region eu-west-1 churn_data.csv S3_BUCKET

time aws s3 cp --region eu-west-1 --recursive . S3_BUCKET


To submit an app:

First put your jar to s3:

time aws s3 cp --region eu-west-1 PATH_TO_JAR S3_BUCKET

Submit to local:

spark-submit --class "churn.ChurnData" --master "local[4]" PATH_TO_JAR

Submit to EMR:


1) As a step:

aws emr add-steps --cluster-id "j-xxx" --steps "Type=spark,Name=ChurnPrepro,Args=[--deploy-mode,cluster,--master,yarn,--conf,spark.yarn.submit.waitAppCompletion=false,--num-executors,2,--executor-cores,2,--executor-memory,5g,--class,churn.ChurnData,PATH_TO_JAR_ON_S3],ActionOnFailure=CONTINUE"


2) Create cluster and submit, terminate on finish:

aws emr create-cluster --name Churner --release-label emr-4.3.0 \

--instance-type m3.xlarge --instance-count 3 --applications Name=Spark  \

--use-default-roles --ec2-attributes KeyName=EC2_KEY_NAME \

--log-uri S3_bucket \

--steps "Type=spark,Name=ChurnPrepro,Args=[--deploy-mode,cluster,--master,yarn,--conf,spark.yarn.submit.waitAppCompletion=false,--num-executors,2,--executor-cores,2,--executor-memory,5g,--class,churn.ChurnData,PATH_TO_JAR_ON_S3]" \

--auto-terminate


3) To package common config, see link for exact configurations
time aws s3 cp --region eu-west-1 myConfig.json s3://dimaspark
4) To package step, see see link for exact step configuration
time aws s3 cp --region eu-west-1 myStep.json s3://dimaspark
5) To submit the whole thing at once with step and config jsons:
aws emr create-cluster --name Churner --release-label emr-4.3.0 \
--instance-type m3.xlarge --instance-count 3 --applications Name=Spark \
--use-default-roles --ec2-attributes KeyName=EC2_KEY_NAME \
--log-uri "s3://dimaspark/logs/" \
--steps "s3://dimaspark/myStep.json"



