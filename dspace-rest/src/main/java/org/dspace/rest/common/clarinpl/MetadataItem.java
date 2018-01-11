package org.dspace.rest.common.clarinpl;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "metadataitem")
public class MetadataItem implements Serializable{

    String name;
    String value;

    public MetadataItem() {
    }

    public MetadataItem(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MetadataItem{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
