package org.dspace.rest.common.clarinpl;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tnaskret on 13.08.15.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "lpmn")
public class ClarinPlEngine {

    @XmlElement(name = "source")
    private SourceElement source;

    @XmlElement(name = "activity")
    private List<ActivityElement> activities = new ArrayList<>();

    @XmlElement(name ="agregate")
    private AgregateElement agregate;

    @XmlElement(name = "output")
    private OutputElement output;

    public List<ActivityElement> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityElement> activities) {
        this.activities = activities;
    }

    public SourceElement getSource() {
        return source;
    }

    public void setSource(SourceElement source) {
        this.source = source;
    }

    public AgregateElement getAgregate() {
        return agregate;
    }

    public void setAgregate(AgregateElement agregate) {
        this.agregate = agregate;
    }

    public OutputElement getOutput() {
        return output;
    }

    public void setOutput(OutputElement output) {
        this.output = output;
    }
}

