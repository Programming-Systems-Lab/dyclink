#!/bin/sh
iorepos=(graphs/2011 graphs/2012 graphs/2013 graphs/2014)

read -s -p "Password: " pw

echo "Exhaustive mode"
for i in "${iorepos[@]}"
do
	java -Xmx60g -cp target/dyclink-0.0.1-SNAPSHOT.jar edu.columbia.psl.cc.analysis.PageRankSelector -target $i -iginit -dbpw $pw 
done

echo "Comparison mode"

repo_length=${#graphrepos[@]}
echo "Repo length: $repo_length"

for ((i=0; i<$repo_length; i++))
do
	for ((j=i+1; j<$repo_length; j++))
	do
		java -Xmx60g -cp target/dyclink-0.0.1-SNAPSHOT.jar edu.columbia.psl.cc.analysis.PageRankSelector -target ${graphrepos[$i]} -test ${graphrepos[$j]} -iginit -dbpw $pw
	done
done