package org.dspace.rest.exceptions;

/**
 * Created by clarin on 16.03.17.
 */
public class UntrustedSourceException  extends Exception
{

    private static final long serialVersionUID = 1L;

    public UntrustedSourceException(String message)
    {
        super(message);
    }

}

