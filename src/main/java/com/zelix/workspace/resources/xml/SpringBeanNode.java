package com.zelix.workspace.resources.xml;

import java.util.ArrayList;
import java.util.List;

public class SpringBeanNode {
	private final List<XmlElement> propertyElements = new ArrayList<>();
	private final XmlElement beanElement;
	private final XmlElement beansElement;

	public SpringBeanNode(XmlElement beanElement, XmlElement beansElement) {
		this.beanElement = beanElement;
		this.beansElement = beansElement;
	}

	public List<XmlElement> getPropertyElements() {
		return this.propertyElements;
	}

	public XmlElement getBeansElement() {
		return this.beansElement;
	}

	public XmlElement getBeanElement() {
		return this.beanElement;
	}

	public void addPropertyElement(XmlElement propertyElement) {
		this.propertyElements.add(propertyElement);
	}
}
