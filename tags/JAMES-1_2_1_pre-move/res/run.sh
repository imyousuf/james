#!/bin/sh
echo
echo "Avalon runner"
echo

if [ "$JAVA_HOME" = "" ] ; then
  echo "ERROR: JAVA_HOME not found in your environment."
  echo
  echo "Please, set the JAVA_HOME variable in your environment to match the"
  echo "location of the Java Virtual Machine you want to use."
  exit 1
fi

if [ ! -f Loader.jar ] ; then
  echo "ERROR: Loader.jar not found."
  echo
  echo "Please execute this 'run.sh' script from within the bin directory"
  echo "by moving to the 'bin' directory and executing './run.sh'."
  echo "If you obtained the source from CVS, build with the 'dist' target"
  echo "then copy the JAMES zip or tar file to a working directory and unbundle it."
  exit 1
fi

LOCAL_CLASSPATH=Loader.jar:../lib/AvalonAware.jar:../lib/xerces.jar:../lib/mail_1_2.jar:../lib/activation.jar

$JAVA_HOME/bin/java -cp $CLASSPATH:$LOCAL_CLASSPATH org.apache.avalon.loader.Main $*
