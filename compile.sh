#!/bin/sh

#must be started from current/this directory

#set alternative javac here
jc="`which javac`"

mkdir -p classes
"$jc" -source 1.6 -target 1.6 -cp .:lib/jlo.jar:lib/jna-3.3.0.jar -d classes src/OClock.java
