/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Unauthenticate the current user. There is no way this action will fail, 
 * so any components inside the action will be executed.
 * 
 * This action will always send an HTTP redirect to the DSpace homepage.
 * 
 * Example: 
 * 
 * <map:action name="UnAuthenticateAction" src="org.dspace.app.xmlui.eperson.UnAuthenticateAction"/>
 * 
 * <map:act type="UnAuthenticateAction">
 *   <map:serialize type="xml"/>
 * </map:act>
 * 
 * @author Scott Phillips
 */

public class UnAuthenticateAction extends AbstractAction
{

    private static Logger log = Logger.getLogger(UnAuthenticateAction.class);

    /**
     * Logout the current user.
     * 
     * @param redirector
     * @param resolver
     * @param objectModel
     *            Cocoon's object model
     * @param source
     * @param parameters
     * @return an empty Map.
     * @throws java.lang.Exception passed through
     */
    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception {

        Context context = ContextUtil.obtainContext(objectModel);
        final HttpServletRequest httpRequest = 
            (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        final HttpServletResponse httpResponse = 
            (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

        EPerson eperson = context.getCurrentUser();

        //Clarin token destroy
        if(eperson != null) {
            String domain = ConfigurationManager.getProperty("dspace.hostname");
            eperson.clearClarinTokenId();
            javax.servlet.http.Cookie clarinCookie = new Cookie(
                    "clarin-pl-token", "");
            clarinCookie.setDomain(domain);
            clarinCookie.setMaxAge(0);
            clarinCookie.setPath("/");
            httpResponse.addCookie(clarinCookie);
        }
        // Actually log the user out.
        AuthenticationUtil.logOut(context, httpRequest);
        
        // Set the user as logged in for the rest of this request so that the cache does not get spoiled.
        context.setCurrentUser(eperson);

        // Forward the user to the home page.
        if(httpRequest.isSecure()) {

            StringBuffer location = new StringBuffer("https://");
				location.append(ConfigurationManager.getProperty("dspace.hostname")).append(
						httpRequest.getContextPath());
				httpResponse.sendRedirect(location.toString());

		}
        else{
            httpResponse.sendRedirect(ConfigurationManager.getProperty("dspace.url"));
        }

        return new HashMap();
    }

}
