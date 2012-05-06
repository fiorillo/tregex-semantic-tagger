#!/bin/sh

ls example-sents/*.head | while read file; do
  touch $file.parsed
  stanford-parser-2012-01-06/lexparser.sh $file > $file.parsed
done

exit 0
