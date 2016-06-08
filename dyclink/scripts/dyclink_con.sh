#!/bin/bash

if [ "$#" -lt 3 ]; then
	echo "Invalid number of input arguments"
	echo "Please provide 1. the location of your codebase 2. the entry point of your system 3. the arguments of your system "
	exit 1
fi

echo "Codebase: $1"
echo "System entry: $2"
echo "Reading Arguments: "
for ((i=3; i<=$#; i++))
do
	echo "${!i}"
	idx=$(echo "$i - 3" | bc)
	#sys_args[$idx]=${!i}
	flatten+="${!i} "
done
echo "Confirm system arguments: $flatten"

echo "Constructing dynamic instruction graphs..."

dyclink_cp="target/dyclink-0.0.1-SNAPSHOT.jar"
total_cp="$dyclink_cp:$1"
echo "Class path: $total_cp"

java -javaagent:target/dyclink-0.0.1-SNAPSHOT.jar -Xmx60g -noverify -cp $total_cp edu.columbia.psl.cc.premain.MIBDriver $2 $flatten

echo "Graph construction ends" 