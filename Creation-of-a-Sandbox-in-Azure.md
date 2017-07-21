# Running the docker vm on azure
##  Install the packages:
```
yum -y update 
```
Create a docker repo:
```
tee /etc/yum.repos.d/docker.repo <<-'EOF'
[dockerrepo]
name=Docker Repository
baseurl=https://yum.dockerproject.org/repo/main/centos/7/
enabled=1
gpgcheck=1
gpgkey=https://yum.dockerproject.org/gpg
EOF

yum install docker-engine
wget http://hortonassets.s3.amazonaws.com/2.5/HDP_2.5_docker.tar.gz
```
    add the following to /etc/fstab:
```    
      tmpfs   /tmp         tmpfs   nodev,nosuid,size=40G          0  0
```
      then type the following:  
```
mount -a
mv HDP_2.5_docker.tar.gz /tmp
```

##  Fix the following Azure / docker issues: azure vms have a 20GB root partition and the HDP 2.5 docker image is 14GB: 

    1) add a new disk on the Azure VM (WARNING: you need to reboot the machine, so perhaps copy the HDP_2.5_docker.tar.gz file back to
       non-volatile storage.
       
    2) create a direct-lvm thin partition to point to the new disk:
      Power on the virtual machine.
      Identify the device name, which is by default /dev/sda, and confirm the new size by running the command:

```
fdisk -l
```
Create a new primary partition; run the command:
```      
fdisk /dev/sdc3 (depending the results of the step above)
          Press p to print the partition table to identify the number of partitions. By default, there are 2: sda1 and sda2.
          Press n to create a new primary partition.
          Press p for primary.
          Press 3 for the partition number, depending on the output of the partition table print.
          Press Enter two times.
          Press t to change the system's partition ID.
          Press 3 to select the newly creation partition.
          Type 8e to change the Hex Code of the partition for Linux LVM.
          Press w to write the changes to the partition table.
```
Restart the virtual machine

Run this command to verify that the changes were saved to the partition table and that the new partition has an 8e type:
```
fdisk -l
```
Run this command to convert the new partition to a physical volume:
[Note]     Note: The number for the sdc can change depending on system setup. Use the sda number that was created in step 5.

```
pvcreate /dev/sdc3
vgcreate docker /dev/sdc3
lvcreate --wipesignatures y -n thinpool docker -l 95%VG
lvcreate --wipesignatures y -n thinpoolmeta docker -l 1%VG
lvconvert -y --zero n -c 512K --thinpool docker/thinpool --poolmetadata docker/thinpoolmet
```       
Edit the /etc/lvm/profile/docker-thinpool.profile, and esure that it has the following:
```
activation {
    thin_pool_autoextend_threshold=80
    thin_pool_autoextend_percent=20
}
```
Run the following commands:
```
lvchange --metadataprofile docker-thinpool docker/thinpool
lvs -o+seg_monitor
rm -rf /var/lib/docker/*
```        
3) Modify the dockerd startup to use the new storage, and increase the Base Device Size of a Docker image to 20GB (either run this on the cmd line:
```    
      /usr/bin/dockerd --storage-driver=devicemapper --storage-opt dm.thinpooldev=/dev/mapper/docker-thinpool --storage-opt dm.use_deferred_removal=true --storage-opt dm.basesize=20G
```      
... or  edit the /usr/lib/systemd/system/docker.service file so the ExecStart line looks like the following:
```
      ExecStart=/usr/bin/dockerd --storage-driver=devicemapper --storage-opt dm.thinpooldev=/dev/mapper/docker-thinpool --storage-opt dm.use_deferred_removal=true --storage-opt dm.basesize=20G
```      
## Import the docker image:
```  
    gunzip -c /tmp/HDP_2.5_docker.tar.gz | docker load
```    
    
##  Start the sandbox image:
```    
    docker run -v hadoop:/hadoop --name pontus-kerb-hbase-solr --hostname "sandbox.hortonworks.com" --privileged -d \
    -p 6080:6080 \
    -p 9090:9090 \
    -p 9000:9000 \
    -p 8000:8000 \
    -p 8020:8020 \
    -p 42111:42111 \
    -p 10500:10500 \
    -p 16030:16030 \
    -p 8042:8042 \
    -p 8040:8040 \
    -p 2100:2100 \
    -p 4200:4200 \
    -p 4040:4040 \
    -p 8050:8050 \
    -p 9996:9996 \
    -p 9995:9995 \
    -p 8080:8080 \
    -p 8088:8088 \
    -p 8886:8886 \
    -p 8889:8889 \
    -p 8443:8443 \
    -p 8744:8744 \
    -p 8888:8888 \
    -p 8188:8188 \
    -p 8983:8983 \
    -p 1000:1000 \
    -p 1100:1100 \
    -p 11000:11000 \
    -p 10001:10001 \
    -p 15000:15000 \
    -p 10000:10000 \
    -p 8993:8993 \
    -p 1988:1988 \
    -p 5007:5007 \
    -p 50070:50070 \
    -p 19888:19888 \
    -p 16010:16010 \
    -p 50111:50111 \
    -p 50075:50075 \
    -p 50095:50095 \
    -p 18080:18080 \
    -p 60000:60000 \
    -p 8090:8090 \
    -p 8091:8091 \
    -p 8005:8005 \
    -p 8086:8086 \
    -p 8082:8082 \
    -p 60080:60080 \
    -p 8765:8765 \
    -p 5011:5011 \
    -p 6001:6001 \
    -p 6003:6003 \
    -p 6008:6008 \
    -p 1220:1220 \
    -p 21000:21000 \
    -p 6188:6188 \
    -p 61888:61888 \
    -p 2222:22 \
    -p 88:88 \
    -p 88:88/udp \
    pontus-kerb-hbase-solr /usr/sbin/sshd -D
    
    /usr/bin/docker start pontus-kerb-hbase-solr  
```    
##  Create the following run-docker.sh script:
```  
#!/bin/bash
echo "Waiting for docker daemon to start up:"
until /usr/bin/docker ps 2>&1| grep STATUS>/dev/null; do  sleep 1; done;  >/dev/null
/usr/bin/docker ps -a | grep pontus-kerb-hbase-solr
if [ $? -eq 0 ]; then
 /usr/bin/docker start pontus-kerb-hbase-solr
else
docker run -v hadoop:/hadoop --name pontus-kerb-hbase-solr --hostname "sandbox.hortonworks.com" --privileged -d \
-p 6080:6080 \
-p 9090:9090 \
-p 9000:9000 \
-p 8000:8000 \
-p 8020:8020 \
-p 42111:42111 \
-p 10500:10500 \
-p 16030:16030 \
-p 8042:8042 \
-p 8040:8040 \
-p 2100:2100 \
-p 4200:4200 \
-p 4040:4040 \
-p 8050:8050 \
-p 9996:9996 \
-p 9995:9995 \
-p 8080:8080 \
-p 8088:8088 \
-p 8886:8886 \
-p 8889:8889 \
-p 8443:8443 \
-p 8744:8744 \
-p 8888:8888 \
-p 8188:8188 \
-p 8983:8983 \
-p 1000:1000 \
-p 1100:1100 \
-p 11000:11000 \
-p 10001:10001 \
-p 15000:15000 \
-p 10000:10000 \
-p 8993:8993 \
-p 1988:1988 \
-p 5007:5007 \
-p 50070:50070 \
-p 19888:19888 \
-p 16010:16010 \
-p 50111:50111 \
-p 50075:50075 \
-p 50095:50095 \
-p 18080:18080 \
-p 60000:60000 \
-p 8090:8090 \
-p 8091:8091 \
-p 8005:8005 \
-p 8086:8086 \
-p 8082:8082 \
-p 60080:60080 \
-p 8765:8765 \
-p 5011:5011 \
-p 6001:6001 \
-p 6003:6003 \
-p 6008:6008 \
-p 1220:1220 \
-p 21000:21000 \
-p 6188:6188 \
-p 61888:61888 \
-p 2222:22 \
-p 88:88 \
-p 88:88/udp \
pontus-kerb-hbase-solr /usr/sbin/sshd -D
fi
#docker exec -t pontus-kerb-hbase-solr /etc/init.d/startup_script start
docker exec -t pontus-kerb-hbase-solr make --makefile /usr/lib/hue/tools/start_scripts/start_deps.mf  -B Startup -j -i
docker exec -t pontus-kerb-hbase-solr nohup su - hue -c '/bin/bash /usr/lib/tutorials/tutorials_app/run/run.sh' &>/dev/nul
docker exec -t pontus-kerb-hbase-solr touch /usr/hdp/current/oozie-server/oozie-server/work/Catalina/localhost/oozie/SESSIONS.ser
docker exec -t pontus-kerb-hbase-solr chown oozie:hadoop /usr/hdp/current/oozie-server/oozie-server/work/Catalina/localhost/oozie/SESSIONS.ser
docker exec -d pontus-kerb-hbase-solr /etc/init.d/tutorials start
docker exec -d pontus-kerb-hbase-solr /etc/init.d/splash
docker exec -d pontus-kerb-hbase-solr /etc/init.d/shellinaboxd start
```
    
## Run the script created above:
```
chmod 755 ./run-docker.sh 
./run-docker.sh
  
docker ps
CONTAINER ID        IMAGE               COMMAND               CREATED             STATUS              PORTS                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      NAMES
62f2d9d1ca07        sandbox             "/usr/sbin/sshd -D"   10 minutes ago      Up 6 minutes        0.0.0.0:1000->1000/tcp, 0.0.0.0:1100->1100/tcp, 0.0.0.0:1220->1220/tcp, 0.0.0.0:1988->1988/tcp, 0.0.0.0:2100->2100/tcp, 0.0.0.0:4040->4040/tcp, 0.0.0.0:4200->4200/tcp, 0.0.0.0:5007->5007/tcp, 0.0.0.0:5011->5011/tcp, 0.0.0.0:6001->6001/tcp, 0.0.0.0:6003->6003/tcp, 0.0.0.0:6008->6008/tcp, 0.0.0.0:6080->6080/tcp, 0.0.0.0:6188->6188/tcp, 0.0.0.0:8000->8000/tcp, 0.0.0.0:8005->8005/tcp, 0.0.0.0:8020->8020/tcp, 0.0.0.0:8040->8040/tcp, 0.0.0.0:8042->8042/tcp, 0.0.0.0:8050->8050/tcp, 0.0.0.0:8080->8080/tcp, 0.0.0.0:8082->8082/tcp, 0.0.0.0:8086->8086/tcp, 0.0.0.0:8088->8088/tcp, 0.0.0.0:8090-8091->8090-8091/tcp, 0.0.0.0:8188->8188/tcp, 0.0.0.0:8443->8443/tcp, 0.0.0.0:8744->8744/tcp, 0.0.0.0:8765->8765/tcp, 0.0.0.0:8886->8886/tcp, 0.0.0.0:8888-8889->8888-8889/tcp, 0.0.0.0:8983->8983/tcp, 0.0.0.0:8993->8993/tcp, 0.0.0.0:9000->9000/tcp, 0.0.0.0:9090->9090/tcp, 0.0.0.0:9995-9996->9995-9996/tcp, 0.0.0.0:10000-10001->10000-10001/tcp, 0.0.0.0:10500->10500/tcp, 0.0.0.0:11000->11000/tcp, 0.0.0.0:15000->15000/tcp, 0.0.0.0:16010->16010/tcp, 0.0.0.0:16030->16030/tcp, 0.0.0.0:18080->18080/tcp, 0.0.0.0:19888->19888/tcp, 0.0.0.0:21000->21000/tcp, 0.0.0.0:42111->42111/tcp, 0.0.0.0:50070->50070/tcp, 0.0.0.0:50075->50075/tcp, 0.0.0.0:50095->50095/tcp, 0.0.0.0:50111->50111/tcp, 0.0.0.0:60000->60000/tcp, 0.0.0.0:60080->60080/tcp, 0.0.0.0:61888->61888/tcp, 0.0.0.0:2222->22/tcp   sandbox
```  
## Reset the root password:
```
[root@Leo5 ~]# docker exec -it 62f2d9d1ca07 /bin/bash
sed: can't read .bash_logout: No such file or directory
[root@sandbox /]# passwd
Changing password for user root.
New password:
Retype new password:
passwd: all authentication tokens updated successfully.
[root@sandbox /]# exit
[root@Leo5 ~]# ssh root@127.0.0.1 -p 2222
root@127.0.0.1's password:
```