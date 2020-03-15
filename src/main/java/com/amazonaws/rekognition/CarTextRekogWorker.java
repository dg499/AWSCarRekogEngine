package com.amazonaws.rekognition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class CarTextRekogWorker {

  public static void main(String[] args) throws Exception {

    ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider("default");
    try {
      credentialsProvider.getCredentials();
    } catch (Exception e) {
      throw new AmazonClientException(
          "Cannot load the credentials from the credential profiles file. "
              + "Please make sure that your credentials file is at the correct "
              + "location (C:\\Users\\dguduru\\.aws\\credentials), and is in valid format.",
          e);
    }

    String bucketName = "njit-cs-643";
    String car = "car";

    AmazonSQS sqs = AmazonSQSClientBuilder.standard().withCredentials(credentialsProvider)
        .withRegion(Regions.US_EAST_1).build();

    AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider)
        .withRegion(Regions.US_EAST_1).build();

    AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
        .withRegion(Regions.US_EAST_1).withCredentials(credentialsProvider).build();

    String myQueueUrl = sqs.getQueueUrl("detected-cars.fifo").getQueueUrl();

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Map<String, String> textDetected = new LinkedHashMap<>();

    try {
      System.out.println(
          "Processing messages received from Object Detection API " + getNameFromUrl(myQueueUrl));

      ReceiveMessageRequest receiveMessageRequest =
          new ReceiveMessageRequest().withMaxNumberOfMessages(10).withQueueUrl(myQueueUrl)
              .withWaitTimeSeconds(0).withVisibilityTimeout(03);



      boolean continue_processing = true;
      while (continue_processing) {
        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
        for (Message message : messages) {
          String key = message.getBody();
          String messageReceiptHandle = messages.get(0).getReceiptHandle();
          sqs.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageReceiptHandle));
          if ("-1".equals(key)) {            
            continue_processing = false;
            break;
          }

          S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, key));
          new FileOperations().writeS3ToFileSystem(object, key);

          com.amazonaws.services.rekognition.model.S3Object s3Object =
              new com.amazonaws.services.rekognition.model.S3Object();
          s3Object.withName(key);
          s3Object.withBucket(bucketName);

          DetectTextRequest request =
              new DetectTextRequest().withImage(new Image().withS3Object(s3Object));

          try {
            DetectTextResult result = rekognitionClient.detectText(request);
            List<TextDetection> textDetections = result.getTextDetections();

            System.out.println("Detected lines and words for " + key);
            new FileOperations().saveFile(key, gson.toJson(textDetections));

            for (TextDetection text : textDetections) {
              if ("LINE".equals(text.getType())) {
                textDetected.put(key, text.getDetectedText());
                break;
              }
              // System.out.println("Detected: " + text.getDetectedText());
              // System.out.println("Confidence: " + text.getConfidence().toString());
              // System.out.println("Id : " + text.getId());
              // System.out.println("Parent Id: " + text.getParentId());
              // System.out.println("Type: " + text.getType());
              // System.out.println();
            }
          } catch (AmazonRekognitionException e) {
            e.printStackTrace();
          }


          
        }
      }

      new FileOperations().saveFile("output", gson.toJson(textDetected));
      System.out.println("text rekognitiion completed successfully");

    } catch (AmazonServiceException ase) {
      System.out.println("Caught an AmazonServiceException, which means your request made it "
          + "to Amazon SQS, but was rejected with an error response for some reason.");
      System.out.println("Error Message:    " + ase.getMessage());
      System.out.println("HTTP Status Code: " + ase.getStatusCode());
      System.out.println("AWS Error Code:   " + ase.getErrorCode());
      System.out.println("Error Type:       " + ase.getErrorType());
      System.out.println("Request ID:       " + ase.getRequestId());
    } catch (AmazonClientException ace) {
      System.out.println("Caught an AmazonClientException, which means the client encountered "
          + "a serious internal problem while trying to communicate with SQS, such as not "
          + "being able to access the network.");
      System.out.println("Error Message: " + ace.getMessage());
    }
  }

  protected static void printObject(Object object, String fileName) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    new FileOperations().saveFile(fileName, gson.toJson(object));
    System.out.println(gson.toJson(object));
  }

  private static String getNameFromUrl(String queueUrl) {
    return queueUrl.substring(queueUrl.lastIndexOf("/") + 1);
  }
}
