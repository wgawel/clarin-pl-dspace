package pl.edu.pwr.cmdi.html;

public abstract class Tag {

	private StringBuilder content = new StringBuilder();
	protected abstract String getOpeningTag();
	protected abstract String getClosingTag();

	public String getContent() {
		return content.toString();
	}
	public void addContent(String content) {
		this.content.append(content);
	}
	public  String getHtml(){
		StringBuilder sb = new StringBuilder();
		sb.append(getOpeningTag());
		sb.append(getContent());
		sb.append(getClosingTag());
		return sb.toString();
	}
}
