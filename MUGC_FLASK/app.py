from flask import Flask, flash, render_template, request, url_for
import subprocess
from subprocess import Popen, PIPE
app = Flask(__name__)


@app.route('/')
def mugc_home():
    return render_template('MUGC_UI.html')

if __name__ == '__main__':
    app.run(debug = True)