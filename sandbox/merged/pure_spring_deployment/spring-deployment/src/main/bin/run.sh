#! /bin/sh
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

# Uncomment to get enable remote debugging
# DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"

# put pid an log files in the standard unix folders, or in the current directory if we have not access
[ -w /var/log/ ] && JAMES_CONSOLE=/var/log/james.console || JAMES_CONSOLE=james.console
[ -w /var/run/ ] && JAMES_PID=/var/run/james.pid || JAMES_PID=james.pid

# Resolve soft links
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
BIN=`dirname "$PRG"`

# right now james needs to start working in the bin folder
cd $BIN
BIN=`pwd`
JAMES_HOME=`dirname "$BIN"`
[ ! -d "$JAMES_HOME/conf" -o ! -d "$JAMES_HOME/lib" ] && echo "Unable to locate JAMES_HOME" && exit 1

start_james() {
  echo 'Booting James'

  # Compose classpath joining conf and all jars in lib.
  JAMES_CP="$JAMES_HOME/conf:"`echo $JAMES_HOME/lib/*.jar | tr ' ' ':'`

  if [ -f $JAMES_PID ]
  then
    if ps -p `cat $JAMES_PID ` >/dev/null 2>/dev/null
      then
        echo "Already Running!!"
        exit 1
    fi
  fi
   
  JAMES_CMD="java $DEBUG -cp '$JAMES_CP' org.apache.james.container.spring.Main"             
  echo "START `date`" >> $JAMES_CONSOLE
  nohup sh -c "exec $JAMES_CMD >>$JAMES_CONSOLE 2>&1" >/dev/null &
  echo $! > $JAMES_PID
  echo "James pid="`cat $JAMES_PID`
}

stop_james() {
  PID=`cat $JAMES_PID 2>/dev/null`
  echo "Shutting down James: $PID"
  kill $PID 2>/dev/null || return
  sleep 5
  kill -9 $PID 2>/dev/null
  rm -f $JAMES_PID
  echo "STOPPED `date`" >>$JAMES_CONSOLE
}
            
case $1 in
 restart)
    stop_james
    sleep 5
    start_james
    ;;
 stop)  
    stop_james
    ;;
 *)
    start_james
    ;;
esac   
