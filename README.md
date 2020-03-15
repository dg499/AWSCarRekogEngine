#AWS NJIT CS 643 Car Recognition Service

This is a sample that demonstrates an image recognition pipeline in AWS, using two EC2 instances, S3, SQS,and Rekognition using the AWS SDK for Java.

## Prerequisites

*   You must have a valid Amazon Web Services developer account.
*   Requires the AWS SDK for Java. For more information on the AWS SDK for Java, see [http://aws.amazon.com/sdkforjava](http://aws.amazon.com/sdkforjava).
*   You must be signed up to use Amazon S3. For more information on Amazon SQS, see [http://aws.amazon.com/s3](http://aws.amazon.com/s3).
*   You must be signed up to use Amazon SQS. For more information on Amazon SQS, see [http://aws.amazon.com/sqs](http://aws.amazon.com/sqs).
*   You must be signed up to use Amazon Rekognition. For more information on Amazon SQS, see [https://aws.amazon.com/rekognition/](https://aws.amazon.com/rekognition/).

## Running the Sample

The basic steps for running the Amazon SQS sample are:

1.  Create a credentials file in the location ~/.aws with name "credentials".

2.  Under the `default` profile fill in your Access Key ID and Secret Access Key:

  ```
  [default]
  aws_access_key_id =
  aws_secret_access_key =
  ```

3.  Save the file.

4.  Run the `CarRekogService.java` file, located in the same directory as the properties file. The sample prints information to the standard output.


Navigate to AWS console and  create 2 EC2 instances (EC2 Rekog Service ), with Amazon Linux AMI, use the same key pair for creating the instances.
Navigate to IAM console and create role to access AWS rekognition service, S3 and SQS service assign appropriate priviliges.
On Ec2 Dashboard assign the newly created role to ec2 instances.

Use the puttygen and generate the private key from the key pair and access the ec2-user@ipaddress to access the service node
and worker node.

upload the jar files to both nodes

set the java home and run java -jar rekog-worker.jar to continuosly poll the sns queue.

set the java home and run java -jar rekog-service.jar to read and send the s3 image object recognitions  to the sns queue.
 
these two instances works  in parallel.

Instance A will read 10 images from an S3 bucket that we created (njit-cs-643) and perform object detection in the images. When a car is detected using
Rekognition, with confidence higher than 90%, the index of that image (e.g., 2.jpg) is sent in SQS.
Instance B reads indexes of images from SQS as soon as these indexes become available in the queue, and
performs text recognition on these images (i.e., downloads them from S3 one by one and uses Rekognition
for text recognition). Note that the two instances work in parallel: for example, instance A is processing
image 3, while instance B is processing image 1 that was recognized as a car by instance A. When instance
A terminates its image processing, it adds index -1 to the queue to signal to instance B that no more indexes
will come. When instance B finishes, it prints to a file,  the indexes of the images that
have both cars and text, and also prints the actual text in each image next to its index.