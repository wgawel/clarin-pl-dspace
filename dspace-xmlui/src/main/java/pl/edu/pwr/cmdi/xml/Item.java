package pl.edu.pwr.cmdi.xml;

public class Item {

	public String ConceptLink;
	public String AppInfo;
	private String value;

	public String getConceptLink() {
		return ConceptLink;
	}
	public void setConceptLink(String conceptLink) {
		ConceptLink = conceptLink;
	}
	public String getAppInfo() {
		return AppInfo;
	}
	public void setAppInfo(String appInfo) {
		AppInfo = appInfo;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Item [ConceptLink=" + ConceptLink + ", AppInfo=" + AppInfo
				+ ", value=" + value + "]";
	}
}