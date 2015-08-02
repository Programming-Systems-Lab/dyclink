#!/bin/bash

libs=("colt_graphs" "jama_graphs" "ejml_graphs" "commonmath_graphs" "ojalgo_graphs" "jcodecs_grphs" "plexus_graphs")

total=${#libs[*]}
echo "total lib number $total"

for (( i = 0 ; i <= $(( $total - 1 )); i++ ))
do
	for (( j = $(( $i + 1)) ; j <= 5; j++ ))
	do
		echo "${libs[$i]} ${libs[$j]}"
		java -Xmx60g -cp "./bin:./lib/*" edu.columbia.psl.cc.analysis.PageRankSelector -template ${libs[$i]} -test ${libs[$j]}
	done
done

#lib1="../colt_graphs"
#lib2="../jama_graphs"
#lib3="../la4j_graphs"

#read -s -p "DB password: " dbpw
#echo 'Start executing dyclink'
#echo 'Pair1: ' + $lib1 + ' ' + $lib2
#java -Xmx60g -cp "bin/:lib/*" edu.columbia.psl.cc.analysis.PageRankSelector $dbpw $lib1 $lib2

#echo 'Pair2: ' + $lib1 + ' ' + $lib3
#java -Xmx60g -cp "bin/:lib/*" edu.columbia.psl.cc.analysis.PageRankSelector $dbpw $lib1 $lib2

#echo 'Pair3: ' + $lib2 + ' ' + $lib3
#java -Xmx60g -cp "bin/:lib/*" edu.columbia.psl.cc.analysis.PageRankSelector $dbpw $lib2 $lib3



