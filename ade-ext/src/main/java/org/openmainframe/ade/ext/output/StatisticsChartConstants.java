/*
 
    Copyright IBM Corp. 2012, 2016
    This file is part of Anomaly Detection Engine for Linux Logs (ADE).

    ADE is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ADE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ADE.  If not, see <http://www.gnu.org/licenses/>.
 
*/
package org.openmainframe.ade.ext.output;

/**
 * Constants for Statistics Chart. 
 */
public final class StatisticsChartConstants{

    private StatisticsChartConstants(){}

    public static final String FullBernoulliClusterAwareScore_rawAnomaly = "FullBernoulliClusterAwareScore.rawAnomaly";
    public static final String FullBernoulliClusterAwareScore_frequency = "FullBernoulliClusterAwareScore.frequency";
    public static final String ClusteringContextScore_clusterId = "ClusteringContextScore.clusterId";
    public static final String LastSeenLoggingScorerContinuous_LastTime = "LastSeenLoggingScorerContinuous.LastTime";
    public static final String LastSeenLoggingScorerContinuous_Main = "LastSeenLoggingScorerContinuous.main";
    public static final String LastSeenScorer_LogProbGivenLast = "LastSeenScorer.LogProbGivenLast";
    public static final String LogNormalScore_mean = "LogNormalScore.mean";
    public static final String LogNormalScore_main = "LogNormalScore.main";
    public static final String LogProb = "logProb";
    public static final String Anomaly = "anomaly";
    public static final String ClusteringContextScore_status = "ClusteringContextScore.status";
    public static final String CriticalWordCountReporter_main = "CriticalWordCountReporter.main";
}
