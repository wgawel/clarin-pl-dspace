package org.dspace.rest.common.clarinpl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by tnaskret on 17.08.15.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "agregate")
public class AgregateElement {

    @XmlAttribute(name = "name")
    private  String name;

    @XmlAttribute(name = "type")
    private String type;

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "source")
    private String source;

    public AgregateElement() {
    }

    public AgregateElement(String id,String name, String type, String source) {
        this.name = name;
        this.type = type;
        this.id = id;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
