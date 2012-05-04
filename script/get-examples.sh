#!/bin/sh

cat templates.txt | while read line; do
  echo "processing $line"
  wd=`echo $line | cut -d- -f1`
  grep "$wd" newscomm-en/newscomm-en.txt > example-sents/$line
done

exit 0
