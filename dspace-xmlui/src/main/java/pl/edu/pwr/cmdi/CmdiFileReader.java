package pl.edu.pwr.cmdi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import pl.edu.pwr.cmdi.xml.*;


public class CmdiFileReader {

	public Document parseCmdiFile(File file)
			throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(file);

		return doc;
	}

	public List<Node> getListForElement(NodeList list) {

		List<Node> result = new ArrayList<Node>();

		for (int temp = 0; temp < list.getLength(); temp++) {
			Node nNode = list.item(temp);
			if (nNode instanceof Text) {
			} else {
				result.add(nNode);
			}
		}
		return result;
	}

	public boolean isCMD(Node n, CMD cmd) {
		return n.getNodeName().toUpperCase().equals(cmd.name());
	}

	public Header getHeader(Node n) throws NoSuchFieldException,
			SecurityException, IllegalArgumentException,
			IllegalAccessException, DOMException {

		Header header = new Header();
		Class<Header> aClass = Header.class;

		if (isCMD(n, CMD.HEADER)) {
			List<Node> childs = getListForElement(n.getChildNodes());
			for (Node child : childs) {
				String name = child.getNodeName();
				Field field = aClass.getField(name.toLowerCase());
				if (child.hasChildNodes()) {
					Node text = child.getFirstChild();
					if (text instanceof Text) {
						field.set(header, text.getNodeValue());
					}
				}
			}
		} else {
			throw new IllegalArgumentException("Node is not header");
		}
		return header;

	}

	public CmdComponent getCmdComponent(Node n)
			throws IllegalArgumentException, IllegalAccessException,
			DOMException {
		CmdComponent c = new CmdComponent();
		Class<CmdComponent> aClass = CmdComponent.class;
		if (isCMD(n, CMD.CMD_COMPONENT)) {
			if (n.hasAttributes()) {
				Field[] fields = aClass.getDeclaredFields();
				for (int i = 0; i < fields.length; i++) {
					try {
						String value = n.getAttributes()
								.getNamedItem(fields[i].getName())
								.getNodeValue();
						fields[i].set(c, value);
					} catch (Exception e) {
					}
				}
			}
			c.setElements(getChildCmdElements(n));
			c.setComponents(getChildCmdComponents(n));
		}
		return c;
	}

	public List<CmdComponent> getChildCmdComponents(Node n)
			throws IllegalArgumentException, IllegalAccessException,
			DOMException {
		List<CmdComponent> cmd = new ArrayList<CmdComponent>();
		if (n.hasChildNodes()) {
			List<Node> childs = getListForElement(n.getChildNodes());
			for (Node child : childs) {
				if (isCMD(child, CMD.CMD_COMPONENT)) {
					cmd.add(getCmdComponent(child));
				}
			}
		}
		return cmd;
	}

	public List<CmdElement> getChildCmdElements(Node n)
			throws IllegalArgumentException, IllegalAccessException,
			DOMException {
		List<CmdElement> cmd = new ArrayList<CmdElement>();
		if (n.hasChildNodes()) {
			List<Node> childs = getListForElement(n.getChildNodes());
			for (Node child : childs) {
				if (isCMD(child, CMD.CMD_ELEMENT)) {
					cmd.add(getCmdElement(child));
				}
			}
		}
		return cmd;
	}

	public CmdElement getCmdElement(Node n) throws IllegalArgumentException,
			IllegalAccessException, DOMException {
		CmdElement c = new CmdElement();
		Class<CmdElement> aClass = CmdElement.class;
		if (isCMD(n, CMD.CMD_ELEMENT)) {
			if (n.hasAttributes()) {
				Field[] fields = aClass.getDeclaredFields();
				for (int i = 0; i < fields.length; i++) {
					try {
						String name = fields[i].getName();
						String value = n.getAttributes().getNamedItem(name)
								.getNodeValue();
						fields[i].set(c, value);
						if (n.hasChildNodes()) {
							if (hasValueSheme(n.getChildNodes())) {
								c.setScheme(getValueScheme(n.getChildNodes()));
							}
							if (hasAttributeList(n.getChildNodes())) {
								c.setAttributes(getAttributeList(n.getChildNodes()));
							}
						}
					} catch (Exception e) {
					}
				}
			}
		}
		return c;
	}

	public boolean hasValueSheme(NodeList nodes) {
		List<Node> list = getListForElement(nodes);
		for (Node n : list) {
			return isCMD(n, CMD.VALUESCHEME);
		}
		return false;
	}

	public boolean hasAttributeList(NodeList nodes) {
		List<Node> list = getListForElement(nodes);
		for (Node n : list) {
			return isCMD(n, CMD.ATTRIBUTELIST);
		}
		return false;
	}

	public ValueScheme getValueScheme(NodeList nodes) {
		List<Node> list = getListForElement(nodes);
		ValueScheme scheme = new ValueScheme();
		for (Node n : list) {
			buildEnumerationScheme(scheme, n);
		}
		return scheme;
	}

	private AttributeList getAttributeList(NodeList nodes)
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException, DOMException {
		List<Node> list = getListForElement(nodes);
		AttributeList result = new AttributeList();
		for (Node n : list) {
			if (isCMD(n, CMD.ATTRIBUTELIST)) {
				Class<Attribute> aClass = pl.edu.pwr.cmdi.xml.Attribute.class;
				List<Node> attributes = getListForElement(n.getChildNodes());
				
				for (Node a : attributes) {
					if (isCMD(a, CMD.ATTRIBUTE)) {
						Attribute attribute = new Attribute();
						List<Node> childs = getListForElement(a.getChildNodes());
						for (Node c : childs) {
							if(isCMD(c, CMD.VALUESCHEME)){
								ValueScheme scheme = new ValueScheme();
								buildEnumerationScheme(scheme, c);
								attribute.setValueScheme(scheme);
							} else {
								if(c.hasChildNodes()){
									String name = c.getNodeName();
									Field field = aClass.getField(name);
								Node text = c.getFirstChild();
								if (text instanceof Text) {
									field.set(attribute, text.getNodeValue());
								}
								}
							}
						}
						result.getAttributes().add(attribute);
					}
				}
			}
		}
		return result;
	}

	private void buildEnumerationScheme(ValueScheme scheme, Node n) {
		if (isCMD(n, CMD.VALUESCHEME)) {
			Class<Item> aClass =Item.class;
			List<Node> enumeration = getListForElement(n.getChildNodes());
			for (Node e : enumeration) {
				if (isCMD(e, CMD.ENUMERATION)) {
					scheme.setEnumeration(true);
					List<Node> items = getListForElement(e.getChildNodes());
					for (Node i : items) {
						if (isCMD(i, CMD.ITEM)) {
							Item item = new Item();
							if (i.hasAttributes()) {
								Field[] fields = aClass.getDeclaredFields();
								for (int j = 0; j < fields.length; j++) {
									try {
										String name = fields[j].getName();
										String value = i.getAttributes()
												.getNamedItem(name)
												.getNodeValue();
										fields[j].set(item, value);
										if (i.hasChildNodes()) {
											Node text = i.getFirstChild();
											if (text instanceof Text) {
												item.setValue(text
														.getNodeValue());
											}
										}
									} catch (Exception ex) {
									}
								}
								scheme.getItems().add(item);
							}
						}
					}
				}
			}
		}
	}
}
