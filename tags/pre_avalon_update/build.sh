#!/bin/sh

echo
echo "James Build System"
echo "-------------------"

export CP=$CLASSPATH
CLASSPATH=lib/xerces.jar

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

chmod u+x ./tools/bin/antRun
chmod u+x ./tools/bin/ant

export PROPOSAL=""

unset ANT_HOME

./tools/bin/ant -emacs $@

export CLASSPATH=$CP
