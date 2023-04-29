# AWS Lambda function example

This example demonstrates how you can consume messages from SQS and publish them to SNS with Lambda function.

## Prerequisites

* You should be familiar with Amazon SQS and SNS services
* SQS queue created
* SNS topic creates, email subscribed
* You are smart enough to figure out what (if) I've missed in this readme :)

## How to

* Clone this repo
* Build and package: execute `./gradlew buildZip` in terminal
* Create Lambda function and select zip archive `build\distributions\aws-sqs-to-sns-lambda-1.0.0.zip` as source code
* Set environment variables `SQS_QUEUE_NAME` for configured SQS queue and `SNS_TOPIC_NAME` for configured SNS topic
* Send some messages to SQS queue
* Trigger your lambda function (use Test tab)
* Observe logs in CloudWatch, check subscribed email
