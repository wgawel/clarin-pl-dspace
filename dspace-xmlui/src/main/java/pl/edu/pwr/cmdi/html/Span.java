package pl.edu.pwr.cmdi.html;

public class Span extends Tag{

	private String style;
	
	public Span(String style) {
		this.style = style;
	}
	@Override
	protected String getOpeningTag() {
		return "<span class=\""+style+"\">";
	}

	@Override
	protected String getClosingTag() {
		return "</span>\n";
	}

}
