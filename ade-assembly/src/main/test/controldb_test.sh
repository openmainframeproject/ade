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

ADE_HOME=`dirname "$0"`/../..  # assumes <ade dir>/bin/test/analysis_comp_test.sh
ADE_HOME=`cd "$ADE_HOME" && pwd`

controldb_setup() {
  echo "running controldb_setup"
  curr_val=$( get_current_prop_val $DB_URL_PROP )
  if [ -z curr_val ]; then
    echo "Failed retrieving current value"
    return 1
  fi

  # Update the setup.props file to have a unique db name.
  echo "Current val: $curr_val"
  test_db_name="${curr_val}_test`date "+%Y%m%d%H%M%S"`"
  update_setup_file "$DB_URL_PROP" $test_db_name
}

###################
# main
###################
# change to ADE_HOME dir because setup.props contains relative paths
eval "cd $ADE_HOME"

. $ADE_HOME/bin/test/test.props  # test properties
. $ADE_HOME/bin/test/utils.sh    # common test utilities

if [ ! -f $SETUP_FILE ]; then
  echo "Unable to locate setup.props. Exiting"
  exit 1
fi

controldb_setup  # perform setup for tests

run_test_check_rc "controldb_01" "bin/controldb" 0
run_test_check_rc "controldb_02" "bin/controldb create" 0
test_sys_name="controldb_03."`date "+%Y%m%d.%H%M%S"`
run_test_check_rc "controldb_03" "bin/controldb dml \"INSERT INTO SOURCES (SOURCE_ID,ANALYSIS_GROUP,FILE_NAME,LOG_TYPE) VALUES ('$test_sys_name',null,null,null)\"" 0
run_test_check_rc "controldb_04" "bin/controldb query \"select * from sources\"" 0
run_test_check_rc "controldb_05" "bin/controldb delete" 0
run_test_check_rc "controldb_06" "bin/controldb drop" 0

print_results

reset

