#!/bin/sh

echo "Mugc processing starting ....."

echo $1 

echo $2

echo $3

#echo $PATH

cd code/mugcmaven
java -cp /code/code/ mugcmaven/Handler $1 $2
wait
java -cp /code/code/mugcmaven src/MUGC
wait

echo "Processing complete, uploading prediction results"


aws configure set aws_access_key_id AKIAJ2ZEPSYLQ3UYKHQQ
aws configure set aws_secret_access_key 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
aws configure set default.region us-east-1

aws s3 cp /code/code/mugcmaven/predictionResults s3://mugctest/$3 --recursive \
    --exclude "*" --include "*.txt" --include "*.pdb" --include "*.dat"

echo "Done"
