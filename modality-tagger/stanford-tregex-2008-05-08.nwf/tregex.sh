#!/bin/sh
export CLASSPATH=$CLASSPATH
java -mx100m -cp 'stanford-tregex.jar:' edu.stanford.nlp.trees.tregex.TregexPattern "$@"
