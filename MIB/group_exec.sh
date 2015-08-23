#!/bin/bash
echo 'Start executing'
java -Xmx60g -XX:-UseSplitVerifier -cp "./bin:lib/*" edu.columbia.psl.cc.analysis.PageRankSelector -graphrepo /home/ubuntu/codejam_mining/graphrepo
echo "codejam mining done"| mail -s "Codejam done" standbyme946@gmail.com