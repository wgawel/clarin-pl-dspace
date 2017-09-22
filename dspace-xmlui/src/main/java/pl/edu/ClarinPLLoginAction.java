package pl.edu;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.eperson.EPerson;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

public class ClarinPLLoginAction extends AbstractAction {

    private static Logger log = Logger.getLogger( ClarinPLLoginAction.class);

    @Override
    public Map act(Redirector redirector, SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws Exception {

        Request request = ObjectModelHelper.getRequest(objectModel);

        if(request.getCookies() != null) {
            for (Cookie c : Arrays.asList(request.getCookies())) {

                if ("clarin-pl-token".equals(c.getName())) {

                    String token = c.getValue();

                    org.dspace.core.Context context = new org.dspace.core.Context();
                    EPerson ePerson = EPerson.findByClarinTokenId(context, token);

                    if(token != null && "".equals(token)) {

                        if(!AuthenticationUtil.isLoggedIn(request) && ePerson != null) {

                            AuthenticationUtil.logIn(objectModel, ePerson);
                            log.info("Clarin Trying Login: " + ePerson.getEmail());
                            String redirectURL = request.getRequestURI();

                            final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                            httpResponse.sendRedirect(redirectURL);
                        }

                    } else {
                        if(AuthenticationUtil.isLoggedIn(request)){
                            AuthenticationUtil.logOut(context,request);
                        }
                    }
                }
            }
        }
        return null;
    }
}
