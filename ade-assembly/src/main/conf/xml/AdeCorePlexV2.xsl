<?xml version="1.0" encoding="UTF-8"?>
<!--

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

-->
<xsl:stylesheet version="2.0"
  xmlns:java="http://www.java.com/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:p="http://www.openmainframe.org/ade/AdeCorePlexV2">

  <!--xsl:function name="java:file-exists" xmlns:file="java.io.File" as="xs:boolean">
    <xsl:param name="file" as="xs:string"/>
    <xsl:param name="base-uri" as="xs:string"/>
    <xsl:variable name="absolute-uri" select="resolve-uri($file, $base-uri)" as="xs:anyURI"/>
    <xsl:sequence select="file:exists(file:new($absolute-uri))"/>
  </xsl:function-->

    <xsl:template match="/">

        <html>

            <title>Anomaly Detection Engine Log Investigation Tool</title>

            <head>

                <link href='./xslt/global.css' rel='stylesheet' type='text/css' />

            </head>


            <body>

                <xsl:variable name="num_intervals" select="p:systems/p:number_intervals" />

            <xsl:variable name="minInInterval" select="1440 div p:systems/p:number_intervals" />

                <xsl:variable name="startTime" select="p:systems/p:start_time" />

                <h1>Anomaly Detection Engine Log Investigation Tool</h1>

                <h2> <xsl:value-of select="p:systems/p:start_time"/></h2>

                <p>

                    Dates:


                    <xsl:value-of select="p:systems/p:start_time" />

                    --

                    <xsl:value-of select="p:systems/p:end_time" />

                    <br/>

                    Number of intervals: <xsl:value-of select="$num_intervals" />

                    <br/>

                    Interval Size : <xsl:value-of select="$minInInterval" /> minutes

                    <br/>

                </p>

                <div id='table-div' style='overflow:auto; border:solid black 1px;width:100%'>

                    <xsl:for-each select="p:systems/p:system">

                        <xsl:sort select="@sys_id" />

                        <table>

                            <tr>

                                <th> System ID </th>

                                <th width='100%'> Anomaly scores </th>

                            </tr>

                            <tr>

                                <td>

                                    <xsl:value-of select="@sys_id" />

                                </td>

                                <td align='center' valign='middle'>

                                    <div id="{@sys_id}" style='position:relative;height:100px'>

                                        <xsl:apply-templates select = "p:interval" >
                                            <xsl:with-param name="num_intervals" select="$num_intervals" />
                                            <xsl:with-param name="minInInterval" select="$minInInterval" />
                                        </xsl:apply-templates>

                                    </div>

                                </td>

                            </tr>

                        </table>

                        <p/>

                        <table>
                            <tr>
                                <th width='140px'>Score</th>
                                <th width='280px'>Meaning</th>
                            </tr>
                            <tr class="bg1" style="color:#FC0000;background-color: #555555;">
                                <td align="left" valign="top">101</td>
                                <td align="left" valign="top">Very rare</td>
                            </tr>
                            <tr class="bg0" style="color:#FF9900;background-color: #555555;">
                                <td align="left" valign="top">95-100</td>
                                <td align="left" valign="top">Quite rare</td>
                            </tr>
                            <tr class="bg1" style="color:#EEEE00;background-color: #555555;">
                                <td align="left" valign="top">80-94</td>
                                <td align="left" valign="top">Unusual</td>
                            </tr>
                            <tr class="bg0" style="color: #00FC00;background-color:#555555;">
                                <td align="left" valign="top">0-79</td>
                                <td align="left" valign="top">Normal</td>
                            </tr>
                        </table>

                        <p/>
                        <table>

                            <tr>
                                <th width="125">
                                Interval Index
                                </th>
                                <th width="125">
                                Interval Time
                                </th>
                                <th width="125">
                                Anomaly Score
                                </th>
                                <th width="125">
                                Number of Unique Messages
                                </th>
                                <th width="125">
                                Num of New Msgs
                                </th>
                                <th width="125">
                                Num of Never Seen Before Msgs
                                </th>
                                <th width="125">
                                Missing
                                </th>
                                <th width="125">
                                Reason for Missing
                                </th>
                <th width="125">
                                Interval, V2
                                </th>
                            </tr>
                            <xsl:apply-templates select = "p:interval" mode="texttable">
                                <xsl:with-param name="minInInterval" select="$minInInterval" />
                                <xsl:with-param name="startTime" select="$startTime" />
                            </xsl:apply-templates>
                        </table>

                    </xsl:for-each>

                </div>

            </body>

        </html>

    </xsl:template>

    <xsl:template match="p:interval">

        <xsl:param name="num_intervals" />

        <xsl:param name="minInInterval" />

        <xsl:variable name="iCount" select="@index" />

        <xsl:variable name="hour">

           <xsl:value-of select="floor((($iCount - 1)* $minInInterval) div 60)"/>

           <xsl:text>:</xsl:text>

           <xsl:value-of select='format-number(((($iCount - 1)* $minInInterval) mod 60),"00")'/>

        </xsl:variable>

        <xsl:variable name="rawHeight" select="p:num_unique_msg_ids div 200.0*50.0" />

        <xsl:variable name="height">

        <xsl:choose>

            <xsl:when test="$rawHeight &gt; 100">

                 <xsl:text>100</xsl:text>

            </xsl:when>

            <xsl:when test="$rawHeight &lt; 5 and $rawHeight &gt; 0">

                 <xsl:text>5</xsl:text>

            </xsl:when>

            <xsl:otherwise>  <xsl:copy-of select="$rawHeight" /> </xsl:otherwise>

        </xsl:choose>

        </xsl:variable>

        <xsl:variable name="rawColor" select="p:anomaly_score"/>

        <xsl:variable name="color">

         <xsl:choose>

           <xsl:when test="($rawColor &gt; 100.0)">

                <xsl:text>#FC0000</xsl:text>

           </xsl:when>

           <xsl:when test="($rawColor &lt;= 100.0 and $rawColor &gt;= 95.0)">

                <xsl:text>#FF9900</xsl:text>

           </xsl:when>

            <xsl:when test="($rawColor &lt; 95.0 and $rawColor &gt;= 80.0)">

                 <xsl:text>#EEEE00</xsl:text>

            </xsl:when>

            <xsl:when test="($rawColor &lt; 80.0 and $rawColor &gt; 0.0)">

                 <xsl:text>#00FC00</xsl:text>

            </xsl:when>

            <xsl:otherwise>#00FC00</xsl:otherwise>

        </xsl:choose>

        </xsl:variable>

        <xsl:variable name="fileName">

           <xsl:text>intervals/interval_</xsl:text>

           <xsl:value-of select="$iCount"/>
           <xsl:text>.xml</xsl:text>

        </xsl:variable>

         <xsl:if test="$height &gt; 0">

            <a>

              <xsl:attribute name="href">

                <xsl:value-of select="$fileName" />

              </xsl:attribute>

                <div title="Time: {$hour}, Anomaly score={p:anomaly_score}, Msg-ids={p:num_unique_msg_ids}"

                    style="left: {($iCount - 1) * 6}px; top: {100 - $height}px; width: 5px; height: {$height}px; clip: rect(0pt, 5px, {$height}px, 0pt); background-color:{$color}; position: absolute;" />

            </a>

        </xsl:if>

    </xsl:template>

    <xsl:template match="p:interval" mode="texttable">

        <xsl:param name="minInInterval" />

        <xsl:param name="startTime" />

        <xsl:variable name="iCount" select="@index" />

        <xsl:variable name="hour">

           <xsl:value-of select="floor((($iCount)* $minInInterval) div 60)"/>

           <xsl:text>:</xsl:text>

           <xsl:value-of select='format-number(((($iCount)* $minInInterval) mod 60),"00")'/>

        </xsl:variable>

        <xsl:variable name="hour_end">

           <xsl:value-of select="floor((($iCount + 1)* $minInInterval) div 60)"/>

           <xsl:text>:</xsl:text>

           <xsl:value-of select='format-number(((($iCount + 1)* $minInInterval) mod 60),"00")'/>

        </xsl:variable>

    <xsl:variable name="fileNameV2">
           <xsl:text>intervals/interval_</xsl:text>
           <xsl:value-of select="$iCount"/>
           <xsl:text>.xml</xsl:text>
        </xsl:variable>

        <tr>
            <td>
                <xsl:value-of select="@index" />
            </td>
            <td>
                <xsl:value-of select="$hour" /> - <xsl:value-of select="$hour_end" />
            </td>
            <td>
                <xsl:value-of select="p:anomaly_score" />
            </td>
            <td>
                <xsl:value-of select="p:num_unique_msg_ids" />
            </td>
            <td>
                <xsl:value-of select="@num_new_messages" />
            </td>
            <td>
                <xsl:value-of select="@num_never_seen_before_messages" />
            </td>
            <td>
                <xsl:value-of select="@missing" />
            </td>
            <td>
                <xsl:value-of select="@missing_reason" />
            </td>
      <td>
        <xsl:choose>
          <xsl:when test="@missing[.!='true']">
            <a>
             <xsl:attribute name="href">
               <xsl:value-of select="$fileNameV2" />
                   </xsl:attribute>
             XML
           </a>
          </xsl:when>
          <xsl:otherwise>
            N/A
          </xsl:otherwise>
        </xsl:choose>
            </td>
        </tr>
    </xsl:template>


</xsl:stylesheet>
