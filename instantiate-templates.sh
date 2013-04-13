#!/bin/sh

set -- `getopt hl:t:o: $*`

if [ $# != 7 ]; then
  echo "usage: $0 -l [lexicon file] -t [template directory] -o [output directory]"
  exit 1
fi
while [ $1 != -- ]; do
  case $1 in
    -h)
      echo "usage: $0 -l [lexicon file] -t [template directory] -o [output directory]"
      exit 0
      ;;
    -l)
      lexicon=$2
      shift
      ;;
    -t)
      template_dir=$2
      shift
      ;;
    -o)
      output_dir=$2
      shift
      ;;
  esac
  shift
done
shift

cat $lexicon | while read line; do
  first=`echo $line | rev | cut -d: -f2- | rev`
  mid=`echo $line | rev | cut -d: -f1 | rev | cut -d, -f1`
  pairs=`echo "$first:$mid" | sed s/,/\ /g`
  templates=`echo $line | rev | cut -d: -f1 | rev | cut -d, -f2- | sed s/,/\ /g | tr '\r' ' '`

  prefix=`echo $line | cut -d, -f1 | cut -d: -f2`

  for template in $templates; do
    command="cat $template_dir/$template.txt"
    for pair in $pairs; do
      label=`echo $pair | cut -d: -f1`
      value=`echo $pair | cut -d: -f2`
      command="$command | sed s/\\\$$label/$value/g"
    done
#    echo $command
    eval $command > $output_dir/$prefix-$template.txt
  done
done

exit 0
