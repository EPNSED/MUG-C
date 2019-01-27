
import json
import urllib.parse
import boto3

print('Loading function')

s3 = boto3.client('s3')
ses = boto3.client('ses')
ecs = boto3.client('ecs')
lambda_client = boto3.client('lambda')


def lambda_handler(event, context):
    print("Received event: " + json.dumps(event, indent=2))

    # Get the object from the event and show its content type
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    try:
        response = s3.get_object(Bucket=bucket, Key=key)
        print(response)
        #List all parameter args
        email_arg = response['Metadata']['useremail']
        s3key_arg = response['Metadata']['s3key']
        pdbID_arg = response['Metadata']['pdbid']
        pdbURL_arg = response['Metadata']['pdburl']
        pdbID_noextention = pdbID_arg.split(".")[0]
        sessionID_arg = s3key_arg.split("/")[0]
        if "pdb.txt" not in s3key_arg: 
            url_arg = getInputFileUrl(s3key_arg)
            arivata_url_arg =shortenURL(url_arg)
        else:
            arivata_url_arg = pdbURL_arg
        if verify_email(email_arg) == True:#check if the email of the user is verified
            send_receipt(email_arg, email_arg)#sent notication job has started
            #AiravataLambdaInterface(sessionID_arg,s3key_arg,expInfo,inputURL_arg,outputFile_arg)
            MugcDockerInit(arivata_url_arg, pdbID_noextention, sessionID_arg)
        else:
            print('Sent verification email')
        print("CONTENT TYPE: " + response['ContentType'])
        return response['ContentType']
    except Exception as e:
        print(e)
        print('Error getting object {} from bucket {}. Make sure they exist and your bucket is in the same region as this function.'.format(key, bucket))
        raise e
        
#####///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#####///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def verify_email(email):#Checks if the email is verified, if not it sends a verification email to the user.
    user_verified = None
    identity_response = ses.get_identity_verification_attributes(
        Identities=[
            email
        ]
    )
    print(identity_response)
    if identity_response['VerificationAttributes']:
        user_verified = True
    else:
        response = ses.verify_email_identity(
            EmailAddress= email
        )#use custom email verification template
        user_verified = False
    return (user_verified)

def send_receipt(sender_arg, recipient_arg):
    # Replace sender@example.com with your "From" address.
    # This address must be verified with Amazon SES.
    SENDER = sender_arg
    
    # Replace recipient@example.com with a "To" address. If your account 
    # is still in the sandbox, this address must be verified.
    RECIPIENT = recipient_arg
    
    # Specify a configuration set. If you do not want to use a configuration
    # set, comment the following variable, and the 
    # ConfigurationSetName=CONFIGURATION_SET argument below.
    #CONFIGURATION_SET = "ConfigSet"
    
    # If necessary, replace us-west-2 with the AWS Region you're using for Amazon SES.
    AWS_REGION = "us-east-1"
    
    # The subject line for the email.
    SUBJECT = "Amazon SES Test (SDK for Python)"
    
    # The email body for recipients with non-HTML email clients.
    BODY_TEXT = ("Amazon SES Test (Python)\r\n"
                 "This email was sent with Amazon SES using the "
                 "AWS SDK for Python (Boto)."
                )
                
    # The HTML body of the email.
    BODY_HTML = """<html>
    <head></head>
    <body>
      <h1>Amazon SES Test (SDK for Python)</h1>
      <p>This email was sent with
        <a href='https://aws.amazon.com/ses/'>Amazon SES</a> using the
        <a href='https://aws.amazon.com/sdk-for-python/'>
          AWS SDK for Python (Boto)</a>.</p>
        <p> Your PDB file has been recieved and is currently being processed. After the results are ready you will recieve another email with your results.</p>
    </body>
    </html>
                """            
    
    # The character encoding for the email.
    CHARSET = "UTF-8"
    
    # Create a new SES resource and specify a region.
    client = boto3.client('ses',region_name=AWS_REGION)
    
    # Try to send the email.
    try:
        #Provide the contents of the email.
        response = client.send_email(
            Destination={
                'ToAddresses': [
                    RECIPIENT,
                ],
            },
            Message={
                'Body': {
                    'Html': {
                        'Charset': CHARSET,
                        'Data': BODY_HTML,
                    },
                    'Text': {
                        'Charset': CHARSET,
                        'Data': BODY_TEXT,
                    },
                },
                'Subject': {
                    'Charset': CHARSET,
                    'Data': SUBJECT,
                },
            },
            Source=SENDER,
            # If you are not using a configuration set, comment or delete the
            # following line
            # ConfigurationSetName=CONFIGURATION_SET,
        )
    # Display an error if something goes wrong.	
    except ClientError as e:
        print(e.response['Error']['Message'])
    else:
        print("Email sent! Message ID:"),
        print(response['MessageId'])

#def AiravataLambdaInterface(sessionID_arg,s3key_arg,expInfo,inputURL_arg,outputFile_arg):
    #Define the stdin as the inputURL
    #Define the stdout as the outputfile
    #expInfo is the experiment name etc.
    #Worker checks prior to using the AiravataWrapperInterface to send the job
    #Create wait for completions then store the file to the same sessionID
    #getInputFileUrl(s3Key)

def MugcDockerInit(pdburl_arg, pdbid_arg, sessionid_arg):
    invoke_response = ecs.run_task(
    cluster='mugctestdocker',
    taskDefinition='mugcdocker',
    overrides={
        'containerOverrides': [
            {
                'name': 'test',
                'command': [
                    pdburl_arg,
                    pdbid_arg,
                    sessionid_arg,
                ]
            },
        ],
        'taskRoleArn': 'arn:aws:iam::005703555151:role/Dockerrunner',
        'executionRoleArn': 'arn:aws:iam::005703555151:role/Dockerrunner'
    },
    count=1,
    startedBy='lambda')
    # msg = {"key":"new_mugcjob", "pdbinfo": pdburl_arg+" "+pdbid_arg}
    # invoke_response = lambda_client.invoke(FunctionName="MugcJava",
    #                                       InvocationType='Event',
    #                                       Payload=json.dumps(msg))
    print(invoke_response)
    
def getInputFileUrl(s3Key):
    #creates the presigned-url
    key = s3Key
    url = s3.generate_presigned_url(
        ClientMethod='get_object',
        Params={
            'Bucket': 'mugctest',
            'Key': key
        }
    )
    print (url)
    return (url)
def shortenURL(url_arg):
    URL = url_arg
    response = urllib.request.urlopen("http://tinyurl.com/api-create.php?url=" + URL)
    r = str(response.read().decode())
    short_url_secure = r.split(":")[0]+"s:"+r.split(":")[1]
    print(short_url_secure)
    return(short_url_secure)