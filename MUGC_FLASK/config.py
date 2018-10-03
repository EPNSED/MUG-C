import os

S3_BUCKET = os.environ.get("mugctest")
S3_KEY = os.environ.get("S3_ACCESS_KEY")
S3_SECRET = os.environ.get("S3_SECRET_ACCESS_KEY")
S3_LOCATION = 'http://{}.s3.amazonaws.com/'.format(S3_BUCKET)
UPLOAD_FOLDER = "/home/Mashiku/Downloads/" #change to /tmp/ later

SECRET_KEY = os.urandom(37)
DEBUG = True