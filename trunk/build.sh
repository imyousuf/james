#! /bin/sh

echo
echo "James Build System"
echo "-------------------"

export CLASSPATH=lib/xerces.jar

chmod u+x ./tools/bin/antRun
chmod u+x ./tools/bin/ant

export PROPOSAL=""

unset ANT_HOME

./tools/bin/ant -emacs $@
