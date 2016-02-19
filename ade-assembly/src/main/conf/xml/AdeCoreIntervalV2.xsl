<?xml version="1.0" encoding="UTF-8"?>
<!--

 Copyright IBM Corp. 2015, 2016
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

-->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:i="http://www.openmainframe.org/ade/AdeCoreIntervalV2">
    <xsl:template match="/">
        <html>
            <title>Anomaly Detection Engine Interval View</title>
            <head>
                <link href='./xslt/global.css' rel='stylesheet' type='text/css'>
                </link>
            </head>
            <body>
                <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
                <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
                <xsl:variable name="number_of_intervals_in_a_day" select="(24 * 60 * 60) div (i:interval/i:model_info/@interval_size_in_sec)" />
                <h1>Anomaly Detection Engine Interval View</h1>
                <p>
                    System identifier:
                    <xsl:value-of select="i:interval/i:sys_id" />
                    <br></br>
                    Dates:
                    <xsl:value-of select="i:interval/i:start_time" />
                    --
                    <xsl:value-of select="i:interval/i:end_time" />
                    <br></br>
                    Number of intervals in a day:
                    <xsl:value-of select="$number_of_intervals_in_a_day" />
                    <br></br>
                    Intervals size in seconds:
                    <xsl:value-of select="i:interval/i:model_info/@interval_size_in_sec" />
                    <br></br>
                    <br></br>
                    Interval anomaly score:
                    <xsl:variable name="display_anomaly_score" select="i:interval/i:anomaly_score" />
                    <xsl:value-of select="$display_anomaly_score" />
                    <br></br>

                </p>

                <div class="scrollTableContainer">
                    <table border="1" cellpadding="3">
                        <tr>
                            <th width='7%'>Message Id</th>
                            <th width='140px'>Time Line</th>
                            <th width='7%'>cluster_context</th>
                            <th width='7%'>Num of instance</th>
                            <th width='7%'>Bernoulli score</th>
                            <th width='7%'>Frequency</th>
                            <th width='7%'>Periodicity status</th>
                            <th width='7%'>Periodicity score</th>
                            <th width='15%'>Last Seen</th>
                            <th width='7%'>Interval Contribution</th>
                            <th width='7%'>Poisson score</th>
                            <th width='7%'>Anomaly score</th>
                            <th width='7%'>User Rules</th>
                            <th>Message</th>
                        </tr>
                        <xsl:for-each select="i:interval/i:interval_message">
                            <tr>
                                <td>
                                    <xsl:value-of select="@msg_id" />
                                </td>
                                <td>
                                    <div id="{@msg_id}" style='position:relative;height:32px;width:140px'>
                                        <div
                                            style="left: 0px; top: 0px; width: 120px; height: 32px; clip: rect(0pt, 120px, 32px, 0pt); background-color: rgb(0, 0, 0); position: absolute;"></div>
                                        <xsl:for-each select="i:time_vec/i:occ">
                                            <div
                                                style="left: {.}px; top: 26px; width: 2px; height: 5px; clip: rect(0pt, 2px, 5px, 0pt); background-color:rgb(0,255,0); position: absolute;"></div>
                                        </xsl:for-each>
                                    </div>
                                </td>
                                <td>
                                  <xsl:choose>
                                    <xsl:when test="i:cluster_status='IN_CONTEXT'">
                                        in context (<xsl:value-of select="i:cluster_id" />)
                                    </xsl:when>
                                    <xsl:when test="i:cluster_status='OUT_CONTEXT'">
                                        out of context (
                                        <xsl:value-of select="i:cluster_id"/> )
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="translate(i:cluster_status,$uppercase, $smallcase)" />
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </td>
                                <td>
                                    <xsl:value-of select="i:num_instances" />
                                </td>
                                <td>
                                    <xsl:variable name="display_bernoulli" select="format-number(i:bernoulli, '0.000;#')" />
                                    <xsl:value-of select="$display_bernoulli" />
                                </td>
                                <td>
                                    <xsl:variable name="display_frequency_int_per_occ"
                                              select="format-number($number_of_intervals_in_a_day div i:bernoulli/@frequency, '0.000;#')" />
                                    <xsl:variable name="frequency_occ_per_day" select="format-number(i:bernoulli/@frequency, '0.000;#')" />
                                    <!--xsl:variable name="frequency_occ_per_day" select="$number_of_intervals_in_a_day div i:bernoulli/@frequency" /-->
                                    <!-- Convert frequency to: X number of occurrence every N days.  X must be at least 1 -->

                                    <xsl:variable name="display_frequency_occ_per_day" select="format-number($frequency_occ_per_day, '0.000;#')" />
                                    <xsl:value-of select="$display_frequency_occ_per_day" />_occ/day

                                    <xsl:choose>
                                      <xsl:when test="1 > $frequency_occ_per_day">
                                        <!-- if the number of occ per day is less than 1, then find out number of days for it to occur once -->
                                        <xsl:variable name="frequency_baseline_number_of_days" select="format-number(1 div $frequency_occ_per_day, '0;#')" />
                                        <xsl:variable name="occ_per_baseline_number_of_days" select="$frequency_occ_per_day * $frequency_baseline_number_of_days" />

                                        <xsl:variable name="display_occ_per_baseline_number_of_days" select="format-number($occ_per_baseline_number_of_days, '0.000;#')" />
                                        <xsl:value-of select="$display_occ_per_baseline_number_of_days" />_occ/<xsl:value-of select="$frequency_baseline_number_of_days" />days
                                      </xsl:when>
                                    </xsl:choose>

                                    <br></br>
                                    <xsl:value-of select="$display_frequency_int_per_occ" />_interval/occ
                                </td>
                                <td>
                                    <xsl:value-of select="i:periodicity/@status" />
                                </td>
                                <td>
                                    <xsl:variable name="display_periodicityScore" select="format-number(i:periodicity/@score, '0.000;#')" />
                                    <xsl:value-of select="$display_periodicityScore" />
                                </td>
                                <td>
                                    <xsl:value-of select="i:periodicity/@last_issued" />
                                </td>
                                <td>
                                    <xsl:variable name="display_intCont" select="format-number(i:intCont, '0.000;#')" />
                                    <xsl:value-of select="$display_intCont" />
                                </td>
                                <td>
                                    <xsl:variable name="display_poisson" select="format-number(i:poisson, '0.0000;#')" />
                                    <xsl:value-of select="$display_poisson" />
                                </td>
                                <td>
                                    <xsl:variable name="display_anomaly" select="format-number(i:anomaly, '0.0000;#')" />
                                    <xsl:value-of select="$display_anomaly" />
                                </td>
                                <td>
                                  <xsl:choose>
                                    <xsl:when test="i:active_rules/i:rule[.][not(i:name/@affected_score = 'true')]">
                                        <h3>Not in effect:</h3>
                                        <ul>
                                        <xsl:for-each select="i:active_rules/i:rule[not(i:name/@affected_score = 'true')]">
                                          <xsl:sort select="i:action" order="descending" />
                                          <li><xsl:value-of select="i:action"/> : <xsl:value-of select="i:name" /></li>
                                        </xsl:for-each>
                                        </ul>
                                    </xsl:when>
                                    <xsl:when test="i:active_rules/i:rule[.][i:name/@affected_score = 'true']">
                                        <h3>In effect:</h3>
                                        <ul>
                                        <xsl:for-each select="i:active_rules/i:rule[i:name/@affected_score = 'true']">
                                          <xsl:sort select="i:action" order="descending" />
                                          <li><xsl:value-of select="i:action"/> : <xsl:value-of select="i:name" /></li>
                                        </xsl:for-each>
                                        </ul>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        N/A
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </td>
                                <td title="{i:text_smp}">
                                    <xsl:value-of select="i:text_sum" />
                                </td>
                            </tr>
                        </xsl:for-each>
                    </table>
                </div>
                <br />
                <br />
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
