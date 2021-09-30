package org.dspace.springsocial;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.social.ApiBinding;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.drive.DriveFile;
import org.springframework.web.client.ResponseExtractor;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Created by okosarko on 11.5.17.
 */
public class AsyncBitstreamAdder {
    Logger log = Logger.getLogger(AsyncBitstreamAdder.class);

    UsersConnectionRepository usersConnectionRepository;

    public AsyncBitstreamAdder(){

    }

    @Inject
    public AsyncBitstreamAdder(UsersConnectionRepository usersConnectionRepository){
        this.usersConnectionRepository = usersConnectionRepository;
    }

    /**
     * Download a file and add it as a bitstream under user with id
     * This code might run in the "future" when a context has already expired, so don't bind it to any current request/context
     * TODO refactor so we handle multiple apiClasses
     */
    @Async
    public <A extends ApiBinding> Future<String> createBitstream(Class<A> apiClass, int epersonId, int itemId, String driveFileId){
        log.debug("===AsyncBitstreamAdder.createBistream start");
        String status = "Done";
        final Context context = createContext();

        if(context != null) {
            try {
                EPerson eperson = EPerson.find(context, epersonId);
                context.setCurrentUser(eperson);
                final Item item = Item.find(context, itemId);
                Connection<A> connection = usersConnectionRepository.createConnectionRepository(Integer.
                        toString(epersonId)).getPrimaryConnection(apiClass);
                if (connection.hasExpired()) {
                    connection.refresh();
                }
                A api = connection.getApi();
                if (api instanceof Google) {
                    Google google = (Google) api;
                    DriveFile driveFile = google.driveOperations().getFile(driveFileId);
                    if(isNotBlank(driveFile.getDownloadUrl())) {
                        final String title = driveFile.getTitle();
                        final String description = driveFile.getDescription();
                        final String mimeType = driveFile.getMimeType();
                        Bundle bundle;
                        //In theory locking on item should be enough, but does different Context return the same Item instances?
                        synchronized (AsyncBitstreamAdder.class) {
                            final Bundle[] bundles = item.getBundles("ORIGINAL");
                            if (bundles.length > 0) {
                                bundle = bundles[0];
                            } else {
                                bundle = item.createBundle("ORIGINAL");
                                item.addBundle(bundle);
                                //created a bundle; commit so other threads can see it
                                item.update();
                                context.commit();
                            }
                        }
                        //ResponseExtractors have an access to the input stream of the connection; so we can fetch large files without OOM.
                        ResponseExtractor<Void> responseExtractor = new BitstreamAddingResponseExtractor(bundle, title, description, context, mimeType);
                        google.driveOperations().downloadFile(driveFile, responseExtractor);
                        item.update();
                    }else{
                        context.complete();
                        status = "No download url. Is the file downloadable? " +
                                 "For example native docs files are not and must be exported.";
                    }
                } else {
                    throw new UnsupportedOperationException("The api type is not supported");
                }

            } catch (Exception e) {
                log.error(e);
                if (context != null) {
                    context.abort();
                }
                status = "Error:\n" + e.getMessage();
            } finally {
                if (context != null) {
                    try {
                        context.complete();
                    } catch (SQLException e) {
                        log.error(e);
                    }
                }
            }
        }
        log.debug("===AsyncBitstreamAdder.createBistream end");
        return new AsyncResult<>(status);
    }

    private Context createContext(){
        try{
            return new Context();
        }catch (SQLException e){
            log.error(e);
            return null;
        }
    }

    /**
     * ResponseExtractors have an access to the input stream of the connection; so we can fetch large files without OOM.
     */
    private class BitstreamAddingResponseExtractor implements ResponseExtractor<Void> {
        private final Bundle bundle;
        private final String title;
        private final String description;
        private final Context context;
        private final String mimeType;

        public BitstreamAddingResponseExtractor(Bundle bundle, String title, String description, Context context, String mimeType) {
            this.bundle = bundle;
            this.title = title;
            this.description = description;
            this.context = context;
            this.mimeType = mimeType;
        }

        @Override
        public Void extractData(ClientHttpResponse response) throws IOException {
            Bitstream bitstream;
            InputStream is = response.getBody();
            try {
                bitstream = bundle.createBitstream(is);
                bitstream.setName(title);
                if (description != null) {
                    bitstream.setDescription(description);
                }
                //guess bitstream format
                BitstreamFormat bitstreamFormat = BitstreamFormat.findByMIMEType(context, mimeType);
                if(bitstreamFormat == null){
                    bitstreamFormat = FormatIdentifier.guessFormat(context, bitstream);
                }
                bitstream.setFormat(bitstreamFormat);
                bitstream.update();
            } catch (AuthorizeException | SQLException e) {
                log.error(e);
            }
            return null;
        }
    }
}
