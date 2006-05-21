<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- Content Stylesheet for "jakarta-site2" Documentation -->
<!-- NOTE:  Changes here should also be reflected in "site.vsl" and vice
     versa, so either Anakia or XSLT can be used for document generation.   -->
<!-- Outstanding Compatibility Issues (with Anakia-based stylesheets):

* Handling of the <image> element to insert relative path prefixes

-->
<!-- $Id$ -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <!-- Output method -->
  <xsl:output method="xhtml" encoding="iso-8859-1" doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd" indent="yes"/>
  <!-- Defined parameters (overrideable) -->
  <xsl:param name="relative-path" select="'.'"/>
  <!-- Defined variables (non-overrideable) -->
  <!-- Process an entire document into an HTML page -->
  <xsl:template match="document">
    <xsl:variable name="site" select="document('project.xml')/site"/>
    <html>
      <head>
        <xsl:apply-templates select="meta"/>
        <title>
          <xsl:value-of select="$site/title"/> - <xsl:value-of select="properties/title"/>
        </title>
        <LINK REL ="stylesheet" TYPE="text/css" HREF="stylesheet.css" TITLE="Style" />
        <xsl:for-each select="properties/author">
          <xsl:variable name="name">
            <xsl:value-of select="."/>
          </xsl:variable>
          <xsl:variable name="email">
            <xsl:value-of select="@email"/>
          </xsl:variable>
          <meta name="author" value="{$name}"/>
          <meta name="email" value="{$email}"/>
        </xsl:for-each>
        <xsl:if test="properties/base">
          <base href="{properties/base/@href}"/>
        </xsl:if>
      </head>
      <body>
        <table class="page-header" border="0" width="100%" cellspacing="0">
          <xsl:comment>PAGE HEADER</xsl:comment>
          <tr>
            <td colspan="2">
              <xsl:comment>ASF LOGO</xsl:comment>
        <a href="http://www.apache.org/">
          <img src="http://www.apache.org/images/asf_logo_wide.gif"
             align="left" alt="The ASF" border="0"/>
        </a>
        <xsl:if test="$site/logo">
          <xsl:variable name="alt">
            <xsl:value-of select="$site/logo"/>
          </xsl:variable>
          <xsl:variable name="home">
            <xsl:value-of select="$site/@href"/>
          </xsl:variable>
          <xsl:variable name="src">
            <xsl:value-of select="$site/logo/@href"/>
          </xsl:variable>

          <xsl:comment>PROJECT LOGO</xsl:comment>
          <a href="{$home}">
            <img src="{$home}{$src}" align="right" alt="{$alt}" border="0"/>
          </a>
        </xsl:if>

            </td>
          </tr>
        </table>
        <table border="0" width="100%" cellspacing="4">
          <tr>
            <xsl:comment>LEFT SIDE NAVIGATION</xsl:comment>
            <td class="left-navbar" valign="top" nowrap="true">
              <xsl:apply-templates select="$site/body/navbar[@name='lhs']"/>
            </td>
            <xsl:comment>MAIN BODY</xsl:comment>
            <td class="main-body" valign="top" align="left">
              <xsl:apply-templates select="body/section"/>
            </td>
            <xsl:comment>RIGHT SIDE NAVIGATION</xsl:comment>
            <td class="right-navbar" valign="top" nowrap="true">
              <xsl:apply-templates select="$site/body/navbar[@name='rhs']"/>
            </td>
          </tr>
          <xsl:comment>FOOTER SEPARATOR</xsl:comment>
          <tr>
            <td colspan="3">
              <hr noshade="" size="1"/>
            </td>
          </tr>
          <tr>
            <td colspan="3">
              <div class="page-footer">
                <em>
        Copyright &#169; 1999-2005, The Apache Software Foundation
        </em>
              </div>
            </td>
          </tr>
        </table>
      </body>
    </html>
  </xsl:template>
  <!-- Process a menu for the navigation bar -->
  <xsl:template match="menu">
    <p>
      <strong>
        <xsl:value-of select="@name"/>
      </strong>
    </p>
    <ul>
      <xsl:apply-templates select="item"/>
    </ul>
  </xsl:template>
  <!-- Process a menu item for the navigation bar -->
  <xsl:template match="item">
    <xsl:variable name="href">
      <xsl:choose>
        <xsl:when test="starts-with(@href, 'http://')">
          <xsl:value-of select="@href"/>
        </xsl:when>
        <xsl:when test="starts-with(@href, '/site')">
          <xsl:text>http://jakarta.apache.org</xsl:text>
          <xsl:value-of select="@href"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$relative-path"/>
          <xsl:value-of select="@href"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <li>
      <a href="{$href}">
        <xsl:value-of select="@name"/>
      </a>
    </li>
  </xsl:template>
  <!-- Process a documentation section -->
  <xsl:template match="section">
    <xsl:variable name="name">
      <xsl:value-of select="@name"/>
    </xsl:variable>
    <div class="section">
      <div class="section-header">
        <a name="{$name}">
          <strong>
            <xsl:value-of select="@name"/>
          </strong>
        </a>
      </div>
      <p>
        <div class="section-body">
          <xsl:apply-templates/>
        </div>
      </p>
    </div>
  </xsl:template>
  <!-- Process a documentation subsection -->
  <xsl:template match="subsection">
    <xsl:variable name="name">
      <xsl:value-of select="@name"/>
    </xsl:variable>
    <div class="subsection">
      <!-- Subsection heading -->
      <div class="subsection-header">
        <a name="{$name}">
          <strong>
            <xsl:value-of select="@name"/>
          </strong>
        </a>
      </div>
      <!-- Subsection body -->
      <div class="subsection-body">
        <xsl:apply-templates/>
      </div>
    </div>
  </xsl:template>
  <!-- Process a source code example -->
  <xsl:template match="source">
    <div class="source">
      <xsl:value-of select="."/>
    </div>
  </xsl:template>
  
  
  
<xsl:template match="*/table">
<table class="detail-table" cellpadding="0" cellspacing="0" >
  <tbody>
    <xsl:apply-templates/>
  </tbody>
</table>

</xsl:template>
  <xsl:template match="tr">

  <tr class="detail-table-row">
<td class="separator-col"></td>
    <xsl:apply-templates/>
  </tr>
  
  </xsl:template>
  
  <xsl:template match="td">
    <td class="detail-table-content" valign="top" align="left">
      <xsl:if test="@colspan">
        <xsl:attribute name="colspan"><xsl:value-of select="@colspan"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="@rowspan">
        <xsl:attribute name="rowspan"><xsl:value-of select="@rowspan"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </td><td class="separator-col"></td>
  </xsl:template>
  <!-- handle th ala site.vsl -->
  <xsl:template match="th">
    <td class="detail-table-header" valign="top">
      <xsl:if test="@colspan">
        <xsl:attribute name="colspan"><xsl:value-of select="@colspan"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="@rowspan">
        <xsl:attribute name="rowspan"><xsl:value-of select="@rowspan"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </td><td class="separator-col"></td>
  </xsl:template>
  <!-- Process everything else by just passing it through -->
  <xsl:template match="*|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|text()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
