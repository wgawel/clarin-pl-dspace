package org.dspace.rest.common.clarin;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "nextclouditem")
public class NextCloudItem implements Serializable {

    String filename;
    String token;
    String link;

    List<MetadataItem> metadata;

    public List<MetadataItem> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataItem> metadata) {
        this.metadata = metadata;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return "NextCloudItem{" +
                "filename='" + filename + '\'' +
                ", token='" + token + '\'' +
                ", link='" + link + '\'' +
                ", metedata=" + metadata +
                '}';
    }
}
