package cz.cuni.mff.ufal.dspace.app.itemimport;

import org.dspace.app.itemimport.ItemImport;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemImportReplacingMetadata extends ItemImport {

    private java.nio.file.FileSystem fs = java.nio.file.FileSystems.getDefault();

    @Override
    protected void replaceItems(Context c, Collection[] mycollections, String sourceDir, String mapFile,
                            boolean template) throws Exception {
        // verify the source directory
        File d = new java.io.File(sourceDir);
        List<Item> processedItems = new ArrayList<>();

        if (d == null || !d.isDirectory())
        {
            throw new Exception("Error, cannot open source directory "
                    + sourceDir);
        }

        // read in HashMap first, to get list of handles & source dirs
        Map<String, String> myHash = readMapFile(mapFile);

        for (Map.Entry<String, String> mapEntry : myHash.entrySet())
        {
            String itemName = mapEntry.getKey();
            String handle = mapEntry.getValue();
            Item item;

            if (handle.indexOf('/') != -1)
            {
                System.out.println("\tReplacing:  " + handle);

                // add new item, locate old one
                item = (Item) HandleManager.resolveToObject(c, handle);
            }
            else
            {
                item = Item.find(c, Integer.parseInt(handle));
            }

            loadMetadata(c, item, java.nio.file.Paths.get(sourceDir, itemName).toString() + fs.getSeparator());
            processedItems.add(item);
        }

        //attempt at saving all changes or none;
        for(Item i : processedItems){
            i.update();
        }
        c.commit();
        c.clearCache();
    }
}
