from flask import Flask, flash, render_template, request, url_for
from wtforms import Form, StringField, SubmitField, RadioField
from flask_wtf.file import FileField, FileRequired
from wtforms.validators import Required, Optional, Email
import boto3
import uuid
# import subprocess
# from subprocess import Popen, PIPE

app = Flask(__name__)
app.config['SECRET_KEY'] = 'hard to guess string'


class InputForm(Form):
    pdbID = StringField('pdbID', validators=[Required()])
    pdbFile = FileField(validators=[Optional()])
    entryType = RadioField('entryType', choices=[('X-ray'),('NMR'), ('Other')])
    email = StringField('email', validators=[Email()])
    submit = SubmitField('Submit')

@app.route('/')
def mugc_home():
    return render_template('MUGC_UI.html')

@app.route('/',methods = ['POST', 'GET'])
def inputData():
   form = InputForm(request.form)
   print form.errors
   if request.method == 'POST':
      data = request.form
      # get email, session Id and store it in User-Defined Metadata
      sessionID = uuid.uuid1()
      pdbID = data['pdbID']
      # create the handler method for storing the pdb file in s3

      print data 
      return 'Celery worker started'

if __name__ == '__main__':
    app.run(debug = True)