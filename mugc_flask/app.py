from flask import Flask, flash, render_template, request, url_for
from wtforms import Form, StringField, SubmitField, RadioField
from flask_wtf.file import FileField, FileRequired
from wtforms.validators import Required, Optional, Email
from werkzeug import secure_filename
import urllib.request
import boto3
import uuid
import os
import io
# import subprocess
# from subprocess import Popen, PIPE

app = Flask(__name__)
app.config.from_object("config")

s3 = boto3.resource('s3')

class InputForm(Form):
    pdbID = StringField('pdbID', validators=[Required()])
    pdbFile = FileField(validators=[Optional()])
    entryType = RadioField('entryType', choices=[('X-ray'),('NMR'), ('Other')])
    email = StringField('email', validators=[Email()])
    submit = SubmitField('Submit')

# Intial Get pdb file from API if pdbid and no file then get the file with request(http) else upload file from user input
# If pdbID then create a txt file with the url to the file and attach meta data to it
def getPDB(pID,pFile):
    resultFile = None
    if pID:
        pdb_url = 'https://files.rcsb.org/download/'+pID
        path = app.config['UPLOAD_FOLDER'] + 'pdb.txt'
        #create and write pdb.txt
        file = open(path,'w')
        file.write(pdb_url)
        file.close() 
        resultFile =  path  # path to pdb.txt
        print (path)
        return pdb_url
    else:
        #resultFile = open(app.config['UPLOAD_FOLDER'] + pFile, 'rb')
        #resultFile = app.config['UPLOAD_FOLDER'] + pFile
        resultFile = pFile
        print(resultFile)
        return resultFile
def getS3Key(pID,pFile):
    key = None
    if pID:
        key = str(uuid.uuid4()) +'/'+ 'pdb.txt'
        return key
    else:
        key = str(uuid.uuid4())+'/'+ pFile
        return key
#get pdb Id for front end
def getPDBID(pID,pFile):
    Id = None
    if pID:
        Id = pID
        return Id
    else:
        Id = pFile
        return Id

def checkpdbType(pID,pFile,pType):
    pdbType = None
    if pID:
        #get the xml response then get the pdb's type
        var_id = pID.split('.pdb')
        urlptype = "https://www.rcsb.org/pdb/rest/describePDB?structureId="+var_id[0]
        print(urlptype)
        response = urllib.request.urlopen(urlptype)
        xml_str = str(response.read())
        print(xml_str)
        if (xml_str.find('X-RAY') != -1): 
            print ("Contains X-RAY type ")
        if (xml_str.find('NMR') != -1):
            print ("Contains NMR")
        else: 
            print ("Other type")
    else:
        #get the xml response then get the pdb's type
        var_id = pFile.split('.pdb')
        urlptype = "https://www.rcsb.org/pdb/rest/describePDB?structureId="+var_id[0]
        print(urlptype)
        response = urllib.request.urlopen(urlptype)
        xml_str = str(response.read())
        print(xml_str)
        if (xml_str.find('X-RAY') != -1): 
            print ("Contains X-RAY type ")
        if (xml_str.find('NMR') != -1):
            print ("Contains NMR")
        else: 
            print ("Other type")
    return pdbType

def checkpdbValid(pFile):
    urlptype = "https://www.rcsb.org/pdb/rest/describePDB?structureId="+pFile
    print(urlptype)
    response = urllib.request.urlopen(urlptype)
    pdb = str(response.read()).find('structureId')
    print(pdb)
    if pdb != -1:
        return True
    else:
        return False

@app.route('/')
def mugc_home():
    return render_template('MUGC_UI.html')

@app.route('/',methods = ['POST', 'GET'])
def inputData():
   form = InputForm(request.form)
   print (form.errors)
   if request.method == 'POST':
      #requests
      data = request.form
      #Parameter variables
      pdbID = None
      pdbFile = None
      entryType = None
      userEmail = None
      pdbUrl = None
      # get User-Defined Metadata 
      sessionID = str(uuid.uuid4()).encode()
      pdbID = data['pdbID']
      pdbFile = request.files['pdbFile']
      entryType = data['entryType']
      userEmail = data['email']
      pdbUrl = 'https://files.rcsb.org/download/'+pdbID
      print (userEmail)
      #confirming User inputs
      confirm_message = None
      #checkpdbType(pdbID,pdbFile,entryType)
      valid_pdbfile = checkpdbValid(pdbFile.filename)
      print (valid_pdbfile)
      if valid_pdbfile or pdbID !='': 
        # create the handler method for storing the pdb file in s3 and Add metadata to s3 object: pdbFile
        pdbData = getPDB(pdbID,pdbFile)
        s3key = getS3Key(pdbID,pdbFile.filename)
        s3.Bucket('mugctest').put_object(Key=s3key, Body=pdbData, Metadata={
            'sessionID': str(sessionID),
            's3key': str(s3key),
            'pdbID': str(pdbID),
            'entryType': str(entryType),
            'userEmail': str(userEmail),
            'pdbUrl': str(pdbUrl)
            })
        confirm_message = 'Job submitted to MUG(C) succesfully, Check your Email!'
      else:
        confirm_message = 'Invalid PDB ID: Please Input the proper PDB file format e.g 1b8c.pdb'
      print (data)
      return render_template('MUGC_Conformation.html', conformationmessage = confirm_message, sessionID = sessionID, pdbID = getPDBID(pdbID, pdbFile), entryType = entryType, userEmail = userEmail)
if __name__ == '__main__':
    app.run(debug = True)