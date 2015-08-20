package pl.edu.pwr.cmdi.html;

public class Form extends Tag{

	private String action;
	private String method;
	
	public Form(String action, String method) {
		this.action = action;
		this.method = method;
	}
	
	@Override
	public String getOpeningTag() {
		return "<form id=xmlForm action=\""+action+"\" method=\""+method+"\">\n";
	}
	@Override
	public String getClosingTag() {
		return "<input type=\"submit\"></input>\n</form>";
	}
}
