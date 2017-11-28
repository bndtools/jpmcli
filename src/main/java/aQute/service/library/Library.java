package aQute.service.library;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import aQute.struct.*;

public interface Library {
	String					LOWEST_VERSION		= "<";
	String					HIGHEST_VERSION		= ">";
	String					EQUAL_VERSION		= "=";
	String					BASELINE_VERSION	= "~";
	static String			SIMPLE_NAME			= "[-\\p{L}0-9_.]+";
	static Pattern			SIMPLE_NAME_PATTERN	= Pattern.compile(SIMPLE_NAME);

	static Pattern			COORDINATES_P		= Pattern
														.compile(
																"\\s*([a-z0-9-_.]+)(?:\\s*:\\s*([a-z0-9-_.]+)(?:\\s*:\\s*([a-z0-9-_.]*))?)?(?:\\s*@\\s*([<>=]?[a-z0-9-_.,\\[\\]\\(\\)]+))?\\s*",
																Pattern.CASE_INSENSITIVE);
	static Pattern			SHA_P				= Pattern
														.compile("\\s*([a-f0-9]{40,40})\\s*", Pattern.CASE_INSENSITIVE);

	static final Pattern	MASTER_QUALIFIER	= Pattern
														.compile("|RELEASE|FINAL|GA|GM|GOLD", Pattern.CASE_INSENSITIVE);

	String					VERSION				= "1.0.0";
	String					OSGI_GROUP			= "osgi";
	String					SHA_GROUP			= "sha";
	String					SIMPLE_GROUP		= "";
	String					WILDCARD_VERSION	= "@*";

	enum Group {
		SIMPLE, OSGI, SHA, MAVEN
	};

	public enum Strategy {
		HIGHEST, LOWEST, BASELINE, EQUAL;
	}

	enum Phase {
		PENDING(false, false, false, "?"), STAGING(false, false, false, "\u25D1"), LOCKED(true, false, false, "\u2298"), MASTER(
				true, true, true, "\u2B24"), RETIRED(true, false, true, "\u25D0"), WITHDRAWN(true, false, true,
				"\u2297"), UNKNOWN(true, false, false, "?");

		final boolean	locked;
		final boolean	listable;
		final boolean	permanent;
		final String	symbol;
		final char		identifier;

		private Phase(boolean locked, boolean listable, boolean permanent, String symbol) {
			this.locked = locked;
			this.listable = listable;
			this.permanent = permanent;
			this.identifier = Character.toUpperCase(toString().charAt(0));
			this.symbol = symbol;
		}

		public boolean isLocked() {
			return locked;
		}

		public boolean isListable() {
			return listable;
		}

		public boolean isPermanent() {
			return permanent;
		}

		public String getSymbol() {
			return symbol;
		}

		public char getIdentifier() {
			return identifier;
		}

		public boolean isStaging() {
			return this == STAGING || this == LOCKED;
		}
	}

	interface DownloadListener {
		void success(File file) throws Exception;

		void failed(File file, String reason) throws Exception;

		boolean progress(int percentage) throws Exception;
	}

	class Info extends struct {
		public String	version	= VERSION;	// TODO must be aligned with the
											// package version
		@Define(description = "Name of the library", pattern = "[\\w_][-\\w\\d_.]*")
		public String	name;
		public URI		location;
		public String	generation;
		public long		masterUpdates;
		public long		updates;
		public String	message;
		public boolean	approved;
		public long		programs;
		public long		revisions;
		public int		fixup;
	}

	class Relocation extends struct {
		public String	groupId;
		public String	artifactId;
		public String	version;
		public String	message;
	}

	class Revision extends SharedTypes.RepoPom {
		// SHA of resource
		public byte[]				_id;
		public Set<URI>				urls			= set();
		public byte[]				md5;
		public URI					pomUrl;
		public byte[]				pom;
		// Logical name. This is Bundle Symbolic Name
		// for OSGi bundles and groupid__artifactid for
		// maven artifacts that are not OSGi
		public String				title;
		public String				bsn;
		public String				baseline;
		public String				qualifier;
		public Phase				phase			= Phase.STAGING;

		public long					size;

		public String				receipt;										// Importer
																					// receipt
		public long					created;										// date
																					// of
																					// insertion
		public long					modified;										// when
																					// phase
																					// is
																					// changed
		public String				message;
		public String				owner;											// email
		public String				releaseSummary;

		public List<String>			signers;
		public Set<String>			category		= set();

		public Map<String,Object>	metadata		= new HashMap<String,Object>();
		public URI					icon;
		public List<String>			process			= list();
		public List<Requirement>	requirements	= list();
		public List<Capability>		capabilities	= list();
		public Set<String>			hashes			= set();
		public List<String>			errors			= list();
		public List<String>			warnings		= list();
		public List<String>			packages		= list();
		public List<URI>			repositories	= list();
		public String				mainClass;
		public String				docUrl;
		public Set<String>			keywords		= set();
		public Relocation			relocation;
		public Set<String>			depos			= set();
		public int					fixup;
		public String				readme;
		/**
		 * The time to check this thing if its urls are still alive.
		 */
		public long					expire;

		@Deprecated
		// rank == 6
		public URI					url;
		@Deprecated
		public Set<URI>				alts			= set();
	}

	class Namespace extends struct {
		public String				ns;
		public Map<String,Object>	ps	= new HashMap<String,Object>();
		public String				name;
	}

	class Requirement extends Namespace {}

	class Capability extends Namespace {}

	class RevisionRef extends struct {

		public RevisionRef() {}

		public RevisionRef(Revision revision) {
			this.revision = revision._id;
			this.bsn = revision.bsn;
			this.urls.addAll(revision.urls);
			this.md5 = revision.md5;
			this.phase = revision.phase;
			this.version = revision.version;
			this.classifier = revision.classifier;
			this.baseline = revision.baseline;
			this.qualifier = revision.qualifier;
			this.releaseSummary = revision.releaseSummary;
			if (revision.scm != null)
				this.tag = revision.scm.tag;
			this.name = revision.name;
			this.title = revision.title;
			this.description = revision.description;
			this.created = revision.created;
			this.errors = revision.errors.size();
			this.groupId = revision.groupId;
			this.artifactId = revision.artifactId;
			this.size = revision.size;
		}

		public byte[]	revision;
		public Set<URI>	urls	= set();
		public byte[]	md5;
		public String	groupId;
		public String	artifactId;
		public String	version;
		public String	classifier;
		public String	packaging;
		public String	bsn;
		public String	title;
		public String	name;
		public String	description;
		public String	baseline;
		public String	qualifier;
		public String	tag;
		public Phase	phase;
		public String	releaseSummary;
		public long		created;
		public int		errors;
		public long		size;
		@Deprecated
		public URI		url;
	}

	class Wiki extends struct {
		public String	text;
		public String	author;
		public long		modified;
	}

	class Program extends struct {
		public byte[]				_id;
		public String				artifactId;
		public String				groupId;
		public URI					icon;
		public Wiki					wiki;
		public int[]				rating		= new int[5];
		public long					modified;
		public List<RevisionRef>	revisions	= new ArrayList<Library.RevisionRef>();
		public Revision				last;
		public Set<String>			category	= set();
		public Set<String>			keywords	= set();
		public Set<String>			classifiers	= set();
		public Set<String>			search		= set();
		public URI					home;

		// # of elements on the final runtime classpath (duplicates removed)
		public int					depth;

		// Number of incoming links
		public int					vote;

		// Total number of bytes on classpath for this dep (duplicates removed)
		public float				weight;

		// Coordinates of programs that require this program
		public Set<String>			inbound;
		// Coordinates that are on the collapsed transient classpath
		public Set<String>			classpath;

		// Any cycles detected (means incomplete classpath)
		public List<String>			cycles;

		// Any artifacts appearing multiple times in the transient classpath
		public Set<String>			overlap;

		/*
		 * Overall repository wide ranking
		 */
		public int					rank;

		public String				depository;
		public String				domain;
		public int					fixup;
	}

	class Category extends struct {
		public String	_id;
		public String	name;
		public String	summary;
		public Wiki		wiki;
		public URI		icon;
	}

	// void master(RevisionRef rev);

	class Response extends struct {
		public Revision		revision;
		public List<String>	errors		= new ArrayList<String>();
		public List<String>	warnings	= new ArrayList<String>();
	}

	void update(Program program) throws Exception;

	void redo(String where, String process) throws Exception;

	boolean master(String bsn) throws Exception;

	Revision delete(String bsn);

	interface Find<T> extends Iterable<T> {
		Find<T> bsn(String bsn) throws Exception;

		Find<T> baseline(String version) throws Exception;

		Find<T> version(String version) throws Exception;

		Find<T> qualifier(String qualifier) throws Exception;

		Find<T> from(long date) throws Exception;

		Find<T> until(long date) throws Exception;

		Find<T> skip(int n) throws Exception;

		Find<T> limit(int n) throws Exception;

		Find<T> ascending(String field) throws Exception;

		Find<T> descending(String field) throws Exception;

		Find<T> where(String field, Object... args) throws Exception;

		Find<T> template(Revision rev) throws Exception;

		T one() throws Exception;

		T first() throws Exception;

		int count() throws Exception;

		Find<T> query(String query) throws Exception;

		Find<T> capability(String ns, String key, Object value) throws Exception;

		boolean callback(Callback<T> visitor) throws Exception;
	}

	Find<Program> findProgram() throws Exception;

	Find<Revision> findRevision() throws Exception;

	Info getInfo() throws Exception;

	Revision scan(URI uri) throws Exception;

	Revision stage(Revision revision) throws Exception;

	/**
	 * is stage( scan(url) )
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	Revision stage(URI url) throws Exception;

	Revision rescan(Coordinate coordinates) throws Exception;

	Revision getRevision(byte[] sha) throws Exception;

	Revision getRevision(String groupId, String artifactId, String classifier, String version) throws Exception;

	/**
	 * Find a revision based on the coordinates. This will break up the
	 * coordinates in group, artifact, classifier and version. It will then get
	 * the Program, and find the best match. I.e. latest allowed version and
	 * also matching the classifier.
	 * 
	 * @param coordinates
	 *            standard coordinates
	 * @return a revision or null if not found
	 */

	Revision getRevision(Coordinate c) throws Exception;

	List< ? extends Revision> getRevisions(Coordinate coordinate) throws Exception;

	Category getCategory(String name) throws Exception;

	Iterable< ? extends Category> getCategories(String where) throws Exception;

	void update(Category c) throws Exception;

	Program getProgram(String groupId, String artifactId) throws Exception;

	Iterable< ? extends Program> getPrograms(String coordinates) throws Exception;

	/**
	 * Queue the scanning of a url. The optional sha specifies the expected sha
	 * of the artifact. If set, the scanning may be ignored if the sha is
	 * already in the database (though then the url should be
	 * 
	 * @param url
	 * @param sha
	 * @param b
	 */

	class ScanRequest extends struct {
		public URI		url;
		public boolean	unique	= false;
		public byte[]	sha		= null;
		public URI		repository;
		public String	message;
		public byte[]	user;
		public boolean	osgi;
		public Phase	phase;
		public boolean	nostage;
		public long		expire	= Long.MAX_VALUE;
	}

	void queueScan(ScanRequest rq) throws Exception;

	Revision scan(ScanRequest rq) throws Exception;

	boolean update(Program p, String... field) throws Exception;

	boolean update(Revision r, String... field) throws Exception;

	Revision stage(Revision revision, String domain) throws Exception;

	Iterable<RevisionRef> getClosure(byte[] revision, boolean optionals) throws Exception;

	Revisions createRevisions(Revisions revisions) throws Exception;

	Revisions getRevisions(byte[] id) throws Exception;

	void setPhase(byte[] id, Phase phase) throws Exception;

}
