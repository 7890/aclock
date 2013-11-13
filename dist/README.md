```
#aclock is a bskt (https://github.com/7890/bskt)
#to update aclock:

#source
./aclock delete OClock.java
./aclock add <path to updated>/OClock.java

#libs
./aclock delete jlo.jar
./aclock add <path to updated>/jlo.jar

#show contents
./aclock list

#show more options
./aclock help

#starting aclock without parameters will start the java program
./aclock

#needs java, javac, liblo.so in LD_LIBRARY_PATH (among others)
#if anything is missing it should be indicated
```
