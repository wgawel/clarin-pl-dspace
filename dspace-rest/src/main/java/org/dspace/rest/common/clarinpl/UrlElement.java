package org.dspace.rest.common.clarinpl;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Created by tnaskret on 17.08.15.
 */

@XmlRootElement(name = "url")
public class UrlElement {

    private String name;

    private String value;

    public UrlElement() {
    }

    public UrlElement(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @XmlAttribute(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlValue
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
