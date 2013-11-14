aclock
======

text clock, control via OSC

```
cd aclock
./compile.sh
./run.sh

-> a terminal will be started that send any keystrokes to aclock via OSC
-> the main terminal will show the current clock string and update upon OSC messages
sendkeys.sh and aclock are configured to use port 6789.

Keyboard Operation (in 2nd terminal):

'^': toggle clock active

'escape' ||
'q': set clock inactive

'a': set clock active

'z': autoselect next component true

'u': autoselect next component false

'insert' ||
'i': toggle autoselect next component setting

'j': autonomous components true (no overflow)

'k': autonomous components false

'l': toggle autonomous components setting

'arrow_left' ||
'd': navigate to left component

'arrow_right' ||
'g': navigate to right component

'arrow_up' ||
'r': increment component value by 1

'arrow_down' ||
'v': decrement component value by 1

'page_up' ||
't': increment component value by 10^(max digits -1)

'page_down' ||
'b': decrement component value by 10^(max digits -1)

'h': navigate to head component (hours)

'm': navigate to 2nd component (minutes)

's': navigate to 3rd component (seconds)

'x': navigate to tail component (milliseconds)
                
',': clear (zero) all components after currently selected

';': clear all components before currently selected
     
':': clear all components before and after currently selected
  
'backspace' ||  
'o': clear currently selected component

'delete' ||
'p': clear all components

'enter' ||     
'.': finish entry, jump to next component (if autoselect next component true)

'+': start addition to currently selected component

'+': start substraction to currently selected component

'0-9' any digit. to set, add and substract values


OSC Messages understood by aclock:

/key s
  1) s: key

/key is
  1) i: int (ignored)
  2) s: key
  (sk/sendkeys compatible)

/set s
  1) s: keys

/delim ssss
  1) s: left delim for selected components
  2) s: right delim for selected components
  3) s: left delim for edited components
  4) s: right delim for edited components

for more info, see src/OClock.java

Advanced:

to display clock strings with shout (https://github.com/7890/shout):

./aclock | while read line; do shout "$line" 1; done

in a second terminal, configure aclock, set delim for shout selection/edit coloring:

oscsend localhost 6789 /delim ssss '\\[' '\\]' '\\(' '\\)'

```
