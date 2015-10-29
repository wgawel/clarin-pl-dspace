package org.dspace.rest.cmdi.html;

import org.dspace.rest.cmdi.xml.CmdElement;

import java.util.ArrayList;
import java.util.List;

public class Element extends Tag{

	private String name;
	private int level;
	private Div div;
	private List<String> enums = new ArrayList<String>();
	
	public Element(String name, int level, boolean isEnum, List<String> enums,boolean isUnbounded) {
		this.name = name;
		this.level = level;
		this.div =  new Div(name,"element","");
		this.enums = enums;
		if(isEnum){
			if( enums.size() > 50){
			  addContent(buildTextbox(this.name, "","text", false));
			} else {
			  addContent(buildSelectbox(this.name, ""));	
			}
		} else {
			addContent(buildTextbox(this.name, "","text",isUnbounded));
		}
	}
	public void setEnumList(List<String> list){
		this.enums = list;
	}
	@Override
	protected String getOpeningTag() {
		return CmdElement.spaces(level)+ div.getOpeningTag();
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
		return CmdElement.spaces(level) + div.getClosingTag();
	}

	public void setName(String name) {
		this.name = name;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	private String buildAttributes(){
		return "";
	}
	private String buildTextbox(String componentName, String formName,String type,boolean isUnbounded){
		StringBuilder sb = new StringBuilder();
		sb.append(CmdElement.spaces(level+1));
		if(isUnbounded){
			sb.append(buildAddButton(componentName));
		}
		sb.append(componentName + ": <input name=\""+formName+"\" type=\""+type+"\" />\n");
		return  sb.toString() ;
	}
	private String buildSelectbox(String componentName, String formName){
		StringBuilder select = new StringBuilder();
		select.append(CmdElement.spaces(level+1) + componentName + ": <select name=\""+formName+"\">\n");
		for(String e:enums){
			select.append(CmdElement.spaces(level+2)+"<option value=\""+ e.trim() +"\">"+e.trim()+"</option>\n");
		}
		select.append(CmdElement.spaces(level+1)+"</select>\n");
		return select.toString();
	}
	private String buildAddButton(String id){
		return "<button type=\"button\" onclick=\"javascript:cloneElement(this)\">+</button>";
	}
}
