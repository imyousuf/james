#! /bin/sh
#
# -----------------------------------------------------------------------------
# Phoenix start script.
#
# Author: Peter Donald <peter@apache.org>

# Environment Variable Prequisites
#
#   PHOENIX_OPTS       (Optional) Java runtime options used when the command is 
#                      executed.
#
#   PHOENIX_TMPDIR     (Optional) Directory path location of temporary directory
#                      the JVM should use (java.io.tmpdir).  Defaults to
#                      $CATALINA_BASE/temp.
#
#   JAVA_HOME          Must point at your Java Development Kit installation.
#
#   PHOENIX_JVM_OPTS   (Optional) Java runtime options used when the command is 
#                       executed.
#
# -----------------------------------------------------------------------------

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

# Checking for JAVA_HOME is required on *nix due
# to some distributions stupidly including kaffe in /usr/bin
if [ "$JAVA_HOME" = "" ] ; then
  echo "ERROR: JAVA_HOME not found in your environment."
  echo
  echo "Please, set the JAVA_HOME variable in your environment to match the"
  echo "location of the Java Virtual Machine you want to use."
  exit 1
fi

# resolve links - $0 may be a softlink
THIS_PROG="$0"

while [ -h "$THIS_PROG" ]; do
  ls=`ls -ld "$THIS_PROG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    THIS_PROG="$link"
  else
    THIS_PROG=`dirname "$THIS_PROG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$THIS_PROG"`
PHOENIX_HOME=`cd "$PRGDIR/.." ; pwd`

unset THIS_PROG

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$PHOENIX_HOME" ] && PHOENIX_HOME=`cygpath --unix "$PHOENIX_HOME"`
fi

if [ -z "$PHOENIX_TMPDIR" ] ; then
  # Define the java.io.tmpdir to use for Phoenix
  PHOENIX_TMPDIR="$PHOENIX_HOME"/temp
  mkdir -p "$PHOENIX_TMPDIR"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  PHOENIX_HOME=`cygpath --path --windows "$PHOENIX_HOME"`
fi

# ----- Execute The Requested Command -----------------------------------------

echo "Using PHOENIX_HOME:   $PHOENIX_HOME"
echo "Using PHOENIX_TMPDIR: $PHOENIX_TMPDIR"
echo "Using JAVA_HOME:      $JAVA_HOME"

#
# Command to overide JVM ext dir
#
# This is needed as some JVM vendors do foolish things
# like placing jaxp/jaas/xml-parser jars in ext dir
# thus breaking Phoenix
#
JVM_OPTS="-Djava.ext.dirs=$PHOENIX_HOME/lib"

if [ "$PHOENIX_SECURE" != "false" ] ; then
  # Make phoenix run with security manager enabled
  JVM_OPTS="$JVM_OPTS -Djava.security.manager"
fi

# Kicking the tires and lighting the fires!!!
$JAVA_HOME/bin/java $JVM_OPTS \
    $JVM_OPTS \
    -Djava.security.policy=jar:file:$PHOENIX_HOME/bin/phoenix-loader.jar!/META-INF/java.policy \
    $PHOENIX_JVM_OPTS \
    -Dphoenix.home="$PHOENIX_HOME" \
    -Djava.io.tmpdir="$PHOENIX_TMPDIR" \
    -jar "$PHOENIX_HOME/bin/phoenix-loader.jar" $*
