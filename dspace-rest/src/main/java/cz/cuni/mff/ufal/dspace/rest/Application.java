package cz.cuni.mff.ufal.dspace.rest;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class Application extends ResourceConfig{
    public Application(){
        packages("org.dspace.rest","cz.cuni.mff.ufal.dspace.rest");
        register(JacksonFeature.class);
    }
}
