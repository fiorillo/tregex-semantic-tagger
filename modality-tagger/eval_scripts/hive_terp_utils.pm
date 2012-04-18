package hive_terp_utils;
use strict;

use Exporter;
our @ISA = 'Exporter';
our @EXPORT;

	# This is known by the SCALECost function inside TERp, too.
	# Please ensure cross-consistency.
our $hive_terp_separator = "''";
push @EXPORT, qw( $hive_terp_separator );

	# This is a "word" which will never appear in the corpus
	# which we use as a surrogate in the case we need to force
	# TERp's hand. 
	#
	# This is known by the SCALECost function inside TERp, too.
	# Please ensure cross-consistency.
our $hive_terp_classforce_token = "terforcetoken";
push @EXPORT, qw( $hive_terp_classforce_token );

our $hive_terp_classforce_prefix = "TC";
push @EXPORT, qw( $hive_terp_classforce_prefix );

	# Z-encode a tag, to work around issues with TERp's builtin
	# tokenization and normalization
sub hive_terp_tag_encoder ($) {
	my ($terptag) = @_;
	$terptag =~ s/Z/ZZ/g;

	$terptag =~ s/@/ZA/g;
	$terptag =~ s/,/ZC/g;
	$terptag =~ s/\$/ZD/g;
	$terptag =~ s/!/ZE/g;
	$terptag =~ s/#/ZH/g;
	$terptag =~ s/:/ZL/g;
	$terptag =~ s/\./ZP/g;
	$terptag =~ s/"/ZQ/g;
	$terptag =~ s/'/ZT/g;
	$terptag =~ s/\?/ZU/g;

	$terptag;
}
push @EXPORT, qw( &hive_terp_tag_encoder );

	# Decode a Z-encoded tag
sub hive_terp_tag_decoder ($) {
	my ($terptag) = @_;

	$terptag =~ s/ZA/@/g;
	$terptag =~ s/ZC/,/g;
	$terptag =~ s/ZD/\$/g;
	$terptag =~ s/ZE/!/g;
	$terptag =~ s/ZH/#/g;
	$terptag =~ s/ZL/:/g;
	$terptag =~ s/ZP/./g;
	$terptag =~ s/ZQ/"/g;
	$terptag =~ s/ZT/'/g;
	$terptag =~ s/ZU/?/g;

	$terptag =~ s/ZZ/Z/g;

	$terptag;
}
push @EXPORT, qw( &hive_terp_tag_decoder );

	# Decode a modality class definition file line
	# Returns [ $class, $string, $additional ]
sub hive_terp_decode_classdef_line ($) {
	($_) = @_;
 	s/^([^#]*)#.*$/\1/;	# Strip out comments, if any.
	if (/^(.+?)\$([^\$&]+?)\s*([\$&]\s*(.*))?$/) {
		return [$2, $1, $4];
	} elsif (/^\s*$/) {
		# Ignore a line of just whitespace (or commentary)
		return undef;
	} else {
		warn "Malformed line or regex bug: ", $_, "\n";
		return undef;
	}
}
push @EXPORT, qw( &hive_terp_decode_classdef_line );

	# Take a classdef line and Z-encode all tags on it
	# Returns as (and calls internally) hive_terp_decode_classdef_line
sub hive_terp_classdef_line_retag ($) {
	my $res = hive_terp_decode_classdef_line (shift @_) ;
	return undef if not defined $res;

	my ($n, $ws, $a) = @$res;

	$n =~ s/\s+//g ;
	my @nws = ( );
	foreach my $wt (split /\s+/, $ws) {
		if($wt =~ /^(.*)${hive_terp_separator}(.*)$/) {
			my $w = $1;
			my $t = hive_terp_tag_encoder($2);
			$wt = "$w${hive_terp_separator}$t";
		}
		push @nws, $wt;
	}
	return [$n, (join " ", @nws), $a];
}
push @EXPORT, qw( &hive_terp_classdef_line_retag );

  # Construct our own encoding of the tree information.  The return arrayref at
  # index $i holds an array with the address of parent, sibling, and rightmost
  # child of the ith word.  Rightmost child may not be useful but it's computed
  # and maintained as the loop runs, so we don't bother getting rid of it.
sub hive_terp_tsurgeon_sentence_info ($) {
  my ($parsed) = @_;

  my @treeinfo = ( );
  {
    # Top node has no parent, no sibling, and currently no child.
    # This node will be shifted off the array at the end.
    $treeinfo[0][0] = -1;  # parent
    $treeinfo[0][1] = -1;  # sibling
    $treeinfo[0][2] = -1;  # rightmost child

    my @pstack = ( -1 );
    foreach my $ix (0 .. $#$parsed) {
      my ($w, $d, $tsr) = @{$$parsed[$ix]};
      $d += 1;

      while ( $d < $#pstack ) { pop @pstack; }
      while ( $d > $#pstack ) { push @pstack, $pstack[$#pstack]; }

      $pstack[$d] = $ix;

      # So far I have no child or sibling
      $treeinfo[$ix+1][1] = -1;
      $treeinfo[$ix+1][2] = -1;

      # My parent is on the stack.
      $treeinfo[$ix+1][0] = $pstack[$d-1];

      # My parent's right-most child's sibling is me,
      # if it has one.
      my $prmc = $treeinfo[$pstack[$d-1]+1][2];
      $treeinfo[$prmc+1][1] = $ix if $prmc != -1;

      # My parent's right-most child is me.
      $treeinfo[$pstack[$d-1]+1][2] = $ix;
    }
    shift @treeinfo;
  }

  return \@treeinfo;
}
push @EXPORT, qw( &hive_terp_tsurgeon_sentence_info );

	# Takes a tsurgeon output file handle and, when called in scalar
	# context  returns a ref to an array with cells being
	# [ $word, $depth, [ $tag, $tag, ... ] ].
	#
	# If called from list context, the result is a list of length
	# two.  The first element is as above; the second is the result
	# of hive_terp_tsurgeon_sentence_info() on the former.
	#
	# Discards intermediate nodes of the syntax tree and their tags
	# if the second argument is false.
sub hive_terp_sentence_from_tsurgeon_file ($$) {
	my ($FH, $ki) = @_;

	my $descending = 0;	# Last motion direction
	my @ptags = ( );	# Parser stack

	my @parsed = ( );	# Indexed by sentence position, contains
				# array refs with [word, [tag, tag, tag, ...] ]

	while ((not $descending or $#ptags >= 0) and ($_ = <$FH>)) {
		chomp;
		# Tragically not all tokens arrive on their own line.  So we
		# chomp off pieces and iterate inernally to lines as well.
		# See "redo" below.
		my $additional = "";
		if (/^\s*\((\S+)\s*(.*)$/) {
			# Contains open paren and tag and possibly other stuff.
			$additional = $2;
			my $tag = $1;
			if ($ki and not $descending and $#ptags >= 0) {
				push @parsed, [undef, $#ptags, $ptags[$#ptags]];
			}
			$descending = 0;
			push @ptags, [$tag];
		} elsif ( /^\s*([^ )]+)\)(.*)$/ ) {
			# Contains a word and a close paren
			$additional = $2;
			my $word = $1;

			$descending = 1;
			my $depth = $#ptags;
			my $tags = pop @ptags;
			push @parsed, [$word, $depth, $tags];
		} elsif ( /^\s*\)(.*)$/ ) {
			# Contains a close paren
			$additional = $1;
			$descending = 1;
			pop @ptags;
		} elsif ( /^\s*(\S+)\s*(.*)$/ ) {
			# Contains an additional tag
			warn "Tsurgeon: additional tag while descending" if $descending;
			$additional = $2;
			push @{$ptags[$#ptags]}, $1;
		} elsif ( /^\s*$/ ) {
			# Ignore a blank line
		} else {
			warn "Malformed tsurgeon fragment: '$_'";
		}

		if ($additional !~ /^\s*$/) {
			$_ = $additional;
			redo;
		}
	}

	return undef if $#ptags > 0;	# EOF while parsing
	if (defined wantarray()) {
		# Called in scalar context; give only the sentence
		return \@parsed if not wantarray();
		return ( \@parsed, hive_terp_tsurgeon_sentence_info(\@parsed) );
	}
}
push @EXPORT, qw( &hive_terp_sentence_from_tsurgeon_file );


1;
