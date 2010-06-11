#! /bin/sh

echo
echo "James Build System"
echo "-------------------"

export CLASSPATH=`echo build/lib/*.jar | tr ' ' ':'`

chmod u+x $PWD/bin/antRun
chmod u+x $PWD/bin/ant

export PROPOSAL=""

unset ANT_HOME

if [ "$1" = "proposal" ]; then
    export PROPOSAL="-buildfile proposal/make/proposal.xml"
fi

$PWD/bin/ant -emacs $PROPOSAL $@ | awk -f $PWD/bin/fixPath.awk
