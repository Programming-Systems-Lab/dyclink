#!/bin/bash

if [ "$#" -lt 2 ]; then
	echo "Invalid number of input arguments"
	echo "Please provide at least one graph repository for DyCLINK to detect code relatives"
	exit 1
fi

echo "Reading Arguments: "
for ((i=1; i<=$#; i++))
do
	echo "${!i}"
	idx=$(echo "$i - 3" | bc)
	#sys_args[$idx]=${!i}
	flatten+="${!i} "
done
echo "Confirm detection arguments: $flatten"

echo "Conduct graph mining to detect code relatives..."

java -Xmx60g -cp target/dyclink-0.0.1-SNAPSHOT.jar edu.columbia.psl.cc.analysis.PageRankSelector $flatten

echo "Graph construction ends" 