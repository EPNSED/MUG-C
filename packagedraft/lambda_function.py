
import json
import urllib.parse
import boto3
from MUG import MUG

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
    resultKey_arg = key
    print("This is the key for the results: "+resultKey_arg)
    resultAuthValidator(bucket, resultKey_arg)
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
            mugin_url_arg = shortenURL(url_arg)
        else:
            mugin_url_arg = pdbURL_arg
        if verify_email(email_arg) == True:#check if the email of the user is verified
            send_receipt(email_arg, email_arg)#sent notication job has started
            bucketname = bucket
            fileObj = s3.get_object(Bucket=bucketname, Key=s3key_arg)
            file_content = fileObj["Body"].read().decode('utf-8')
            MUG.runPrediction(file_content, pdbID_noextention, sessionID_arg, 'predictionResults', bucketname, metal = 'CA')
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
    SUBJECT = "MUGC: Processing ordered PDB file"
    
    # The email body for recipients with non-HTML email clients.
    BODY_TEXT = ("Amazon SES Test (Python)\r\n"
                 "This email was sent with Amazon SES using the "
                 "AWS SDK for Python (Boto)."
                )
                
    # The HTML body of the email.
    BODY_HTML = """<html>
    <head></head>
    <body>
      <h1>MUGC: Processing ordered</h1>
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

def MugcDockerInit(pdburl_arg, pdbid_arg, sessionid_arg):
    invoke_response = ecs.run_task(
    cluster='mugctestdocker',
    taskDefinition='mugcdocker',
    overrides={
        'containerOverrides': [
            {
                'name': 'mugcdocker',
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
    print(invoke_response)
#################################################################################################

#################################################################################################

    
def SendMugcDockerOutput(s3key_arg, email_arg):
    devUrl = 'https://101rrgsi71.execute-api.us-east-1.amazonaws.com/dev/display?pdbUrl='
    key = s3key_arg.split("/")[0]
    pdbID_arg = s3key_arg.split("/")[1].split("_")[0]
    print(pdbID_arg)
    pdbSite = "https://s3.amazonaws.com/mugctest/"+key+"/"+pdbID_arg+"_site.pdb"
    txtSite = "https://s3.amazonaws.com/mugctest/"+key+"/"+pdbID_arg+"_site.txt"
    # Replace sender@example.com with your "From" address.
    # This address must be verified with Amazon SES.
    SENDER = email_arg
    
    # Replace recipient@example.com with a "To" address. If your account 
    # is still in the sandbox, this address must be verified.
    RECIPIENT = email_arg
    
    # Specify a configuration set. If you do not want to use a configuration
    # set, comment the following variable, and the 
    # ConfigurationSetName=CONFIGURATION_SET argument below.
    #CONFIGURATION_SET = "ConfigSet"
    
    # If necessary, replace us-west-2 with the AWS Region you're using for Amazon SES.
    AWS_REGION = "us-east-1"
    
    # The subject line for the email.
    SUBJECT = "MUG(C) Results: "+pdbID_arg
    
    # The email body for recipients with non-HTML email clients.
    BODY_TEXT = ("MUG(C) Results\r\n"
                 "This email contains the links to your files "
                 "PDB Site file:"
                 "PDB Site text file:"
                )
                
    # The HTML body of the email.
    BODY_HTML = """<html>
    <head></head>
    <body>
      <h1>MUG(C) Results: """+pdbID_arg+"""</h1>
      <p>This email contains links to the MUG(C) outputs:
        <a href='"""+pdbSite+"""'>PDB Site</a> using the
        <a href='"""+txtSite+"""'>
         PDB Site text file</a>.</p>
        <p> Your PDB file has been sucessfully processed.</p>
        <a href='"""+devUrl+pdbSite+"""'>
         View PDB Site on MUGC-Viewer</a>.</p>
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

def resultAuthValidator(bucket, resultKey_arg):
    if "site" in resultKey_arg:
        email_arg = None
        wasSent = None
        keyForEmail_arg = getKeyForResultEmail(bucket, resultKey_arg)
        print("This is the key for email arg: "+keyForEmail_arg)
        try:
            if "site" in resultKey_arg:
                responseForEmail = s3.get_object(Bucket=bucket, Key=keyForEmail_arg)
                resultEmail_arg = responseForEmail['Metadata']['useremail']
                email_arg = resultEmail_arg
                writer = s3.head_object(Bucket = bucket, Key = resultKey_arg)
                newMeta = writer["Metadata"]
                newMeta["wasSent"] = "True"
                s3.copy_object(Bucket = bucket, Key = resultKey_arg, CopySource = bucket + '/' + resultKey_arg, Metadata = newMeta, MetadataDirective='REPLACE')
                if wasSent != "True" and email_arg != None:
                    SendMugcDockerOutput(resultKey_arg, email_arg)
        except Exception as e:
            print(e)
            print('Error getting object {} from bucket {}. Make sure they exist and your bucket is in the same region as this function.'.format(keyForEmail_arg, bucket))

def getKeyForResultEmail(bucket, currKey):
    finalKey = None
    prefix = currKey.split("/")[0]
    response = s3.list_objects(Bucket=bucket, Prefix=prefix, Delimiter='')
    strResponse = str(response['Contents'])
    print(strResponse)
    if 'pdb.txt' in strResponse:
        finalKey = currKey.split("/")[0]+"/"+"pdb.txt"
    else:
        finalKey = currKey.split("/")[0]+"/"+currKey.split("/")[1].split("_")[0]+".pdb"
    print(finalKey)
    return(finalKey)
    
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
    
#################################################################################################

#################################################################################################

def write2S3(file_name_arg, data_arg, folder_path_arg, typefolder_arg):
    string = data_arg
    
    file_name = file_name_arg
    lambda_path = "/tmp/" + file_name
    s3_path = folder_path_arg + "/"+ typefolder_arg + "/" + file_name
    
    with open(lambda_path, 'w+') as file:
        file.write(string)
        file.close()
    
    s3 = boto3.resource('s3')
    s3.meta.client.upload_file(lambda_path, 's3bucket', s3_path)
    
def readS3File(file_name_arg):
    bucketname = ''
    filename = file_name_arg
    print("Filename and : ", filename)
    fileObj = s3.get_object(Bucket=bucketname, Key=filename)
    file_content = fileObj["Body"].read().decode('utf-8')
    print(file_content)