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

ADE_HOME=`dirname "$0"`/../..  # assumes <ade dir>/bin/test/upload_test.sh
ADE_HOME=`cd "$ADE_HOME" && pwd`

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

run_test_check_rc "upload_01" "bin/upload" 0
run_test_check_rc "upload_02" "bin/upload -d $UPLOAD_DIR" 0
run_test_check_rc "upload_03" "bin/upload --inputDir $UPLOAD_DIR" 0
run_test_check_rc "upload_04" "bin/upload -dump_parse_report" 0
run_test_check_rc "upload_05" "bin/upload -d $UPLOAD_DIR -dump_parse_report" 0
run_test_check_rc "upload_06" "bin/upload -f $UPLOAD_FILE" 0
run_test_check_rc "upload_07" "bin/upload --inputFile $UPLOAD_FILE" 0
run_test_check_rc "upload_08" "bin/upload -f $UPLOAD_FILE -dump_parse_report" 0
run_test_check_rc "upload_09" "bin/upload -h" 0
run_test_check_rc "upload_10" "bin/upload --help" 0
run_test_check_rc "upload_11" "bin/upload -o linux -f $UPLOAD_FILE" 0
run_test_check_rc "upload_12" "bin/upload -f $UPLOAD_FILE -s sys1.openmainframe.org" 0
run_test_check_rc "upload_13" "bin/upload -f $UPLOAD_FILE -s sys2.openmainframe.org" 0
run_test_check_rc "upload_14" "bin/upload -f $UPLOAD_FILE -years 2015" 0
run_test_check_rc "upload_15" "bin/upload -f $UPLOAD_FILE -years 2099" 0

print_results

reset

