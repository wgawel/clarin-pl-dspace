package pl.edu.pwr.cmdi.xml;

import java.util.ArrayList;
import java.util.List;

public class AttributeList {

	private List<Attribute> attributes = new ArrayList<Attribute>();

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String toString() {
		return "AttributeList [attributes=" + attributes + "]";
	}
}
