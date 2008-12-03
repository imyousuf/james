<?xml version ='1.0'?>
<xsl:stylesheet version="1.1"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:strip-space elements = "*" />
	<xsl:param name='MailetPackage'/>
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
	Ignore comments 
-->
	<xsl:template match="comment()"/>

<!-- 
	Ignore existing processors (for safety)
-->
	<xsl:template match="processor[@name='root']">
		<xsl:element name='processor'>
			<xsl:attribute name='name'>root</xsl:attribute>
			<xsl:element name='mailet'>
				<xsl:attribute name='match'>All</xsl:attribute>
				<xsl:attribute name='class'>SieveMailet</xsl:attribute>
			</xsl:element>
		</xsl:element>
	</xsl:template>		
	
<!-- 
	Ignore existing processors (for safety)
-->
	<xsl:template match="processor"/>	
	
<!-- 
	Substitute mailet packages
-->
	<xsl:template match="mailetpackages">
		<xsl:element name='mailetpackages'>
			<xsl:element name='mailetpackage'>
			<xsl:text>org.apache.james.imapserver.sieve</xsl:text>
			</xsl:element>
		</xsl:element>
	</xsl:template>	
	
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