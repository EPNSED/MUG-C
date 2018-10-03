from flask import Flask, flash, render_template, request, url_for
from wtforms import Form, StringField, SubmitField, RadioField
from flask_wtf.file import FileField, FileRequired
from wtforms.validators import Required, Optional, Email
from werkzeug import secure_filename
import requests
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

# Get pdb file from API if pdbid and no file then get the file with request(http) else upload file from user input
def getPDB(pID,pFile):
    resultFile = None
    if pID:
        pdb_url = 'https://files.rcsb.org/download/'+pID
        resultFile = requests.get(pdb_url) # or the url
        return resultFile
    else:
        resultFile = app.config['UPLOAD_FOLDER'] + pFile
        return resultFile
def getS3Key(pID,pFile):
    key = None
    if pID != None and pFile == None:
        key = pID
        return key
    else:
        key = pFile
        return key

@app.route('/')
def mugc_home():
    return render_template('MUGC_UI.html')

@app.route('/',methods = ['POST', 'GET'])
def inputData():
   form = InputForm(request.form)
   print form.errors
   if request.method == 'POST':
      #requests
      data = request.form
      #Parameter variables
      pdbID = None
      pdbFile = None
      entryType = None
      userEmail = None
      # get User-Defined Metadata 
      sessionID = str(uuid.uuid4()).encode()
      pdbID = data['pdbID']
      pdbFile = data['pdbFile']
      entryType = data['entryType']
      userEmail = data['email'] 
      # create the handler method for storing the pdb file in s3 and Add metadata to s3 object: pdbFile
      pdbData = open(getPDB(pdbID,pdbFile), 'rb')
      s3.Bucket('mugctest').put_object(Key=getS3Key(pdbID,pdbFile), Body=getPDB(pdbID,pdbFile), Metadata={
        'sessionID': sessionID,
        'pdbID': pdbID,
        'entryType': entryType,
        'userEmail': userEmail
        })
      print data 
      return 'Create AWS API Gate way event handlers'

if __name__ == '__main__':
    app.run(debug = True)