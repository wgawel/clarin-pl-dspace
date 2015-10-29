package org.dspace.rest.cmdi.xml;

import java.util.ArrayList;
import java.util.List;

public class CmdComponent {

	public String name; 
	public String ComponentId;
	public String CardinalityMin; 
	public String CardinalityMax;

	private List<CmdComponent> components = new ArrayList<CmdComponent>();
	private List<CmdElement> elements = new ArrayList<CmdElement>();

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getComponentId() {
		return ComponentId;
	}
	public void setComponentId(String componentId) {
		ComponentId = componentId;
	}
	public String getCardinalityMin() {
		return CardinalityMin;
	}
	public void setCardinalityMin(String cardinalityMin) {
		CardinalityMin = cardinalityMin;
	}
	public String getCardinalityMax() {
		return CardinalityMax;
	}
	public void setCardinalityMax(String cardinalityMax) {
		CardinalityMax = cardinalityMax;
	}
	public List<CmdComponent> getComponents() {
		return components;
	}
	public void setComponents(List<CmdComponent> components) {
		this.components = components;
	}
	public List<CmdElement> getElements() {
		return elements;
	}
	public void setElements(List<CmdElement> elements) {
		this.elements = elements;
	}
	public boolean hasElements(){
		if(elements != null && elements.size() > 0){
			return true;
		}
		return false;
	}
	public boolean hasComponents(){
		if(components != null && components.size() > 0){
			return true;
		}
		return false;
	}
	public String buildXML(int level){
		StringBuilder sb = new StringBuilder();
		sb.append(CmdElement.spaces(level)+"<").append(name).append(">\n");
		for(CmdElement e: elements){
			sb.append(e.getTag(level + 1));
		}
		for(CmdComponent c: components){
			sb.append(c.buildXML(level + 1));
		}
		sb.append(CmdElement.spaces(level)+ "</").append(name).append(">\n");
		return sb.toString();
	}

	@Override
	public String toString() {
		return "CmdComponent [name=" + name + ", ComponentId=" + ComponentId
				+ ", CardinalityMin=" + CardinalityMin + ", CardinalityMax="
				+ CardinalityMax + ", \n    components=" + components + ", \n    elements="	+ elements + "]";
	}
	
}
