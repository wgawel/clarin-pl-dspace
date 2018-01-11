package org.dspace.rest.common.clarinpl;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by clarin on 10.01.18.
 */
@XmlRootElement(name = "license")
public class License {

    String rights;
    String uri;
    String label;

    public License() {
    }

    public License(String rights, String uri, String label) {
        this.rights = rights;
        this.uri = uri;
        this.label = label;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
