from flask import Flask, flash, render_template, request, url_for
from wtforms import Form, StringField, SubmitField, RadioField
from flask_wtf.file import FileField, FileRequired
from wtforms.validators import Required, Optional, Email
from werkzeug import secure_filename
import boto3
import uuid
import os
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
    if pID != None and pFile == None:
        pdb_url = 'https://files.rcsb.org/download/'+pID
        resultFile = pdb_url # or request(pdb_url)
        return resultFile
    else:
        resultFile = app.config['UPLOAD_FOLDER'] + pFile
        return resultFile

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
      sessionID = uuid.uuid4()
      pdbID = data['pdbID']
      pdbFile = data['pdbFile']
      entryType = data['entryType']
      userEmail = data['email']
      print data 
      # create the handler method for storing the pdb file in s3
      s3.meta.client.upload_file(getPDB(pdbID,pdbFile), 'mugctest', userEmail)
      # Add metadata to s3 object: pdbFile
      s3.Object('mugctest', userEmail).put(Metadata={
        'sessionID': sessionID,
        'pdbID': pdbID,
        'entryType': entryType,
        'userEmail': userEmail
        })
      print data 
      return 'Create AWS API Gate way event handlers'

if __name__ == '__main__':
    app.run(debug = True)