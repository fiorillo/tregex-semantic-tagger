#!/bin/sh

ls example-sents/*.parsed | while read file; do
  root=`echo $file | cut -d. -f1 | cut -d/ -f2`
  echo $root
  #stanford-tregex-2012-03-09/tsurgeon.sh -treeFile $file 
done

exit 0
