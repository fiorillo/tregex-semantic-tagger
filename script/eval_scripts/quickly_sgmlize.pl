#!/usr/bin/perl

# reads from stdin one line per sentence and produces on standard output a fake
# NIST SGML file.  It accepts one or three parameters:
#
# 	Mandatorially, one of "ref" "tst" or "src", the NIST SGML set tags.
#
#       If the others are given, they are the reported setid and docid for the
#	document at hand; they default to "unspecified".

die "Need to know what type of file to generate" if($#ARGV < 0);
my $TYPE = $ARGV[0];

my $SYSID = "devtest";
if($#ARGV >= 1) {
	$SYSID = $ARGV[1];
}

my $SETID = "unspecified";
my $DOCID = "unspecified";
if($#ARGV >= 3) {
	$SETID = $ARGV[2];
	$DOCID = $ARGV[3];
}

# SGML header... whole thing is one document.
print "<${TYPE}set trglang=\"en\" setid=\"${SETID}\" srclang=\"any\">\n";
print "<doc sysid=\"${SYSID}\" docid=\"${DOCID}\" genre=\"unk\" origlang=\"unk\">\n";

my $segid = 1;
while (my $line = <STDIN>) {
	chomp $line;
	# next if (length $line == 0);
	if ($line =~ /\|\|\|/) {
		print STDERR "Line '$line' appears to be or contain".
			     " Joshua markup.  This is only a warning.\n";
		# next;
	}

	print "<seg id=\"${segid}\"> $line </seg>\n";
	$segid++;
}

print "</doc>\n</${TYPE}set>\n";
