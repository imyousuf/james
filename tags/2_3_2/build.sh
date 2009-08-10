#!/bin/sh
#
################################################################
# Licensed to the Apache Software Foundation (ASF) under one   #
# or more contributor license agreements.  See the NOTICE file #
# distributed with this work for additional information        #
# regarding copyright ownership.  The ASF licenses this file   #
# to you under the Apache License, Version 2.0 (the            #
# "License"); you may not use this file except in compliance   #
# with the License.  You may obtain a copy of the License at   #
#                                                              #
#   http://www.apache.org/licenses/LICENSE-2.0                 #
#                                                              # 
# Unless required by applicable law or agreed to in writing,   #
# software distributed under the License is distributed on an  #
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       #
# KIND, either express or implied.  See the License for the    #
# specific language governing permissions and limitations      #
# under the License.                                           #
################################################################  
#
echo
echo "James Build System"
echo "-------------------"

export OLD_ANT_HOME=$ANT_HOME

ANT_HOME=./tools
export ANT_HOME

OLD_CLASSPATH=$CLASSPATH
export OLD_CLASSPATH

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

PROPOSAL=""
export PROPOSAL


${ANT_HOME}/bin/ant -emacs $@

CLASSPATH=$OLD_CLASSPATH
export CLASSPATH

ANT_HOME=$OLD_ANT_HOME
export ANT_HOME
