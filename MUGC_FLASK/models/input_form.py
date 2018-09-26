from flask.ext.wtf import Form
from wtforms import StringField, SubmitField, RadioField
from flask_wtf.file import FileField, FileRequired
from wtforms.validators import Required, Optional, Email

class InputForm(Form):
    pdbID = StringField('pdbID', validators=[Required()])
    pdbFile = FileField(validators=[Optional()))
    entryType = RadioField('entryType', choices=[('entry_type_X-ray'),('entry_type_NMR'), ('entry_type_Other')])
    email = StringField('email', validators=[Email()])
    submit = SubmitField('Submit')