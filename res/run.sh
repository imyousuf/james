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

LOCAL_CLASSPATH=Loader.jar:../lib/AvalonAware.jar:../lib/xerces.jar:../lib/mail_1_1_3.jar:../lib/activation.jar

$JAVA_HOME/bin/java -cp $CLASSPATH:$LOCAL_CLASSPATH org.apache.avalon.loader.Main $*
