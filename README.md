## scalaSparkChurn
Testdriving AWS + Spark + Scala with churn dataset, convert to tutorial when done.


# Tools, I use sbt console and intellij, when done will write step-by-step guide on environment setup

https://cwiki.apache.org/confluence/display/SPARK/Useful+Developer+Tools#UsefulDeveloperTools-IDESetup

https://cwiki.apache.org/confluence/display/SPARK/Useful+Developer+Tools

http://hackers-duffers.logdown.com/posts/245018-configuring-spark-with-intellij

Originally I set up my project manually, but there is a nice template here: https://github.com/anabranch/Apache-Spark-Scala-Project-Template

# Tools for AWS

To install AWS cli, python required:

`pip install awscli`

On first use:

`aws configure`

`aws emr create-default-roles`


# Working with EMR on AWS

Assuming you have your account setup and you have created keypair on one of the regions, let's first create an EMR cluster with Spark, ssh to it and then terminate it. I will set logs to be written to a bucket on S3 and provide it with name of my keypair.

For example, assuming my bucket is DUMMYBUCKET and name of keypair is DUMMYKEY. When I just started, I though KeyName parameter required path to DUMMYKEY.pem, but no, just the name.

`aws emr create-cluster --name "MyClu" --release-label emr-4.3.0 --log-uri DUMMYBUCKET --instance-type m3.xlarge --instance-count 3 --applications Name=Spark --use-default-roles --ec2-attributes KeyName=DUMMYKEY`

As a result you will get cluster id, somethin like J-12398213, this will be used to identify your cluster.

`aws emr ssh --cluster-id "J-12398213" --key-pair-file DUMMYKEY.pem`

Now, to terminate the cluster, there should be a CLI command, which always fails for me, so I just go to my AWS console and shut it down manually. I'll deal with it next time I edit this text.

I used a few files I put to S3. In case you want to upload them via CLI, first approach just loads the file, second recursively loads the whole folder:

`time aws s3 cp --region eu-west-1 churn_data.csv DUMMYBUCKET`

`time aws s3 cp --region eu-west-1 --recursive . DUMMYBUCKET`


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



