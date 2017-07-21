# One off setup

Login to the docker host
```
ssh -p 2222 root@localhost
[pa55word] by default
```

# Ongoing builds
Go to the repository's root:
```
cd ~/redaction
git pull
mvn -Dmaven.test.skip=true clean install
scp -P 2222 */target/pontus*.jar localhost:/opt/pontus
```
