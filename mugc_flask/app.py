from flask import Flask, flash, render_template, request, url_for, redirect, Blueprint
from flask_login import login_user, logout_user, login_required, LoginManager, UserMixin
from wtforms import Form, StringField, SubmitField, RadioField
from flask_wtf.file import FileField, FileRequired
from wtforms.validators import Required, Optional, Email
from boto3.dynamodb.conditions import Key, Attr
from flywheel import Model, Field
import datetime
from datetime import datetime
from werkzeug import secure_filename
import urllib.request
from flask_bcrypt import Bcrypt
import boto3
import uuid
import os
import io
# import subprocess
# from subprocess import Popen, PIPE
from core.forms.user import LoginForm, RegisterForm
from tables import Results

app = Flask(__name__)
app.config.from_object("config")
login_manager = LoginManager()
login_manager.init_app(app)
bcrypt = Bcrypt(app)


s3 = boto3.resource('s3')
dynamodb = boto3.resource('dynamodb')
Table = dynamodb.Table('users_test')
       

class User(UserMixin):
   @login_manager.user_loader
   def user_loader(email):
       response = Table.scan(FilterExpression=Attr('email').eq(email))
       if email not in response['Items']:
           print(email)
           user = User()
           user.id = email
           return

       return user


   @login_manager.request_loader
   def request_loader(request):
       
       email = request.form.get('email')
       password = request.form.get('password')
       response = Table.scan(FilterExpression=Attr('email').eq(email))
       print(response['Items'])
       users = response['Items']
       if email not in users:
           print(email)
       return

       user = User()
       user.id = email

       # DO NOT ever store passwords in plaintext and always compare password
       # hashes using constant-time comparison!
       hashed=response['Items'][0]['info']['password'].encode('utf-8')
       user.is_authenticated = hashed == bcrypt.hashpw(password, hashed)

       return user


# Intial Get pdb file from API if pdbid and no file then get the file with request(http) 
#else upload file from user input
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


################
#### routes ####
################

@app.route('/register', methods=['GET','POST'])
def register():
    if request.method == 'POST':
        # get User-Defined Metadata
        email = request.form['email']
        password = bcrypt.generate_password_hash(request.form['password']).decode('UTF-8')
        Table.put_item(
            Item ={ 
                'email': email,
                'password': password
        })
        #login_user(user)

        flash('Thank you for registering.', 'success')
        return redirect(url_for("login"))

    return render_template('user/register.html') #form=form)

@app.route('/', methods=['GET', 'POST'])
@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        # get User-Defined Metadata
        email = request.form['email']
        password = request.form['password']
        response = Table.get_item(
        Key={
            'email': email
        })
        db = response['Item']
        print(db)
        #user = User(db, password)
        user = User()
        user.id =email
        response=redirect("/")
        response.set_cookie('YourSessionCookie', user.id)
        login_user(user)
        if user.is_authenticated:
            # login_user(user)
            print("Working login, you are logged in")
            flash('You are logged in. Welcome!', 'success')
            return redirect(url_for('mugc_home'))
        else:
            flash('Invalid email and/or password.', 'danger')
            return render_template('user/login.html')
    return render_template('user/login.html', title='Please Login')

@app.route('/logout')
@login_required
def logout():
    logout_user()
    flash('You were logged out. Bye!', 'success')
    return redirect(url_for('login'))

@app.route('/log')
@login_required
def activitylog():
    #Get the table data and display it
    # display results
    
    table = Results(results)
    table.border = True
    return render_template('results.html', table=table)

@app.route('/display')
def display():
    #Get the url or file and display it
    # display molecule
    return render_template('index.html')

@app.route('/mugc')
def mugc_home():
    return render_template('mugc.html')

@app.route('/mugc',methods = ['POST', 'GET'])
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
      entryType = data['entryType']
      userEmail = data['email']
      pdbUrl = 'https://files.rcsb.org/download/'+pdbID
      print (userEmail)
      #confirming User inputs
      confirm_message = None
      if pdbID: 
        # create the handler method for storing the pdb file in s3 and Add metadata to s3 object: pdbFile
        pdbData = getPDB(pdbID,'')
        s3key = getS3Key(pdbID,'')
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
          # create the handler method for storing the pdb file in s3 and Add metadata to s3 object: pdbFile
        pdbFile = request.files['pdbFile']
        s3key = getS3Key(pdbID,pdbFile.filename)
        s3.Bucket('mugctest').put_object(Key=s3key, Body=pdbFile, Metadata={
            'sessionID': str(sessionID),
            's3key': str(s3key),
            'pdbID': str(pdbFile.filename),
            'entryType': str(entryType),
            'userEmail': str(userEmail),
            'pdbUrl': str(pdbUrl)
            })
        confirm_message = 'Job submitted to MUG(C) succesfully, Check your Email!'
      print (data)
      return render_template('MUGC_Conformation.html', conformationmessage = confirm_message, 
      sessionID = sessionID, pdbID = getPDBID(pdbID, pdbFile), entryType = entryType, 
      userEmail = userEmail)
if __name__ == '__main__':
    app.run(debug = True)