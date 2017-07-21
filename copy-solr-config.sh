#!/bin/bash

/opt/lucidworks-hdpsearch/solr/server/scripts/cloud-scripts/zkcli.sh -zkhost localhost:2181 -cmd putfile /configs/labs/solrconfig.xml solrconfig.xml
