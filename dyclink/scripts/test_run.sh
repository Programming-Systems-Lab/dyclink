#!/bin/bash

cache_dir="./cache"
if [ -d $cache_dir ]; then
	echo "Clean cache..."
	rm -rf $cache_dir
	mkdir $cache_dir
fi

echo "Executing test-run program"
java -javaagent:target/dyclink-0.0.1-SNAPSHOT.jar -noverify -cp "target/dyclink-0.0.1-SNAPSHOT.jar:target/test-classes/" edu.columbia.psl.cc.premain.MIBDriver cc.expbase.TestRun
echo "Test-run ends"

echo "Detecting code relatives"
java -Xmx60g -cp target/dyclink-0.0.1-SNAPSHOT.jar edu.columbia.psl.cc.analysis.PageRankSelector -target ./graphs
echo "Detection ends"

echo "Please check your results under the results directory"