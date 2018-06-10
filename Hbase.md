# Kerberization and Preparation of redaction on Hbase

0) Run the following to get credentials:
```
kinit -k -t /etc/security/keytabs/hbase.service.keytab  hbase/sandbox.hortonworks.com
```


Then follow the tutorials here:
http://hortonworks.com/hadoop-tutorial/introduction-apache-hbase-concepts-apache-phoenix-new-backup-restore-utility-hbase/#section_1

... and here:

http://hortonworks.com/hadoop-tutorial/introduction-apache-hbase-concepts-apache-phoenix-new-backup-restore-utility-hbase/#section_2



1) create phoenix table, by running the following commands inside the phoenix CLI:
```
create table "driver_dangerous_event" ("row" VARCHAR primary key,"events"."driverId" VARCHAR,"events"."driverName" VARCHAR,
"events"."eventTime" VARCHAR,"events"."eventType" VARCHAR,"events"."latitudeColumn" VARCHAR,
"events"."longitudeColumn" VARCHAR,"events"."routeId" VARCHAR,"events"."routeName" VARCHAR,
"events"."truckId" VARCHAR);
```
2) copy redaction jar files in /opt/pontus, and into hdfs:
```
hadoop fs -copyFromLocal /opt/pontus/pontus-redaction-hbase-coproc-0.99.0.jar /apps/hbase
```

3) create the redaction rules files and directories:
```
hadoop fs -mkdir /apps/hbase/data/acls
hadoop fs -mkdir /apps/hbase/data/acls/read
hadoop fs -mkdir /apps/hbase/data/acls/read/uk.manager
hadoop fs -mkdir /apps/hbase/data/acls/read/superuser
hadoop fs -mkdir /apps/hbase/data/acls/read/uk.manager/special
hadoop fs -mkdir /apps/hbase/data/acls/read/uk.manager/special/operation_unicorn

printf '.*' > metadata
printf '(?!x)x' > redaction_denied_all
printf '(?!x)x' > redaction_denied
printf '.*' > redaction_allowed
hadoop fs -copyFromLocal redaction_* /apps/hbase/data/acls/read/superuser
hadoop fs -copyFromLocal metadata /apps/hbase/data/acls/read/superuser

printf 'sssshhhh' > redaction_denied_all
printf 'denied' > redaction_denied
printf '.*' > redaction_allowed
printf 'uk.manager/special.*' > metadata
hadoop fs -copyFromLocal redaction_* /apps/hbase/data/acls/read/uk.manager/special
hadoop fs -copyFromLocal metadata /apps/hbase/data/acls/read/uk.manager/special
```


4) In the habase shell, set the redaction layer before the other attributes:
```
hbase(main):008:0> describe 'driver_dangerous_event'
Table driver_dangerous_event is ENABLED
driver_dangerous_event, {TABLE_ATTRIBUTES => {coprocessor$1 => '|org.apache.phoenix.coprocessor.ScanRegionObserver|805306366|', coprocessor$2 => '|org.apache.phoenix.coprocessor.UngroupedAggregateRegionObserver|805306366|', coprocessor$3 => '|org.apache.phoenix.coprocessor.GroupedAggregateRegionObserver|805306366|', coprocessor$4 =>'|org.apache.phoenix.coprocessor.ServerCachingEndpointImpl|805306366|', coprocessor$5 =>'|org.apache.phoenix.hbase.index.Indexer|805306366|org.apache.hadoop.hbase.index.codec.class=org.apache.phoenix.index.PhoenixIndexCodec,index.builder=org.apache.phoenix.index.PhoenixIndexBuilder'}
COLUMN FAMILIES DESCRIPTION
{NAME => 'events', BLOOMFILTER => 'ROW', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => 'FOREVER', COMPRESSION => 'NONE', MIN_
VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}
1 row(s) in 0.0790 seconds

alter 'driver_dangerous_event', METHOD => 'table_att_unset', NAME => 'coprocessor$1'
alter 'driver_dangerous_event', METHOD => 'table_att_unset', NAME => 'coprocessor$2'
alter 'driver_dangerous_event', METHOD => 'table_att_unset', NAME => 'coprocessor$3'
alter 'driver_dangerous_event', METHOD => 'table_att_unset', NAME => 'coprocessor$4'
alter 'driver_dangerous_event', METHOD => 'table_att_unset', NAME => 'coprocessor$5'
alter 'driver_dangerous_event', 'coprocessor'=>'hdfs:///apps/hbase/pontus-redaction-hbase-coproc-0.99.0.jar|uk.gov.homeoffice.pontus.hbase.coprocessor.pole.security.PoleSecurityCoprocessorMetadataOnly|1001'
alter 'driver_dangerous_event', 'coprocessor'=>'|org.apache.phoenix.coprocessor.ScanRegionObserver|805306366|'
alter 'driver_dangerous_event', 'coprocessor'=>'|org.apache.phoenix.coprocessor.UngroupedAggregateRegionObserver|805306366|'
alter 'driver_dangerous_event', 'coprocessor'=>'|org.apache.phoenix.coprocessor.GroupedAggregateRegionObserver|805306366|'
alter 'driver_dangerous_event', 'coprocessor'=>'|org.apache.phoenix.coprocessor.ServerCachingEndpointImpl|805306366|'
alter 'driver_dangerous_event', 'coprocessor'=>'|org.apache.phoenix.hbase.index.Indexer|805306366|org.apache.hadoop.hbase.index.codec.class=org.apache.phoenix.index.PhoenixIndexCodec,index.builder=org.apache.phoenix.index.PhoenixIndexBuilder'
alter 'driver_dangerous_event', 'coprocessor'=>'hdfs:///apps/hbase/pontus-redaction-hbase-coproc-0.99.0.jar|uk.gov.homeoffice.pontus.hbase.coprocessor.pole.security.PoleSecurityCoprocessorRedactOnly|805306367'


```


5) Add JWT credentials:
```
java -cp /opt/pontus/pontus-redaction-hbase-coproc-0.99.0.jar JWTStoreClient hbase '{ "jwtClaim": "/uk.manager/special" }'
java -cp /opt/pontus/pontus-redaction-hbase-coproc-0.99.0.jar JWTStoreClient leo '{ "jwtClaim": "/uk.manager/special" }'

java -cp /opt/pontus/pontus-redaction-hbase-coproc-0.99.0.jar JWTStoreClient /jwt/users/leo/admin@YOUR_REALM_GOES_HERE '{ "jwtClaim": "/uk.manager/special" }'


```

6) Optionally, start the HDFSNotifier, which will send Kafka messages to the redaction layer whenever there is a change of the redaction rules in HDFS (NOTE, use this for testing only):

```
java -cp /opt/pontus/pontus-redaction-hbase-coproc-0.99.0.jar -Djava.security.auth.login.config=/opt/pontus/jaas_policy_store.conf  -Dhadoop.security.authentication=kerberos -Dhadoop.security.authorization=true  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=61888   HDFSNotifier

or 

Manually send a notification via Kafka:
java -Djava.security.auth.login.config=/opt/pontus/pontus-hbase/current/conf/hbase-kafka-jaas.conf -cp /opt/pontus/pontus-redaction/current/lib/pontus-redaction-hbase-coproc-0.0.1-SNAPSHOT.jar:opt/pontus/pontus-hbase/current/conf -Dpontus.redaction.zk=`hostname -f`:2181 -Dbootstrap.servers=`hostname -f`:9092 -Dssl.keystore.location=/etc/pki/java/localhost.jks -Dssl.keystore.password=pa55word -Dssl.truststore.location=/etc/pki/java/truststore.jks -Dssl.truststore.password=changeit  PolicyNotifier /tmp/hbase-root/hbase/acls/read/superuser/metadata

```

7) Back in the hbase cli, add a few metadata entries:
```
put 'driver_dangerous_event', '1', 'events:rmdq', 'uk.manager/special/operation-not-redacted'
put 'driver_dangerous_event', '2', 'events:rmdq', 'uk.manager/special/operation-not-redacted'
put 'driver_dangerous_event', '3', 'events:rmdq', 'uk.manager/special/operation-not-redacted'
put 'driver_dangerous_event', '4', 'events:rmdq', 'uk.manager/special/operation-not-redacted'

scan 'driver_dangerous_event'
```





