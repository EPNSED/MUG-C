from flask import Flask, flash, render_template, request
import subprocess
from subprocess import Popen, PIPE
app = Flask(__name__)


@app.route('/')
def add():
    return render_template('MUGC_UI.html')

if __name__ == '__main__':
    app.run(debug = True)