#! /bin/sh

if [ "$JAVA_HOME" = "" ] ; then
  echo "ERROR: JAVA_HOME not found in your environment."
  echo
  echo "Please, set the JAVA_HOME variable in your environment to match the"
  echo "location of the Java Virtual Machine you want to use."
  exit 1
fi
JAMES_CLASSPATH=avalon-loader.jar:../lib/xerces.jar:../lib/mail_1_2.jar:../lib/activation.jar:../lib/dnsjava.jar:../lib/town.jar

$JAVA_HOME/bin/java -cp $CLASSPATH:$JAMES_CLASSPATH $AVALON_JVM_FLAGS AvalonLoader $*
