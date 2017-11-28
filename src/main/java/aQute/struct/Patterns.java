package aQute.struct;

import java.util.regex.*;

/**
 * Never use capturing groups so they can be used as building blocks.
 */
public interface Patterns {
	String	SIMPLE_NAME			= "[\\p{L}_][-\\p{L}0-9_]*";
	String	QUALIFIED_NAME		= "[\\p{L}_][-\\p{L}0-9_.]*";
	String	HEX					= "(?:[0-9a-fA-F][0-9a-fA-Z])+";
	String	SHA_1				= "(?:" + HEX + "){20,20}";
	String	SLASHED_PATH		= QUALIFIED_NAME + "(?:/" + QUALIFIED_NAME + ")*";
	String	NUMMERIC			= "[0-9]+";

	Pattern	SIMPLE_NAME_P		= Pattern.compile(SIMPLE_NAME);
	Pattern	QUALIFIED_NAME_P	= Pattern.compile(QUALIFIED_NAME);
	Pattern	HEX_P				= Pattern.compile(HEX);
	Pattern	SHA_1_P				= Pattern.compile(SHA_1);
	Pattern	SLASHED_PATH_P		= Pattern.compile(SLASHED_PATH);
	Pattern	NUMMERIC_P			= Pattern.compile(NUMMERIC);
}
