<?xml version ='1.0'?>
<xsl:stylesheet version="1.1"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:strip-space elements = "*" />
	<xsl:output method = "xml" indent='yes'/>
<!-- 
	Basically copy everything  
--> 
	<xsl:template match="@*|node()">
	  <xsl:copy>
	    <xsl:apply-templates select="@*|node()"/>
	  </xsl:copy>
	</xsl:template>
<!-- 
	But: 
-->
<!-- 
Virtual hosting
 -->
    <!-- Enable -->
 	<xsl:template match="config/James/enableVirtualHosting">
		<xsl:element name='enableVirtualHosting'>
			<xsl:text>true</xsl:text>
	    </xsl:element>
	</xsl:template>
	<xsl:template match="config/domainlist/domainnames">
		<xsl:element name='domainnames'>
			<xsl:element name='domainname'>
				<xsl:text>localhost</xsl:text>
		    </xsl:element>
		    <xsl:element name='domainname'>
				<xsl:text>example.org</xsl:text>
		    </xsl:element>
	    </xsl:element>
	</xsl:template>
<!-- 
	Ignore comments 
-->
	<xsl:template match="comment()"/>
<!--
	Use high ports  
-->
	<xsl:template match="config/smtpserver/port">
		<xsl:element name='port'>
			<xsl:text>10025</xsl:text>
	    </xsl:element>
	</xsl:template>
	<xsl:template match="config/pop3server/port">
		<xsl:element name='port'>
			<xsl:text>10110</xsl:text>
	    </xsl:element>
	</xsl:template>
	<xsl:template match="config/imapserver/port">
		<xsl:element name='port'>
			<xsl:text>10043</xsl:text>
	    </xsl:element>
	</xsl:template>
	<xsl:template match="config/nntpserver/port">
		<xsl:element name='port'>
			<xsl:text>10119</xsl:text>
	    </xsl:element>
	</xsl:template>
		<xsl:template match="config/remotemanager/port">
		<xsl:element name='port'>
			<xsl:text>10445</xsl:text>
	    </xsl:element>
	</xsl:template>
</xsl:stylesheet>