#!/usr/bin/env python
import os

S3_BUCKET = os.environ.get("mugc-data")
S3_KEY = os.environ.get("S3_ACCESS_KEY")
S3_SECRET = os.environ.get("S3_SECRET_ACCESS_KEY")
S3_LOCATION = 'http://{}.s3.amazonaws.com/'.format(S3_BUCKET)
UPLOAD_FOLDER = "/tmp/"

dev_info = {'S3_BUCKET ': 'mugc-data',
            'TABLE_ID': 'root'}

SECRET_KEY = os.urandom(37)
DEBUG = True