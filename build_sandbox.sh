#!/bin/bash
git pull; mvn -T1.0C -Dmaven.test.skip=true -DskipTests=true clean install; scp -P 2222 */target/pontus*.jar */target/releases/*zip  localhost:/opt/pontus;
