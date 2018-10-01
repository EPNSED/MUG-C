# MUGC AWS Lambda functions Architecture 

##Objective: 
Create a flask application(Lambda Function, Front-end component) that collects a pdb file and a email address for the user.
    Then stores the data in Amazon s3 storage with metadata of session Id and email, and the pdb file. 
    Once the data is stored set up Controller component(API gateway or SQS) That send s the data to the ariavata API and waits for computation to be finished and waits for Result complete data event)
    that takes input data and feeds it into GSU hpc computational servers running MUGC java application that takes in the pdb file, 
    does its computation then output data files(List.txt of binding sites) and (pdb file).
    Then use the science gateway api to send the output data to the Amazon s3 bucket with Session ID, Email, pdb file and Results tag. 
    Then the AWS API Gateway will use the results data to email the user the recipt and a link to the website with feature 8(Implementation of view pdb file in UI).

##Requirements
1. [AWS Lambda](https://aws.amazon.com/lambda/)
    * [AWS s3](https://aws.amazon.com/sdk-for-python/)
    * [AWS Async Capabilities](https://docs.aws.amazon.com/aws-technical-content/latest/microservices-on-aws/asynchronous-communication-and-lightweight-messaging.html)
2. Flask Front-End
    * [Fullstack Flask](https://www.fullstackpython.com/aws-lambda.html)
    * [Microservices](https://www.gun.io/blog/serverless-microservices-with-zappa-and-flask)
3. [Amazon API Gateway or SQS]()
    * Send Data to ariavata API
        * Data Handler Methods(GET files form s3 and meta data object) and Data Event Handler Methods(Send data to ariavata API and )
    * [ariavata API](https://airavata.readthedocs.io/en/latest/technical-documentation/airavata-api/)
4. MUGC Java Application
    * [ACoRE](https://help.rs.gsu.edu/display/PD/ACoRE)
5. AWS Lambda Email Function Method

##Architecture

export: export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/apps/Python-3.7.1rc1/Debug-Build/lib/


