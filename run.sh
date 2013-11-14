#!/bin/sh

#must be started from current/this directory

#set alternative java here
j="`which java`"

chmod 755 bin/sendkeys.sh

$TERM `pwd`/bin/sendkeys.sh &

LD_LIBRARY_PATH=/usr/local/lib:/usr/lib "$j" -cp .:lib/jlo.jar:lib/jna-3.3.0.jar:classes OClock 6789
