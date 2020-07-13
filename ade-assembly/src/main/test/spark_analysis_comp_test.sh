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

#************************************************************************
# Script: analysis_comp_test.sh
#
# Usage: analysis_comp_test.sh
#
# This script will upload/train/analyze a baseline set of syslog files 
# so that the resulting analysis ouput (xml files) can be compared to the 
# baseline. The script is intended to be run after any change to the 
# analytics code to point any changes from the existing baseline. Changes 
# to the local constants below should be made to customize to your 
# environment.
#
#********************************************************************************

ADE_HOME=`dirname "$0"`/../..  # assumes <ade dir>/bin/test/analysis_comp_test.sh
ADE_HOME=`cd "$ADE_HOME" && pwd`

#***********************************************
# local constants
#***********************************************
BASELINE_DIR="$ADE_HOME/baseline"
BASELINE_UPLOAD_DIR="$BASELINE_DIR/spark/upload"
BASELINE_ANALYZE_DIR="$BASELINE_DIR/spark/analyze"
BASELINE_OUTPUT_DIR="$BASELINE_DIR/output"

BIN_DIR="$ADE_HOME/bin"

ANALYSIS_COMPARE_LOG="/tmp/spark_compare_`date "+%Y%m%d%H%M%S"`.out"

#***********************************************
# analysis group constants
#***********************************************
AG_PREFIX="regression_ag_"
AG_NAME="$AG_PREFIX`date "+%Y%m%d%H%M%S"`"
AG_JSON_DEF_FILENAME="/tmp/reg_ag.json"
AG_JSON_DEF="{ \
 \"groups\":{\"modelgroups\":[{\"name\" : \"$AG_NAME\", \"dataType\": \"syslog\", \"evaluationOrder\" : 1, \"ruleName\" : \"default\"}]}, \
 \"rules\":[{\"name\" : \"default\", \"description\" : \"regression test rule to match all systems\", \"membershipRule\" : \"*\" }] \
}"

#***********************************************
# Constants pointing to properties in setup 
# file (conf/setup.props)
#***********************************************
DB_URL_PROP="ade.databaseUrl"
OUTDIR_PROP="ade.outputPath"
ANALYSIS_OUTDIR_PROP="ade.analysisOutputPath"

#***********************************************
# sub-routines
#***********************************************
issue_command() {
  cmd=$@

  echo "**********************************"
  echo "CMD = $cmd"

  eval "$cmd >/tmp/cmdout 2>&1"
  rc=$?

  COMMAND_OUT=$(cat /tmp/cmdout)
  echo "RC: $rc"
  echo "$COMMAND_OUT"
  echo "**********************************"
  rm /tmp/cmdout

  return $rc
}

get_current_prop_val() {
  prop_name=$1

  if [ -z $prop_name ]; then
    echo "get_current_prop_val: no property name given"
    return 1
  fi
  
  prop_val=`grep "$prop_name=" $ADE_SETUP_FILE | cut -d \= -f 2`

  if [ -z prop_val ]; then
    echo "get_current_prop_val: unable to get property value"
    return 2
  fi
  
  echo "$prop_val"
}

update_setup_file() {
  # if backup not created yet do it
  if [ ! -f $ADE_SETUP_FILE.bak ]; then
    cp $ADE_SETUP_FILE $ADE_SETUP_FILE.bak
  fi

  prop_name=$1
  prop_val=$2

  if [ -z $prop_name ]; then
    echo "update_setup_file: no property name given."
    return 1
  elif [ -z $prop_val ]; then
    echo "update_setup_file: no property value."
    return 1 
  fi

  grep "$prop_name=" $ADE_SETUP_FILE
  if [ $? -ne 0 ]; then
    echo "update_setup_file: property $prop_name not found in $ADE_SETUP_FILE"
    return 1
  fi

  echo "update_setup_file: changing $prop_name to $prop_val"
  tmpfilename="/tmp/`basename $ADE_SETUP_FILE`"
  CMD=`sed "s~\(${prop_name}=\).*\$~\1\${prop_val}~" $ADE_SETUP_FILE > $tmpfilename`
  mv -f $tmpfilename $ADE_SETUP_FILE

  return 0
}

# The database name and output directory are defined in 
# the setup file (conf/setup.props). In order to prevent
# contaminating the current database and output directory
# while running the test this method will change the 
# values in the setup file.
change_dbname_and_output_dir() {
  curr_db_val=$( get_current_prop_val $DB_URL_PROP )
  if [ -z curr_db_val ]; then
    echo "Failed retrieving current database name value"
    return 1
  fi
  echo "current database value: $curr_db_val"
  

  curr_outdir_val=$( get_current_prop_val $OUTDIR_PROP )
  if [ -z curr_outdir_val ]; then
    echo "Failed retrieving current output directory value"
    return 1
  fi
  echo "current output directory value: $curr_outdir_val"

  # in setup.props change ade.databaseUrl to temp value
  test_db_name="${curr_db_val}_regtest`date "+%Y%m%d%H%M%S"`"
  update_setup_file "$DB_URL_PROP" $test_db_name
  if [ $? -ne 0 ]; then
    return 1
  fi
 
  # in setup.props change ade.outputPath value to temp value
  test_outdir_name="${curr_outdir_val}regtest`date "+%Y%m%d%H%M%S"`"
  update_setup_file "$OUTDIR_PROP" $test_outdir_name
  if [ $? -ne 0 ]; then
    return 1
  fi

  # in setup.props change ade.analysisOutputPath to temp value
  test_analysis_outdir_name="$test_outdir_name/continuous"
  update_setup_file "$ANALYSIS_OUTDIR_PROP" $test_analysis_outdir_name
  if [ $? -ne 0 ]; then
    return 1
  fi
  
  return 0
}

check_test_env() {
  if [ ! -d "$BASELINE_DIR" ]; then
    echo "ERROR: Unable to locate baseline directory: $BASELINE_DIR"
    exit 1
  fi

  if [ ! -d "$BASELINE_UPLOAD_DIR" ]; then
    echo "ERROR: Unable to locate baseline test spark upload directory: $BASELINE_UPLOAD_DIR"
    exit 1
  fi

  if [ ! -d "$BASELINE_ANALYZE_DIR" ]; then
    echo "ERROR: Unable to locate baseline test spark analyze directory: $BASELINE_ANALYZE_DIR"
    exit 1
  fi

  if [ ! -d "$BASELINE_OUTPUT_DIR" ]; then
    echo "ERROR: Unable to locate baseline output directory: $BASELINE_OUTPUT_DIR"
    exit 1
  fi

  if [ ! -d "$BIN_DIR" ]; then
    echo "ERROR: Unable to find location of bin directory: $BIN_DIR"
    exit 1
  fi

  return 0
}

cleanup_and_exit() {
  exit_code=$1

  if [ -z $exit_code ]; then
    exit_code=0
  fi

  echo "Performing test cleanup..."

  # reset db name and output directory (restore conf/setup.props)
  if [ -f $ADE_SETUP_FILE.bak ]; then
    mv $ADE_SETUP_FILE $ADE_SETUP_FILE.regtest
    mv $ADE_SETUP_FILE.bak $ADE_SETUP_FILE
  fi

  echo "Completed with RC=$exit_code"
  exit $exit_code
}

###################
# main
###################
# change to ADE_HOME dir because setup.props contains relative paths
eval "cd $ADE_HOME"

. bin/env.sh

check_test_env

# decompress any compressed baseline files
find $BASELINE_OUTPUT_DIR -maxdepth 1 -type f -name "*.tgz" -exec tar -zxf {} --directory=$BASELINE_OUTPUT_DIR \;

# Change the database name in setup.props to prevent contaminating anything in current database.
change_dbname_and_output_dir
if [ $? -ne 0 ]; then
  echo "Failed to perform temporary change to db name and output directory. Exiting"
  cleanup_and_exit 1
fi

# Create the database specified in setup.props
issue_command "$BIN_DIR/controldb create"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to create database"
  cleanup_and_exit 1
fi

## create test group for analysis
echo $AG_JSON_DEF > $AG_JSON_DEF_FILENAME  # copy group definition into a file
issue_command "$BIN_DIR/updategroups -j $AG_JSON_DEF_FILENAME"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to define analysis group. Exiting"
  cleanup_and_exit 1
fi

## upload logfile for system
issue_command "$BIN_DIR/upload -d $BASELINE_UPLOAD_DIR"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to upload data from $BASELINE_UPLOAD_DIR. Exiting"
  cleanup_and_exit 1
fi

## train analysis group
issue_command "$BIN_DIR/train $AG_NAME"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to train analysis group $AG_NAME. Exiting"
  cleanup_and_exit 1
fi

## analyze logfile
issue_command "$BIN_DIR/analyze -f $BASELINE_ANALYZE_DIR/spark_analyze.tar.gz"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to analyze data from $BASELINE_ANALYZE_DIR. Exiting"
  cleanup_and_exit 1
fi 

echo
echo "Performing compare of baseline to new analysis results..."

$ADE_JAVA -cp $ADE_CLASSPATH -Dade.setUpFilePath=$ADE_SETUP_FILE org.openmainframe.ade.ext.regression.AdeAnalysisOutputCompare -b "$BASELINE_OUTPUT_DIR" >$ANALYSIS_COMPARE_LOG
rc=$?

echo "Analysis comparison output written to $ANALYSIS_COMPARE_LOG"

cleanup_and_exit $rc

