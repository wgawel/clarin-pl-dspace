package org.dspace.rest.common.clarinpl;

import javax.xml.bind.annotation.*;

/**
 * Created by tnaskret on 17.08.15.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "dspace")
public class DspaceElement {

    @XmlAttribute
    private String type;

    @XmlAttribute(name = "item_id")
    private String item_id;

    @XmlAttribute(name = "repo_id")
    private String repo_id;

    @XmlValue
    private String value;

    public DspaceElement(){
    }

    public DspaceElement(String type, String item_id, String repo_id, String value) {
        this.type = type;
        this.item_id = item_id;
        this.repo_id = repo_id;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getItem_id() {
        return item_id;
    }

    public void setItem_id(String item_id) {
        this.item_id = item_id;
    }

    public String getRepo_id() {
        return repo_id;
    }

    public void setRepo_id(String repo_id) {
        this.repo_id = repo_id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
