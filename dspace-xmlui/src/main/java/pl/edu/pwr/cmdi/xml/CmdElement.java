package pl.edu.pwr.cmdi.xml;

import java.util.ArrayList;
import java.util.List;



public class CmdElement {

	public String name; 
	public String ConceptLink;
	public String ValueScheme; 
	public String CardinalityMin; 
	public String CardinalityMax;
	public String DisplayPriority;
	public String Multilingual;

	private String value;
	private ValueScheme scheme;
	private AttributeList attributes;
	
	public boolean isEnumeration(){
		if(scheme != null && scheme.isEnumeration){
			return true;
		}
		return false;
	}
	
	public List<String> getEnumerationValues(){
		List<String> list = new ArrayList<String>();
		if(isEnumeration()){
			for(Item i: scheme.getItems()){
				list.add(i.getValue());
			}
		}
		return list;
	}
	
	public boolean hasAtributes(){
		if(attributes != null && attributes.getAttributes().size() > 0){
			return true;
		}
		return false;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getConceptLink() {
		return ConceptLink;
	}
	public void setConceptLink(String conceptLink) {
		ConceptLink = conceptLink;
	}
	public String getValueScheme() {
		return ValueScheme;
	}
	public void setValueScheme(String valueScheme) {
		ValueScheme = valueScheme;
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
	public String getDisplayPriority() {
		return DisplayPriority;
	}
	public void setDisplayPriority(String displayPriority) {
		DisplayPriority = displayPriority;
	}
	public String getMultilingual() {
		return Multilingual;
	}
	public void setMultilingual(String multilingual) {
		Multilingual = multilingual;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public ValueScheme getScheme() {
		return scheme;
	}
	public void setScheme(ValueScheme scheme) {
		this.scheme = scheme;
	}
	public AttributeList getAttributes() {
		return attributes;
	}
	public void setAttributes(AttributeList attributes) {
		this.attributes = attributes;
	}

	public String getTag(int level){
		StringBuilder sb = new StringBuilder();
		sb.append(spaces(level) + "<").append(name);
		if( attributes !=null && 
			attributes.getAttributes().size() > 0){
			for(Attribute a : attributes.getAttributes()){
				sb.append(" "+a.getName()+"=\"\"");
			}
		}
		sb.append(">");
		sb.append("</").append(name).append(">\n");
		return sb.toString();
	}

	public static String spaces(int level){
		String base =  new String(new char[level]).replace("\0", "  ");
		return base;
	}

	@Override
	public String toString() {
		return "CmdElement [name=" + name + ", ConceptLink=" + ConceptLink
				+ ", ValueScheme=" + ValueScheme + ", CardinalityMin="
				+ CardinalityMin + ", CardinalityMax=" + CardinalityMax
				+ ", DisplayPriority=" + DisplayPriority + ", Multilingual="
				+ Multilingual + ", value=" + value + ", scheme=" + scheme
				+ ", attributes=" + attributes + "]";
	}
}
