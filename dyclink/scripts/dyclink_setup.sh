#!/bin/bash
if [ ! -d ./cache ]; then
	echo "Creating cache directory"
	mkdir cache
fi

if [ ! -d ./results ]; then
	echo "Creating results directory"
	mkdir results
fi

if [ ! -d ./log ]; then
	echo "Creating log directory"
	mkdir log
fi

if [ ! -d ./debug ]; then
	echo "Creating debug directory"
	mkdir debug
fi

if [ ! -d ./graphs ]; then
	echo "Creating graphs directory"
	mkdir graphs
fi