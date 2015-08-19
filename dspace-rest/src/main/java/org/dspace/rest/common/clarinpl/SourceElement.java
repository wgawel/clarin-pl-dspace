package org.dspace.rest.common.clarinpl;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tnaskret on 17.08.15.
 */
@XmlRootElement(name = "source")
public class SourceElement {

    private String id;

    private String dspace_login;

    private List<UrlElement> urls = new ArrayList<>();

    public SourceElement(){
    }

    public SourceElement(String id, String dspace_login) {
        this.id = id;
        this.dspace_login = dspace_login;
    }

    @XmlAttribute(name = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute(name = "dspace_login")
    public String getDspace_login() {
        return dspace_login;
    }

    public void setDspace_login(String dspace_login) {
        this.dspace_login = dspace_login;
    }

    @XmlElement(name = "url")
    public List<UrlElement> getUrls() {
        return urls;
    }

    public void setUrls(List<UrlElement> urls) {
        this.urls = urls;
    }

    public void addUrl(UrlElement url){
        this.urls.add(url);
    }
}
