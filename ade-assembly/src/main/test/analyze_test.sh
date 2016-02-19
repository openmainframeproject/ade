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

#***********************************************
# sub-routines
#***********************************************
upload_log_data() {
  issue_command "bin/upload -d $UPLOAD_DIR"
}

train_all_ag() {
  issue_command "bin/train all"
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

create_test_db  # create test database to work with
if [ $? -ne 0 ]; then
  echo "Unable to create test database. Exiting"
  reset
  exit 1
fi

create_default_ag   # create default analysis group
if [ $? -ne 0 ]; then
  echo "Unable to create default analysis group. Exiting"
  reset
  exit 1
fi

upload_log_data  # upload log data
if [ $? -ne 0 ]; then
  echo "Unable to upload log data. Exiting"
  reset
  exit 1
fi

train_all_ag  # train all analysis groups
if [ $? -ne 0 ]; then
  echo "Unable to train all analysis groups. Exiting"
  reset 
  exit 1
fi


run_test_check_rc "analyze_01" "bin/analyze" 0
run_test_check_rc "analyze_02" "bin/analyze -d $ANALYZE_DIR" 0
run_test_check_rc "analyze_03" "bin/analyze --inputDir $ANALYZE_DIR" 0
run_test_check_rc "analyze_04" "bin/analyze -dump_parse_report" 0
run_test_check_rc "analyze_05" "bin/analyze -d $ANALYZE_DIR -dump_parse_report" 0
run_test_check_rc "analyze_06" "bin/analyze -f $ANALYZE_FILE" 0
run_test_check_rc "analyze_07" "bin/analyze --inputFile $ANALYZE_FILE" 0
run_test_check_rc "analyze_08" "bin/analyze -f $ANALYZE_FILE -dump_parse_report" 0
run_test_check_rc "analyze_09" "bin/analyze -h" 0
run_test_check_rc "analyze_10" "bin/analyze --help" 0
run_test_check_rc "analyze_11" "bin/analyze -o linux -f $ANALYZE_FILE" 0
run_test_check_rc "analyze_12" "bin/analyze -f $ANALYZE_FILE -s sys1.openmainframe.org" 0
run_test_check_rc "analyze_13" "bin/analyze -f $ANALYZE_FILE -s sys2.openmainframe.org" 0
run_test_check_rc "analyze_14" "bin/analyze -f $ANALYZE_FILE -years 2015" 0
run_test_check_rc "analyze_15" "bin/analyze -f $ANALYZE_FILE -years 2099" 0 

print_results

reset

