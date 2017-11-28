package aQute.service.library;

import java.net.*;
import java.util.*;

import aQute.struct.*;

/**
 * *** DERIVED FROM MAVEN POM *** The following types are aligned with the types
 * in the maven POM and supported by the aQute.maven.Pom class. There is no
 * inheritance in this case because we do not need (nor want) all the details in
 * the official POM). However, by aligning the data types we can use the
 * {@link struct#merge(struct, String...)} function to merge the pom data into
 * our revision. The consequence is of course that this data must track the pom
 * data.
 */
public interface SharedTypes {

	/**
	 * Licenses are legal documents defining how and when a project (or parts of
	 * a project) may be used. Note that a project should list only licenses
	 * that may apply directly to this project, and not list licenses that apply
	 * to this project's dependencies. Maven currently does little with these
	 * documents other than displays them on generated sites. However, there is
	 * talk of flexing for different types of licenses, forcing users to accept
	 * license agreements for certain types of (non open source) projects. name,
	 * url and comments: are self explanatory, and have been encountered before
	 * in other capacities. The fourth license element is: distribution: This
	 * describes how the project may be legally distributed. The two stated
	 * methods are repo (they may be downloaded from a Maven repository) or
	 * manual (they must be manually installed).
	 */
	class License extends struct {
		public String	name;
		public URI		url;
		public String	distribution;
		public String	comments;
	}

	/**
	 * Mailing lists are a great tool for keeping in touch with people about a
	 * project. Most mailing lists are for developers and users.
	 */
	class MailingList extends struct {
		public String		name;
		/**
		 * There elements specify the email addresses which are used for
		 * performing the relative actions To subscribe to the user list above,
		 * a user would send an email to user-subscribe@127.0.0.1.
		 */
		public String		subscribe;
		/**
		 * There elements specify the email addresses which are used for
		 * performing the relative actions To subscribe to the user list above,
		 * a user would send an email to user-subscribe@127.0.0.1.
		 */
		public String		unsubscribe;
		/**
		 * The email address which one would use in order to post to the mailing
		 * list. Note that not all mailing lists have the ability to post to
		 * (such as a build failure list)
		 */
		public String		post;
		/**
		 * This element specifies the url of the archive of old mailing list
		 * emails, if one exists.
		 */
		public String		archive;
		/**
		 * If there are mirrored archives, they can be specified under
		 * otherArchives.
		 */
		public List<String>	otherArchives;
	}

	/**
	 * Contributors are like developers yet play an ancillary role in a
	 * project's lifecycle. Perhaps the contributor sent in a bug fix, or added
	 * some important documentation. A healthy open source project will likely
	 * have more contributors than developers.
	 */
	class Contributor extends struct {
		/**
		 * These correspond to the developer's unique ID across an organization.
		 */
		public String		id;
		/**
		 * These correspond to the developer's normal name.
		 */
		public String		name;
		/**
		 * These correspond to the developer's email.
		 */
		public String		email;
		/**
		 * As you probably guessed, these are the developer's organization name.
		 */
		public String		organization;
		/**
		 * Organization's website
		 */
		public URI			organizationUrl;
		/**
		 * : A role should specify the standard actions that the person is
		 * responsible for. Like a single person can wear many hats, a single
		 * person can take on multiple roles.
		 */
		public List<String>	roles = list();

		/**
		 * A numerical offset in hours from GMT where the developer lives.
		 */
		public long			timezone;

		/**
		 * properties: This element is where any other properties about the
		 * person goes. For example, a link to a personal image or an instant
		 * messenger handle. Different plugins may use these properties, or they
		 * may simply be for other developers who read the POM.
		 */
		Map<String,String>	properties;
	}

	/**
	 * All projects consist of files that were created, at some time, by a
	 * person. Like the other systems that surround a project, so to do the
	 * people involved with a project have a stake in the project. Developers
	 * are presumably members of the project's core development. Note that,
	 * although an organization may have many developers (programmers) as
	 * members, it is not good form to list them all as developers, but only
	 * those who are immediately responsible for the code. A good rule of thumb
	 * is, if the person should not be contacted about the project, they need
	 * not be listed here.
	 */
	class Developer extends Contributor {}

	/**
	 * This defines the defect tracking system (Bugzilla, TestTrack, ClearQuest,
	 * etc) used. Although there is nothing stopping a plugin from using this
	 * information for something, its primarily used for generating project
	 * documentation.
	 */
	class IssueManagement extends struct {
		public String	system;
		public URI		url;
	}

	class Configuration extends struct {
		public String	address;
	}

	/**
	 * A notifier is the manner in which people are notified of certain build
	 * statuses. In the following example, this POM is setting a notifier of
	 * type mail (meaning email), and configuring the email address to use on
	 * the specified triggers sendOnError, sendOnFailure, and not sendOnSuccess
	 * or sendOnWarning.
	 */
	class Notifier extends struct {
		public String			type;
		public boolean			sendOnError;
		public boolean			sendOnFailure;
		public boolean			sendOnSuccess;
		public boolean			sendOnWarning;
		public Configuration	configuration;
	}

	/**
	 * Continuous integration build systems based upon triggers or timings (such
	 * as, hourly or daily) have grown in favor over manual builds in the past
	 * few years. As build systems have become more standardized, so have the
	 * systems that run the trigger those builds. Although the majority of the
	 * configuration is up to the specific program used (Continuum, Cruise
	 * Control, etc.), there are a few configurations which may take place
	 * within the POM. Maven has captured a few of the recurring settings within
	 * the set of notifier elements.
	 */
	class CiManagement extends struct {
		public String			system;
		public URI				url;
		public List<Notifier>	notifiers	= list();
	}

	/**
	 * SCM (Software Configuration Management, also called Source Code/Control
	 * Management or, succinctly, version control) is an integral part of any
	 * healthy project. If your Maven project uses an SCM system (it does,
	 * doesn't it?) then here is where you would place that information into the
	 * POM.
	 */
	class SCM extends struct {
		/**
		 * The two connection elements convey to how one is to connect to the
		 * version control system through Maven. Where connection requires read
		 * access for Maven to be able to find the source code (for example, an
		 * update), developerConnection requires a connection that will give
		 * write access. The Maven project has spawned another project named
		 * Maven SCM, which creates a common API for any SCMs that wish to
		 * implement it. The most popular are CVS and Subversion, however, there
		 * is a growing list of other supported SCMs. All SCM connections are
		 * made through a common URL structure.
		 * 
		 * <pre>
		 * scm:[provider]:[provider_specific]
		 * </pre>
		 * 
		 * Where provider is the type of SCM system. For example, connecting to
		 * a CVS repository may look like this:
		 * {@code scm:cvs:pserver:127.0.0.1:/cvs/root:my-project}
		 */
		public String	connection;
		/**
		 * See {@link #connection} but then for developers.
		 */
		public String	developerConnection;

		/**
		 * Specifies the tag that this project lives under. HEAD (meaning, the
		 * SCM root) should be the default.
		 */
		public String	tag;
		/**
		 * A publicly browsable repository. For example, via ViewCVS. This
		 * should be a URI but unfortunately, it is too often a git reference or
		 * plain text.
		 */
		public String	url;
	}

	/**
	 * Most projects are run by some sort of organization (business, private
	 * group, etc.). Here is where the most basic information is set.
	 */
	class Organization extends struct {
		public String	name;
		public URI		url;
	}

	/**
	 * The real POM conflates the idea of an artifact description (the role it
	 * fulfills in the repo) and the role of a build recipe. In out library
	 * model we are not interested in the recipe, only the descriptive
	 * information.
	 */
	class RepoPom extends struct {
		/**
		 * Must be 4.0.0
		 */
		public String				modelVersion;
		public String				groupId;
		public String				artifactId;
		public String				version;
		public String				classifier;
		public String				packaging;

		/**
		 * name: Projects tend to have conversational names, beyond the
		 * artifactId. The Sun engineers did not refer to their project as
		 * "java-1.5", but rather just called it "Tiger". Here is where to set
		 * that value.
		 */
		public String				name;
		/**
		 * Licenses are legal documents defining how and when a project (or
		 * parts of a project) may be used. Note that a project should list only
		 * licenses that may apply directly to this project, and not list
		 * licenses that apply to this project's dependencies. Maven currently
		 * does little with these documents other than displays them on
		 * generated sites. However, there is talk of flexing for different
		 * types of licenses, forcing users to accept license agreements for
		 * certain types of (non open source) projects.
		 */

		public List<License>		licenses		= list();
		/**
		 * description: Description of a project is always good. Although this
		 * should not replace formal documentation, a quick comment to any
		 * readers of the POM is always helpful.
		 */
		public String				description;

		/**
		 * inceptionYear: This is another good documentation point. It will at
		 * least help you remember where you have spent the last few years of
		 * your life.
		 */
		public int					inceptionYear;

		/**
		 * Most projects are run by some sort of organization (business, private
		 * group, etc.). Here is where the most basic information is set.
		 */
		public Organization			organization;

		/**
		 * All projects consist of files that were created, at some time, by a
		 * person. Like the other systems that surround a project, so to do the
		 * people involved with a project have a stake in the project.
		 * Developers are presumably members of the project's core development.
		 * Note that, although an organization may have many developers
		 * (programmers) as members, it is not good form to list them all as
		 * developers, but only those who are immediately responsible for the
		 * code. A good rule of thumb is, if the person should not be contacted
		 * about the project, they need not be listed here.
		 */
		public List<Developer>		developers		= list();

		/**
		 * Contributors are like developers yet play an ancillary role in a
		 * project's lifecycle. Perhaps the contributor sent in a bug fix, or
		 * added some important documentation. A healthy open source project
		 * will likely have more contributors than developers.
		 */
		public List<Contributor>	contributors	= list();

		/**
		 * This defines the defect tracking system (Bugzilla, TestTrack,
		 * ClearQuest, etc) used. Although there is nothing stopping a plugin
		 * from using this information for something, its primarily used for
		 * generating project documentation.
		 */
		public IssueManagement		issueManagement;

		/**
		 * Mailing lists are a great tool for keeping in touch with people about
		 * a project. Most mailing lists are for developers and users.
		 */

		public List<MailingList>	mailingLists	= list();

		/**
		 * SCM (Software Configuration Management, also called Source
		 * Code/Control Management or, succinctly, version control) is an
		 * integral part of any healthy project. If your Maven project uses an
		 * SCM system (it does, doesn't it?) then here is where you would place
		 * that information into the POM.
		 */
		public SCM					scm;

	}
}
