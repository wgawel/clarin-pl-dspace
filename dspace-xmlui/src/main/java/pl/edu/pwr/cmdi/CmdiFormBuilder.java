package pl.edu.pwr.cmdi;


import pl.edu.pwr.cmdi.html.Component;
import pl.edu.pwr.cmdi.html.Element;
import pl.edu.pwr.cmdi.html.Form;
import pl.edu.pwr.cmdi.xml.CmdComponent;
import pl.edu.pwr.cmdi.xml.CmdElement;

public class CmdiFormBuilder {

	public String build(CmdComponent cmd){
	
		Form form = buildForm("test.php", "POST");
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
