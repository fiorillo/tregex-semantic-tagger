#!/usr/bin/perl

# reads a list of word classes on STDIN and generates a TERp SCALECost word
# class configuration file on STDOUT.  Takes the prefix to get to the word
# class files as its argument.
#
# All classes are configured identically with some small provision made
# for off-diagonal S/T/Y entries, which will all be overriden together.

die "Need filename prefix" if ($#ARGV < 0);
$FNAME_PREFIX=$ARGV[0];

# Scores of "default" will be omitted.

%singscores = (
	'I' => 2.0,
	'D' => 2.0,
	'M' => "default",
	'H' => "default",
	);

%msingscores = (
	'I' => ".2",
	'D' => ".5",
	'M' => "default",
	'H' => "default",
);

%singoverrides = (
	'm.name/I' => "2.0",
	'm.name/D' => "2.0",
);

%pairscores = (
	# on-diagonal S, S[OTHER], general off-diagonal S
	'S' => [ 0.1, 5.0, 5.0 ],
	'T' => [ 0.15, 5.0, 5.0 ],
	'Y' => [ 0.15, 5.0, 5.0 ],
	);

%ondiagoverrides = (
	'm.name' => "default",	# The DICE metric scales this value
	'm.conj' => ".3",
);

%otheroverrides = (
);

%offdiagoverrides = (
	'Belief/FirmBelief' => 1.0,
);

@classes = () ;

sub maybe_cost {
	print ((join " ", @_)."\n") unless $_[(scalar @_) - 1] eq "default";
}

sub sing {
	($s, $c) = @_;
	
	my $score = $singscores{$s};
	$score = $msingscores{$s} if $c =~ /^m\./;
	$score = $singoverrides{"$c/$s"} if exists $singoverrides{"$c/$s"};

	maybe_cost($s, $c, $score);
}

sub ondiag {
	($s, $c) = @_;
	my $score = ${$pairscores{$s}}[0];
	$score = $ondiagoverrides{$c} if exists $ondiagoverrides{$c};

	maybe_cost($s, $c, $c, $score);
}

sub otherdiag { 
	($s, $c) = @_;

	my $score = ${$pairscores{$s}}[1];
	$score = $otheroverrides{$c} if exists $otheroverrides{$c};

	maybe_cost($s, $c, "OTHER", $score);
	maybe_cost($s, "OTHER", $c, $score);
}

sub offdiag { 
	($s, $c) = @_;
	map {
		if( exists $offdiagoverrides{"$c/$_"} ) {
			maybe_cost($s, $c, $_, $offdiagoverrides{"$c/$_"});
			maybe_cost($s, $_, $c, $offdiagoverrides{"$c/$_"});
		} elsif ( exists $offdiagoverrides{"$_/$c"} ) {
			maybe_cost($s, $c, $_, $offdiagoverrides{"$_/$c"});
			maybe_cost($s, $_, $c, $offdiagoverrides{"$_/$c"});
		} else {
			maybe_cost($s, $c, $_, ${$pairscores{$s}}[2]);
			maybe_cost($s, $_, $c, ${$pairscores{$s}}[2]);
		}
	} @classes;
}

while($c = <STDIN>) {
	chomp $c;

	print "CLASS $c ${FNAME_PREFIX}wds.$c\n";
	map { sing($_, $c) } (keys %singscores);
	map {
		ondiag($_, $c);
		otherdiag($_, $c);
		offdiag($_, $c);
	} (keys %pairscores);

	push @classes, $c;
}
