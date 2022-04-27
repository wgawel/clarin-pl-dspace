<!--
	/* Created for LINDAT/CLARIN */
	Rendering specific to the item display page.
	Author: Amir Kamran 
-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:atom="http://www.w3.org/2005/Atom" xmlns:ore="http://www.openarchives.org/ore/terms/"
	xmlns:oreatom="http://www.openarchives.org/ore/atom/" xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xalan="http://xml.apache.org/xalan" xmlns:encoder="xalan://java.net.URLEncoder"
	xmlns:java="http://xml.apache.org/xalan/java" 
	xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
	xmlns:solrClientUtils="org.apache.solr.client.solrj.util.ClientUtils"
	xmlns:confman="org.dspace.core.ConfigurationManager"
    xmlns:str="http://exslt.org/strings"
    xmlns:isocodes="cz.cuni.mff.ufal.IsoLangCodes"
    xmlns:ft="cz.cuni.mff.ufal.FileTreeViewGenerator"
	exclude-result-prefixes="xalan java encoder solrClientUtils i18n dri mets dim xlink xsl confman util isocodes ft">
	
	<xsl:output indent="yes" />

	<xsl:variable name="contextPath" select="concat('/', substring-after(substring-after(confman:getProperty('dspace.url'), '://'), '/'))"/>
	<xsl:template name="itemSummaryView-DIM">
		<!-- Generate the info about the item from the metadata section -->
		<xsl:apply-templates
			select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
			mode="itemSummaryView-DIM" />

		<!-- Generate the bitstream information from the file section -->
		<xsl:choose>
			<xsl:when
				test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
				<xsl:apply-templates
					select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
					<xsl:with-param name="context" select="." />
					<xsl:with-param name="primaryBitstream"
						select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID" />
				</xsl:apply-templates>
			</xsl:when>
			<!-- Special case for handling ORE resource maps stored as DSpace bitstreams -->
			<xsl:when test="./mets:fileSec/mets:fileGrp[@USE='ORE']">
				<xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='ORE']" />
			</xsl:when>			
		</xsl:choose>

		<!-- Generate the Creative Commons license information from the file section 
			(DSpace deposit license hidden by default) -->
		<!-- xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/ -->
		
	</xsl:template>


	<xsl:template match="dim:dim" mode="itemSummaryView-DIM">
		<div class="item-summary-view-metadata">
			<xsl:call-template name="itemSummaryView-DIM-fields" />
		</div>		
	</xsl:template>
	
	<!-- An item rendered in the detailView pattern, the "full item record" view of a DSpace item in Manakin. -->
    <xsl:template name="itemDetailView-DIM" priority="10">
        <!-- Output all of the metadata about the item from the metadata section -->
        <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
            mode="itemDetailView-DIM"/>

		<!-- Generate the bitstream information from the file section -->
        <xsl:choose>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                    <xsl:with-param name="context" select="."/>
                    <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                </xsl:apply-templates>
            </xsl:when>
            <!-- Special case for handling ORE resource maps stored as DSpace bitstreams -->
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='ORE']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='ORE']"/>
            </xsl:when>
        </xsl:choose>

    </xsl:template>
	

	<xsl:template name="itemSummaryView-DIM-fields">
		<xsl:param name="clause" select="'1'" />
		<xsl:param name="phase" select="'even'" />
		<xsl:variable name="otherPhase">
			<xsl:choose>
				<xsl:when test="$phase = 'even'">
					<xsl:text>odd</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>even</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>


		<xsl:choose>
			<!-- identifier.uri row -->
			<xsl:when test="$clause = 2 and (dim:field[@element='identifier' and @qualifier='uri'])">
				<div class="refbox">
					<xsl:attribute name="handle">
                        <xsl:value-of select="substring-after(/mets:METS/@ID,'hdl:')" />
                    </xsl:attribute>               		
					<xsl:attribute name="title">
                        <xsl:value-of select="dim:field[@element='title'][not(@qualifier)]/node()" />
                    </xsl:attribute>
               		&#160;               		
				</div>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>


			<!-- Title row -->
			<xsl:when test="$clause = 1">
				<!-- printer icon -->
				<a href="javascript:window.print();" class="print-link pull-right" style="position: relative; top: 10px;">
					<i class="fa fa-print">&#160;</i>
				</a>

				<xsl:choose>
					<xsl:when
						test="count(dim:field[@element='title'][not(@qualifier)]) &gt; 1">
						<h3 id="item-title" class="item-name">
							<xsl:value-of
								select="dim:field[@element='title'][not(@qualifier)][1]/node()" />
						</h3>
						<div>
							<span class="bold">
								<i18n:text>xmlui.dri2xhtml.METS-1.0.item-title</i18n:text>
							</span>
							<span>
								<xsl:for-each select="dim:field[@element='title'][not(@qualifier)]">
									<xsl:value-of select="./node()" />
									<xsl:if
										test="count(following-sibling::dim:field[@element='title'][not(@qualifier)]) != 0">
										<xsl:text>; </xsl:text>
										<br />
									</xsl:if>
								</xsl:for-each>
							</span>
						</div>
					</xsl:when>
					<xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) = 1">
						<h3 style="border-bottom: 2px solid #F0F0F0; padding-bottom: 5px;">
							<xsl:value-of
								select="dim:field[@element='title'][not(@qualifier)][1]/node()" />
						</h3>
					</xsl:when>
					<xsl:otherwise>
						<h3>
							<i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
						</h3>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<!-- Author(s) row -->
			<xsl:when
				test="$clause = 3 and (dim:field[@element='contributor'][@qualifier='author' or @qualifier='other'] or dim:field[@element='creator'])">

					<xsl:if test="dim:field[@mdschema='local' and @element='branding']">
						<div class="item-branding label pull-right">
							<a>
								<xsl:attribute name="href">
									<xsl:copy-of select="$contextPath"/>
									<xsl:value-of select="concat('/discover?filtertype=branding&amp;filter_relational_operator=equals&amp;filter=',encoder:encode(dim:field[@mdschema='local' and @element='branding'][1]/node()))"/>
								</xsl:attribute>
								<xsl:value-of select="dim:field[@mdschema='local' and @element='branding'][1]/node()"/>
							</a>
						</div>
					</xsl:if>				
				
					<dl id="item-authors" class="dl-horizontal" style="clear:both;">
					<dt style="text-align: left">
						<i class="fa fa-pencil">&#160;</i>
						<span><i18n:text>xmlui.UFAL.artifactbrowser.authors</i18n:text></span>
					</dt>
					<dd style="padding-right: 40px;">
					<xsl:choose>
						<xsl:when test="dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']">
							<xsl:call-template name="authors_with_short_summary_view" />
						</xsl:when>
						<xsl:when test="dim:field[@element='creator']">
							<xsl:for-each select="dim:field[@element='creator']">
								<a>
								<xsl:attribute name="href">
									<xsl:copy-of select="$contextPath"/>
									<xsl:value-of select="concat('/discover?filtertype=author&amp;filter_relational_operator=equals&amp;filter=',encoder:encode(node()))"/>
								</xsl:attribute>

								<xsl:copy-of select="node()" />
								</a>
								<xsl:if
									test="count(following-sibling::dim:field[@element='creator']) != 0">
									<xsl:text>; </xsl:text>
								</xsl:if>
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
						</xsl:otherwise>
					</xsl:choose>

					</dd>
				</dl>				

				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<!-- PID row -->
			<xsl:when test="$clause = 4 and (dim:field[@element='identifier' and @qualifier='uri'])">
				<dl id="item-pid" class="dl-horizontal" style="clear:both;">
                    <dt style="text-align: left">
                        <i class="fa fa-share">&#160;</i>
                        <span><i18n:text>xmlui.dri2xhtml.METS-1.0.item-pid</i18n:text></span>
                    </dt>
                    <dd style="padding-right: 40px;">
                        <a id="item_pid">
                            <xsl:attribute name="href">
                                <xsl:value-of select="dim:field[@element='identifier' and @qualifier='uri']" />
                            </xsl:attribute>
                            <xsl:value-of select="dim:field[@element='identifier' and @qualifier='uri']" />
                        </a>
                        <button class="repo-copy-btn pull-right" data-clipboard-target="#item_pid" />
                    </dd>
				</dl>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<!-- Project URL(s) row -->
			<xsl:when
				test="$clause = 5 and dim:field[@element='source'][@qualifier='uri']">
						<dl id="project-url" class="dl-horizontal">
							<dt style="text-align: left">
								<i class="fa fa-link">&#160;</i>
								<i18n:text>xmlui.dri2xhtml.METS-1.0.item-project-uri</i18n:text>
							</dt>
							<dd>
								<xsl:for-each
									select="dim:field[@element='source' and @qualifier='uri']">
								<xsl:if test="self::node()[text()!='']">
									<a target="_blank">
										<xsl:attribute name="href">
		                            <xsl:copy-of select="./node()" />
		                        </xsl:attribute>
										<xsl:copy-of select="./node()" />
									</a>
									<xsl:if
										test="count(following-sibling::dim:field[@element='source' and @qualifier='uri']) != 0">
										<br />
									</xsl:if>
								</xsl:if>
								</xsl:for-each>
							</dd>
						</dl>
					
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<!-- Demo URL(s) row -->
			<xsl:when
				test="$clause = 6 and dim:field[@mdschema='local' and @element='demo' and @qualifier='uri']">
						<dl id="demo-url" class="dl-horizontal">
							<dt style="text-align: left">
								<i class="fa fa-external-link">&#160;</i>
								<i18n:text>xmlui.dri2xhtml.METS-1.0.item-demo-uri</i18n:text>
							</dt>
							<dd>
								<xsl:for-each
									select="dim:field[@mdschema='local' and @element='demo' and @qualifier='uri']">
								<xsl:if test="self::node()[text()!='']">
									<a target="_blank">
										<xsl:attribute name="href">
		                            <xsl:copy-of select="./node()" />
		                        </xsl:attribute>
										<xsl:copy-of select="./node()" />
									</a>
									<xsl:if
										test="count(following-sibling::dim:field[@mdschema='local' and @element='demo' and @qualifier='uri']) != 0">
										<br />
									</xsl:if>
								</xsl:if>
								</xsl:for-each>
							</dd>
						</dl>

				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<!-- referenced by -->
			<xsl:when
					test="$clause = 7 and dim:field[@element='relation'][@qualifier='isreferencedby']">
				<dl id="relation-isreferencedby" class="dl-horizontal">
					<dt style="text-align: left">
						<i class="fa fa-link">&#160;</i>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-relation-isreferencedby</i18n:text>
					</dt>
					<dd>
						<xsl:for-each
								select="dim:field[@element='relation' and @qualifier='isreferencedby']">
							<xsl:if test="self::node()[text()!='']">
								<a target="_blank">
									<xsl:attribute name="href">
										<xsl:copy-of select="./node()" />
									</xsl:attribute>
									<xsl:copy-of select="./node()" />
								</a>
								<xsl:if
										test="count(following-sibling::dim:field[@element='relation' and @qualifier='isreferencedby']) != 0">
									<br />
								</xsl:if>
							</xsl:if>
						</xsl:for-each>
					</dd>
				</dl>

				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<!-- date.issued row -->
			<xsl:when
				test="$clause = 8 and (dim:field[@element='date' and @qualifier='issued'])">
				<dl id="date-issued" class="dl-horizontal">
					<dt style="text-align: left">
						<i class="fa fa-calendar">&#160;</i>					
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>
					</dt>
					<dd>
						<xsl:call-template name="date_issued_formatted_value"/>
					</dd>
				</dl>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<!-- type row -->
			<xsl:when
				test="$clause = 9 and ((dim:field[@element='type' and not(@qualifier)]) or (dim:field[@qualifier='mediaType']))">
					<dl id="item-type" class="dl-horizontal">
					<dt style="text-align: left">
						<i class="fa fa-tag">&#160;</i>
												<i18n:text>xmlui.dri2xhtml.METS-1.0.item-type</i18n:text>
					</dt>
					<dd>
						<xsl:for-each
								select="dim:field[(@element='type' and not(@qualifier)) or @qualifier='mediaType']">
							<xsl:sort select="." />
							<a>
								<xsl:attribute name="href">
									<xsl:value-of
											select="concat($contextPath, '/discover?filtertype=type&amp;filter_relational_operator=equals&amp;filter=',encoder:encode(.))"/>
								</xsl:attribute>
								<xsl:value-of select="." />
							</a>
							<xsl:if test="position() != last()">
								<xsl:text>, </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</dd>
				</dl>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>				

			<!-- size row -->
			<xsl:when test="$clause = 10">
					<xsl:variable name="sizeInfo">
						<xsl:choose>
							<xsl:when test="dim:field[@mdschema='local' and @element='size' and @qualifier='info'][1]/node()">
								<xsl:copy-of select="dim:field[@mdschema='local' and @element='size' and @qualifier='info']"/>
							</xsl:when>
							<xsl:when test="dim:field[@mdschema='metashare' and @element='ResourceInfo#TextInfo#SizeInfo' and @qualifier='size']">
								<xsl:call-template name="convert_metashare_size">
									<xsl:with-param name="size" select="dim:field[@mdschema='metashare' and @element='ResourceInfo#TextInfo#SizeInfo' and @qualifier='size']" />
									<xsl:with-param name="unit" select="dim:field[@mdschema='metashare' and @element='ResourceInfo#TextInfo#SizeInfo' and @qualifier='sizeUnit']" />
									<xsl:with-param name="multiplier" select="dim:field[@mdschema='metashare' and @element='ResourceInfo#TextInfo#SizeInfo' and @qualifier='sizeUnitMultiplier']" />
								</xsl:call-template>
							</xsl:when>
						</xsl:choose>
					</xsl:variable>
					<xsl:if test="not($sizeInfo='')">
							
						<dl id="item-type" class="dl-horizontal">
						<dt style="text-align: left">
							<i class="fa fa-arrows-alt">&#160;</i>
													<i18n:text>xmlui.dri2xhtml.METS-1.0.item-size-info</i18n:text>
						</dt>
						<dd>
							<xsl:for-each select="xalan:nodeset($sizeInfo)/node()">
								<xsl:value-of select="substring-before(.,'@@')" />
								<xsl:text> </xsl:text>
								<xsl:call-template name="plural-to-singular-en">
										<xsl:with-param name="value" select="substring-after(.,'@@')" />
										<xsl:with-param name="number" select="substring-before(.,'@@')" />
								</xsl:call-template>
								<xsl:if test="position()!=last()"><xsl:text>, </xsl:text></xsl:if>
							</xsl:for-each>
						</dd>
						</dl>
					</xsl:if>
					<xsl:call-template name="itemSummaryView-DIM-fields">
						<xsl:with-param name="clause" select="($clause + 1)" />
						<xsl:with-param name="phase" select="$otherPhase" />
					</xsl:call-template>
			</xsl:when>

			<!-- type languages -->
			<xsl:when
				test="$clause = 11 and (dim:field[@element='language' and @qualifier='iso'])">
					<dl id="item-languages" class="dl-horizontal">
					<dt style="text-align: left">
						<i class="fa fa-flag ">&#160;</i>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-langs</i18n:text>
					</dt>
					<dd>
					
					<xsl:choose>
						<xsl:when test="dim:field[@element='language'][@qualifier='iso']">
							<xsl:for-each
								select="dim:field[@element='language'][@qualifier='iso']">
								<xsl:sort select="isocodes:getLangForCode(node())" />
								<a>
									<xsl:attribute name="href">
										<xsl:copy-of select="$contextPath"/>
										<xsl:value-of select="concat('/discover?filtertype=language&amp;filter_relational_operator=equals&amp;filter=',isocodes:getLangForCode(node()))"/>
									</xsl:attribute>
									<span class="language-iso-code"><xsl:copy-of select="isocodes:getLangForCode(node())" /></span>
								</a>
								<xsl:if
									test="position() != last()">
									<xsl:text>, </xsl:text>
								</xsl:if>
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<i18n:text>xmlui.dri2xhtml.METS-1.0.no-language</i18n:text>
						</xsl:otherwise>
					</xsl:choose>
						
					</dd>
				</dl>
				<!-- go to next round -->
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<!-- Abstract row -->
			<!-- xsl:when
				test="$clause = 10 and (dim:field[@element='description' and @qualifier='abstract' and descendant::text()])">
				<div class="simple-item-view-description">
					<h3>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>
						:
					</h3>
					<div>
						<xsl:if
							test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
							<div class="spacer">&#160;</div>
						</xsl:if>
						<xsl:for-each
							select="dim:field[@element='description' and @qualifier='abstract']">
							<xsl:choose>
								<xsl:when test="node()">
									<xsl:copy-of select="node()" />
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>&#160;</xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:if
								test="count(following-sibling::dim:field[@element='description' and @qualifier='abstract']) != 0">
								<div class="spacer">&#160;</div>
							</xsl:if>
						</xsl:for-each>
						<xsl:if
							test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
							<div class="spacer">&#160;</div>
						</xsl:if>
					</div>
				</div>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when-->

			<!-- Description row -->
			<xsl:when
				test="$clause = 12 and (dim:field[@element='description' and not(@qualifier)])">
				<dl id="item-description" class="dl-horizontal linkify">
					<dt style="text-align: left">
						<i class="fa fa-file-text-o">&#160;</i>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>
					</dt>
					<dd style="white-space: pre-line;">
						<xsl:if
							test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1 and not(count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1)">
							<div class="spacer">&#160;</div>
						</xsl:if>
						<xsl:for-each
							select="dim:field[@element='description' and not(@qualifier)]">
							<xsl:copy-of select="./node()" />
							<xsl:if
								test="count(following-sibling::dim:field[@element='description' and not(@qualifier)]) != 0">
								<div class="spacer">&#160;</div>
							</xsl:if>
						</xsl:for-each>
						<xsl:if
							test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1">
							<div class="spacer">&#160;</div>
						</xsl:if>
					</dd>
				</dl>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>
			
			<!-- Publisher row -->
			<xsl:when
				test="$clause = 13 and (dim:field[@element='publisher' and not(@qualifier)])">
				<dl id="item-publisher" class="dl-horizontal">
					<dt style="text-align: left">
						<i class="fa fa-copy">&#160;</i>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-publisher</i18n:text>
					</dt>
					<dd>
						<xsl:if
							test="count(dim:field[@element='publisher' and not(@qualifier)]) = 0">
							<div class="spacer">&#160;</div>
						</xsl:if>						
						<xsl:for-each
							select="dim:field[@element='publisher' and not(@qualifier)]">
							<a>

								<xsl:attribute name="href">
									<xsl:copy-of select="$contextPath"/>
									<xsl:value-of select="concat('/discover?filtertype=publisher&amp;filter_relational_operator=equals&amp;filter=',encoder:encode(node()))"/>
								</xsl:attribute>
								<xsl:copy-of select="./node()" />									
							</a>													
							<xsl:if
								test="count(following-sibling::dim:field[@element='publisher' and not(@qualifier)]) != 0">
								<div class="spacer">&#160;</div>
							</xsl:if>
						</xsl:for-each>						
					</dd>
				</dl>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<!-- Sponsors row -->
			<xsl:when
				test="$clause = 14 and ((dim:field[@element='sponsor' and not(@qualifier)]) or (dim:field[@element='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo'] and dim:field[@qualifier='projectName']))">
				<dl id="item-sponsor" class="dl-horizontal">
					<dt style="text-align: left">
						<i class="fa fa-money">&#160;</i>
						<i18n:text>xmlui.UFAL.artifactbrowser.acknowledgement</i18n:text>
					</dt>
					<dd>
						<xsl:variable name="my_elem">
							<xsl:choose>
								<xsl:when test="dim:field[@element='sponsor' and not(@qualifier)]">
									<xsl:copy-of select="dim:field[@element='sponsor' and not(@qualifier)]"/>
								</xsl:when>
								<xsl:when test="dim:field[@element='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo' and @qualifier='projectName']">
									<xsl:copy-of select="dim:field[@element='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo' and @qualifier='projectName']"/>
								</xsl:when>
							</xsl:choose>	
						</xsl:variable>
						<xsl:if
							test="count(xalan:nodeset($my_elem)/node()) = 0">
							<div class="spacer">&#160;</div>
						</xsl:if>						
						<xsl:for-each
							select="xalan:nodeset($my_elem)/node()">
								<div class="funding">
									<xsl:choose>
										<xsl:when test="xalan:nodeset($my_elem)/dim:field[@element='sponsor']"> 
											<xsl:for-each select="xalan:tokenize(./node(),'@@')">
												<xsl:choose>
													<xsl:when test="position()=1">
														<p class="funding-org"><xsl:value-of select="."/></p>
													</xsl:when>
													<xsl:when test="position()=2">
														<p class="funding-code">
															<i18n:translate>
																<i18n:text>xmlui.UFAL.artifactbrowser.project.code</i18n:text>
																<i18n:param><xsl:value-of select="."/></i18n:param>
															</i18n:translate>
														</p>
													</xsl:when>
													<xsl:when test="position()=3">
														<p class="funding-name">
															<i18n:translate>
																<i18n:text>xmlui.UFAL.artifactbrowser.project.name</i18n:text>
																<i18n:param><xsl:value-of select="."/></i18n:param>
															</i18n:translate>
														</p>
													</xsl:when>
												</xsl:choose>
											</xsl:for-each>
										</xsl:when>
										<xsl:otherwise>
											<p class="funding-name"><xsl:value-of select="concat('Project name: ' , substring-after(.,'-'))"/></p>
										</xsl:otherwise>
									</xsl:choose>
								</div>
								
							<xsl:if
								test="count(xalan:nodeset($my_elem)/node()) != position()">
								<div class="spacer">&#160;</div>
							</xsl:if>
						</xsl:for-each>						
					</dd>
				</dl>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>
			
			<!-- Subject keywords -->
			<xsl:when
				test="$clause = 15 and (dim:field[@element='subject' and not(@qualifier)])">
				<dl id="item-subject" class="dl-horizontal">
					<dt style="text-align: left">
						<i class="fa fa-tags">&#160;</i>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-subject</i18n:text>
					</dt>
					<dd>
						<xsl:for-each
							select="dim:field[@element='subject' and not(@qualifier)]">
							<span class="tag">
								<a class="label label-primary">
									<xsl:attribute name="href">
										<xsl:copy-of select="$contextPath"/>
										<xsl:value-of select="concat('/discover?filtertype=subject&amp;filter_relational_operator=equals&amp;filter=',encoder:encode(node()))"/>
									</xsl:attribute>
									<xsl:copy-of select="node()" />
								</a>
							</span>														
						</xsl:for-each>
					</dd>
				</dl>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>
			
			<!-- Collections -->
  			<xsl:when
				test="$clause = 16 and $ufal-collection-references">
				<dl id="item-subject" class="dl-horizontal">
					<dt style="text-align: left">
						<i class="fa fa-sitemap">&#160;</i>
						<i18n:text>xmlui.ufal.METS-1.0.item-collections</i18n:text>
					</dt>
					<dd>						
						<xsl:for-each select="$ufal-collection-references">
							<span><a>
							<xsl:variable name="collection" select="document(concat('cocoon:/',./@url))" />							
							<xsl:attribute name="href"><xsl:value-of select="$collection/mets:METS/@OBJID" /></xsl:attribute>
							<xsl:copy-of select="$collection/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='title'][1]" />
							</a></span>
							<xsl:if test="count(following-sibling::dri:reference) != 0">
								<xsl:text>, </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</dd>
				</dl>
				<xsl:call-template name="itemSummaryView-DIM-fields">
					<xsl:with-param name="clause" select="($clause + 1)" />
					<xsl:with-param name="phase" select="$otherPhase" />
				</xsl:call-template>
			</xsl:when>

			<xsl:when test="$clause = 17 and $ds_item_view_toggle_url != ''">

                <!-- other versions -->
                <xsl:choose>
                    <xsl:when
                            test="count(dim:field[@element='relation' and @qualifier='isreplacedby' and @mdschema='dc']) &gt;= 1">
                        <div class="alert container" id="versions">
                            <div class="row">
                                <div class="col-sm-1">
                                    <i class="fa fa-info-circle fa-3x">&#160;</i>
                                </div>
                                <!-- replacedby info -->
                                <xsl:if test="count(dim:field[@element='relation' and @qualifier='isreplacedby' and @mdschema='dc']) &gt;= 1">
                                    <div id="replaced_by_alert" class="col-sm-11">
                                       <span>
                                            <xsl:choose>
                                                <xsl:when test="count(dim:field[@element='relation' and @qualifier='isreplacedby' and @mdschema='dc']) = 1">
                                                        <i18n:text>xmlui.UFAL.artifactbrowser.item_view.replaced_one</i18n:text><br/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                        <i18n:text>xmlui.UFAL.artifactbrowser.item_view.replaced_many</i18n:text><br/>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                            <xsl:for-each select="dim:field[@element='relation' and @qualifier='isreplacedby' and @mdschema='dc']">
                                                <div>
                                                    <a>
                                                        <xsl:attribute name="href">
                                                                <xsl:value-of select="." />
                                                        </xsl:attribute>
                                                        <xsl:value-of select="." />
                                                    </a>
                                                </div>
                                            </xsl:for-each>
                                       </span>
                                    </div>
                                </xsl:if>
                            </div>
                            <xsl:call-template name="versions-dropdown" />
                        </div>
                    </xsl:when>
                    <xsl:when
                            test="count(dim:field[@element='relation' and @qualifier='replaces' and @mdschema='dc']) &gt;= 1">
                        <dl id="item-versions" class="dl-horizontal">
                        <dt style="text-align: left">
                            <i class="fa fa-code-fork">&#160;</i>
                            <i18n:text>xmlui.UFAL.artifactbrowser.item_view.versions_dt</i18n:text>
                        </dt>
                        <dd>
                            <xsl:call-template name="versions-dropdown" />
                        </dd>
                        </dl>
                    </xsl:when>
                </xsl:choose>


                    <dl class="dl-horizontal">
                        <dt style="text-align: left">
                            <a class="btn btn-link" style="padding-left:0">
                                <xsl:attribute name="href"><xsl:value-of select="$ds_item_view_toggle_url" /></xsl:attribute>
                                <i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_full</i18n:text>
                            </a>
                        </dt>
                        <dd class="text-right">
                            &#160;
                        </dd>
                    </dl>

            </xsl:when>

			<!-- recurse without changing phase if we didn't output anything -->
			<xsl:otherwise>
				<!-- IMPORTANT: This test should be updated if clauses are added! -->
				<xsl:if test="$clause &lt; 17">
					<xsl:call-template name="itemSummaryView-DIM-fields">
						<xsl:with-param name="clause" select="($clause + 1)" />
						<xsl:with-param name="phase" select="$phase" />
					</xsl:call-template>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>

		<!-- Generate the Creative Commons license information from the file section 
			(DSpace deposit license hidden by default) -->
		<!-- xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/ -->
				
	</xsl:template>


	<xsl:template match="dim:dim" mode="itemDetailView-DIM">
		<xsl:if test="dim:field[@element='identifier' and @qualifier='uri']">
				<div class="refbox">
					<xsl:attribute name="handle">
                        <xsl:value-of select="substring-after(/mets:METS/@ID,'hdl:')" />
                    </xsl:attribute>               		
					<xsl:attribute name="title">
                        <xsl:value-of select="dim:field[@element='title'][not(@qualifier)]/node()" />
                    </xsl:attribute>
                    &#160;
				</div>		
		</xsl:if>	
		<table class="table">
			<xsl:apply-templates mode="itemDetailView-DIM" select="dim:field[@mdschema != 'metashare' or (@qualifier != 'resourceType' and @qualifier != 'description' and  @qualifier != 'resourceName' and @qualifier != 'license')]" />
		</table>
		<span class="Z3988">
			<xsl:attribute name="title">
                 <xsl:call-template name="renderCOinS" />
            </xsl:attribute>
			&#xFEFF; <!-- non-breaking space to force separating the end tag -->
		</span>
	</xsl:template>

	<xsl:template match="dim:field" mode="itemDetailView-DIM">
		<tr>
			<td class="label-cell">
				<xsl:if test="./@mdschema != 'local'">
					<xsl:value-of select="./@mdschema" />
					<xsl:text>.</xsl:text>
				</xsl:if>
				<xsl:value-of select="./@element" />
				<xsl:if test="./@qualifier">
					<xsl:text>.</xsl:text>
					<xsl:value-of select="./@qualifier" />
				</xsl:if>
			</td>
			<td class="linkify">
				<xsl:choose>
					<xsl:when test="./@mdschema = 'local'">
						<xsl:value-of select="java:replaceAll(java:java.lang.String.new(.),'@@', ' ')"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:copy-of select="./node()" />
					</xsl:otherwise>
				</xsl:choose>
				<xsl:if test="./@authority and ./@confidence">
					<xsl:call-template name="authorityConfidenceIcon">
						<xsl:with-param name="confidence" select="./@confidence" />
					</xsl:call-template>
				</xsl:if>
			</td>
			<!-- we do not use metadata language properly 
      <td>
				<xsl:value-of select="./@language" />
			</td>
			-->
		</tr>
	</xsl:template>

	<!--dont render the item-view-toggle automatically in the summary view, 
		only when it get's called -->
	<xsl:template
		match="dri:p[contains(@rend , 'item-view-toggle') and
        (preceding-sibling::dri:referenceSet[@type = 'summaryView'] or following-sibling::dri:referenceSet[@type = 'summaryView'])]">
	</xsl:template>

	<!-- dont render the head on the item view page -->
	<xsl:template match="dri:div[@n='item-view']/dri:head"
		priority="5">
	</xsl:template>

	<xsl:template match="mets:fileGrp[@USE='CONTENT']">
		<xsl:param name="context" />
		<xsl:param name="primaryBitstream" select="-1" />			
		<div id="files_section">			
			<h4>
				<i class="fa fa-paperclip">&#160;</i>
				<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text>			
			</h4>				
				<xsl:if test="/mets:METS/@OBJID">							
					<xsl:variable name="download-all-url"><xsl:value-of select="concat(/mets:METS/@OBJID,'/allzip')" /></xsl:variable>
					<xsl:call-template name="download-all">
						<xsl:with-param name="download-all-url" select="$download-all-url" />
					</xsl:call-template>
				</xsl:if>						   			
					
			<!-- Generate UFAL licenses -->
			<xsl:apply-templates select="//mets:mdWrap[@OTHERMDTYPE='UFAL_LICENSES']/mets:xmlData/license" />
			<div class="thumbnails">
<!-- 			<xsl:choose>
					If one exists and it's of text/html MIME type, only display the 
						primary bitstream
					<xsl:when test="mets:file[@ID=$primaryBitstream]/@MIMETYPE='text/html'">
						<xsl:apply-templates select="mets:file[@ID=$primaryBitstream]">
							<xsl:with-param name="context" select="$context" />
						</xsl:apply-templates>
					</xsl:when>
					Otherwise, iterate over and display all of them
					<xsl:otherwise>
 -->					
	 					<xsl:apply-templates select="mets:file">
							<!--Do not sort any more bitstream order can be changed -->
							<!--<xsl:sort data-type="number" select="boolean(./@ID=$primaryBitstream)" 
								order="descending" /> -->
							<!--<xsl:sort select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/> -->
							<xsl:with-param name="context" select="$context" />
						</xsl:apply-templates>
<!-- 				</xsl:otherwise>
				</xsl:choose> -->
			</div>
		</div>
	</xsl:template>

	<xsl:template match="mets:file">
		<xsl:param name="context" select="." />
		<xsl:variable name="admid" select="@ADMID" />
        <xsl:variable name="formatted-file-size">
            <xsl:call-template name="format-size">                   
                <xsl:with-param name="size" select="@SIZE" />
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="md5_checksum" select="@CHECKSUM"/>
	<xsl:variable name="thumbnail">
		<xsl:if test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=current()/@GROUPID]">
			<xsl:value-of select="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=current()/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
		</xsl:if>
	</xsl:variable>
			<div class="thumbnail" style="margin-bottom: 10px;">
				<a>
					<xsl:attribute name="href">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
                    </xsl:attribute>
					<xsl:choose>
						<xsl:when test="@MIMETYPE='video/mp4'">
							<div style="text-align: left;">
								<!-- preload="metadata" would appear in access.log -->
								<video controls="controls" preload="none">
									<xsl:attribute name="height"><xsl:value-of select="240"/></xsl:attribute>
									<xsl:if test="string($thumbnail)">
										<xsl:attribute name="poster"><xsl:value-of select="$thumbnail" /></xsl:attribute>
									</xsl:if>
									<source>
										<xsl:attribute name="src"><xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/></xsl:attribute>
										<xsl:attribute name="type"><xsl:value-of select="@MIMETYPE"/></xsl:attribute>
									</source>
									Your browser does not support the video tag.
								</video>
							</div>
						</xsl:when>
						<xsl:when test="string($thumbnail)">
							<img alt="Thumbnail" class="pull-right">
								<xsl:attribute name="src">
									<xsl:value-of select="$thumbnail" />
								</xsl:attribute>
								<xsl:attribute name="style">height: <xsl:value-of select="$thumbnail.maxheight"/>px;</xsl:attribute>
							</img>
						</xsl:when>
						<xsl:otherwise>
							<xsl:variable name="mime-type" select="translate(self::node()/@MIMETYPE,'/','-')" />
							<img class="pull-right" alt="Icon" src="{$theme-path}/images/mime/{$mime-type}.png" onerror="this.src='{$theme-path}/images/mime/application-octet-stream.png'" style="height: {$thumbnail.maxheight}px;" />				
						</xsl:otherwise>
					</xsl:choose>
				</a>
				<dl class="dl-horizontal">
					<dt>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-name</i18n:text>
					</dt>
					<dd>
						<xsl:attribute name="title"><xsl:value-of
							select="mets:FLocat[@LOCTYPE='URL']/@xlink:title" /></xsl:attribute>
						<xsl:value-of
							select="mets:FLocat[@LOCTYPE='URL']/@xlink:title" />
					</dd>
					
					<xsl:for-each select="mets:Local/*[local-name()!='file' and local-name()!='redirectToURL' ]">
						<dt>
							<xsl:value-of
									select="local-name()" />
						</dt>
						<dd>
							<xsl:value-of
									select="./node()" />
						</dd>
					</xsl:for-each>
					
					<dt>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text>
					</dt>
                    <dd>
                       <xsl:copy-of select="$formatted-file-size" />
				    </dd>
					<dt>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text>
					</dt>
					<dd>
						<xsl:call-template name="getFileTypeDesc">
							<xsl:with-param name="mimetype">
								<xsl:value-of select="substring-before(@MIMETYPE,'/')" />
								<xsl:text>/</xsl:text>
								<xsl:value-of select="substring-after(@MIMETYPE,'/')" />
							</xsl:with-param>
						</xsl:call-template>
					</dd>
					<xsl:if test="mets:FLocat[@LOCTYPE='URL']/@xlink:label != ''">
						<dt>
							<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-description</i18n:text>
						</dt>
						<dd>
								 	<xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label" />
						</dd>
					</xsl:if>
					<dt>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-checksum</i18n:text>
					</dt>
					<dd>
						<xsl:value-of select="$md5_checksum"/>
					</dd>
				</dl>
				<a class="filebutton label label-info">
					<xsl:attribute name="href">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
                    </xsl:attribute>				
                    <i class="fa fa-chevron-circle-down">&#160;</i>
                    <i18n:translate>
                        <i18n:text>xmlui.UFAL.artifactbrowser.item-download-file</i18n:text>
                        <i18n:param><xsl:copy-of select="$formatted-file-size"/></i18n:param>
                    </i18n:translate>
				</a>
				<!-- previews -->
				<xsl:if test="/mets:METS/mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license/@label='PUB'">
                    <xsl:if test="mets:Local/mets:file">
                        <a class="filebutton label label-info" role="button" data-toggle="collapse">
                            <xsl:attribute name="href">
                                <xsl:text>#file_</xsl:text><xsl:value-of select="@ID" />
                            </xsl:attribute>
                            <i class="fa fa-eye">&#160;</i>
                            <i18n:text>xmlui.UFAL.artifactbrowser.item_view.preview</i18n:text>
                        </a>
                        <div class="collapse">
                            <xsl:attribute name="id">
                                <xsl:text>file_</xsl:text><xsl:value-of select="@ID" />
                            </xsl:attribute>
                            <div class="panel panel-info" style="margin: 5px 1px 1px 1px;">
                                <div class="bold panel-heading text-center" style="height: auto; padding: 0px;">
                                    <i class="fa fa-eye">&#160;</i>
                                    <i18n:text>xmlui.UFAL.artifactbrowser.item_view.file_preview</i18n:text>
                                    <a role="button" data-toggle="collapse" class="pull-right">
                                        <xsl:attribute name="href">
                                            <xsl:text>#file_</xsl:text><xsl:value-of select="@ID" />
                                        </xsl:attribute>
                                        <i class="fa fa-remove">&#160;</i>
                                    </a>
                                </div>
                                <div class="panel-body" style="max-height: 200px; overflow: scroll;">
                                    <xsl:variable name="files">
                                        <xsl:copy-of select="mets:Local/mets:file"/>
                                    </xsl:variable>
                                    <xsl:choose>
                                        <xsl:when test="@MIMETYPE='text/plain'">
                                            <pre>
                                                <xsl:value-of select="$files"/> . . .
                                            </pre>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <!-- output is not escaped as ft:parse returns html element that we
                                                 need to render -->
                                            <xsl:value-of select="ft:parse($files)" disable-output-escaping="yes" />
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </div>
                            </div>
                        </div>
                    </xsl:if>
                    <xsl:if test="@MIMETYPE='text/html'" >
                        <a class="filebutton label label-info" role="button" data-toggle="collapse">
                            <xsl:attribute name="href">
                                <xsl:text>#file_</xsl:text><xsl:value-of select="@ID" />
                            </xsl:attribute>
                            <i class="fa fa-eye">&#160;</i>
                            <i18n:text>xmlui.UFAL.artifactbrowser.item_view.preview</i18n:text>
                        </a>
                        <div class="collapse">
                            <xsl:attribute name="id">
                                <xsl:text>file_</xsl:text><xsl:value-of select="@ID" />
                            </xsl:attribute>
                            <div class="panel panel-info" style="margin: 5px 1px 1px 1px;">
                                <div class="bold panel-heading text-center" style="height: auto; padding: 0px;">
                                    <i class="fa fa-eye">&#160;</i>
                                    <i18n:text>xmlui.UFAL.artifactbrowser.item_view.file_preview</i18n:text>
                                    <a role="button" data-toggle="collapse" class="pull-right">
                                        <xsl:attribute name="href">
                                            <xsl:text>#file_</xsl:text><xsl:value-of select="@ID" />
                                        </xsl:attribute>
                                        <i class="fa fa-remove">&#160;</i>
                                    </a>
                                </div>
                                <div class="panel-body" style="max-height: 500px; overflow: hidden; padding: 0px;">
                                     <iframe frameborder="0" scrolling="yes" height="500" width="100%">
                                        <xsl:attribute name="src">
                                            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
                                        </xsl:attribute>
                                        &#160;
                                     </iframe>
                                </div>
                            </div>
                        </div>
                    </xsl:if>
				</xsl:if>
			</div>			
	</xsl:template>

	<xsl:template match="license">
		<div class="alert alert-info text-center">
			<i18n:translate>
				<i18n:text>xmlui.UFAL.artifactbrowser.item_view.licensed_under</i18n:text>
				<i18n:param>
					<div class="label label-{@label}">
						<xsl:value-of select="@label_title" />
					</div>
				</i18n:param>
			</i18n:translate>
		<a>
		<xsl:attribute name="href">
			<xsl:value-of select="@url" />
		</xsl:attribute>
		<xsl:value-of select="./node()" />
		</a>
		<xsl:if test="labels">
            <div style="padding: 5px;">
            	<xsl:apply-templates select="labels" />
            </div>
		</xsl:if>
        </div>
	</xsl:template>

	<xsl:template name="download-all">
        <xsl:param name="download-all-url" />
        <xsl:variable name="file-count" select="count(/mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file)" />
        <xsl:variable name="file-size" select="sum(/mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file/@SIZE)" />
        <xsl:variable name="formatted-file-size">        
            <xsl:call-template name="format-size">                   
                <xsl:with-param name="size" select="$file-size" />
            </xsl:call-template>
        </xsl:variable>

   		<xsl:if test="$file-count &gt; $lr.download.all.limit.min.file.count and $file-size &lt; $lr.download.all.limit.max.file.size">
            <!-- download all only under certain conditions (number and size of files) -->     
                               
            <a id="download-all-button" class="btn btn-primary">
                <xsl:choose>
                    <xsl:when test="$file-size &gt; $lr.download.all.alert.min.file.size">
                        <!-- display alert before downloading large files -->
                        <xsl:attribute name="href">#</xsl:attribute>
                        <xsl:attribute name="onclick">$('#download_all_alert').toggle();$('#download_all_alert').focus(); return false;</xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- direct download of smaller files -->
                        <xsl:attribute name="data-href"><xsl:value-of select="$download-all-url" /></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
                <i class="fa fa-download fa-3x" style="display:block;">&#160;</i>
                <i18n:translate>                
                    <i18n:text>xmlui.UFAL.artifactbrowser.item-download-all-files</i18n:text>
                    <i18n:param><xsl:copy-of select="$formatted-file-size" /></i18n:param>
                </i18n:translate>
            </a>

            <div id="download_all_alert" class="alert alert-warning" style="margin-top: 20px; display: none;">
			<button type="button" class="close" onclick="$('#download_all_alert').hide();">&#215;</button>
			<p>Large Size</p>
			<p style="margin-bottom: 10px;"><small class="text-warning">The requested files are being packed into one large file. This process can take some time, please be patient.</small></p>			
          				<a data-href="{$download-all-url}" style="text-decoration: none;"><button class="btn btn-warning btn-sm">Continue</button></a>
          				<button type="button" class="btn btn-default btn-sm" onclick="javascript:$('#download_all_alert').hide();">Cancel</button>
            </div>

        </xsl:if>

	</xsl:template>

	<xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
	<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

	<xsl:template match="label">
		<img width="24" height="24"
			src="{$theme-path}/images/licenses/{translate(@label, $uppercase, $smallcase)}.png"
			alt="{@label_title}" title="{@label_title}" />
	</xsl:template>
	
    <!--
    File Type Mapping template

    This maps format MIME Types to human friendly File Type descriptions.
    Essentially, it looks for a corresponding 'key' in your messages.xml of this
    format: xmlui.dri2xhtml.mimetype.{MIME Type}

    (e.g.) <message key="xmlui.dri2xhtml.mimetype.application/pdf">PDF</message>

    If a key is found, the translated value is displayed as the File Type (e.g. PDF)
    If a key is NOT found, the MIME Type is displayed by default (e.g. application/pdf)
    -->
    <xsl:template name="getFileTypeDesc">
      <xsl:param name="mimetype"/>

      <!--Build full key name for MIME type (format: xmlui.dri2xhtml.mimetype.{MIME type})-->
      <xsl:variable name="mimetype-key">xmlui.dri2xhtml.mimetype.<xsl:value-of select='$mimetype'/></xsl:variable>

      <!--Lookup the MIME Type's key in messages.xml language file.  If not found, just display MIME Type-->
      <i18n:text i18n:key="{$mimetype-key}"><xsl:value-of select="$mimetype"/></i18n:text>
    </xsl:template>
    
	<xsl:template match="dri:div[@id='cz.cuni.mff.ufal.dspace.app.xmlui.aspect.statistics.PiwikStatisticsTransformer.div.report']" priority="10">
        <xsl:call-template name="visits_over_time" />
	</xsl:template>

    <xsl:template name="visits_over_time">
        <xsl:variable name="reportURL">
            <xsl:value-of select="concat($context-path, '/', substring-before($request-uri, '/piwik-statistics'))"/>
            <xsl:text disable-output-escaping="yes">/piwik</xsl:text>
        </xsl:variable>
        <div type="piwikchart">
            <xsl:attribute name="data-url"><xsl:value-of select="$reportURL" /></xsl:attribute>
        </div>
    </xsl:template>

    <xsl:template name="versions-dropdown">
       <div id="view-versions" class="dropdown row">
           <div class="col-sm-1">
               <button class="btn btn-default dropdown-toggle" type="button"
                       data-toggle="dropdown">
                   <xsl:attribute name="data-handle">
                       <xsl:value-of select="substring-after(/mets:METS/@ID,'hdl:')" />
                   </xsl:attribute>
                   <img style="display:none;" src="{$theme-path}/images/loading.gif" width="16px" height="16px" />
                   <span><i18n:text>xmlui.UFAL.artifactbrowser.item_view.versions_dropdown</i18n:text></span>
                   <span class="caret">&#160;</span>
                   <div class="row">
                       <div class="col-sm-1">
                           <ul class="dropdown-menu" style="text-align:left;">
                           </ul>
                       </div>
                   </div>
               </button>
           </div>
       </div>
    </xsl:template>

    <xsl:variable name="etal_limit" select="5"/>
    <xsl:template name="authors_with_short_summary_view">
	<xsl:variable name="authors_count" select="count(dim:field[@element='contributor'][@qualifier='author' or @qualifier='other'])"/>
	<xsl:choose>
		<xsl:when test="$authors_count &gt; $etal_limit">
			<details>
				<summary>
					<!-- APA 6+: A; et al. -->
					<!-- this selects just the first author; it's a for-each loop to set a context for the print_author template -->
					<xsl:for-each select="dim:field[@element='contributor'][@qualifier='author' or @qualifier='other'][position() = 1]">
						<xsl:call-template name="print_author" />
						<xsl:text>;</xsl:text>
					</xsl:for-each>
					<xsl:text> et al.</xsl:text>
					<span style="display: list-item"><i18n:text>xmlui.UFAL.artifactbrowser.item_view.show_all_authors</i18n:text></span>
				</summary>
				<xsl:call-template name="print_all_authors" />
			</details>
		</xsl:when>
		<xsl:otherwise>
			<xsl:call-template name="few_authors_formatted" />
		</xsl:otherwise>
	</xsl:choose>
    </xsl:template>

    <xsl:template name="few_authors_formatted">
	<xsl:for-each select="dim:field[@element='contributor'][@qualifier='author' or @qualifier='other'][position() &lt;= $etal_limit]">
		<xsl:call-template name="print_author" />
		<xsl:choose>
			<xsl:when test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']) = 1">
				<xsl:text> and </xsl:text>
			</xsl:when>
			<xsl:when test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']) &gt; 1">
				<xsl:text>;</xsl:text>
			</xsl:when>
		</xsl:choose>
	</xsl:for-each>
    </xsl:template>

    <xsl:template name="print_all_authors">
	<xsl:for-each
		select="dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']">
		<xsl:call-template name="print_author" />
		<xsl:if
			test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']) != 0">
			<xsl:text>; </xsl:text>
		</xsl:if>
	</xsl:for-each>
    </xsl:template>

    <xsl:template name="print_author">
	<span>
		<xsl:if test="@authority">
			<xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
		</xsl:if>
		<a>

			<xsl:attribute name="href">
				<xsl:copy-of select="$contextPath"/>
				<xsl:value-of select="concat('/discover?filtertype=author&amp;filter_relational_operator=equals&amp;filter=',encoder:encode(node()))"/>
			</xsl:attribute>
	<xsl:copy-of select="node()" />
	</a>

	</span>
    </xsl:template>

	<xsl:template name="date_issued_formatted_value">
		<xsl:variable name="date" select="substring(./dim:field[@element='date' and @qualifier='issued'],1,10)"/>
		<xsl:choose>
			<xsl:when test="contains(dim:field[@mdschema='local' and @element='approximateDate' and @qualifier='issued'], ',')">
				<i18n:translate>
					<i18n:text>xmlui.UFAL.artifactbrowser.item_view.date_list</i18n:text>
					<i18n:param><xsl:value-of select="dim:field[@mdschema='local' and @element='approximateDate' and @qualifier='issued']"/></i18n:param>
				</i18n:translate>
			</xsl:when>
			<xsl:when test="dim:field[@mdschema='local' and @element='approximateDate' and @qualifier='issued']">
				<i18n:translate>
					<i18n:text>xmlui.UFAL.artifactbrowser.item_view.date_unknown</i18n:text>
					<i18n:param><xsl:value-of select="dim:field[@mdschema='local' and @element='approximateDate' and @qualifier='issued']"/></i18n:param>
				</i18n:translate>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$date" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>

