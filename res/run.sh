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

LOCAL_CLASSPATH=Avalon.jar;JAMES.jar;../lib/activation.jar;../lib/mail.jar;../lib/xerces_1_0_1.jar;../lib/dnsjava.jar

$JAVA_HOME/bin/java -cp $CLASSPATH:$LOCAL_CLASSPATH org.apache.avalon.Main $*
