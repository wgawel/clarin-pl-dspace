/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.atteo.evo.inflector.English;
import org.dspace.rest.Resource;
import org.dspace.core.ConfigurationManager;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 10/7/13
 * Time: 12:11 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "dspaceobject")
@XmlSeeAlso({Item.class, Collection.class, Community.class})
public class DSpaceObject {
    private Integer id;

    private String name;
    private String handle;
    private String type;

    @XmlElement(name = "link", required = true)
    private String link;

    private List<String> expand = new ArrayList<String>();

    public DSpaceObject() {

    }

    public DSpaceObject(org.dspace.content.DSpaceObject dso) {
        setId(dso.getID());
        setName(dso.getName());
        setHandle(dso.getHandle());
        setType(dso.getTypeText().toLowerCase());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName(){
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getLink() {
        return ConfigurationManager.getProperty("dspace.baseUrl") + "/rest/" + English.plural(getType()) + "/" + getId();
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }


    @XmlElement(required = true)
    public List<String> getExpand() {
        return expand;
    }

    public void setExpand(List<String> expand) {
        this.expand = expand;
    }

    public void addExpand(String expandableAttribute) {
        this.expand.add(expandableAttribute);
    }
}
