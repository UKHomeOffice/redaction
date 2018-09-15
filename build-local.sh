#!/bin/bash
DIR="$( cd "$(dirname "$0")" ; pwd -P )"
VERSION=0.99.0
echo DIR is $DIR
export DISTDIR="$DIR/../pontus-dist/opt/pontus/pontus-redaction/pontus-redaction-$VERSION";

CURDIR=`pwd`
cd $DIR
git pull
mvn -DskipTests clean install

if [[ ! -d $DISTDIR ]]; then
  mkdir -p $DISTDIR
fi


cd $DISTDIR

rm -rf *

mkdir -p $DISTDIR/lib


cp -r $DIR/*/target/pontus*jar $DISTDIR/lib


cd ..

if [[ -f current ]]; then
  unlink current
fi
ln -s pontus-redaction-$VERSION current

cd $CURDIR
