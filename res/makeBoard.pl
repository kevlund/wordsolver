#!/usr/bin/perl

@letters = ('a'..'z');
$height = @ARGV[0];
$width = @ARGV[1];

for($i = 0; $i < ($height); $i++) {
    for($j = 0; $j < ($width); $j++) {
	   print $letters[int(rand(25))]." ";
	}
	print "\n";
}
