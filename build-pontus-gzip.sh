#!/bin/bash
git pull; 
mvn -T1.0C -Dmaven.test.skip=true -DskipTests=true clean install; 
mkdir pontus

cp  */target/pontus*.jar */target/releases/*zip  pontus
tar cpvfz pontus.tar.gz pontus

scp pontus.tar.gz 10.227.252.41:
ssh -t -t 10.227.252.41 sudo cp ~centos/pontus.tar.gz /var/www/html/download
ssh -t -t 10.227.252.41 sudo chown -R lighttpd:lighttpd /var/www/html/download
