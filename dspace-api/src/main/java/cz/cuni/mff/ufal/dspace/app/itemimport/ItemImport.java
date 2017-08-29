package cz.cuni.mff.ufal.dspace.app.itemimport;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Created by okosarko on 24.1.17.
 */
public class ItemImport extends org.dspace.app.itemimport.ItemImport {


    @Override
    protected Item addItem(Context c, Collection[] mycollections, String path,
                        String itemname, PrintWriter mapOut, boolean template) throws Exception{
        Item item = super.addItem(c, mycollections, path, itemname, mapOut, template);
        Bitstream[] bits = item.getNonInternalBitstreams();
        for(Bitstream bit : bits){
            loadBitstreamMetadata(c, bit, path + File.separatorChar + itemname
                    + File.separatorChar);
            bit.update();
        }
        c.commit();
        return item;
    }

    private void loadBitstreamMetadata(Context c, final Bitstream bit, String path) throws SAXException, AuthorizeException,
            TransformerException, IOException, SQLException, ParserConfigurationException {
        FilenameFilter bitstreamMetadataFileFilter = new FilenameFilter()
        {
            public boolean accept(File dir, String n)
            {
                return n.startsWith(bit.getName() + "_metadata_");
            }
        };
        File folder = new File(path);
        File[] files = folder.listFiles(bitstreamMetadataFileFilter);
        for(File file : files){
            loadDublinCore(c, bit, file.getAbsolutePath());
        }

    }
}
