class InputForm(Form):
    pdbID = StringField('pdbID', validators=[Required()])
    pdbFile = FileField(validators=[Optional()])
    entryType = RadioField('entryType', choices=[('X-ray'),('NMR'), ('Other')])
    email = StringField('email', validators=[Email()])
    submit = SubmitField('Submit')

class EmailForm(Form):
    email = StringField('email', validators=[Email()])
    password = StringField('password', validators=[Email()])
    submit = SubmitField('Submit')
