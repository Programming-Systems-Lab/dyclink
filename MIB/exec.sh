#!/bin/bash

target_lib="colt_graphs"
libs=("jama_graphs" "commonmath_graphs" "ojalgo_graphs" "jcodecs_grphs" "plexus_graphs")

total=${#libs[*]}
echo "target lib $target_lib"
echo "total lib number $total"

for (( i = 0 ; i <= $(( $total - 1 )); i++ ))
do
	echo "Start executing $target_lib ${libs[$i]}"
	#java -Xmx60g -cp "./bin:./lib/*" edu.columbia.psl.cc.analysis.PageRankSelector -template $target_lib -test ${libs[$j]}
	echo "$target_lib ${libs[$i]} completes"| mail -s "$target_lib ${libs[$i]} pair" standbyme946@gmail.com
done

#for (( i = 0 ; i <= $(( $total - 1 )); i++ ))
#do
#	for (( j = $(( $i + 1)) ; j <= $(( $total - 1 )); j++ ))
#	do
#		echo "Start executing ${libs[$i]} ${libs[$j]}"
#		java -Xmx60g -cp "./bin:./lib/*" edu.columbia.psl.cc.analysis.PageRankSelector -template ${libs[$i]} -test ${libs[$j]}
#		echo "${libs[$i]} ${libs[$j]} completes"| mail -s "${libs[$i]} ${libs[$j]} pair" standbyme946@gmail.com
#	done
#done

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



