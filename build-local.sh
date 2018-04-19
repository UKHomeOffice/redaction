#!/bin/bash

git pull; 
mvn -T1.0C -Dmaven.test.skip=true -DskipTests=true clean install; 
cp */target/pontus*.jar */target/releases/*zip ../pontus-dist/pontus/common 
