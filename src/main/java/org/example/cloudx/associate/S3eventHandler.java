package org.example.cloudx.associate;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

public class S3eventHandler implements RequestHandler<S3Event, String> {

  @Override
  public String handleRequest(S3Event input, Context context) {
    final var log = context.getLogger();
    input.getRecords().stream()
        .map(record -> record.getS3().getObject().getKey())
        .forEach(key -> log.log("New image uploaded: " + key));
    return "Dymmy";
  }

}
