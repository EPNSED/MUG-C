# MUGC AWS Architecture 

## Objective: 
Create a flask application(Front-end component) that collects a pdb file and a email address for the user.
    Then stores the data in Amazon s3 storage with metadata of session Id and email, and the pdb file. The Flask app is deloped with Zappa. s3 Bucket triggers  api gate way component with a lamba function.
    Once the data is stored set up Controller component(API gateway or SQS) That send s the data to the ariavata API and waits for computation to be finished and waits for Result complete data event)
    that takes input data and feeds it into GSU hpc computational servers running MUGC java application that takes in the pdb file, 
    does its computation then output data files(List.txt of binding sites) and (pdb file).
    Then use the science gateway api to send the output data to the Amazon s3 bucket with Session ID, Email, pdb file and Results tag. 
    Then the AWS API Gateway will use the results data to email the user the recipt and a link to the website with feature 8(Implementation of view pdb file in UI).

## Requirements
1. [Zappa](https://www.zappa.io/)
    * Deploy the app with Zappa
    ```
    zappa init 
    ```
2. [AWS Lambda](https://aws.amazon.com/lambda/)
    * [AWS s3](https://aws.amazon.com/sdk-for-python/)
    * [AWS Async Capabilities](https://docs.aws.amazon.com/aws-technical-content/latest/microservices-on-aws/asynchronous-communication-and-lightweight-messaging.html)
3. Flask Front-End
    * [Fullstack Flask](https://www.fullstackpython.com/aws-lambda.html)
    * [Microservices](https://www.gun.io/blog/serverless-microservices-with-zappa-and-flask)
4. [Amazon API Gateway]()
    * Send Data to ariavata API
        * Data Handler Methods(GET files form s3 and meta data object) and Data Event Handler Methods(Send data to ariavata API and )
    * [ariavata API](https://airavata.readthedocs.io/en/latest/technical-documentation/airavata-api/)
5. MUGC Java Application
    * [ACoRE](https://help.rs.gsu.edu/display/PD/ACoRE)
6. [AWS Simple Email Service](https://aws.amazon.com/ses/)
7. AWS Cloudwatch

## Process

Create the php to python client and Java application connection. please see you understand the php client they sends, as well as the java code the chemistry group sent . so that you can prepare the interface between those segments.






```
export PATH=/apps/Python-3.6.6/Debug_Build/bin/:$PATH
```

```
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/apps/Python-3.6.6/Debug_Build/lib
```

```
export JAVA_HOME=/usr/java/jdk1.8.0_191-amd64/
```

Added s3 TRigger manually


https://files.rcsb.org/download/1b8c.pdb 1b8c

Create a s3 bucket timer for resources to delete after a week or month.

javac -cp /home/Mashiku/Documents/Dev/MUG-C/ src/MUGC.java

## Deploy dev enviroment:
Install prerequisite. yum install git gcc gcc-c++

1. Git clone the repository (git clone 

2. cd into mugc_flask

3. set up python3 virtual enviroment.
    python3 -m venv venv

4. run: source ./venv/bin/activate

5. pip install -r requirements.txt

6. Open the [IAM Console](https://console.aws.amazon.com/iam/home?#home).

7. In the navigation pane of the console, choose Users.

8. Choose your IAM user name (not the check box).

9. Choose the Security credentials tab and then choose Create access key.

10. To see the new access key, choose Show. Your credentials will look something like this:

Access key ID: AKIAIOSFODNN7EXAMPLE

Secret access key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

11. To download the key pair, choose Download .csv file. Store the keys in a secure location.

Keep the keys confidential in order to protect your AWS account, and never email them. Do not share them outside your organization, even if an inquiry appears to come from AWS or Amazon.com. No one who legitimately represents Amazon will ever ask you for your secret key.

12. run: aws configure

13. Then cd into your mugc_flask

14. Run zappa deploy dev

https://www.h3xed.com/web-development/how-to-make-all-objects-in-amazon-s3-bucket-public-by-default

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PublicReadGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::mugctest/*"
        }
    ]
}
```
