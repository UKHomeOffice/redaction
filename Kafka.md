Kafka kerberization

login as root and run the following commands:

```
cd /opt
wget 'http://www.mirrorservice.org/sites/ftp.apache.org/kafka/0.10.1.0/kafka_2.11-0.10.1.0.tgz'
tar xvfz kafka_2.11-0.10.1.0.tgz
ln -s kafka_2.11-0.10.1.0 kafka
chown -R kafka:kafka kafka_2.11-0.10.1.0
```
Edit /opt/kafka/config/server.properties:

```
listeners=SSL://:9093,SASL_SSL://:9094
security.inter.broker.protocol=SSL
ssl.client.auth=required
ssl.keystore.location=/opt/kafka/security/kafka.server.keystore.jks
ssl.keystore.password=pa55word
ssl.key.password=pa55word
ssl.truststore.location=/opt/kafka/security/kafka.server.truststore.jks
ssl.truststore.password=pa55word
sasl.kerberos.service.name=kafka
zookeeper.set.acl=true
#authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer
#super.users=User:CN=kafka.sandbox.hortonworks.com,OU=Dev,O=Pontus,L=London,ST=Unknown,C=UK

```
Run the following:
```
su - kafka
mkdir /opt/kafka/security

```
Create and execute the following file: 
/opt/kafka/security/create-keystores.sh
```
#!/bin/bash
PASSWORD=pa55word
VALIDITY=36500
keytool -keystore kafka.server.keystore.jks -alias localhost -validity $VALIDITY -genkey
openssl req -new -x509 -keyout ca-key -out ca-cert -days $VALIDITY
keytool -keystore kafka.server.truststore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.client.truststore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.server.keystore.jks -alias localhost -certreq -file cert-file
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days $VALIDITY -CAcreateserial -passin pass:$PASSWORD
keytool -keystore kafka.server.keystore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.server.keystore.jks -alias localhost -import -file cert-signed
keytool -keystore kafka.client.keystore.jks -alias localhost -validity $VALIDITY -genkey
keytool -keystore kafka.client.keystore.jks -alias localhost -certreq -file cert-file
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days $VALIDITY -CAcreateserial -passin pass:$PASSWORD
keytool -keystore kafka.client.keystore.jks -alias CARoot -import -file ca-cert
keytool -keystore kafka.client.keystore.jks -alias localhost -import -file cert-signed

```

Create the following file:
[root@sandbox security]# cat jaas_server.conf
```
KafkaServer {
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   useTicketCache=true
   renewTicket=true
   storeKey=true
   keyTab="/etc/security/keytabs/kafka.service.keytab"
   principal="kafka/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE";
};
Client {
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   useTicketCache=true
   renewTicket=true
   storeKey=true
   keyTab="/etc/security/keytabs/kafka.service.keytab"
   principal="kafka/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE";
};
hdfs {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  storeKey=true
  useTicketCache=false
  keyTab="/etc/security/keytabs/hdfs.headless.keytab"
  principal="hdfs-sandbox@YOUR_REALM_GOES_HERE";

};
KafkaClient {
   com.sun.security.auth.module.Krb5LoginModule required
   useTicketCache=true
   renewTicket=true
   serviceName="kafka";
};
```


Create the following file: [root@sandbox security]# cat jaas_policy_store.conf
```
Client {
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   useTicketCache=true
   renewTicket=true
   keyTab="/etc/security/keytabs/hbase.service.keytab"
   serviceName="zookeeper"
   principal="hbase/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE";
};
KafkaServer {
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   useTicketCache=true
   renewTicket=true
   storeKey=true
   keyTab="/etc/security/keytabs/kafka.service.keytab"
   principal="kafka/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE";
};
hdfs {
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   storeKey=true
   renewTicket=true
   useTicketCache=true
   keyTab="/etc/security/keytabs/hdfs.headless.keytab"
   principal="hdfs-sandbox@YOUR_REALM_GOES_HERE";

};
KafkaClient {
   com.sun.security.auth.module.Krb5LoginModule required
   useTicketCache=true
   useKeyTab=true
   renewTicket=true
   serviceName="kafka"
   keyTab="/etc/security/keytabs/hbase.service.keytab"
   principal="hbase/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE";
};

```

Add the following KAFKA_OPTS to the kafka-server-start.sh file, so the file /opt/kafka/bin/kafka-server-start.sh looks like the following:
```
#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


export KAFKA_OPTS='-Djava.security.auth.login.config=/opt/kafka/security/jaas_server.conf -Dsun.security.krb5.debug=true'

if [ $# -lt 1 ];
then
        echo "USAGE: $0 [-daemon] server.properties [--override property=value]*"
        exit 1
fi
base_dir=$(dirname $0)

if [ "x$KAFKA_LOG4J_OPTS" = "x" ]; then
    export KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:$base_dir/../config/log4j.properties"
fi

if [ "x$KAFKA_HEAP_OPTS" = "x" ]; then
    export KAFKA_HEAP_OPTS="-Xmx1G -Xms1G"
fi

EXTRA_ARGS=${EXTRA_ARGS-'-name kafkaServer -loggc'}

COMMAND=$1
case $COMMAND in
  -daemon)
    EXTRA_ARGS="-daemon "$EXTRA_ARGS
    shift
    ;;
  *)
    ;;
esac

exec $base_dir/kafka-run-class.sh $EXTRA_ARGS kafka.Kafka "$@"


```

Edit  /home/solr/solr_jaas.conf so it looks like this:
```
Client {
       com.sun.security.auth.module.Krb5LoginModule required
       refreshKrb5Config=true
       useTicketCache=true
       renewTGT=true
       doNotPrompt=true
       useKeyTab=true
       keyTab="/etc/security/keytabs/spnego.service.keytab"
       storeKey=true
       debug=true
       isInitiator=true
       principal="HTTP/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE";

};
KafkaClient {
   com.sun.security.auth.module.Krb5LoginModule required
   refreshKrb5Config=true
   useTicketCache=true
   renewTGT=true
   doNotPrompt=true
   useKeyTab=true
   keyTab="/etc/security/keytabs/spnego.service.keytab"
   storeKey=true
   debug=true
   isInitiator=true
   principal="HTTP/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE"
   serviceName="kafka";
};

```




Run the Secure broker: 

```
/opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties

```

Start the HDFSNotifier for auto updates when a policy has changed:
```
java -cp /opt/pontus/pontus-redaction-hbase-coproc-0.0.1-SNAPSHOT.jar -Djava.security.auth.login.config=/opt/kafka/security/jaas_policy_store.conf -Dhadoop.security.authentication=kerberos -Dhadoop.security.authorization=true -Dlog4j.configuration=file:///opt/pontus/log4j.properties -Dbootstrap.servers=sandbox.hortonworks.com:9094  HDFSNotifier

```

****