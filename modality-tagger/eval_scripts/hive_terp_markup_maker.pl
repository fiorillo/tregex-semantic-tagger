#!/usr/bin/perl

# Reads from a plain, a POS-tagged file, a Phoenix-style tag file, the
# Tsurgeon output file, and the HATERp class configuration.
#
# Produces an SGML file with potentially many reference alernatives for each
# input sentence and tags of the form that HIVE TERp can use.
#
# It expects POS tags to look like (TAG word), with the whole sentence in ( ).
# Phoenix input looks like SENT START END TAG with SENT, START, and END being
# numeric sentence and offset indicators; TAG, naturally, is the Phoenix tag.
#
# See ~aphillips/xml2tags.pl for a script which produces Phoenix files from
# Phoenix's native output.  We consume "format 1" from that script.

use Algorithm::Combinatorics qw(subsets) ;
use Data::Dumper;
use File::Basename;
use File::Spec::Functions qw(rel2abs);
use Getopt::Long;

use strict;
use warnings;

BEGIN {
  my $SELFNAME = rel2abs($0);
  my $SELFPATH = dirname($SELFNAME);
  push @INC, $SELFPATH;
}
use hive_terp_utils;

my $DEBUG = 0;
my $ARGTSURGEON=undef;
my $SKIP_POS_LINES = 0;
my $REFBASE = "evalsys";
my $SETID = "unspecified";
my $DOCID = "unspecified";
GetOptions(
	'debug!' => \$DEBUG,
	'skippos:i' => \$SKIP_POS_LINES,
	'setid:s' => \$SETID,
	'docid:s' => \$DOCID,
	'refbase:s' => \$REFBASE,
	'tsurgeon!' => \$ARGTSURGEON,
) or die;
my $CLASSFILE = shift @ARGV;

die "Need classfile" if not defined $CLASSFILE;

my $TERFORCEPFX = $hive_terp_classforce_token
                . $hive_terp_separator
                . $hive_terp_classforce_prefix ;


my @CLASSES = ( );
open CLASSFILE, "<", $CLASSFILE or die $!;
while (<CLASSFILE>)
{
  chomp $_;
  push @CLASSES, hive_terp_tag_encoder($_);
}
close CLASSFILE;

print STDERR "Word classes: ", (join " ", @CLASSES), "\n" if $DEBUG;

my $txtf = shift @ARGV or die;
my ($txtbase, $txtpart) = ($txtf, "");
($txtbase, $txtpart) = ($1,$2) if ($txtf =~ /^(.*)(\.\d+)$/);
my $posf = $txtbase.".tagged".$txtpart;
my $phxf = $txtbase.".norm.phxout.tags_simple".$txtpart;
my $tsgf = $txtbase.".tsurgeon".$txtpart;

open TXTFILE, "<", $txtf or die;

open POSFILE, "<", $posf or die;
for my $i (1 .. $SKIP_POS_LINES) { scalar <POSFILE>; }

my $MUSTTSURG   = (defined $ARGTSURGEON and $ARGTSURGEON);
my $SHOULDTSURG = (not defined $ARGTSURGEON or $ARGTSURGEON);
(not $SHOULDTSURG)
	or (open TSGFILE, "<", $tsgf)
	or ($MUSTTSURG and die "Can't open tsurgeon file")
	or warn "Running without Tsurgeon input.  No reference expansion.";

open PHXFILE, "<", $phxf or die;
my ($phx_sent, $phx_start, $phx_end, $phx_tag);

sub next_phxline () {
	my $phxline = <PHXFILE>;
	($phx_sent, $phx_start, $phx_end, $phx_tag)
		= defined $phxline ? split /\s+/, $phxline : ( );
}
next_phxline();		# Prime phoenix readout

my @refset = ( );	# Hash of arrayrefs, keyed by segment ID
			# then by order of alternative generation.

my $maxalts = 0;	# Max index of alternatives.  i.e. one fewer
			# than the number of "systems" we'll emit.

my $linecnt = 0;	# The current segment counter.

my $alts;	# For scope reasons, this is up here.
while (my $posline = <POSFILE>) {
	chomp $posline;
	$posline =~ s/^\((.*)\)$/$1/;	# Strip ( )

	my $txtline = <TXTFILE>;
	my @txtwords = split /\s+/, $txtline;

	my $poswords = $posline;
	$poswords =~ s/\([^ ]+ ([^ )]+)\)/$1/g;
	my @poswords = split /\s+/, $poswords;

	my $postags = $posline;
	$postags =~ s/\(([^ ]+) [^ )]+\)/$1/g;
	my @postags = split /\s+/, $postags;

	die if ($#poswords != $#postags);

	my $isparsed = 1;

	if( $#poswords < 0 ) {
		$isparsed = 0;
		warn "Sentence $linecnt (in $txtf) is not parsed.";
	} else {
		die if ($#poswords != $#txtwords);
		foreach my $ix (0 .. $#poswords) {
			die if (lc $poswords[$ix]) ne (lc $txtwords[$ix]);
		}
	}

	my @phxtags = ( );
	while( defined $phx_sent and $phx_sent == $linecnt ) {
		for my $ix ($phx_start .. $phx_end - 1) {
			die ("Bad phoenix tag? "
				. Dumper(\@txtwords, $ix, $phx_tag))
				if $ix > $#txtwords;
			$phxtags[$ix] = [ ] if not defined $phxtags[$ix];
			push @{$phxtags[$ix]}, $phx_tag;
		}
		next_phxline();
	}

	my @out = ( );
	my @hyphennames = ( );
	foreach my $word (@txtwords) {
		my $pos = shift @postags;
		my $phxtags = shift @phxtags;
		my $terptag;
		$terptag = hive_terp_tag_encoder($pos) if defined $pos;
					# No parse or NNP/NNPS/JJ tag
		$terptag = "name" if ((not $isparsed or $pos =~ /^(NNP|JJ)/)
					# Have some tag
				  and defined $phxtags
					# PERSON, GPE, and GPE-ite
				  and grep { /^(GPE|PERSON$)/ } @$phxtags) ;
		if(defined $terptag) {
			push @out, "${word}${hive_terp_separator}${terptag}";

			if ($terptag eq "name" and $word =~ /-/) {
				push @hyphennames, $#out;
			}
		} else {
			push @out, $word;
		}
	}


	my $base = (join " ", @out);
	
	$alts = [ ];
	push @$alts, $base;
	my @edits = ( );

	# For each named entity that's got a hypen, propose an edit with
	# hyphens broken out and each word tagged as a name.  This is really
	# more like a kind of tokenization than anything else, but we've already
	# got all the right pieces here.
	foreach my $hnix (@hyphennames) {
		my $hyphenated = $txtwords[$hnix];
		my $repl = join " - ",
			(map { "$_${hive_terp_separator}name"}
				(split /-+/, $hyphenated));
		push @edits, [ $hnix, $repl, undef ];
	}

	if ($SHOULDTSURG) {
		if(not $isparsed) {
			my $mustbefail = <TSGFILE>;
			die "Malformed tsurgeon output?" if $mustbefail !~ /^\(\)$/;

			next;
		}

		my ($parsed, $treeinfo) =
			hive_terp_sentence_from_tsurgeon_file(*TSGFILE, 0);
		next if $#$parsed < 0;

		foreach my $ix (0 .. $#poswords) {
			die "$$parsed[$ix][0] $poswords[$ix]" if (lc $$parsed[$ix][0]) ne (lc $poswords[$ix]);
		}

		
		# See if we've got triggers
		FINDTRIG: foreach my $ix (0 .. $#$treeinfo) {
		  my ($w, $d, $tsr) = @{$$parsed[$ix]};
		  my ($p, $s, $r) = @{$$treeinfo[$ix]};
		
		  my $class = undef; 
		  if( grep { !/TrigNeg/ and /Trig(.*)/ and ($class = $1) } @$tsr) {
		    print STDERR "Trig at $ix ($$parsed[$ix][0]; $class)\n" if $DEBUG ;
		
		    my $neg=undef;
		    my $check = $p+1;    # Leftmost child on my strata
		    FINDNEG: while ($check != -1) {
		      if ( grep { /TrigNeg/ } @{$$parsed[$check][2]} ) {
		        $neg=$check;
		        last FINDNEG;
		      }
		
		      # Move along the sibling chain
		      $check = $$treeinfo[$check][1];
		    }
		
		    # See if this is a negatable class.
		    my $candneg = ((substr $class, 0, 3) eq "Not")
		                      ? (substr $class, 3)
		                      : "Not$class" ;
		
		      # If we don't believe the negation, don't, unless we didn't
		      # believe the class either.  It's always possible somebody
		      # knows something we don't.
		    if (not grep { /^\Q$candneg\E$/ } @CLASSES) {
		      if (grep { /^\Q$class\E$/ } @CLASSES) { 
		        next FINDTRIG;
		      } else {
		        warn "I don't know about class $class; negating anyway.";
		      }
		    }
		
		    if (defined $neg) {
		      print STDERR "  With negation at $neg ($$parsed[$neg][0])\n" if $DEBUG;
		      # Erase negation token and negate class.
		      push @edits, [ $ix, "${TERFORCEPFX}${candneg}", $neg ];
		    } else {
		      # Insert a negation token and negate class.
		      push @edits, [ $ix
		                     , "${TERFORCEPFX}Negation ${TERFORCEPFX}${candneg}"
		                     , undef];
		    }
		  }
		}
	}
		
	my $iter = subsets(\@edits);
	GENALT: while (my $ess = $iter->next) {
	
	  next GENALT if $#$ess < 0;
			
	  my @mods = ( );
	  foreach my $edit (@$ess) {
	    my ($ix, $repl, $erase) = @$edit;
	    $mods[$erase] = "" if defined $erase;
	    $mods[$ix] = $repl;
	  }

	  my @alt = ( );
	  foreach my $wix (0 .. $#txtwords) {
	    if ( defined $mods[$wix] ) {
	      push @alt, $mods[$wix];
	    } else { 
	      push @alt, $txtwords[$wix];
	    }
	  }
	
	  push @$alts, (join " ", @alt);
	}
} continue {
	if ($#$alts > $maxalts) { $maxalts = $#$alts };

	push @refset, $alts;
	$linecnt++;
}

# debugging
# push @{$refset[10]}, "foo";
# $maxalts++;

print "<refset trglang=\"en\" setid=\"${SETID}\" srclang=\"any\">\n";
foreach my $rix (0 .. $maxalts) {
print "<doc sysid=\"$REFBASE.$rix\" docid=\"${DOCID}\"",
      " genre=\"unk\" origlang=\"unk\">\n";
  foreach my $six (0 .. $#refset) {

    next if $#{$refset[$six]} < $rix;

    print "<seg id=\"" . ($six+1) . "\"> ";
    print @{$refset[$six]}[$rix];
    print " </seg>\n";
  }
print "</doc><!-- $rix -->\n";
}
print "</refset>\n";
