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

ADE_HOME=`dirname "$0"`/../..  # assumes <ade dir>/bin/test/train_test.sh
ADE_HOME=`cd "$ADE_HOME" && pwd`

upload_log_data() {
  issue_command "bin/upload -d $UPLOAD_DIR"
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

run_test_check_rc "train_01" "bin/train" 0
run_test_check_rc "train_02" "bin/train all" 0
run_test_check_rc "train_03" "bin/train all 01/01/1970" 0
run_test_check_rc "train_04" "bin/train all 01/01/1970 12/31/2099" 0
run_test_check_rc "train_05" "bin/train default" 0
run_test_check_rc "train_06" "bin/train default 01/01/1970" 0
run_test_check_rc "train_07" "bin/train default 01/01/1970 12/31/2099" 0
run_test_check_rc "train_08" "bin/train all 12/30/2099 12/31/2099" 0    
run_test_check_rc "train_09" "bin/train default 12/30/2099 12/31/2099" 0 
run_test_check_rc "train_10" "bin/train nondefault" 0 
run_test_check_rc "train_11" "bin/train nondefault 01/01/1970 12/31/2099" 0

print_results

reset

