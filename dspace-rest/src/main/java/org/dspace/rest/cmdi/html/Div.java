package org.dspace.rest.cmdi.html;

public class Div extends Tag{

	private String id;
	private String clazz;
	private String style;

	public Div(String id, String clazz, String style) {
		super();
		this.id = id;
		this.clazz = clazz;
		this.style = style;
	}

	@Override
	protected String getOpeningTag() {
		return "<div id=\""+getId()+"\" class=\""+getClazz()+"\" style=\""+getStyle()+"\">\n";
	}

	@Override
	protected String getClosingTag() {
		return "</div>\n";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}
}
