<?xml version="1.0" encoding="UTF-8"?>
<!--
 
 Copyright IBM Corp. 2011, 2016
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/xpath-functions" version="1.0">
	<xsl:template match="/">
		<html>
			<title>Ade-V3 Interval View</title>
			<head>
				<link href='./xslt/global.css' rel='stylesheet' type='text/css'>
				</link>
				<script type="text/javascript">
				</script>				  
			</head>
			<body>
				<xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
				<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
				<xsl:variable name="StartTime"
							  select="AnalyzedInterval/StartTimeUnix  div 1000" />
				<xsl:variable name="EndTime"
							  select="AnalyzedInterval/EndTimeUnix  div 1000" />
				<xsl:variable name="Scale" select="($EndTime - $StartTime) div 120" /> 
				<h1>Ade-V3 Interval View</h1>
				<p>
					Source:
					<xsl:value-of select="AnalyzedInterval/Source" />
					<br></br>
					Dates:
					<xsl:value-of select="AnalyzedInterval/StartTime" />
					--
					<xsl:value-of select="AnalyzedInterval/EndTime" />
					<br></br>
					<!-- <xsl:value-of select="xs:dateTime('1970-01-01T00:00:00')" /> -->
					<!--<xsl:value-of select="xs:format-dateTime($EndTime, '[D] [MN,*-3] 
						[Y] [h]:[m01][PN,*-2] [ZN,*-3]')"/> -->
					<br></br>

					<table class='white' border="0">
						<tr>
							<td width='30%'>
								Interval anomaly score:
					<xsl:value-of select="AnalyzedInterval/AnomalyScore" />
					</td>
					<td width='30%'>
					Interval Log Likelihood 
								<xsl:value-of
									select="format-number(sum(AnalyzedInterval/AnalyzedMessageSummary/ScoreSet/Score[@ScoreName='logProb']/@Score),'#0.00')" />
					</td>
					<td width='30%'>
								Total number of messages
								<xsl:value-of
									select="sum(AnalyzedInterval/AnalyzedMessageSummary/NumOccurrences)" />
					</td>
				  </tr>
					</table>
				  </p>

				<div class="scrollTableContainer">
					<table border="1">
						<COLGROUP>
							<COL align="right" />
							<COL />
							<COL />
							<COL align="center" />
							<COL align="center" />			
							<COL align="char" char="." />			
							<COL align="char" char="." />
							<COL align="char" char="." />
							<COL align="char" char="." />
							<COL align="char" char="." />
							<COL />
						</COLGROUP>
						<tr>
							<th width='7%'>###</th>
							<th width='7%'>Message Id</th>
							<th width='120px'>Time Line</th>
							<th width='7%'>Instance num</th>
							<th width='5%'>cluster Id</th>
							<th width='7%'>Log prob</th>
							<th width='7%'>LogNormal</th>
							<th width='7%'>Bern LogP</th>							
							<th width='7%'>Bernoulli</th>														
							<th width='7%'>Anomaly score</th>
							<th>Message</th>
						</tr>
						<xsl:for-each select="AnalyzedInterval/AnalyzedMessageSummary">
						    <xsl:variable name="baseRow" select="position()"/>							                                                                
							<tr>
								<td>								
									<xsl:value-of select="$baseRow" />
								</td>
								<td>
									<xsl:value-of select="@MsgId" />									
								</td>
								<td >
									<div id="{@MsgId}" style='position:relative;height:32px;width:140px'>
										<div
											style="left: 0px; top: 0px; width: 120px; height: 32px; clip: rect(0pt, 120px, 32px, 0pt); background-color: rgb(0, 0, 0); position: absolute;"></div>
										<xsl:for-each select="TimeLine/Occurrence">
											<div
												style="left: {(.) * 120}px; top: 1px; width: 1px; height: 30px; clip: rect(0pt, 2px, 30px, 0pt); background-color:rgb(0,255,0); position: absolute;"></div>
											 <xsl:value-of select="@bar" />
										</xsl:for-each>
									</div>
								</td>
								<td>
									<xsl:value-of select="NumOccurrences" />

								</td>
								<td>
								  <xsl:choose> 
										<xsl:when
											test="ScoreSet/Score[@ScoreName='LogNormalScore.isNew']/@Score = 1.0">
									  <strong style="color:#DF0000;">NEW</strong> 									  
									</xsl:when>
									<xsl:otherwise>
											<xsl:value-of
												select="format-number(ScoreSet/Score[@ScoreName='ClusteringUniquifyScore.clusterId']/@Score,'#0')" />
									</xsl:otherwise> 
								  </xsl:choose>
								</td>
								<td>
									<xsl:value-of
										select="format-number(ScoreSet/Score[@ScoreName='logProb']/@Score,'#0.000')" />

								</td>
								<td>
									<xsl:value-of
										select="format-number(ScoreSet/Score[@ScoreName='LogNormalScore.logProb']/@Score,'#0.000')" />
								</td>
								<td>
									<xsl:value-of
										select="format-number(ScoreSet/Score[@ScoreName='BernoulliScore.logProb']/@Score,'#0.000')" />
								</td>
								<td>
									<xsl:value-of
										select="format-number(ScoreSet/Score[@ScoreName='BernoulliScore.main']/@Score,'#0.000')" />

								</td>
								<td>
									<xsl:value-of select="format-number(AnomalyScore,'#0.000')" />
								</td>
								<td title="{text_smp}">
									<xsl:value-of select="SummarizedText" />
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

