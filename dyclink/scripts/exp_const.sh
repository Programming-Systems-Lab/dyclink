#!/bin/bash
echo "Executing all projects in year $1"
java -cp target/dyclink-0.0.1-SNAPSHOT.jar edu.columbia.psl.cc.util.CrowdExecutor codebase/bin $1