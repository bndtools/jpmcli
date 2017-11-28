package aQute.service.library;

import java.util.*;
import java.util.regex.*;

import aQute.lib.hex.*;
import aQute.service.library.Library.Group;
import aQute.service.library.Library.Phase;
import aQute.service.library.Library.Revision;
import aQute.service.library.Library.RevisionRef;
import aQute.struct.*;

/**
 * A core concept in the Library is the coordinate, it designates a range of revisions or programs. The syntax of the Coordinate is:
 * 
 * <pre>
 * coordinate ::= group [ ':' artifact [ ':' (classifier | ) ]] ['@' [ strategy ] [ '[' phases ']' ] version ]
 * group      ::= SIMPLE_NAME
 * artifact   ::= SIMPLE_NAME
 * classifier ::= SIMPLE_NAME
 * strategy   ::= '<' | '>' | '=' | '~'
 * phases     ::= '*' | phase+
 * phase      ::= 'P'|'L'|'S'|'M'|'R'|'W'
 * version    ::= {@code maven-version}
 * </pre>
 */
public class Coordinate {
	public static String	$artifact				= Patterns.HEX + "|" + Library.SIMPLE_NAME,
			$group = Library.SIMPLE_NAME;
	public static String	$classifier				= "(?:" + Library.SIMPLE_NAME + ")";
	public static String	$baseline				= "\\d+(?:\\.\\d+(?:\\.\\d+)?)?";
	public static String	$qualifier				= "[-\\w\\d\\._]*";
	public static String	$version				= "(?:@" //
															+ "((" + $baseline + ")(\\."
															+ $qualifier
															+ ")?)?([*=~!])?)?";

	// @formatter:off

	public static String	$coordinate				= "(" + $group + ")(?::(" + $artifact + ")(?::(" + $classifier
															+ ")?)?)?" + $version;
	// @formatter:on
	public static Pattern	COORDINATE_P			= Pattern.compile($coordinate, Pattern.CASE_INSENSITIVE);
	public static Pattern	COORDINATE_VERSION_P	= Pattern.compile($version, Pattern.CASE_INSENSITIVE);
	final Group				group;
	final String			groupId;
	final String			artifactId;
	final String			classifier;
	final String			version;
	final String			baseline;
	final String			qualifier;
	final String			coordinate;
	final Set<Phase>		phases;
	private boolean			exact;

	public boolean isValid(String coordinate) {
		return COORDINATE_P.matcher(coordinate.replaceAll("__", ":")).matches();
	}

	public Coordinate(String coordinate) {
		this(COORDINATE_P.matcher(coordinate.replaceAll("__", ":")));
	}

	public Coordinate(Matcher matcher) {
		if (!matcher.matches())
			throw new IllegalArgumentException(matcher.group() + " does not match coordinate pattern: "
					+ matcher.pattern().pattern());
		this.coordinate = matcher.group(0);
		String groupId = matcher.group(1);
		String artifactId = matcher.group(2);
		String classifier = matcher.group(3);
		String version = matcher.group(4);
		String baseline = matcher.group(5);
		String qualifier = matcher.group(6);
		String modifier = matcher.group(7);
		
		// We accept single SHAs and single names as valid
		// identifiers. Rewrite the coordinate in that case

		if (artifactId == null && classifier == null) {
			artifactId = groupId;
			if (Library.SHA_P.matcher(groupId).matches()) {
				groupId = Library.SHA_GROUP;
				version = "0.0.0";
				baseline = "0.0.0";
			} else {
				groupId = Library.SIMPLE_GROUP;
			}
		}

		if (Library.SHA_GROUP.equals(groupId)) {
			this.group = Group.SHA;
		} else if (Library.OSGI_GROUP.equals(groupId)) {
			this.group = Group.OSGI;
		} else if (Library.SIMPLE_GROUP.equals(groupId)) {
			this.group = Group.SIMPLE;
		} else {
			this.group = Group.MAVEN;
		}

		if (this.group == Group.SHA) {
			Matcher m = Patterns.SHA_1_P.matcher(artifactId);
			if (!m.matches())
				throw new IllegalArgumentException("Not a valid SHA-1 " + artifactId);
		}

		if ( modifier == null)
			modifier = "=";
		
		switch( modifier.charAt(0)) {
			default:
			case '=':
				phases = EnumSet.of(Phase.MASTER);
				exact = true;
				break;
			case '*':
				phases = EnumSet.of(Phase.LOCKED, Phase.MASTER, Phase.STAGING);
				exact = false;
				break;
			case '~':
				phases = EnumSet.allOf(Phase.class);
				exact = false;
				break;
			case '!':
				phases = EnumSet.of(Phase.WITHDRAWN, Phase.RETIRED, Phase.UNKNOWN, Phase.PENDING);
				exact = false;
				break;
		}
		
		
		this.exact = "=".equals(modifier);
		this.groupId = groupId;
		this.artifactId = artifactId;
		if (classifier == null || classifier.isEmpty())
			this.classifier = null;
		else
			this.classifier = classifier;

		if (qualifier != null && !qualifier.isEmpty()) {
			if (qualifier.startsWith("."))
				this.qualifier = qualifier.substring(1);
			else
				this.qualifier = qualifier;
		} else {
			this.qualifier = null;
		}

		if (version != null) {
			String parts[] = baseline.split("\\.");
			long[] nrs = new long[3];
			for (int i = 0; i < parts.length; i++)
				nrs[i] = Long.parseLong(parts[i]);

			this.baseline = nrs[0] + "." + nrs[1] + "." + nrs[2];
		} else
			this.baseline = null;

		this.version = version;
	}

	public Coordinate(String groupId, String artifactId, String classifier, String version) {
		this(construct(groupId, artifactId, classifier, version, true, false));
	}

	public Coordinate(String groupId, String artifactId) {
		this(construct(groupId, artifactId, null, null, false, false));
	}

	public Coordinate(Revision revision) {
		this(construct(revision.groupId, revision.artifactId, revision.classifier, revision.version, true, true));
	}

	public Coordinate(String groupId, String artifactId, String classifier) {
		this(construct(groupId, artifactId, classifier, null, false, false));
	}

	public Coordinate(RevisionRef r) {
		this(construct(r.groupId, r.artifactId, r.classifier, r.version, true, false));
	}

	public static String construct(String groupId, String artifactId, String classifier, String version, boolean exact,
			boolean staging) {
		StringBuilder sb = new StringBuilder();
		sb.append(groupId).append(":").append(artifactId);
		if (!(classifier == null || classifier.isEmpty()))
			sb.append(":").append(classifier);
		if (version != null || exact || staging) {
			sb.append("@");
			if (version != null)
				sb.append(version);
			if (exact)
				sb.append('=');
			else if (staging)
				sb.append('*');
		}
		return sb.toString();
	}

	public String getGroupId() {
		return groupId;
	}

	public boolean isSha() {
		return group == Group.SHA;
	}

	public byte[] getSha() {
		assert isSha();
		return Hex.toByteArray(artifactId);
	}

	public boolean isVisible(Phase phase) {
		return phases.contains(phase);
	}

	public boolean isExact() {
		return exact;
	}

	public Group getGroup() {
		return group;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getVersion() {
		return version;
	}

	public String getBaseline() {
		return baseline;
	}

	public String getQualifier() {
		return qualifier;
	}

	public boolean hasClassifier() {
		return classifier != null;
	}

	public String toString() {
		return coordinate;
	}

	public Set<Phase> getPhases() {
		return phases;
	}
}
