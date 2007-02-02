#! /bin/sh
#
# -----------------------------------------------------------------------------
# Phoenix start script.
#
# Author: Alexis Agahi <alag@users.sourceforge.net>
#         Peter Donald <peter at apache.org>
#
# Environment Variable Prequisites
#
#   PHOENIX_OPTS       (Optional) Java runtime options used when the command is
#                      executed.
#
#   PHOENIX_TMPDIR     (Optional) Directory path location of temporary directory
#                      the JVM should use (java.io.tmpdir).  Defaults to
#                      $PHOENIX_BASE/temp.
#
#   JAVA_HOME          Must point at your Java Development Kit installation.
#
#   PHOENIX_JVM_OPTS   (Optional) Java runtime options used when the command is
#                       executed.
#
#   PHOENIX_KILLDELAY  (Optional) When shutting the server this script sends s
#                      SIGTERM signal then delays for a time before forcefully
#                      shutting down the process if it is still alive. This
#                      variable controls the delay and defaults to 5 (seconds)
#
# -----------------------------------------------------------------------------
JSVC=./jsvc
USER=nobody
PIDFILE=/var/run/james.pid


usage()
{
    echo "Usage: $0 {start|stop}"
    exit 1
}

[ $# -gt 0 ] || usage

##################################################
# Get the action & configs
##################################################

ACTION=$1
shift
ARGS="$*"



# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

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

#setup time between signals to kill phoenix 
if [ -z "$PHOENIX_KILLDELAY" ] ; then
  PHOENIX_KILLDELAY=5
fi
      

unset THIS_PROG

if [ -r "$PHOENIX_HOME"/bin/setenv.sh ]; then
  . "$PHOENIX_HOME"/bin/setenv.sh
fi

# Checking for JAVA_HOME is required on *nix due
# to some distributions stupidly including kaffe in /usr/bin
if [ "$JAVA_HOME" = "" ] ; then
  echo "ERROR: JAVA_HOME not found in your environment."
  echo
  echo "Please, set the JAVA_HOME variable in your environment to match the"
  echo "location of the Java Virtual Machine you want to use."
  exit 1
fi

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
  PHOENIX_TMPDIR=`cygpath --path --windows "$PHOENIX_TMPDIR"`
fi

# ----- Execute The Requested Command -----------------------------------------

echo "Using PHOENIX_HOME:   $PHOENIX_HOME"
echo "Using PHOENIX_TMPDIR: $PHOENIX_TMPDIR"
echo "Using JAVA_HOME:      $JAVA_HOME"

# Uncomment to get enable remote debugging
# DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"
#
# Command to overide JVM ext dir
#
# This is needed as some JVM vendors do foolish things
# like placing jaxp/jaas/xml-parser jars in ext dir
# thus breaking Phoenix
#
JVM_EXT_DIRS="$PHOENIX_HOME/lib:$PHOENIX_HOME/tools/lib"
if $cygwin; then
  JVM_EXT_DIRS=`cygpath --path --windows "$JVM_EXT_DIRS"`
fi
JVM_OPTS="-Djava.ext.dirs=$JVM_EXT_DIRS"

if [ "$PHOENIX_SECURE" != "false" ] ; then
  # Make phoenix run with security manager enabled
  JVM_OPTS="$JVM_OPTS -Djava.security.manager"
fi

# change to the bin directory
cd $PHOENIX_HOME/bin

# Get the run cmd
RUN_CMD="$JSVC -user $USER -pidfile $PIDFILE $JVM_OPTS -outfile $PHOENIX_HOME/logs/james.out -errfile $PHOENIX_HOME/logs/james.err \
    $JVM_OPTS \
    $DEBUG \
    -Djava.security.policy=jar:file:$PHOENIX_HOME/bin/phoenix-loader.jar!/META-INF/java.policy \
    $PHOENIX_JVM_OPTS \
    -Dphoenix.home="$PHOENIX_HOME" \
    -Djava.io.tmpdir="$PHOENIX_TMPDIR" \
    -cp "$PHOENIX_HOME/bin/phoenix-loader.jar:$PHOENIX_HOME/bin/commons-daemon.jar:$PHOENIX_HOME/bin/phoenix-daemon-loader-0.1.jar" org.apache.avalon.phoenix.launcher.CommonsDaemonLauncher $*"


case "$ACTION" in
  start)
    #
    echo "Starting James Server"
    $RUN_CMD
    ;;

  stop)
    #
    echo "Stopping James Server"
    kill `cat $PIDFILE`
    ;;

*)
        usage
        ;;
esac

exit 0



