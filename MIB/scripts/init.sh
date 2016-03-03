#!/bin/bash
CACHE="./cache/"
CONFIG="./config/"
DEBUG="./debug/"
LABELMAP="./labelmap/"
LIB="./lib/"
LOG="./log/"
OP="./opcodes/"
RESULT="./results/"
TEMPLATE="./template/"
TEST="./test/"
REPO="./graphrepo/"

if [ ! -d "$CACHE" ]; then
	mkdir "$CACHE"
	echo "Create $CACHE directory"
fi

if [ ! -d "$CONFIG" ]; then
	mkdir "$CONFIG"
	echo "Create $CONFIG directory"
fi

if [ ! -d "$DEBUG" ]; then
	mkdir "$DEBUG"
	echo "Create $DEBUG directory"
fi

if [ ! -d "$LABELMAP" ]; then
	mkdir "$LABELMAP"
	echo "Create $LABELMAP directory"
fi

if [ ! -d "$LIB" ]; then
	mkdir "$LIB"
	echo "Createe $LIB directory"
fi

if [ ! -d "$LOG" ]; then
	mkdir "$LOG"
	echo "Create $LOG directory"
fi

if [ ! -d "$OP" ]; then
	mkdir "$OP"
	echo "Create $OP directory"
fi

if [ ! -d "$RESULT" ]; then
	mkdir "$RESULT"
	echo "Create $RESULT directory"
fi

if [ ! -d "$TEMPLATE" ]; then
	mkdir "$TEMPLATE"
	echo "Create $TEMPLATE directory"
fi

if [ ! -d "$TEST" ]; then
	mkdir "$TEST"
	echo "Create $TEST directory"
fi

if [ ! -d "$REPO" ]; then
	mkdir "$REPO"
	echo "Create $REPO directory"
fi

echo "Complete directory set up"
