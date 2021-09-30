package cz.cuni.mff.ufal.utils;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class PageStructureUtil {
    public static final ConfigurationService configurationService = new DSpace().getConfigurationService();
    public static Node interpolateVariables(Node node){
        if(node != null) {
            if (node.getNodeType() == Node.TEXT_NODE) {
                Map<String, String> valuesMap = configurationService.getAllProperties();
                StrSubstitutor sub = new StrSubstitutor(valuesMap);
                String templateString = node.getTextContent();
                String interpolatedString = sub.replace(templateString);
                node.setTextContent(interpolatedString);
            } else if (node.hasChildNodes()) {
                NodeList children = node.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    interpolateVariables(child);
                }
            }
        }
        return node;
    }

    public static Node documentReadAndInterpolate(String documentPath) throws ParserConfigurationException, IOException, SAXException {
        return interpolateVariables(documentRead(pathToInputSource(documentPath)));
    }

    public static Node readFooter(String footerPath, String collectionHandle) throws IOException,
            ParserConfigurationException, SAXException {
        if(collectionHandle != null) {
            collectionHandle = collectionHandle.replaceFirst("hdl:", "");
            boolean disabled = configurationService.getPropertyAsType("lr.lr.tracker.collection."
                    + collectionHandle + ".disabled", false);
            if (disabled) {
                // remove tracking code
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Files.copy(Paths.get(footerPath), baos);
                String footer = baos.toString("UTF-8");
                int trackingCodeStart = footer.indexOf("<!-- TRACKING CODE -->");
                int trackingCodeEnd = footer.indexOf("<!-- End TRACKING CODE -->");
                if (trackingCodeStart >= 0 && trackingCodeEnd >= 0) {
                    footer = footer.substring(0, trackingCodeStart) + footer.substring(trackingCodeEnd);
                    return documentRead(new InputSource(new StringReader(footer)));

                }
            }
        }
        //by default do nothing
        return documentRead(pathToInputSource(footerPath));
    }

    private static InputSource pathToInputSource(String documentPath) throws IOException {
        return new InputSource(new BufferedInputStream(Files.newInputStream(Paths.get(documentPath))));
    }

    private static Node documentRead(InputSource is) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        return doc.getDocumentElement();
    }
}
