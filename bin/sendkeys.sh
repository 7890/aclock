#!/bin/bash

#/tb/130701

#send keystrokes as osc messages

#http://tldp.org/LDP/abs/html/internal.html
#http://wiki.bash-hackers.org/scripting/terminalcodes

THOST=localhost
TPORT=6789

quiet=0

if [ x"$1" = "x-h" ]
then
	echo "sendkey.sh help"
	echo "syntax: sendkey.sh (<target host> (<target port> (<quiet>))"
	echo "DUMMY"
	exit 0
fi

if [ x"$1" = "x-q" ]
then
	quiet=1
fi

function echo_()
{
	if [ $quiet -ne 1 ]
	then
		echo "${1}"
	fi
}

function send()
{
	oscsend "${THOST}" "${TPORT}" /key s "${1}"
}

function process()
{
	code=`printf "%d" "'${1}"`

	if [ $code -eq 127 ]
	then
		echo_ "backspace"; send "backspace"
		return
	fi

	if [ $code -gt -1 ]
	then
		echo_ "$1 $code"; send "${1}"
	else
		echo_ "key not supported"
	fi
}

while true
do
  read -sn1 a

  test "$a" != `echo -en "\e"` \
	&& process "$a"

  test "$a" == `echo -en "\e"` || continue
  read -sn1 a

  test "$a" == "[" || continue
  read -sn1 a

case "$a" in
	A) echo_ "up"; send "arrow_up";;
	B) echo_ "down"; send "arrow_down";;
	C) echo_ "right"; send "arrow_right";;
	D) echo_ "left"; send "arrow_left";;
	2) echo_ "insert"; read -sn1 a; send "insert";;
	3) echo_ "delete"; read -sn1 a; send "delete";;
	5) echo_ "pageup"; read -sn1 a; send "page_up";;
	6) echo_ "pagedown"; read -sn1 a; send "page_down";;
	*) echo_ "UNKNOWN" $a;;
esac

done
