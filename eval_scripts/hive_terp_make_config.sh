#!/bin/bash

# This script builds a HIVE TERp configuration in "terp.config".  It's mostly
# a convenience wrapper around the other scripts in this directory, which do
# the heavy lifting.
#
# Once the configuration is built, one typically invokes TERp like this:
#   time .../terp_source/bin/terpa terp.config/terp.param $OTHER_PARAM_FILE
# where $OTHER_PARAM_FILE should name a file which contains something like
#   Reference File (filename)                : sample.ref.sgm
#   Hypothesis File (filename)               : sample.hyp.sgm
#   Output Formats (list)                    : param nist pra html
#   Output Prefix (filename)                 : terp.out/


CONFDIR=`readlink -f ${1:-./terp.config} | sed -e s:/*$::`

SELF=$0
SELFABS=`readlink -f $0`
SELFPATH=`dirname ${SELFABS}`

die() {
	echo $@
	exit
}

mkdir -p $CONFDIR
pushd $CONFDIR > /dev/null

# Clean up if there's an earlier copy of us
rm -f wds.* terp.classes.*

# Generate wds.* and terp.classes.list
perl $SELFPATH/hive_terp_class_files.pl \
	< /export/common/data/processed/SIMT-SCALE/modality/modalityclasses/final-modality-morph-variants.txt \
	> terp.classes.list \
	|| die "Failed to make class files"

# Generate TERP class config file
perl $SELFPATH/hive_terp_class_config.pl "${CONFDIR}/" \
	< terp.classes.list \
	> terp.classes.conf \
	|| die "Failed to make class config file"

# Generate TERP config file
cat > terp.param <<HERE
TERp Cost Function (Java Classname)      : SCALECost
Word Class File (filename)               : ${CONFDIR}/terp.classes.conf
HERE

popd > /dev/null
