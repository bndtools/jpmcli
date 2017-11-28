package aQute.struct;

import java.util.*;

import aQute.struct.struct.Error;

public class InvalidException extends Exception {
	private static final long	serialVersionUID	= 1L;
	private List<Error>			errors;

	public InvalidException(String why, List<struct.Error> errors) {
		super(why + ": " + errors.toString());
		this.errors = errors;
	}

	public List<Error> getErrors() {
		return errors;
	}
}
