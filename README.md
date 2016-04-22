## scalaSparkChurn
Testdriving AWS + Spark + Scala with churn dataset, convert to tutorial when done.


# Tools:

I use sbt console and intellij. Here are some links to help you get started.

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

Assuming you have your AWS account and you have created keypair for one of the regions, let's first create an EMR cluster with Spark, ssh to it and then terminate it. I will set logs to be written to a bucket on S3 and provide it with name of my keypair.

For example, assuming my bucket is DUMMYBUCKET and name of keypair is DUMMYKEY. When I just started, I thought that KeyName parameter required path to DUMMYKEY.pem, but no, just put the keypair name.

`aws emr create-cluster --name "MyClu" --release-label emr-4.3.0 --log-uri DUMMYBUCKET --instance-type m3.xlarge --instance-count 3 --applications Name=Spark --use-default-roles --ec2-attributes KeyName=DUMMYKEY`

As a result you will get cluster id, something like J-12398213, this will be used to identify your cluster.

`aws emr ssh --cluster-id "J-12398213" --key-pair-file DUMMYKEY.pem`

Now, to terminate the cluster, there should be a CLI command, which always fails for me, so I just go to my AWS console and shut it down manually. I'll deal with it next time I edit this text.

As an input, I put a few files to S3. In case you want to upload them via CLI, first approach just loads the file, second recursively loads the whole folder:

`time aws s3 cp --region eu-west-1 churn_data.csv DUMMYBUCKET`

`time aws s3 cp --region eu-west-1 --recursive . DUMMYBUCKET`

# To submit an app:

Assuming we are done with coding and want to throw this at EMR. First we need to create a .jar file. From your project folder run, it will create a jar and tell you the path to it:

`sbt package`

I've read that there might be dependency problems, that sbt-assembly plugin may be used to create uber jar etc., but this is a toy problem and here I didn't run into any trouble. 

Let's test that it works by submitting it to local:

`spark-submit --class "churn.ChurnData" --master "local[4]" myApp.jar`

Check that it created outputs that you expect, in this case it wrote processed data to S3. Now I put my jar to S3:

`time aws s3 cp --region eu-west-1 myApp.jar DUMMYBUCKET`

# Submitting to EMR

I've found multiple ways to achieve this, here are a few that worked for me.

### 1) As a step, assuming cluster is running:

`aws emr add-steps --cluster-id "J-12398213" --steps "Type=spark,Name=ChurnPrepro,Args=[--deploy-mode,cluster,--master,yarn,--conf,spark.yarn.submit.waitAppCompletion=false,--num-executors,2,--executor-cores,2,--executor-memory,5g,--class,churn.ChurnData,s3://DUMMYBUCKET/myApp.jar],ActionOnFailure=CONTINUE"`

You can check the AWS documentation to get better idea what this does, but in general, you specify what type of step it is (Spark), where is the jar and which class to use, some technical specs for executors (still trying to figure out optimal) and what to do if job fails.

### 2) Create cluster and submit, terminate on finish:

```
aws emr create-cluster --name Churner --release-label emr-4.3.0 \
--instance-type m3.xlarge --instance-count 3 --applications Name=Spark  \
--use-default-roles --ec2-attributes KeyName=DUMMYKEY \
--log-uri DUMMYBUCKET \
--steps "Type=spark,Name=ChurnPrepro,Args=[--deploy-mode,cluster,--master,yarn,--conf,spark.yarn.submit.waitAppCompletion=false,--num-executors,2,--executor-cores,2,--executor-memory,5g,--class,churn.ChurnData,s3://DUMMYBUCKET/myApp.jar]" \
--auto-terminate
```

Here the cluster creation is just combined with step.

### 3) Package common configuration to reduce clutter. 
We put the technicalities to myConfig.json and use it instead of specifying all the info about memory reqs and cores per executor. Also package step into json, together with config. The myStep.json contains all that stuff from --steps parameter.

`time aws s3 cp --region eu-west-1 myConfig.json DUMMYBUCKET`
`time aws s3 cp --region eu-west-1 myStep.json DUMMYBUCKET`

To submit the whole thing at once with step and config jsons:

```
aws emr create-cluster --name Churner --release-label emr-4.3.0 \
--instance-type m3.xlarge --instance-count 3 --applications Name=Spark \
--use-default-roles --ec2-attributes KeyName=DUMMYKEY \
--log-uri "s3://DUMMYBUCKET/logs/" \
--steps "s3://DUMMYBUCKET/myStep.json"
```



