#!/bin/sh

if [ -f "$1" ]; then
  file=`pwd`/$1
else
  echo $1 > `pwd`/tmp/tmp.txt
  file=`pwd`/tmp/tmp.txt
fi

cd `pwd`/tools/stanford-parser/
./lexparser.sh $file

exit 0
