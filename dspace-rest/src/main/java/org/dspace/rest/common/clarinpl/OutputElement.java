package org.dspace.rest.common.clarinpl;

import javax.xml.bind.annotation.*;

/**
 * Created by tnaskret on 17.08.15.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "output")
public class OutputElement {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "source")
    private String source;

    @XmlElement(name = "dspace")
    private DspaceElement dpsace;

    public OutputElement() {
    }

    public OutputElement(String id, String source, DspaceElement dpsace) {
        this.id = id;
        this.source = source;
        this.dpsace = dpsace;
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

    public DspaceElement getDpsace() {
        return dpsace;
    }

    public void setDpsace(DspaceElement dpsace) {
        this.dpsace = dpsace;
    }
}
