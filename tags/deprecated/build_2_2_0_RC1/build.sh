#!/bin/sh

echo
echo "James Build System"
echo "-------------------"

export ANT_HOME=$ANT_HOME
ANT_HOME=./tools

export OLD_CLASSPATH=$CLASSPATH
CLASSPATH=phoenix-bin/lib/xercesImpl-2.0.2.jar:phoenix-bin/lib/xml-apis.jar

## Setup the Anakia stuff
if [ -d ../jakarta-site2/lib ] ; then
for i in ../jakarta-site2/lib/velocity*.jar
do
    CLASSPATH=${CLASSPATH}:$i
done
for i in ../jakarta-site2/lib/jdom*.jar
do
    CLASSPATH=${CLASSPATH}:$i
done
for i in ../jakarta-site2/lib/xerces*.jar
do
    CLASSPATH=${CLASSPATH}:$i
done
echo "Jakarta-Site2 Module Found"
fi

export CLASSPATH

chmod u+x ${ANT_HOME}/bin/antRun
chmod u+x ${ANT_HOME}/bin/ant

export PROPOSAL=""


${ANT_HOME}/bin/ant -emacs $@

export CLASSPATH=$OLD_CLASSPATH
export ANT_HOME=$OLD_ANT_HOME
