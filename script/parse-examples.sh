#!/bin/sh

ls example-sents | while read file; do
  head example-sents/$file > example-sents/$file.head
done

exit 0
