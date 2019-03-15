from wtforms import Form, StringField, SubmitField, RadioField
from flask_wtf.file import FileField, FileRequired
from wtforms.validators import Required, Optional, Email

class LoginForm(Form):
    email = StringField('email', validators=[Email()])
    password = StringField('password', validators=[Email()])
    submit = SubmitField('Submit')

class RegisterForm(Form):
    email = StringField('email', validators=[Email()])
    password = StringField('password', validators=[Email()])
    submit = SubmitField('Submit')
