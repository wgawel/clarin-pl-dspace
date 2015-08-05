package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

public class ResetProcessAction extends AbstractAction{

	private Item item = null;
	
	@Override
	public Map act(Redirector arg0, SourceResolver arg1, Map objectModel, String arg3,
			Parameters parameters) throws Exception {
		
		final HttpServletResponse httpResponse = (HttpServletResponse) objectModel
				.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
		final HttpServletRequest httpRequest = (HttpServletRequest) objectModel
				.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
		int itemID = parameters.getParameterAsInteger("itemID", -1);
	
		Context context = ContextUtil.obtainContext(objectModel);
		String handle = parameters.getParameter("handle", null);
		
		loadItemObject(handle, itemID, context);
		
		item.setNLPEngineToken(null);
		item.setProcessStatus("READY");
		
		httpResponse.sendRedirect(httpRequest.getContextPath() + "/handle/"	+ handle);
		return null;
	}

	private void loadItemObject(String handle, int itemID, Context context)
			throws SQLException {
		DSpaceObject dso = null;

		if (itemID > -1) {
			// Referenced by internal itemID
			item = Item.find(context, itemID);
		} else if (handle != null) {
			// Reference by an item's handle.
			dso = HandleManager.resolveToObject(context, handle);

			if (dso instanceof Item) {
				item = (Item) dso;
			}
		}
	}
}
