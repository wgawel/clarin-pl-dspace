/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.controlpanel.AbstractControlPanelTab;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 
 */
public class ControlPanelCmdiProfileTab extends AbstractControlPanelTab {

	private Request request = null;
	private static Logger log = Logger.getLogger(ControlPanelCmdiProfileTab.class);

	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		request = ObjectModelHelper.getRequest(objectModel);
		try {
			HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
			httpResponse.sendRedirect(contextPath + "/admin/profiles");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

