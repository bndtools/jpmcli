# Just Another Package Manager for Java
The purpose of the this command is to maintain a set of commands and services available via the command
line. It has the same purpose as [npm][1] but then for Java. The command can install java command line
commands directly from a URL or file path. The only requirement is that the JAR file is an _executable jar_
with a Main-Class header.

Originally jpm was backed by a server that indexed a lot of repositories. However, this server was shutdown because
nobody was really interested in it at the time. However, since you can also install executable JARs from the
commandline it is still very useful. Unfortunately the command has not been refactored to use Maven Central or
other Maven repos as backend.

## Install

You should first install _jpm_. To install download the JAR and run the `init` command. For example:

    $ curl https://repo1.maven.org/maven2/biz/aQute/bnd/biz.aQute.jpm.run/3.5.0/biz.aQute.jpm.run-3.5.0.jar >t.jar
    $ java -jar t.jar init
    Home dir      /Users/aqute/Library/PackageManager
    Bin  dir      /Users/aqute/Library/PackageManager/bin

After you've installed it, you should be able to do:

    $ jpm version
    1.0.0.201311261409
    $

## Help
To find your way around jpm, you can use help.

    $ jpm help
    Just Another Package Manager (for Java)
    Maintains a local repository of Java jars (apps or libs). Can automatically link
    these jars to an OS command or OS service. For more information see

    Available commands:

      ...

For each command, more extensive help is available with `help <command>`.

    $ jpm help version

    NAME
      version                     - Show the current version. The qualifier
                                    represents the build date.

    SYNOPSIS
       version 


## Installing
The first thing you are likely want to do is installing a command. bnd, the Swiss army knife for OSGi
is a good example to start. We can install bnd simply by:

    $ jpm install


## Help
jpm has extensive help information. If you just enter the word `help` then a list of commands is given.

    $ jpm help
    Maintains a local repository of Java jars (apps or libs).
    Can automatically link these jars to an OS command or OS
    service.

    Available commands: artifact, candidates, certificate,
    command, deinit, find, gc, init, install, jpm, keys, log,
    platform, put, register, restart, service, settings, setup, start,
    status, stop, trace, version, winreg

Later version can have additional commands and/or a different text. More detailed information can be obtained with:

    $ jpm help jpm
    NAME
      jpm - Options valid for all commands. Must be given before
      sub command

    SYNOPSIS
       jpm [options]  ...

    OPTIONS
       [ -b, --base <string>] - Specify a new base directory
       (default working directory).
       ...
You can also suffix the `help` command with one of the listed commands:

The help information is shown in three sections:

* NAME - Displays the name and descriptive information.
* OPTIONS - If any. Will show in detail what options can be used and what they mean. It can also contain an indication of what parameters are expected.
* SYNOPSIS - A short command line with all the options.

## Types
There are a number of recurring types used in the command line.

* url - A well known url, like 'http://bndtools.org'
* file - A path on the file system in the local standard. That is on Windows use the back slash, on other operating systems use the forward slash.

## Commands

### version
Display the version of the jpm command. This is the current version number and the qualifier is the date.

###

[1]: https://npmjs.org
