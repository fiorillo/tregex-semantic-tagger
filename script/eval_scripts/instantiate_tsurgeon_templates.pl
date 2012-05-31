#!/usr/bin/perl

# read from STDIN the lines of a HATERp class definition file and
# instantiates tsurgeon teplates as directed.
#
# Options:
#   --[no]debug		Produce verbose debugging output on STDERR
#   --[no]keepgoing	Keep going if we encounter errors (produce warnings
#			instead).  This may result in output missing
#			instantiations, but is useful for testing.
#
# Templates are expected to contain three variables:
#	**		Replaced with the head word from the class definition
#	TrigLabel	Replaced by Trig$CLASS, CLASS derived from definition
#	TargLabel	Replaced by Targ$CLASS, CLASS derived from definition
#
# Note that classes are Z-encoded (see hive_terp_utils's hive_terp_tag_encoder)
# when used as part of the tsurgeon output.

use strict;

use Data::Dumper;
use File::Basename;
use File::Spec::Functions qw(rel2abs);
use Getopt::Long;

BEGIN {
  my $SELFNAME = rel2abs($0);
  my $SELFPATH = dirname($SELFNAME);
  push @INC, $SELFPATH;
}
use hive_terp_utils;

my $DEBUG = 0;
my $KEEPGOING = 0;
my %OPTIONS = (
	"debug!" => \$DEBUG,
	"keepgoing!" => \$KEEPGOING,
);
GetOptions(%OPTIONS) or die;

sub unable_open ($) {
	my ($s) = @_;
	my $str = "Unable to open $s : $!";
	if ($KEEPGOING) { warn $str; } else { die $str; }
}

my $TEMPLBASE = shift @ARGV;
die "Need template directory" unless -e $TEMPLBASE;

my $OUTDIR = shift @ARGV;
die "Need output directory" unless -e $OUTDIR;

while (my $line = <STDIN>) {
        chomp $line;
        my $res = hive_terp_decode_classdef_line($line);
        next if not defined $res;
        my ($class, $wordtagged, $additional) = @$res;
	$class = hive_terp_tag_encoder($class);
 
	if (not defined $additional) {
	        print STDERR "Skipping $line\n" if $DEBUG;
		next;
	}
        my @wordstags = split /\s+/,$wordtagged ;
	my @words = map { split /$hive_terp_separator/, $_} (@wordstags);
        my ($headword, @temps) = split /(?<!\\),\s*/, $additional;

	my $prettywords = join " ", @words;
	if (not grep { /(^|\s+)\Q$headword\E(''|\s+|$)/ } (@words) ) {
		warn "It looks like this line might have mismatched "
		   . "head-word ($headword) and tagged words ($prettywords): "
		   . "$line";
	}

	print STDERR "Word $prettywords ($headword) in class $class : templates ",
	 		(join ", ", @temps), "\n" if $DEBUG ;

        TEMPL: foreach my $templ (@temps) {
		$templ =~ s/^\s*(\S+)\s*$/\1/;
                open TEMPLATE, "<", "${TEMPLBASE}/${templ}"
                  or open TEMPLATE, "<", "${TEMPLBASE}/${templ}.txt"
		  or (unable_open ("template '$templ' for '$headword'")
		      xor next TEMPL);
		open OUTPUT, ">", "${OUTDIR}/${headword}-${templ}.txt"
		  or (unable_open ("output '$headword-$templ'")
		      xor next TEMPL);
		my $didsubst = 0;
                while (<TEMPLATE>) {
                        s/\*\*/$headword/ and $didsubst++;
                        s/TrigLabel/Trig$class/ and $didsubst++;
                        s/TargLabel/Targ$class/ and $didsubst++;
			print OUTPUT;
                }
		close OUTPUT;
                close TEMPLATE;
		warn "Template $templ for $headword underwent no substitutions"
		  if $didsubst == 0;
        }
}
