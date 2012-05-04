#!/bin/sh

cat templates.txt | while read line; do
  echo "processing $line"
  wd=`cut -d- -f1 < echo "$line"`
  grep "$wd" newscomm-en/newscomm-en.txt > example-sents/$line
done

exit 0
