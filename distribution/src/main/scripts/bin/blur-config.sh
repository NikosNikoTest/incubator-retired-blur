#!/usr/bin/env bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

export BLUR_HOME="$bin"/..
export BLUR_HOME_CONF=$BLUR_HOME/conf

. $BLUR_HOME/conf/blur-env.sh
if [ -z "$JAVA_HOME" ]; then
  if which java >/dev/null 2>&1 ; then
    export JAVA_HOME=`java -cp $bin/../lib/blur-util-*.jar org.apache.blur.FindJavaHome` 
  fi
fi
if [ -z "$JAVA_HOME" ]; then
  cat 1>&2 <<EOF
+======================================================================+
|      Error: JAVA_HOME is not set and Java could not be found         |
+----------------------------------------------------------------------+
| Please download the latest Sun JDK from the Sun Java web site        |
|       > http://java.sun.com/javase/downloads/ <                      |
|                                                                      |
| Hadoop and Blur requires Java 1.6 or later.                          |
| NOTE: This script will find Sun Java whether you install using the   |
|       binary or the RPM based installer.                             |
+======================================================================+
EOF
  exit 1
fi

export JAVA=$JAVA_HOME/bin/java

export BLUR_LOGS=${BLUR_LOGS:=$BLUR_HOME/logs}

if [ ! -d "$BLUR_LOGS" ]; then
  mkdir -p $BLUR_LOGS
fi

if [ ! -d "$BLUR_HOME/pids" ]; then
  mkdir -p $BLUR_HOME/pids
fi

BLUR_CLASSPATH=$BLUR_HOME/conf

for f in $BLUR_HOME/lib/*.jar; do
  BLUR_CLASSPATH=${BLUR_CLASSPATH}:$f;
done

for f in $BLUR_HOME/lib/*.war; do
  BLUR_CLASSPATH=${BLUR_CLASSPATH}:$f;
done

if [ -z "$HADOOP_HOME" ]; then
	export HADOOP_HOME=`ls -d1 $BLUR_HOME/lib/hadoop-*/ | head -1`
fi

for f in $HADOOP_HOME/hadoop*/*.jar; do
  BLUR_CLASSPATH=${BLUR_CLASSPATH}:$f;
done

for f in $HADOOP_HOME/lib/*.jar; do
  BLUR_CLASSPATH=${BLUR_CLASSPATH}:$f;
done

export BLUR_CLASSPATH

HOSTNAME=`hostname`
