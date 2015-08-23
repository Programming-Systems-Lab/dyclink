#!/bin/bash

codejam_id="R5P1Y12"

echo "Start crowd-exectue CodeJam $codejam_id"
java -cp "../lib/*" -Xmx8g edu.columbia.psl.cc.util.CrowdExecutor "" "./bin" "$codejam_id"

echo "$codejam_id ends"| mail -s "$codejam_id" standbyme946@gmail.com
