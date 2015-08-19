package org.dspace.rest.common.clarinpl;

import javax.xml.bind.annotation.*;

/**
 * Created by tnaskret on 13.08.15.
 */

@XmlRootElement(name = "activity")
public class ActivityElement {


    private String id;

    private String name;

    private String source;

    private String options;

    public ActivityElement() {
    }

    public ActivityElement(String id, String name, String source, String options) {
        this.id = id;
        this.name = name;
        this.source = source;
        this.options = options;
    }

    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @XmlAttribute

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }
}
