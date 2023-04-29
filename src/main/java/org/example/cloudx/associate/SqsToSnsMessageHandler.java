package org.example.cloudx.associate;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SqsToSnsMessageHandler implements RequestHandler<Map<String, Object>, Void> {

  private static final String AWS_REGION = System.getenv("AWS_REGION");
  private static final String SQS_QUEUE_NAME = System.getenv("SQS_QUEUE_NAME");
  private static final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");
  private static final String API_CALL = "API CALL";
  private static final String DETAIL_TYPE = "detail-type";
  private static final Integer RECEIVE_MESSAGES_TIMEOUT = 5;
  private static final Integer MAX_NUMBER_OF_MESSAGES = 3;

  private final AmazonSNS amazonSns = AmazonSNSClientBuilder.standard()
      .withRegion(AWS_REGION)
      .build();

  private final AmazonSQS amazonSqs = AmazonSQSClientBuilder.standard()
      .withRegion(AWS_REGION)
      .build();

  private LambdaLogger logger;

  @Override
  public Void handleRequest(Map<String, Object> input, Context context) {
    this.logger = context.getLogger();

    final var sqsMessages = pollForSqsMessages();
    final var processedMessagesCount = processSqsMessages(sqsMessages);

    final var detailType = input.getOrDefault(DETAIL_TYPE, API_CALL);

    logger.log("Lambda function name = %s. Detail type = %s. Processed messages count = %d."
        .formatted(context.getFunctionName(), detailType, processedMessagesCount));

    return null;
  }

  private List<Message> pollForSqsMessages() {
    try {
      final var queueUrlRequest = new GetQueueUrlRequest().withQueueName(SQS_QUEUE_NAME);
      final var queueUrl = amazonSqs.getQueueUrl(queueUrlRequest).getQueueUrl();

      final var request = new ReceiveMessageRequest()
          .withQueueUrl(queueUrl)
          .withMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES)
          .withWaitTimeSeconds(RECEIVE_MESSAGES_TIMEOUT);

      List<Message> messages = IntStream.of(0, MAX_NUMBER_OF_MESSAGES)
          .mapToObj(i -> amazonSqs.receiveMessage(request).getMessages())
          .flatMap(List::stream)
          .toList();
      cleanUp(messages, queueUrl);
      return messages;
    } catch (final SdkClientException exc) {
      logger.log("Failed to receive messages from %s. Details: %s".formatted(SQS_QUEUE_NAME, exc.getMessage()));
      return List.of();
    }
  }

  private void cleanUp(final List<Message> sqsMessages, final String queueUrl) {
    if (!sqsMessages.isEmpty()) {
      logger.log("Deleting SQS messages from %s".formatted(queueUrl));
      try {
        sqsMessages.forEach(msg -> amazonSqs.deleteMessage(queueUrl, msg.getReceiptHandle()));
        logger.log("Deleted SQS messages from %s".formatted(queueUrl));
      } catch (final SdkClientException exc) {
        logger.log("Failed to delete SQS messages from %s".formatted(queueUrl));
      }
    }
  }

  private int processSqsMessages(final List<Message> sqsMessages) {
    if (sqsMessages.isEmpty()) {
      logger.log("No SQS messages to process");
      return 0;
    }

    try {
      final var snsMessage = sqsMessages.stream()
          .map(Message::getBody)
          .collect(Collectors.joining("\n\n"));

      final var request = new PublishRequest()
          .withTopicArn(SNS_TOPIC_ARN)
          .withSubject("Notification from CloudX web application")
          .withMessage(snsMessage);
      amazonSns.publish(request);
      return sqsMessages.size();
    } catch (final SdkClientException exc) {
      logger.log("Failed to process SQS messages");
      return 0;
    }
  }

}
