#! /bin/sh

echo
echo "James Build System"
echo "-------------------"

export CLASSPATH=lib/xerces.jar:lib/velocity-0.72.jar:lib/jdom-b5.jar

chmod u+x ./tools/bin/antRun
chmod u+x ./tools/bin/ant

export PROPOSAL=""

unset ANT_HOME

./tools/bin/ant -emacs $@
