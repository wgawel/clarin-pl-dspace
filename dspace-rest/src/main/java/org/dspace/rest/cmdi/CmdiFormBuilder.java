package org.dspace.rest.cmdi;


import org.dspace.rest.cmdi.html.Component;
import org.dspace.rest.cmdi.html.Element;
import org.dspace.rest.cmdi.html.Form;
import org.dspace.rest.cmdi.xml.CmdComponent;
import org.dspace.rest.cmdi.xml.CmdElement;

public class CmdiFormBuilder {

	public String build(CmdComponent cmd){
	
		Form form = buildForm("###", "POST");
		form.addContent(buildRootComponent(cmd));
		return form.getHtml();
	}
	private String buildRootComponent(CmdComponent cmd){
		return content(cmd, 0);
	}
	private Form buildForm(String action, String method){
		Form f = new Form(action, method);
		return f;
	}
	
	private String content(CmdComponent cmd, int level){
		Component root = new Component(cmd.getName(), level, cmd.getCardinalityMax().toLowerCase().equals("unbounded"));
		for(CmdElement e: cmd.getElements()){
			Element ele = new Element(e.getName(),level+1, e.isEnumeration(),
					e.getEnumerationValues(),e.CardinalityMax.toLowerCase().equals("unbounded"));
			root.addContent(ele.getHtml());
		}
		for(CmdComponent c : cmd.getComponents()){
			root.addContent(content(c,level+1));
		}
		return root.getHtml();
	}

}
