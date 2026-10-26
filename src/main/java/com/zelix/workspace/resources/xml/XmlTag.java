package com.zelix.workspace.resources.xml;

public class XmlTag {
	public final XmlElement element;

	public XmlTag(XmlElement element) {
		this.element = element;
	}

	@Override
	public String toString() {
		return this.element.getName();
	}
}
