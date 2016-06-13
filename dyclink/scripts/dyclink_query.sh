#!/bin/bash

if [ $# -gt 4 ] || [ $# -lt 3 ]; then
	echo "Invalid number of input arguments"
	echo "Please provide at least one comparison id for DyCLINK to query code relatives"
	exit 1
fi

if [ $# -eq 3 ]; then
	java -cp target/dyclink-0.0.1-SNAPSHOT.jar edu.columbia.psl.cc.util.CodeRelQueryInterface -compId $1 -insts $2 -similarity $3
else
	if [ $4 == "-f" ]; then
		java -cp target/dyclink-0.0.1-SNAPSHOT.jar edu.columbia.psl.cc.util.CodeRelQueryInterface -compId $1 -insts $2 -similarity $3 -filter
	else
		echo "Invalid flag: $4"
	fi 	
fi