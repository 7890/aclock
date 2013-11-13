#!/bin/sh

#must be started from current/this directory

mkdir -p classes
javac -cp .:lib/jlo.jar:lib/jna-3.3.0.jar -d classes src/OClock.java
