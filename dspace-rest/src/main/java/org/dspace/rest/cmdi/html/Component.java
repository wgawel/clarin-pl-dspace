package org.dspace.rest.cmdi.html;


import org.dspace.rest.cmdi.xml.CmdElement;

public class Component extends Tag{

	private String name;
	private int level;
	private Div div;

	public Component(String name, int level,boolean isUnbounded) {
		this.name = name;
		this.level = level;
		this.div =  new Div(name,"component","");
		StringBuilder sb = new StringBuilder();
		Span title = new Span("component-name");
		title.addContent(this.name);
		if(isUnbounded){
			sb.append(buildAddButton());
		}
		sb.append(CmdElement.spaces(level + 1)+title.getHtml());
		addContent(sb.toString());
	}
	@Override
	protected String getOpeningTag() {
		return CmdElement.spaces(level)+div.getOpeningTag();
	}
	@Override
	public void addContent(String content) {
		div.addContent(content);
	}
	@Override
	public String getContent() {
		return div.getContent();
	}
	@Override
	protected String getClosingTag() {
		return CmdElement.spaces(level)+div.getClosingTag();
	}

	public void setName(String name) {
		this.name = name;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	private String buildAddButton(){
		return "<button type=\"button\" onclick=\"javascript:cloneElement(this)\">+</button>";
	}
}
