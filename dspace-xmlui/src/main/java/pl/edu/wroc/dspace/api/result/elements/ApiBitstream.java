/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.wroc.dspace.api.result.elements;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.ConfigurationManager;
import pl.edu.wroc.dspace.api.result.IJsonData;

/**
 *
 * @author wgawel
 */
public class ApiBitstream implements IJsonData {
    private final int bitstreamId;
    private final String bitstreamUrl;
    private final String bitstreamUrlWithFilename;

    private static final Logger log = Logger.getLogger(ApiBitstream.class);
    
    public ApiBitstream(int bitstreamId, String bitstreamUrl, String bitstreamUrlWithFilename) {
        this.bitstreamId = bitstreamId;
        this.bitstreamUrl = bitstreamUrl;
        this.bitstreamUrlWithFilename = bitstreamUrlWithFilename;
    }
    
    public ApiBitstream(Bitstream bitstream) {
        String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
        
        this.bitstreamId = bitstream.getID();
        this.bitstreamUrl = dspaceUrl + "/bitstream/id/" +bitstream.getID() + "/";
        this.bitstreamUrlWithFilename = this.bitstreamUrl + bitstream.getName();
    }
    
    @Override
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
