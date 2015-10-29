package org.dspace.rest.cmdi.xml;

import java.util.ArrayList;
import java.util.List;

public class ValueScheme {

	boolean isEnumeration = false;
	private List<Item> items = new ArrayList<Item>();

	public boolean isEnumeration() {
		return isEnumeration;
	}


	public void setEnumeration(boolean isEnumeration) {
		this.isEnumeration = isEnumeration;
	}


	public List<Item> getItems() {
		return items;
	}


	public void setItems(List<Item> items) {
		this.items = items;
	}

	@Override
	public String toString() {
		return "ValueScheme [isEnumeration=" + isEnumeration + ", items="
				+ items + "]";
	}
}

