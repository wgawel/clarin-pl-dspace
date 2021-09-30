<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : footer.xsl
    Created on : April 14, 2012, 4:48 PM
    Author     : sedlak
    Description:
        Purpose of transformation follows.
-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:mets="http://www.loc.gov/METS/"
        xmlns:xlink="http://www.w3.org/TR/xlink/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        xmlns:xhtml="http://www.w3.org/1999/xhtml"
        xmlns:mods="http://www.loc.gov/mods/v3"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:confman="org.dspace.core.ConfigurationManager"
        xmlns:psu="cz.cuni.mff.ufal.utils.PageStructureUtil"
        xmlns:file="java.io.File"
        xmlns="http://www.w3.org/1999/xhtml"
        exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman file">

    

    <xsl:template name="buildFooter">
    <a class="hidden" id="repository_path">
                    <xsl:attribute name="href">
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    </xsl:attribute>
                    <xsl:text>&#160;</xsl:text>
                </a>

      <xsl:variable name="currentLocale" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='currentLocale']"/>
      <xsl:variable name="localizedDiskPath" select="concat($theme-path-on-disk,'/lib/lindat/',$currentLocale,'/footer.htm')" />
      <xsl:variable name="defaultDiskPath" select="concat($theme-path-on-disk,'/lib/lindat/','/footer.htm')" />
      <xsl:variable name="path" select="file:new($localizedDiskPath)"/>
      <xsl:variable name="collection"
                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container']"/>
      <xsl:choose>
          <xsl:when test="file:isFile($path)">
              <xsl:copy-of select="psu:readFooter($localizedDiskPath, $collection)" />
          </xsl:when>
          <xsl:otherwise>
              <xsl:copy-of select="psu:readFooter($defaultDiskPath, $collection)" />
          </xsl:otherwise>
      </xsl:choose>
    </xsl:template>


</xsl:stylesheet>
