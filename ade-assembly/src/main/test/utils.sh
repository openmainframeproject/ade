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

update_setup_file() {
  # if backup not created yet do it
  if [ ! -f $SETUP_FILE.bak ]; then
    cp $SETUP_FILE $SETUP_FILE.bak
  fi

  prop_name=$1
  prop_val=$2

  if [ -z $prop_name ]; then
    echo "update_setup_file: no property name given."
    return 1
  elif [ -z $prop_val ]; then
    echo "update_setup_file: no property value."
    return 2
  fi

  grep "$prop_name=" $SETUP_FILE
  if [ $? -ne 0 ]; then
    echo "update_setup_file: property $prop_name not found in $SETUP_FILE"
    return 3
  fi

  echo "update_setup_file: changing $prop_name to $prop_val"
  tmpfilename="/tmp/`basename $SETUP_FILE`"
  CMD=`sed "s~\(${prop_name}=\).*\$~\1\${prop_val}~" $SETUP_FILE > $tmpfilename`
  mv -f $tmpfilename $SETUP_FILE
}

get_current_prop_val() {
  prop_name=$1

  if [ -z $prop_name ]; then
    echo "get_current_prop_val: no property name given"
    return 1
  fi

  prop_val=`grep "$prop_name=" $SETUP_FILE | cut -d \= -f 2`
  #echo "get_current_prop_val: prop_val=$prop_val"

  if [ -z prop_val ]; then
    echo "get_current_prop_val: unable to get property value"
    return 2
  fi

  echo "$prop_val"
}

reset() {
  echo "reset: cleaning up from test $CURRENT_TEST"

  if [ -f $SETUP_FILE.bak ]; then
    mv $SETUP_FILE $SETUP_FILE.$CURRENT_TEST
    mv $SETUP_FILE.bak $SETUP_FILE
  fi

  # TODO - stash log files too
}

######################################
# run_test_check_rc()
# Parameters:
#    Test name (required)
#    Command to execute (required)
#    Expected RC (0 if not specified)
######################################
run_test_check_rc() {
  CURRENT_TEST=$1
  cmd=$2
  exp_rc=$3

  if [ -z $exp_rc ]; then
    exp_rc=0
  fi

  issue_command "$2"
  check_rc $exp_rc $?
}

check_rc() {
  exp_rc=$1
  rc=$2

  if [ $rc -ne $exp_rc ]; then
    echo "FAILURE: $CURRENT_TEST failure RC=$rc"
    export FAILURES="$FAILURES\t$CURRENT_TEST -- Expected RC=$exp_rc Actual RC=$rc\n"
  else # rc=exp_rc
    echo "SUCCESS: $CURRENT_TEST RC=$rc"
    export SUCCESSES="$SUCCESSES\t$CURRENT_TEST -- Expected RC=$exp_rc Actual RC=$rc\n"
  fi
}

print_results() {
  echo "*************************************************"
  echo "Test Results:"
  if [ ! -z FAILURES ]; then
    echo "  FAILED:"
    echo -e "    $FAILURES"
  fi

  if [ ! -z SUCCESS ]; then
    echo "  PASSED:"
    echo -e "    $SUCCESSES"
  fi
  echo "*************************************************"
}

create_test_db() {
  echo "running create_test_db..."
  curr_val=$( get_current_prop_val $DB_URL_PROP )
  if [ -z $curr_val ]; then
    echo "Failed retrieving current value"
    return 1
  fi

  # Update the setup.props file to have a unique db name. 
  echo "Current val: $curr_val"
  test_db_name="${curr_val}_test`date "+%Y%m%d%H%M%S"`"
  update_setup_file "$DB_URL_PROP" $test_db_name

  # create test db with controldb command 
  issue_command "bin/controldb create"
  return $?
}

create_default_ag() {
  AG_JSON_DEF_FILENAME="/tmp/reg_ag.json"
  AG_JSON_DEF="{ \
    \"groups\":{\"modelgroups\":[{\"name\" : \"default\", \"dataType\": \"syslog\", \"evaluationOrder\" : 1, \"ruleName\" : \"default\"}]}, \
    \"rules\":[{\"name\" : \"default\", \"description\" : \"regression test rule to match all systems\", \"membershipRule\" : \"*\" }] \
    }"
  echo $AG_JSON_DEF > $AG_JSON_DEF_FILENAME  # copy group definition into a file
  issue_command "bin/updategroups -j $AG_JSON_DEF_FILENAME"
  return $?
}


