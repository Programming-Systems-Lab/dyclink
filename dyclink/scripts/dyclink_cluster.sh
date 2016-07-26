#!/bin/bash

if [ $# != 5 ]; then
	echo "Invalid number of input arguments"
	exit 1
fi

java -cp target/dyclink-0.0.1-SNAPSHOT.jar edu.columbia.psl.cc.util.ClusterAnalyzer -start $1 -end $2 -k $3 -insts $4 -similarity $5 -filter