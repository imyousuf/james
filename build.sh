#!/bin/sh

echo
echo "James Build System"
echo "-------------------"

CLASSPATH=lib/xerces.jar

for i in ../jakarta-site2/lib/*.jar
do
    CLASSPATH=${CLASSPATH}:$i
done

export CLASSPATH

chmod u+x ./tools/bin/antRun
chmod u+x ./tools/bin/ant

export PROPOSAL=""

unset ANT_HOME

./tools/bin/ant -emacs $@
