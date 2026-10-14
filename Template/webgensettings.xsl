<?xml version="1.0" encoding="UTF-8"?>
<!-- Renders WEB_GEN_SETTINGS (index.xml) as an HTML overview.
     XSLT 1.0, works with the JDK's built-in javax.xml.transform. -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" encoding="UTF-8" indent="yes"/>

  <!-- Keys for grouping GROUP elements (XSLT 1.0 has no for-each-group) -->
  <xsl:key name="byCat"      match="GROUP" use="@CPCATEGORY"/>
  <xsl:key name="byCatEvent" match="GROUP" use="concat(@CPCATEGORY, '|', @CPNAME)"/>

  <xsl:template match="/WEB_GEN_SETTINGS">
    <html>
      <head>
        <meta charset="UTF-8"/>
        <title><xsl:value-of select="SETTINGS/@TITLE"/> – Web settings</title>
        <style>
          body  { font-family: system-ui, sans-serif; margin: 1.5em; color: #222; }
          h1    { margin-bottom: 0; }
          h2    { margin-top: 1.6em; border-bottom: 1px solid #ccc; }
          table { border-collapse: collapse; font-size: 0.9em; }
          th, td { border: 1px solid #ccc; padding: 3px 8px; text-align: left; }
          th    { background: #f0f0f0; }
          tr:nth-child(even) td { background: #fafafa; }
          .kv th { width: 12em; }
          .empty { color: #aaa; font-style: italic; }
          .true  { color: #070; font-weight: 600; }
          .false { color: #a00; }
          .ts    { font-family: monospace; white-space: nowrap; }
          h3.cat { margin: 1.2em 0 0.4em; }
          table.groups { margin-bottom: 1em; min-width: 36em; }
          table.groups caption { text-align: left; font-weight: 600; padding: 4px 0; }
        </style>
      </head>
      <body>
        <h1><xsl:value-of select="SETTINGS/@TITLE"/></h1>
        <p><xsl:value-of select="SETTINGS/@DESCRIPTION"/></p>

        <h2>Settings</h2>
        <xsl:apply-templates select="SETTINGS" mode="kv"/>

        <h2>FTP</h2>
        <xsl:apply-templates select="FTP" mode="kv"/>

        <h2>Groups (<xsl:value-of select="count(GROUP)"/>)</h2>
        <!-- Muenchian grouping: take the first GROUP of each category ... -->
        <xsl:for-each select="GROUP[generate-id() = generate-id(key('byCat', @CPCATEGORY)[1])]">
          <xsl:sort select="@CPCATEGORY"/>
          <xsl:variable name="cat" select="@CPCATEGORY"/>
          <xsl:if test="$cat != ''">
            <h3 class="cat"><xsl:value-of select="$cat"/></h3>
          </xsl:if>

          <!-- ... then the first GROUP of each event within that category -->
          <xsl:for-each select="key('byCat', $cat)[generate-id() = generate-id(key('byCatEvent', concat(@CPCATEGORY, '|', @CPNAME))[1])]">
            <xsl:sort select="@CPNAME"/>
            <xsl:variable name="groups" select="key('byCatEvent', concat(@CPCATEGORY, '|', @CPNAME))"/>
            <table class="groups">
              <caption><xsl:value-of select="@CPNAME"/> (<xsl:value-of select="count($groups)"/>)</caption>
              <tr><th>Stage</th><th>Group</th><th>Sort</th>
                  <th>Enabled</th><th>Disabled</th><th>Last upload</th></tr>
              <xsl:for-each select="$groups">
                <!-- sort order: empty values last, others numeric -->
                <xsl:sort select="string-length(@GRSORTORDER) = 0"/>
                <xsl:sort select="@GRSORTORDER" data-type="number"/>
                <xsl:sort select="@GRSTAGE"/>
                <xsl:sort select="@GRNAME"/>
                <tr>
                  <td><xsl:value-of select="@GRSTAGE"/></td>
                  <td><xsl:value-of select="@GRNAME"/></td>
                  <td><xsl:value-of select="@GRSORTORDER"/></td>
                  <td><xsl:call-template name="value"><xsl:with-param name="v" select="@ENABLED"/></xsl:call-template></td>
                  <td><xsl:call-template name="value"><xsl:with-param name="v" select="@DISABLED"/></xsl:call-template></td>
                  <td class="ts"><xsl:value-of select="@TS"/></td>
                </tr>
              </xsl:for-each>
            </table>
          </xsl:for-each>
        </xsl:for-each>

        <h2>Reports</h2>
        <table>
          <tr><th>Name</th><th>Active</th><th>Last upload</th></tr>
          <xsl:for-each select="REPORT">
            <tr>
              <td><xsl:value-of select="@NAME"/></td>
              <td><xsl:call-template name="value"><xsl:with-param name="v" select="@ACTIVE"/></xsl:call-template></td>
              <td class="ts"><xsl:value-of select="@TS"/></td>
            </tr>
          </xsl:for-each>
        </table>

        <h2>Dates</h2>
        <table>
          <tr><th>Date</th><th>Last upload</th></tr>
          <xsl:for-each select="DATE">
            <xsl:sort select="@NAME"/>
            <tr>
              <td><xsl:value-of select="@NAME"/></td>
              <td class="ts"><xsl:value-of select="@TS"/></td>
            </tr>
          </xsl:for-each>
        </table>
      </body>
    </html>
  </xsl:template>

  <!-- One element's attributes as a name/value table -->
  <xsl:template match="*" mode="kv">
    <table class="kv">
      <xsl:for-each select="@*">
        <xsl:sort select="name()"/>
        <tr>
          <th><xsl:value-of select="name()"/></th>
          <td>
            <xsl:choose>
              <!-- never show the password, even if it is encrypted -->
              <xsl:when test="name() = 'PASSWORD'">••••••</xsl:when>
              <xsl:when test="name() = 'TS'"><span class="ts"><xsl:value-of select="."/></span></xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="value"><xsl:with-param name="v" select="."/></xsl:call-template>
              </xsl:otherwise>
            </xsl:choose>
          </td>
        </tr>
      </xsl:for-each>
    </table>
  </xsl:template>

  <!-- Formats a value: empty marked, booleans colored -->
  <xsl:template name="value">
    <xsl:param name="v"/>
    <xsl:choose>
      <xsl:when test="string($v) = ''"><span class="empty">(empty)</span></xsl:when>
      <xsl:when test="$v = 'true' or $v = 'false'"><span class="{$v}"><xsl:value-of select="$v"/></span></xsl:when>
      <xsl:otherwise><xsl:value-of select="$v"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
