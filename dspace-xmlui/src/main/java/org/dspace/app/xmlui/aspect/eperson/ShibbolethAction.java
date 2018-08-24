/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.sitemap.PatternException;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Attempt to authenticate the user based upon their presented shibboleth credentials. 
 * This action uses the http parameters as supplied by Shibboleth SP.
 * Read dspace.cfg for configuration detail.
 * 
 * If the authentication attempt is successful then an HTTP redirect will be
 * sent to the browser redirecting them to their original location in the 
 * system before authenticated or if none is supplied back to the DSpace 
 * homepage. The action will also return true, thus contents of the action will
 * be excuted.
 * 
 * If the authentication attempt fails, the action returns false.
 * 
 * Example use:
 * 
 * <map:act name="Shibboleth">
 *   <map:serialize type="xml"/>
 * </map:act>
 * <map:transform type="try-to-login-again-transformer">
 *
 * based on class by <a href="mailto:bliong@melcoe.mq.edu.au">Bruc Liong, MELCOE</a>
 * modified for LINDAT/CLARIN
 */

public class ShibbolethAction extends AbstractAction
{

    private static final Logger log = Logger.getLogger(ShibbolethAction.class);
    /**
     * Attempt to authenticate the user. 
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        try
        {
            // rely on implicit authN of Shib
            Context context = AuthenticationUtil.authenticate(objectModel, null, null, null);
            String domain = ConfigurationManager.getProperty("dspace.hostname");

            EPerson eperson = null;
            if(context != null)
            {
                eperson = context.getCurrentUser();
            }

            if (eperson != null)
            {
                Request request = ObjectModelHelper.getRequest(objectModel);
            	// The user has successfully logged in
                String redirectTo = request.getParameter("login_redirect");

                String redirectURL = request.getRequestURI();
                if(redirectURL.contains("?")){
                    redirectURL = redirectURL.substring(0, redirectURL.indexOf("?"));
                }

                final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

                if(null != redirectTo && !"".equals(redirectTo)){
                    javax.servlet.http.Cookie redirectCookie = new Cookie(
                            "login-redirect", redirectTo);
                    redirectCookie.setDomain(domain);
                    redirectCookie.setPath("/");
                    httpResponse.addCookie(redirectCookie);
                    httpResponse.sendRedirect(redirectURL);
                    return null;
                }

            	if (AuthenticationUtil.isInterupptedRequest(objectModel))
            	{
            		// Resume the request and set the redirect target URL to
            		// that of the originally interrupted request.
            		redirectURL += AuthenticationUtil.resumeInterruptedRequest(objectModel);
            	}
            	else
            	{
            		// Otherwise direct the user to the specified 'loginredirect' page (or homepage by default)
            		String loginRedirect = ConfigurationManager.getProperty("xmlui.user.loginredirect");
            		if(loginRedirect==null) {
            			loginRedirect = (String)request.getSession().getAttribute("xmlui.user.loginredirect");
            		}
            		if(loginRedirect != null && loginRedirect.endsWith("login")) {
            			loginRedirect = "/";
            		}
            		redirectURL += (loginRedirect != null) ? loginRedirect.trim() : "/";	
            	}

            	//String email = eperson.getEmail();
            	//if (email == null) {
            		//redirectURL = request.getContextPath() + "/set-email";
            	//} else

                if ( null == eperson.getWelcome() &&
            	        ConfigurationManager.getBooleanProperty("lr", "lr.login.welcome.message", false) ) 
            	{
                    // tocheck: users without emails should not be authenticated unless
                    // emails are provided
                    redirectURL = request.getContextPath() + "/welcome-message";
                    request.getSession().setAttribute("shib.welcome", request.getSession().getAttribute("shib.welcome"));
            	}


                eperson.setClarinTokenId();
                eperson.update();

                javax.servlet.http.Cookie clarinPlCookie = new Cookie(
                        "clarin-pl-token",
                        eperson.getClarinToken());
                clarinPlCookie.setDomain(domain);
                clarinPlCookie.setPath("/");

                httpResponse.addCookie(clarinPlCookie);

                for(Cookie c : Arrays.asList(request.getCookies())){

                    if("login-redirect".equals(c.getName())){
                        redirectTo = c.getValue();
                        javax.servlet.http.Cookie redirectCookie = new Cookie(
                                "login-redirect", "");
                        redirectCookie.setDomain(domain);
                        redirectCookie.setMaxAge(0);
                        redirectCookie.setPath("/");
                        httpResponse.addCookie(redirectCookie);
                    }
                }

                if(null != redirectTo && !"".equals(redirectTo)){
                    redirectURL = redirectTo;
                }

                log.error("Redirect to: "+redirectURL);
                httpResponse.sendRedirect(redirectURL);
                
                // log the user out for the rest of this current request, however they will be reauthenticated
                // fully when they come back from the redirect. This prevents caching problems where part of the
                // request is performed for the user was authenticated and the other half after it succeeded. This
                // way the user is fully authenticated from the start of the request.
                //
                // TODO: have no idea what this is, but leave it as it is, could be broken
                context.setCurrentUser(null);
                
                return new HashMap();
            }
        }
        catch (SQLException sqle)
        {
            throw new PatternException("Unable to perform Shibboleth authentication",
                    sqle);
        }
        
        return null;
    }

}
