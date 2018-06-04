package aQute.jpm.lib;

import aQute.service.library.Coordinate;
import aQute.service.library.Library.Program;
import aQute.service.library.Library.Revision;
import aQute.service.library.Library.RevisionRef;

import java.util.List;

public class MavenCentralLibrary {

	public MavenCentralLibrary() {
	}

	public Revision getRevisionByCoordinate(Coordinate c) {
		return null;
	}

	public Revision getRevision(byte[] sha) {
		return null;
	}

	public Iterable<RevisionRef> getClosure(byte[] _id, boolean b) {
		return null;
	}

	public List<Program> getQueryPrograms(String query, int skip, int limit) {
		return null;
	}

	public Iterable<Revision> getRevisionsByCoordinate(Coordinate c) {
		return null;
	}

}
