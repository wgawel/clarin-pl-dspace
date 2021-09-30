<?xml version="1.0" encoding="UTF-8" ?>
<!--
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:doc="http://www.lyncode.com/xoai"
    xmlns:h="http://clarin-pl.eu/ns/experimental/html"
    xmlns:confman="org.dspace.core.ConfigurationManager"
    exclude-result-prefixes="doc confman"
    version="1.0">

    <!-- repository name -->
    <xsl:variable name="dspace.name" select="confman:getProperty('dspace.name')"/>
    <xsl:variable name="authorsLimitLT" select="6"/>

    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" cdata-section-elements="h:html"/>

    <xsl:variable name="title"><xsl:call-template name="title"/></xsl:variable>
    <xsl:variable name="authors"><xsl:call-template name="authors"/></xsl:variable>
    <xsl:variable name="pid"><xsl:call-template name="pid"/></xsl:variable>
    <xsl:variable name="repository"><xsl:call-template name="repository"/></xsl:variable>
    <xsl:variable name="year"><xsl:call-template name="year"/></xsl:variable>
    <xsl:variable name="publisher"><xsl:call-template name="publisher"/></xsl:variable>

    <xsl:template match="/">

        <h:html xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://lindat.mff.cuni.cz/ns/experimental/html http://lindat.mff.cuni.cz/schemas/experimental/html.xsd">
            <xsl:choose>
            <xsl:when
                    test="doc:metadata/doc:element[@name='local']/doc:element[@name='refbox']/doc:element[@name='format']/doc:element/doc:field[@name='value']/text()">
                <xsl:call-template name="interpolate-variables">
                    <xsl:with-param name="value" select="doc:metadata/doc:element[@name='local']/doc:element[@name='refbox']/doc:element[@name='format']/doc:element/doc:field[@name='value']/text()"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="$authors != ''">
                            <xsl:copy-of select="$authors"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$publisher"/>
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:if test="$year != ''">
                        <xsl:text>, </xsl:text>
                        <xsl:copy-of select="$year"/>
                    </xsl:if>
                    <xsl:if test="$title != ''">
                        <xsl:text>, </xsl:text>
                        <xsl:copy-of select="$title"/>
                    </xsl:if>
                    <xsl:if test="$repository != ''">
                        <xsl:text>, </xsl:text>
                        <xsl:copy-of select="$repository"/>
                    </xsl:if>
                    <xsl:if test="$pid != ''">
                        <xsl:text>, </xsl:text>
                        <xsl:copy-of select="$pid"/>
                    </xsl:if>
                    <xsl:text>.</xsl:text>
            </xsl:otherwise>
            </xsl:choose>
        </h:html>
    </xsl:template>

    <xsl:template name="title">
        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
            <i><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']"/></i>
        </xsl:if>
    </xsl:template>

    <xsl:template name="authors">
        <xsl:variable name="authorsCount" select="count(doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value'] | doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='other']/doc:element/doc:field[@name='value'])"/>
        <xsl:if test="$authorsCount &gt; 0">
            <!-- The select is limited to max $authorsLimitLT - 1 nodes -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value'] | doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='other']/doc:element/doc:field[@name='value']">
                <!--
                   In theory you would put the test as [position() &lt; $authorsLimitLT] in the select above.
                   That was broken for cases where there were two authors. The second author was duplicated,
                   ie. 3 positions instead of 2. That would be an issue with the processor we are using in java,
                   fine with xsltproc.
                   If you decide to change it test it well.
                -->
                <xsl:if test="position() &lt; $authorsLimitLT">
                  <xsl:if test="position() = 1 or $authorsCount &lt; $authorsLimitLT">
                    <xsl:value-of select="."/>
                    <xsl:choose>
                        <!-- if `$authorsLimitLT - 1` or less authors use 'and' before the last name -->

                        <xsl:when test="position() = last()-1 and $authorsCount &lt; $authorsLimitLT"> and </xsl:when>
                        <xsl:when test="position() &lt; last() and position() &lt; $authorsLimitLT - 1">; </xsl:when>
                    </xsl:choose>
                  </xsl:if>
                  <!-- last position and more authors than we display, add 'et al.', eg. 3rd author and the max displayed is 3 -->
                  <xsl:if test="position() = $authorsLimitLT - 1 and $authorsCount &gt; $authorsLimitLT - 1">et al.</xsl:if>
                </xsl:if>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

    <xsl:template name="pid">
        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']"/>
                </xsl:attribute>
                <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']"/>
            </a>
        </xsl:if>
    </xsl:template>

    <xsl:template name="repository">
        <xsl:value-of select="$dspace.name"/>
    </xsl:template>

    <xsl:template name="year">
        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
            <xsl:value-of select="substring(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value'],1,4)"/>
        </xsl:if>
    </xsl:template>

    <xsl:template name="publisher">
        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']"/>
    </xsl:template>

    <xsl:template name="interpolate-variables">
        <xsl:param name="value" />
        <xsl:choose>
            <xsl:when
                    test="contains($value, '{title}') or contains($value, '{authors}') or contains($value, '{pid}') or contains($value, '{repository}') or contains($value, '{year}') or contains($value, '{publisher}')">
                <xsl:choose>
                    <xsl:when test="starts-with($value,'{title}')">
                        <xsl:copy-of select="$title"/>
                        <xsl:call-template name="interpolate-variables">
                            <xsl:with-param name="value" select="substring-after($value,'{title}')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="starts-with($value,'{authors}')">
                        <xsl:copy-of select="$authors"/>
                        <xsl:call-template name="interpolate-variables">
                            <xsl:with-param name="value" select="substring-after($value,'{authors}')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="starts-with($value,'{pid}')">
                        <xsl:copy-of select="$pid"/>
                        <xsl:call-template name="interpolate-variables">
                            <xsl:with-param name="value" select="substring-after($value,'{pid}')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="starts-with($value,'{repository}')">
                        <xsl:copy-of select="$repository"/>
                        <xsl:call-template name="interpolate-variables">
                            <xsl:with-param name="value" select="substring-after($value,'{repository}')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="starts-with($value,'{year}')">
                        <xsl:copy-of select="$year"/>
                        <xsl:call-template name="interpolate-variables">
                            <xsl:with-param name="value" select="substring-after($value,'{year}')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="starts-with($value,'{publisher}')">
                        <xsl:copy-of select="$publisher"/>
                        <xsl:call-template name="interpolate-variables">
                            <xsl:with-param name="value" select="substring-after($value,'{publisher}')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <!-- we have a known variable but not at the start -->
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="starts-with($value, '{')">
                                <xsl:value-of select="'{'"/>
                                <xsl:call-template name="interpolate-variables">
                                    <xsl:with-param name="value" select="substring-after($value, '{')"/>
                                </xsl:call-template>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="substring-before($value, '{')"/>
                                <xsl:call-template name="interpolate-variables">
                                    <xsl:with-param name="value" select="concat('{', substring-after($value, '{'))"/>
                                </xsl:call-template>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$value" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
