/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package pl.edu.wroc.dspace.api;

import org.dspace.app.xmlui.cocoon.*;
import com.google.gson.Gson;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.vocabulary.ControlledVocabulary;
import org.xml.sax.SAXException;
import org.apache.cocoon.servlet.multipart.Part;
import org.apache.cocoon.servlet.multipart.PartOnDisk;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Wojciech Gaweł (wojtekgaw at gmail com)
 */
public class APIHandleReader extends AbstractReader {

    private static final Logger log = Logger.getLogger(APIHandleReader.class);

    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        log.info("JSONHandleReader - start");
        Request request = ObjectModelHelper.getRequest(objectModel);
        String testParam = request.getParameter("testParam");
        
        Part filePart = (Part) request.get("paramFile");
        //File file = ((PartOnDisk)filePart).getFile();
        
        try {
            if (testParam == null) {
                testParam = "NULL";
            } else if (testParam.isEmpty()) {
                testParam = "isEmpty";
            }
            String test  = "test_content;param="+testParam+"\n file = "+filePart.getFileName();
            
            Gson gson = new Gson();

            String jsonString = gson.toJson(test);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
            IOUtils.copy(inputStream, out);
            out.flush();
        } catch (Exception e) {
            log.error("Error while generating json output: " + testParam, e);
        }
    }
    
}
