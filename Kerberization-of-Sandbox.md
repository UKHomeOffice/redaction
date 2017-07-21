# Instructions on how to kerberize the linux sandbox 

Note: do this before clicking on the Ambari Kerberize green button

1) run the following as root:
```
yum install krb5-libs krb5-server krb5-workstation
```

2) create /var/kerberos/krb5kdc/kdc.conf:
```
[kdcdefaults]
 kdc_ports = 88
 kdc_tcp_ports = 88

[realms]
 YOUR_REALM_GOES_HERE = {
  #master_key_type = aes256-cts
  acl_file = /var/kerberos/krb5kdc/kadm5.acl
  dict_file = /usr/share/dict/words
  admin_keytab = /var/kerberos/krb5kdc/kadm5.keytab
  supported_enctypes = aes256-cts:normal aes128-cts:normal des3-hmac-sha1:normal arcfour-hmac:normal des-hmac-sha1:normal des-cbc-md5:normal des-cbc-crc:normal
 }
```
 
3) create /etc/krb5.conf:
```
[logging]
 default = FILE:/var/log/krb5libs.log
 kdc = FILE:/var/log/krb5kdc.log
 admin_server = FILE:/var/log/kadmind.log

[libdefaults]
 default_realm = YOUR_REALM_GOES_HERE
 dns_lookup_realm = false
 dns_lookup_kdc = false
 ticket_lifetime = 30d
 renew_lifetime = 7d
 forwardable = true
 udp_preference_limit = 1

[realms]
 YOUR_REALM_GOES_HERE = {
  kdc = 127.0.0.1
  admin_server = 127.0.0.1
 }

[domain_realm]
 .YOUR_REALM_GOES_HERE = YOUR_REALM_GOES_HERE
 YOUR_REALM_GOES_HERE = YOUR_REALM_GOES_HERE
```
3c) create /var/kerberos/krb5kdc/kadm5.acl:
```
*/admin@YOUR_REALM_GOES_HERE *
```
 
4) run the following to create the database: 
```
kdb5_util -r YOUR_REALM_GOES_HERE create  -s
# NOTE: just add a password when prompted for the key; it is not a proper md5 key.
# if all goes wrong and you start getting errors such as  
```

5) run the following to create the root principal:
```
kadmin.local -q 'addprinc root/admin'
# NOTE: if you get errors like this, then ensure that you do rm -rf /var/kerberos/krb5kdc/*
# kadmin.local: Cannot find master key record in database while initializing kadmin.local interface
```
6) run the following to start the services:
``` 
/sbin/service krb5kdc start
/sbin/service kadmin start
```

7) install jce:
Get the binary from here: 
http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html

unzip the file, and copy the jars here: 
```
/usr/lib/jvm/java/jre/lib/security
```





