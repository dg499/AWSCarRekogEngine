package com.amazonaws.rekognition;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.greengrass.model.Logger;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class CarRekogService {

  public static void main(String[] args) throws IOException {
    
    // create a group with s3readonly, rekogservicerole, sqs roles.
    // create user with programmatic access and add this user to the group
    // command prompt or on ec2 instance, enter "aws configure" and enter the secret and
    // programmatic access keys.
    // configure java on ec2 free tier instance.
    // upload the jar files generated from this program and modify the permissions using chmod
    // command.


    AWSCredentials credentials = null;
    try {
      credentials = new ProfileCredentialsProvider("default").getCredentials();
    } catch (Exception e) {
      throw new AmazonClientException(
          "Cannot load the credentials from the credential profiles file. "
              + "Please make sure that your credentials file is at the correct "
              + "location (C:\\Users\\<username>\\.aws\\credentials), and is in valid format.",
          e);
    }

    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(Regions.US_EAST_1).build();

    AmazonRekognition rekognitionClient =
        AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_1)
            .withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

    AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(Regions.US_EAST_1).build();

    final String bucketName = "njit-cs-643";

    // queue message processing should end with -1 signal, standard queue doesn't fit in this
    // scenario.  fifo queue maintains the order of the messages.
    final String queueUrl = sqsClient.getQueueUrl("detected-cars.fifo").getQueueUrl();

    try {

//      System.out.println("Listing objects");
      ObjectListing objectListing =
          s3Client.listObjects(new ListObjectsRequest().withBucketName(bucketName));

      for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
        DetectLabelsRequest request = new DetectLabelsRequest()
            .withImage(new Image().withS3Object(
                new S3Object().withName(objectSummary.getKey()).withBucket(bucketName)))
            .withMinConfidence(90F);

        DetectLabelsResult result = rekognitionClient.detectLabels(request);
        List<Label> labels = result.getLabels();


        for (Label label : labels) {
          if (!"Car".equals(label.getName())) {
            continue;
          }
//          System.out.println("Detected labels for " + objectSummary.getKey() + "\n");

          // com.amazonaws.services.s3.model.S3Object object = s3.getObject(new
          // GetObjectRequest(bucketName, objectSummary.getKey()));
          // new FileOperations().writeS3ToFileSystem(object, objectSummary.getKey());

          SendMessageRequest sendMessageRequest =
              new SendMessageRequest(queueUrl, objectSummary.getKey());
          sendMessageRequest.setMessageGroupId("group1");
          sendMessageRequest.setMessageDeduplicationId(UUID.randomUUID().toString());


          sqsClient.sendMessage(sendMessageRequest);

          System.out.print("Label: " + label.getName());
          System.out.println("  Confidence: " + label.getConfidence().toString() + "\n");
          System.out.println("--------------------");
        }
        System.out.println("--------------------");
      }

      System.out.println("signaling the object detection completion");

      SendMessageRequest sendMessageRequest = new SendMessageRequest(queueUrl, "-1");
      sendMessageRequest.setMessageGroupId("group1");
      sendMessageRequest.setMessageDeduplicationId(UUID.randomUUID().toString());
      sqsClient.sendMessage(sendMessageRequest);
      
      System.out.println();

    } catch (AmazonServiceException ase) {
      System.err.println("Caught an AmazonServiceException, which means your request made it "
          + "to Amazon S3, but was rejected with an error response for some reason.");
      System.err.println("Error Message:    " + ase.getMessage());
      System.err.println("HTTP Status Code: " + ase.getStatusCode());
      System.err.println("AWS Error Code:   " + ase.getErrorCode());
      System.err.println("Error Type:       " + ase.getErrorType());
      System.err.println("Request ID:       " + ase.getRequestId());
    } catch (AmazonClientException ace) {
      System.err.println("Caught an AmazonClientException, which means the client encountered "
          + "a serious internal problem while trying to communicate with S3, "
          + "such as not being able to access the network.");
      System.err.println("Error Message: " + ace.getMessage());
    }
  }

}
