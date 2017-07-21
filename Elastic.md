# Elastic Search Installations

The Elastic Search redaction has a few permutations thanks to archaic incompatibilities between versions; here are the variables:

Versions:
- version 2.x
- version 5.x 

Authentication Modules:
- SearchGuard
- Shield

Thus, we need to produce 4 versions of the redaction layer.  
- Elastic Search 5.x using SearchGuard (implemented, except for issue #4)
- Elastic Search 2.x using SearchGuard (outstanding, see issue #5 )
- Elastic Search 5.x using Shield (outstanding, see issue #6 )
- Elastic Search 2.x using Shield (outstanding, see issue #7)

## Elastic Search 2.x using Shield

Unfortunately, Shield does not support Kerberos as a default realm.  Thus, we need to get an open source (Apache license) implementation as well.  That implementation has been bundled with the project.  Here are the steps to set it up:

### One-off installation from scratch

#### Get the redaction files to the docker image
Login as root to the host box  (the server that hosts the sandbox docker image), and run the following:
```
cd /root/redaction

# Whenever there's a change in the code, here's how to rebuild it:
git pull; mvn -Dmaven.test.skip=true clean install; scp -P 2222 */target/pontus*.jar */target/releases/ela*zip  localhost:/opt/pontus;

```
#### Install the binaries
Login as root to the sandbox docker image, and run the following:
```
sudo su -
useradd elastic  -G hadoop
cd /opt
wget https://download.elastic.co/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/2.4.3/elasticsearch-2.4.3.tar.gz
tar xvfz elasticsearch-2.4.3.tar.gz
mv elasticsearch-2.4.3 elasticsearch-2.4.3-shield
cd /opt/elasticsearch-2.4.3-shield
[root@sandbox elasticsearch-2.4.3-shield]# bin/plugin install license
-> Installing license...
Plugins directory [/opt/elasticsearch-2.4.3-shield/plugins] does not exist. Creating...
Trying https://download.elastic.co/elasticsearch/release/org/elasticsearch/plugin/license/2.4.3/license-2.4.3.zip ...
Downloading .......DONE
Verifying https://download.elastic.co/elasticsearch/release/org/elasticsearch/plugin/license/2.4.3/license-2.4.3.zip checksums if available ...
Downloading .DONE
Installed license into /opt/elasticsearch-2.4.3-shield/plugins/license
[root@sandbox elasticsearch-2.4.3-shield]# bin/plugin install shield
-> Installing shield...
Trying https://download.elastic.co/elasticsearch/release/org/elasticsearch/plugin/shield/2.4.3/shield-2.4.3.zip ...
Downloading ...............................................................................................................................................DONE
Verifying https://download.elastic.co/elasticsearch/release/org/elasticsearch/plugin/shield/2.4.3/shield-2.4.3.zip checksums if available ...
Downloading .DONE
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@     WARNING: plugin requires additional permissions     @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
* java.lang.RuntimePermission setFactory
See http://docs.oracle.com/javase/8/docs/technotes/guides/security/permissions.html
for descriptions of what these permissions allow and the associated risks.

Continue with installation? [y/N]y
Installed shield into /opt/elasticsearch-2.4.3-shield/plugins/shield

cd /opt

chown -R elastic:elastic elasticsearch-2.4.3-shield

su - elastic

cd /opt/elasticsearch-2.4.3-shield

bin/plugin install file:///opt/pontus/elasticsearch-shield-kerberos-realm-2.4.3.zip


tee /opt/elasticsearch-2.4.3-shield/config/elasticsearch.yml <<'EOF'
shield.authc.realms.cc-kerberos.type: cc-kerberos
shield.authc.realms.cc-kerberos.order: 0
shield.authc.realms.cc-kerberos.acceptor_keytab_path: /etc/security/keytabs/spnego.service.keytab
shield.authc.realms.cc-kerberos.acceptor_principal: HTTP/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE 
shield.authc.realms.cc-kerberos.strip_realm_from_principal: true
de.codecentric.realm.cc-kerberos.krb5.file_path: /etc/krb5.conf
de.codecentric.realm.cc-kerberos.krb_debug: true
security.manager.enabled: false  
shield.authc.realms.cc-kerberos.roles.users:
  - solr
  - leo/admin
shield.authc.realms.cc-kerberos.roles.admin:
  - solr
  - leo/admin
  
  
http.port: 9090
cluster.name: pontus
network.host: 172.17.0.2
EOF

printf '\nES_CLASSPATH="/opt/pontus/pontus-redaction-elastic-2.x-shield-0.0.1-SNAPSHOT.jar:$ES_HOME/lib/*:$ES_HOME/plugins/elasticsearch-shield-kerberos-realm/*:$ES_HOME/plugins/license/*:$ES_HOME/plugins/shield/*"' >> /opt/elasticsearch-2.4.3-shield/bin/elasticsearch.in.sh
# add the following to the .bash_profile:
echo 'export ES_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=61888  -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin  -Djava.security.krb5.realm=YOUR_REALM_GOES_HERE  -Djava.security.krb5.kdc=sandbox.hortonworks.com  -Djavax.security.auth.useSubjectCredsOnly=false  -Djava.security.auth.login.config=/home/solr/solr_jaas.conf  -Dsolr.kerberos.principal=HTTP/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE  -Dsolr.kerberos.keytab=/etc/security/keytabs/spnego.service.keytab -Dsolr.kerberos.cookie.domain=sandbox.hortonworks.com  -Dhost=sandbox.hortonworks.com  -Dsun.security.spnego.debug=true  -Dsun.security.krb5.debug=true  -Dsun.security.jgss.debug=true  -Dsun.security.spnego.msinterop=true  -Dhadoop.security.authentication=kerberos -Djava.security.auth.login.config=/opt/pontus/jaas_policy_store.conf  -Dhadoop.security.authentication=kerberos -Dhadoop.security.authorization=true  -Dgroup.id=solr_sandbox.hortonworks.com  -Dsolr.kerberos.name.rules=DEFAULT -Dpontus.enablePlugin=true"'>> ~elastic/.bash_profile


cp /opt/elasticsearch-2.4.3-shield/bin/elasticsearch  /opt/elasticsearch-2.4.3-shield/bin/elasticsearch.orig
sed -i s/org.elasticsearch.bootstrap.Elasticsearch/org.elasticsearch.bootstrap.PontusElasticEmbeddedKafkaSubscriber/g /opt/elasticsearch-2.4.3-shield/bin/elasticsearch


```





## Elastic Search 5.x using SearchGuard

### Copy the redaction files from git
login as root to the host box (the server that hosts the sandbox docker image), and run the following:
```
cd /root/redaction
# Note: this will typically be a one-off task.
scp -P 2222 pontus-redaction-elastic-5.x-searchguard/config/* localhost:/opt/elasticsearch/config

# Then, whenever there's a change in the code, here's how to rebuild it:
git pull; mvn -Dmaven.test.skip=true clean install; scp -P 2222 */target/pontus*.jar  localhost:/opt/pontus;

```

login to sandbox as root, and run the following to install elastic:
```
#DO NOT USE THE RPM; IT IS EVIL, and FULL OF ISSUES (e.g. it creates weird directory structures)!!!!!
#rpm -Ui https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.0.2.rpm
useradd elastic  -G hadoop
cd /opt
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.0.2.tar.gz
tar xvf elasticsearch-5.0.2.tar.gz
ln -s elasticsearch-5.0.2 elasticsearch
chown -R elastic:hadoop /opt/elasticsearch-5.0.2
printf '\nES_CLASSPATH="/opt/pontus/pontus-redaction-elastic-5.x-searchguard-0.0.1-SNAPSHOT.jar:$ES_HOME/lib/*:$ES_HOME/plugins/search-guard-5/*"' >> /opt/elasticsearch/bin/elasticsearch.in.sh
# add the following to the .bash_profile:
echo 'export ES_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=61888  -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin  -Djava.security.krb5.realm=YOUR_REALM_GOES_HERE  -Djava.security.krb5.kdc=sandbox.hortonworks.com  -Djavax.security.auth.useSubjectCredsOnly=false  -Djava.security.auth.login.config=/home/solr/solr_jaas.conf  -Dsolr.kerberos.principal=HTTP/sandbox.hortonworks.com@YOUR_REALM_GOES_HERE  -Dsolr.kerberos.keytab=/etc/security/keytabs/spnego.service.keytab -Dsolr.kerberos.cookie.domain=sandbox.hortonworks.com  -Dhost=sandbox.hortonworks.com  -Dsun.security.spnego.debug=true  -Dsun.security.krb5.debug=true  -Dsun.security.jgss.debug=true  -Dsun.security.spnego.msinterop=true  -Dhadoop.security.authentication=kerberos -Djava.security.auth.login.config=/opt/pontus/jaas_policy_store.conf  -Dhadoop.security.authentication=kerberos -Dhadoop.security.authorization=true  -Dgroup.id=solr_sandbox.hortonworks.com  -Dsolr.kerberos.name.rules=DEFAULT -Dpontus.enablePlugin=true"'>> ~elastic/.bash_profile

su - elastic
cp /opt/elasticsearch/bin/elasticsearch /opt/elasticsearch/bin/elasticsearch.orig
sed -i s/org.elasticsearch.bootstrap.Elasticsearch/org.elasticsearch.bootstrap.PontusElasticEmbeddedKafkaSubscriber/g /opt/elasticsearch/bin/elasticsearch
```

Set the following items in the elasticsearch.yml file:
```
tee /opt/elasticsearch/config/elasticsearch.yml <<'EOF'
searchguard.kerberos.krb5_filepath: /etc/krb5.conf
searchguard.kerberos.acceptor_keytab_filepath: /etc/security/keytabs/HTTP.sandbox.hortonworks.com.keytab
searchguard.ssl.transport.enabled: true
searchguard.ssl.transport.keystore_filepath: CN=localhost-keystore.jks
searchguard.ssl.transport.keystore_password: 7bf985c6005ec24dbe6f
searchguard.ssl.transport.truststore_filepath: truststore.jks
searchguard.ssl.transport.truststore_password: 5ad771e520d2e1e19225
searchguard.ssl.transport.enforce_hostname_verification: false
searchguard.ssl.http.enabled: true
searchguard.ssl.http.keystore_filepath: CN=localhost-keystore.jks
searchguard.ssl.http.keystore_password: 7bf985c6005ec24dbe6f
searchguard.ssl.http.truststore_filepath: truststore.jks
searchguard.ssl.http.truststore_password: 5ad771e520d2e1e19225
searchguard.audit.type: internal_elasticsearch
searchguard.authcz.admin_dn:
  - CN=Leo Martins,OU=Dev,O=Pontus,L=London,ST=Unknown,C=UK
  - CN=sgadmin
http.port: 9090
cluster.name: pontus
network.host: 172.17.0.2
EOF
```


Create a sg_config.yml file:
```
tee /opt/elasticsearch/config/sg_config.yml <<-'EOF'
authc:
  kerberos_auth_domain: 
    enabled: true
    order: 1
    http_authenticator:
      type: kerberos
      challenge: true
      config:
        krb_debug: false
        acceptor_principal: 'HTTP/sandbox.hortonworks.com'
        strip_realm_from_principal: false
    authentication_backend:
      type: noop
EOF

cd /opt/elasticsearch
bin/elasticsearch-plugin install -b com.floragunn:search-guard-5:5.0.2-8
cd /opt/elasticsearch/plugins/search-guard-5/
wget -O dlic-search-guard-auth-http-kerberos-5.0-2.jar 'http://search.maven.org/remotecontent?filepath=com/floragunn/dlic-search-guard-auth-http-kerberos/5.0-2/dlic-search-guard-auth-http-kerberos-5.0-2.jar'

/opt/elasticsearch/plugins/search-guard-5/tools/sgadmin.sh -cd /opt/elasticsearch/config -cn pontus -h sandbox.hortonworks.com -ks /opt/elasticsearch/config/CN\=localhost-keystore.jks  -kst JKS -nhnv -nrhn -p 9300 -ts /opt/elasticsearch/config/truststore.jks -kspass 7bf985c6005ec24dbe6f -tspass 5ad771e520d2e1e19225 -tst JKS


```
[NOTE] NOTE: Always run the following after making a change to the sg_config.yml file:
```
/opt/elasticsearch/plugins/search-guard-5/tools/sgadmin.sh -cd /opt/elasticsearch/config -cn pontus -h sandbox.hortonworks.com -ks /opt/elasticsearch/config/CN\=localhost-keystore.jks  -kst JKS -nhnv -nrhn -p 9300 -ts /opt/elasticsearch/config/truststore.jks -kspass 7bf985c6005ec24dbe6f -tspass 5ad771e520d2e1e19225 -tst JKS

```


Sample query URL:
```
https://sandbox.hortonworks.com:9090/_search?size=10&pretty=true
```


# Troubleshooting

## Issue: curl not working with SPNEGO (Kerberos)

Only cURL versions 7.38 and above work with SPNEGO (Kerberos); to fix this, you must install a later version using the following procedures:

1)  create a new file /etc/yum.repos.d/city-fan.repo, and put the following text in it:
```
[CityFan]
name=City Fan Repo
baseurl=http://www.city-fan.org/ftp/contrib/yum-repo/rhel$releasever/$basearch/
enabled=1
gpgcheck=0
```
2)  run the following commands:
```
yum clean all
yum install curl 
```
# Testing Elastic
Here's a good way to sanity check the environment:

```
# login to the sandbox docker image, and run the following (from a user shell that can see the keytab file):
kinit HTTP/sandbox.hortonworks.com -k -t /etc/security/keytabs/spnego.service.keytab
curl -u : --negotiate -XGET 'http://sandbox.hortonworks.com:9090/pontus/identity/_search?pretty'
```


