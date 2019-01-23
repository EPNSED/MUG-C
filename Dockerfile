FROM openjdk:8-jre-alpine

RUN apk --update add python py-pip openssl ca-certificates
RUN apk --update add --virtual build-dependencies python-dev build-base wget \
  && pip install --upgrade pip \
  && pip install boto3 \
  && pip install awscli \
  && apk del build-dependencies 

ADD . /code

ADD . /code/mugcmaven

ADD . /code/mugcmaven/inputdata

ADD . /code/mugcmaven/outputdata

ADD . /code/mugcmaven/predictionResults

ADD . /code/mugcmaven/src

WORKDIR /code

ADD . /code/mugcmaven/Handler.class

COPY ./code/mugcmaven/Handler.class /

COPY ./code/mugcmaven/Handler.java /

COPY ./code/entrypoint.sh /

RUN chmod 777 /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

CMD [] 
