#!/bin/sh

ls example-sents/*.parsed | while read file; do
  root=`cut -d. -f1 '$file'`
  echo $root
  #stanford-tregex-2012-03-09/tsurgeon.sh -treeFile $file 
done

exit 0
