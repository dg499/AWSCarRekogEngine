#AWS NJIT CS 643 Car Recognition Service

This is a sample that demonstrates an image recognition pipeline in AWS, using two EC2 instances, S3, SQS,and Rekognition using the AWS SDK for Java.

## Prerequisites

*   You must have a valid Amazon Web Services developer account.
*   Requires the AWS SDK for Java. For more information on the AWS SDK for Java, see [http://aws.amazon.com/sdkforjava](http://aws.amazon.com/sdkforjava).
*   You must be signed up to use Amazon S3. For more information on Amazon SQS, see [http://aws.amazon.com/s3](http://aws.amazon.com/s3).
*   You must be signed up to use Amazon SQS. For more information on Amazon SQS, see [http://aws.amazon.com/sqs](http://aws.amazon.com/sqs).
*   You must be signed up to use Amazon Rekognition. For more information on Amazon SQS, see [https://aws.amazon.com/rekognition/](https://aws.amazon.com/rekognition/).

## Running the Sample

IAM setup
     create a group with s3readonly, rekogservicerole, sqs roles.
     create user with programmatic access and add assign this user to the group.

Following are the basic steps for running the this Car Rekog Worker and Service Programs:

Logging into EC2 Instances:

login into the ec2 instances using the key-pair created during the ec2 creation process through putty.
use puttygen to extract private key and open putty and load the auth file and login into ec2.
ec2-user@public-ip
 
1. use aws configure to create the credentials to access s3, rekog service, and sqs queues on both ec2-rekog-service and ec2-rekog-worker instances.
    `aws configure`
     enter access key id <>
     enter programmatic accees key <>
     enter region: us-east-1 <>
     enter output format: json <>

2.  Creates the credentials file in the location ~/.aws with name "credentials".

3.  `cat credentials` command lists the `default` profile with Access Key ID and Secret Access Key:

  ```
  [default]
  aws_access_key_id =
  aws_secret_access_key =
  ```

4.  check  java is installed and configured in ec2 instance. otherwise configure it using below commands.
	`sudo yum update -y
	 sudo yum list | grep openjdk
	 sudo yum install java-1.8.0-openjdk.x86_64 -y
	 sudo update-alternatives --config java
	 pwd
	 ls -ltr
	 java -version`

5. create the worker jar file by updating the pom.xml file by replacing all 3 occurances

	`<mainClass>com.amazonaws.rekognition.CarRekogService</mainClass>`
	
	 with
	
	`<mainClass>com.amazonaws.rekognition.CarTextRekogWorker</mainClass>`
	
	and run the maven build in eclipse or from command prompt.
	
	`mvn package`
	
	once the build is completed, artifacts are stored in target directory.
	
6. using winscp copy the jar file from target directory to ec2 instance and modify the permssions to execute the jar file.

	`pwd
	 ls -ltr
	 chmod +x worker.jar
	 ls -ltr
	 java -jar worker.jar`

7. worker program is running and polling on "detected-cars.fifo", as soon as the message arrives to the queue, this program reads the message
and gets the object from s3 bucket, and sends it to the text rekog enginer, once the text is rekognized and it keeps track of texts rekognized
in hashmap, and stores the objects to the ec2 instance(for testing purpose) and deletes the messages from queue. processing completes when there is a 
-1 message received from the queue, worker writes the status to output directory output.txt file and close the program.

	`pwd
	 cd output
	 cat *output.txt
	 cd ..`

8. once the program terminates clean up the output directory and images stored on file system using the below commands.

	 `pwd
	 ls -lrt
	 rm -rf output
	 rm -rf *.jpg
	 ls -lrt`
	 
9. to terminate this program use ctrl + c.

10. repeat the steps 1-4 to login into ec2, configuring aws service access permissions and java.

11. create the service jar file by updating the pom.xml file
	
	`<mainClass>com.amazonaws.rekognition.CarTextRekogWorker</mainClass>` in all 3 places.
	
	and run the maven build in eclpse or from command prompt.
	
	`mvn package`
	
	once the build is completed, artifacts are stored in target directory.
		
12. using winscp copy the jar file from target directory to ec2 instance and modify the permssions to execute the jar file.

	`pwd
	 ls -ltr
	 chmod +x service.jar
	 ls -ltr
	 java -jar service.jar`	
	 
13 service jar program reads the images from njit-cs-643 bucket and send it to the rekog service for object and scene detection with minimum
confidence of 90 percent, the images with car labels having 90 percent confidence are put into the queue to be picked up by another instance
for processing text rekognitions.

14 once all the images are processed, car rekog service signals the end of program by sending -1 message to the quue and terminates.



Instance A will read 10 images from an S3 bucket that we created (njit-cs-643) and perform object detection in the images. 
When a car is detected using Rekognition, with confidence higher than 90%, the index of that image (e.g., 2.jpg) is sent in SQS.

Instance B reads indexes of images from SQS as soon as these indexes become available in the queue, and
performs text recognition on these images (i.e., downloads them from S3 one by one and uses Rekognition
for text recognition). 

Note: these two instances work in parallel: for example, instance A is processing
image 3, while instance B is processing image 1 that was recognized as a car by instance A. When instance
A terminates its image processing, it adds index -1 to the queue to signal to instance B that no more indexes
will come. When instance B finishes, it prints to a file,  the indexes of the images that
have both cars and text, and also prints the actual text in each image next to its index.

