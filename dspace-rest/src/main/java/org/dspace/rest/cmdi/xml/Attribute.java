package org.dspace.rest.cmdi.xml;


public class Attribute {

	public String Name;
	public String ConceptLink;
	public String Type;
	private ValueScheme ValueScheme; 

	public ValueScheme getValueScheme() {
		return ValueScheme;
	}
	public void setValueScheme(ValueScheme valueScheme) {
		this.ValueScheme = valueScheme;
	}
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public String getConceptLink() {
		return ConceptLink;
	}
	public void setConceptLink(String conceptLink) {
		ConceptLink = conceptLink;
	}
	public String getType() {
		return Type;
	}
	public void setType(String type) {
		Type = type;
	}

	@Override
	public String toString() {
		return "Attribute [Name=" + Name + ", ConceptLink=" + ConceptLink
				+ ", Type=" + Type + ", valueScheme=" + ValueScheme + "]";
	}
}
