#!/bin/sh

ls example-sents/*.parsed | while read file; do
  root=`echo $file | cut -d. -f1 | cut -d/ -f2`
  cd stanford-tregex-2012-03-09
  ./tsurgeon.sh -treeFile $file ../modality-tagger/modality-patterns/instantiated-templates/$root.txt > ../output/$root.tree
done

exit 0
