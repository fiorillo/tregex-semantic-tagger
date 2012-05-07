#!/bin/sh

root=/Users/matthewfiorillo/Documents/school/tsurgeon
cd $root
ls example-sents/*.parsed | while read file; do
  cd $root/stanford-tregex-2012-03-09
  name=`echo $file | cut -d. -f1 | cut -d/ -f2`
  ./tsurgeon.sh -treeFile $root/example-sents/$name.txt.head.parsed $root/modality-tagger/modality-patterns/instantiated-templates/$name.txt > $root/output/$name.tree
done

exit 0
