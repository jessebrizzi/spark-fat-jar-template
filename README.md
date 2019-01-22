# Setting up a Scheduled Spark Scala job on AWS

Have you written a Scala Spark job that processes a massive amount of data on an intimidating amount of RAM and you want to run it daily/weekly/monthly on a schedule on [AWS](https://aws.amazon.com)?
I had to do this recently, and couldn't find a good tutorial on the full process to get the spark job running.
Included in this article and accompanying repository is everything you need to get your Scala Spark job running on AWS [Data Pipeline](https://aws.amazon.com/datapipeline/) and [EMR](https://aws.amazon.com/emr/).

## Code Repo

This tutorial is not going to walk you through the process of actually writing your specific Scala Spark job to do whatever number crunching you need. 
There are already plenty of resources available ([1](https://www.analyticsvidhya.com/blog/2017/01/scala/), [2](https://spark.apache.org/docs/latest/quick-start.html), [3](https://www.coursera.org/learn/scala-spark-big-data)) to get you started on that. 
The code template for setting up a Spark Scala job is available in [this GitHub repo](https://github.com/jessebrizzi/spark-fat-jar-template).

Assuming that you have already written your Spark Job and are only using the [AWS Java SDK](https://aws.amazon.com/sdk-for-java/) to connect to your AWS data stores, drop your code in the [Main](https://github.com/jessebrizzi/spark-fat-jar-template/blob/master/src/main/scala/SparkJob.scala#L4) function of [SparkJob.scala](https://github.com/jessebrizzi/spark-fat-jar-template/blob/master/src/main/scala/SparkJob.scala) and run the [deploy.sh](https://github.com/jessebrizzi/spark-fat-jar-template/blob/master/deploy.sh) script to upload the fat jar to your S3 bucket. 

If you do take other dependencies, then it may take some extra work on your part. 
To run a Scala Spark job on AWS you need to compile a [fat jar](https://stackoverflow.com/questions/19150811/what-is-a-fat-jar) that contains the byte code for your job and all of the libraries it needs to run.
This project already has the [sbt-assembly](https://github.com/sbt/sbt-assembly) plugin setup and a [`assemblyMergeStrategy`](https://github.com/jessebrizzi/spark-fat-jar-template/blob/master/build.sbt#L13) set up to package the Spark, Hadoop, and AWS SDK together in the fat jar.
If you need to add in other libraries that do not play well with each other, or are using a noncompatible version of Spark for this current repo, there are a few good [resources](http://queirozf.com/entries/creating-scala-fat-jars-for-spark-on-sbt-with-sbt-assembly-plugin) available to help you through the needed `build.sbt` modifications.

Outside of the previously mentioned needed changes you need to set a few parameters in the [deploy.sh](https://github.com/jessebrizzi/spark-fat-jar-template/blob/master/deploy.sh) script. 
Mainly the [deploymentPath](https://github.com/jessebrizzi/spark-fat-jar-template/blob/master/deploy.sh#L2) to your specific S3 bucket, adding a profile to the [AWS CLI command](https://github.com/jessebrizzi/spark-fat-jar-template/blob/master/deploy.sh#L14) to upload to your specific S3 bucket if it's private, and changing the resulting fat jar [name](https://github.com/jessebrizzi/spark-fat-jar-template/blob/master/deploy.sh#L3) if you please. 
The deploy script uses the [AWS CLI](https://aws.amazon.com/cli/) to upload the fat jar to S3, so if you do not have it installed and configured you will need to go through the [steps to do that](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html) before using the `deploy.sh` script. 

## Setting Up The Job On AWS

Once your fat jar is uploaded to S3 it's time to set up the scheduled job on AWS Data Pipeline.

#### Create AWS Data Pipeline

Log on to the AWS dash, navigate to the [AWS Data Pipeline](https://console.aws.amazon.com/datapipeline) console, and click the `Create new pipeline` button.

<img src="https://i.imgur.com/o9omjFr.png" width="800"/>

#### Load The Spark Job Template Definition

Add a name for your pipeline and select the `Import a definition` source option. 
Included in the [GitHub repo](https://github.com/jessebrizzi/spark-fat-jar-template) for the Spark template is a [datapipeline.json](https://github.com/jessebrizzi/spark-fat-jar-template/blob/master/datapipeline.json) file that you can import that contains a pre-defined data pipeline for your Spark job that should simplify the setup processes.
The definition contains the node configuration to fire the pipeline off on a schedule and notify you via an [AWS SNS](https://aws.amazon.com/sns/) alarm on a success or failure run.
All of your needed configuration options have been parameterized to simplify this process. 

<img src="https://i.imgur.com/K1EXQRx.png" width="800"/>

#### Set Your Parameters 

<img src="https://i.imgur.com/6JqVHi3.png" width="800"/>

Set the min parameter to the `EMR step(s)` option.
Update the S3 path and fat jar name to point to the fat jar uploaded earlier with the `deploy.sh` script.
Set `EC2KeyPair` so that the data pipeline has access to your EC2 instances.
Select the master node instance type and the number of and type of core-nodes for the Spark cluster.
Finally, we need to set the ARN for both the success and failure notifications for the Spark job so you will know if something goes wrong with your scheduled job. 

#### Create AWS SNS 

Open a new tab and navigate to the [AWS SNS](https://console.aws.amazon.com/sns/v2/) console.
Click the `Create Topic` option.

<img src="https://i.imgur.com/wMlXUuO.png" width="800"/>

In the pop-up window set a topic name and display name for your success alarm and hit the `create topic` button. 

<img src="https://i.imgur.com/QtmsevM.png" width="800"/>

You will be navigated to the topic details page for your new SNS Topic.
Click the `create subscription` button.

<img src="https://i.imgur.com/x7t8yUT.png" width="800"/>

Here you can choose the type of notification you want, for this example, we will set up an email notification for this alarm.
Place your email address in the `Endpoint` field and hit `Create subscription`.

<img src="https://i.imgur.com/C13ikXf.png" width="800"/>

Check your email inbox and confirm the subscription to your SNS alarm. 

<img src="https://i.imgur.com/2Wksf2l.png" width="800"/>

Once you have successfully confirmed your subscription in your email, you should see it listed in the `Subscriptions` table on the `Topic details` page for your SNS Alarm.

<img src="https://i.imgur.com/W6SAfep.png" width="800"/>

You will need to repeat this process for both of your success and failure alarms.
On each of the `Topic details` pages for both of your alarms, you need to copy the `Topic ARN` value and paste it into their respective parameter fields in your Data Pipeline setup.

#### Schedule your Job

<img src="https://i.imgur.com/C9R631G.png" width="800"/>

Once your ARN parameters are set for your alarms you can move on to scheduling when this pipeline will run.
Set the cadence for when the job will run and make sure you set your starting and ending dates to something in the future.
You can also set the job to run only on pipeline activation if you would rather manually start your job.

#### Finish The Pipeline Setup

Once you have your parameters set and scheduled all you need to do is set an S3 bucket location for the logs from the pipeline executions and you can click `Activate` at the bottom of the page.
This will run a check on all of your settings and confirm that everything should work. 
Some things you might run into here could be setting improper values for the instance types for your clusters (Note: not all EC2 types work in EMR and Data Pipeline), or your schedule settings don't make sense. 

<img src="https://i.imgur.com/NV3OIlc.png" width="800"/>

#### Test Your Job

<img src="https://i.imgur.com/UQ5w5oE.png" width="800"/>

Once your job activates successfully you should be done!
If you are doing a test run first, just wait for the time that the pipeline is scheduled to start and it should run and email you on the result of the run.
If you want to view the console/standard output of your job you can find it in the `Emr Step Logs` link and click the `stdout.gz` link to view/download the output.
Here you will also find the error output if something goes wrong in `stderr.gz`.
The logs from the Spark job in your pipeline are in the `controller.gz` file if you need to do some debugging with your pipeline.
