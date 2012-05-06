#!/bin/sh

ls example-sents | while read file; do
  head $file > $file.head
done

exit 0
