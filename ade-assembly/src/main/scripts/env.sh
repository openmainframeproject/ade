#!/bin/bash
#************************************************************************
# Copyright IBM Corp. 2016                                              *
# This file is part of Anomaly Detection Engine for Linux Logs (ADE).   *
#                                                                       *
# ADE is free software: you can redistribute it and/or modify           *
# it under the terms of the GNU General Public License as published by  *
# the Free Software Foundation, either version 3 of the License, or     *
# (at your option) any later version.                                   *
#                                                                       *
# ADE is distributed in the hope that it will be useful,                *
# but WITHOUT ANY WARRANTY; without even the implied warranty of        *
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         * 
# GNU General Public License for more details.                          *
#                                                                       *
# You should have received a copy of the GNU General Public License     *
# along with ADE.  If not, see <http://www.gnu.org/licenses/>.          *
#************************************************************************

export ADE_VERSION=${project.version}

export ADE_JAVA=java

export DB_CLIENT_PATH=$HOME/db-derby-10.11.1.1-bin/lib/derbyclient.jar

export ADE_CONF=$ADE_HOME/conf

# Log implementation can be changed - defaults to log4j
export SLF4J_BINDING=$ADE_HOME/lib/slf4j-log4j12-1.7.12.jar
export LOGGER_IMPLEMENTATION=$ADE_HOME/lib/log4j-1.2.17.jar
#export LOG_CMD_OPT="-Dlog4j.configuration=log4j.properties"

export ADE_SETUP_FILE=$ADE_CONF/setup.props

export ADE_CLASSPATH=$ADE_HOME/lib/ade-core-$ADE_VERSION.jar
export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_HOME/lib/ade-ext-$ADE_VERSION.jar

export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_HOME/lib/commons-cli-1.2.jar
export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_HOME/lib/joda-time-2.3.jar
export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_HOME/lib/commons-lang3-3.4.jar
export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_HOME/lib/commons-io-2.0.1.jar
export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_HOME/lib/commons-math3-3.5.jar
export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_HOME/lib/wink-json4j-1.4.jar
export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_HOME/lib/xmlunit-1.6.jar
export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_HOME/lib/slf4j-api-1.7.12.jar
export ADE_CLASSPATH=$ADE_CLASSPATH:$SLF4J_BINDING:$LOGGER_IMPLEMENTATION

# conf dir on classpath so log config file will be found
export ADE_CLASSPATH=$ADE_CLASSPATH:$ADE_CONF

export ADE_CLASSPATH=$ADE_CLASSPATH:$DB_CLIENT_PATH

echo "ADE_CONF=$ADE_CONF"
echo "ADE_CLASSPATH=$ADE_CLASSPATH"

