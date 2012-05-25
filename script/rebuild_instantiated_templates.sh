#!/bin/sh

PROJECT_ROOT=`cat script/project-root.txt`
EVALSCRIPTS=${PROJECT_ROOT}/script/eval_scripts/
MODALCLASSES=${PROJECT_ROOT}
PATTERN_DIR=${PROJECT_ROOT}/modality-patterns

cd $PATTERN_DIR
rm -rf instantiated-templates
mkdir instantiated-templates

${EVALSCRIPTS}/instantiate_tsurgeon_templates.pl generalized-templates instantiated-templates \
	--keepgoing \
	< ${MODALCLASSES}/final-modality-morph-variants.txt \
	2> instantiated-templates/__instantiator_errors

if [ -s instantiated-templates/__instantiator_errors ]; then
	echo "Warning: Errors encountered while processing templates.";
	echo "  Please see instantiated-templates/__instantiator_errors .";
fi;
