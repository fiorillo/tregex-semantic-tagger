#!/bin/sh

ls example-sents/*.head | while read file; do
  stanford-parser-2012-01-06/lexparser.sh example-sents/$file > example-sents/$file.parsed
done

exit 0
