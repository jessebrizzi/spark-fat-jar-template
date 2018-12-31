#!/bin/bash
deploymentPath=s3://PATH/TO/YOUR/BUCKET/
name=SparkJobFatJar
delim='>>>>>> '

read -p "You are about to assemble & deploy the spark cluster fat jar. Type 'y' if you actually want to do this. " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
    echo $delim "Assembling the fat JAR"
    sbt assembly

    echo $delim "Pushing fat JAR to s3 Deployment Dir"
    aws s3 cp target/scala-2.11/SparkJobFatJar-assembly-1.0.jar $deploymentPath$name.jar

    echo $delim "Done"
else
    echo $delim "Exiting, nothing happened."
fi