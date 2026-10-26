package com.zelix.workspace.resources.xml;

import com.zelix.exceptions.XMLParseException;
import com.zelix.klassmaster.config.Options;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.enumerations.EmptyEnumeration;
import com.zelix.utils.references.AtomicReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class XmlElement {
	private List<Object> contentParts = new ArrayList<>(10);
	private AtomicReference<String> currentNameRef;
	private Map<String, AtomicReference<String>> attributeNameRefs;
	private String originalName;
	private AtomicReference<String> nameRef;
	private Map<String, AtomicReference<String>> attributeValueRefs;

	public String getName() {
		return this.nameRef != null ? this.nameRef.get() : null;
	}

	public void setName(String name) {
		this.nameRef.set(name);
	}

	public void addNameToken(String name) {
		AtomicReference<String> nameTokenRef = new AtomicReference<>(name);
		if (this.nameRef == null) {
			this.originalName = name;
			this.nameRef = nameTokenRef;
		} else {
			this.currentNameRef = nameTokenRef;
		}

		this.contentParts.add(nameTokenRef);
	}

	public Enumeration<String> getAttributeNames() {
		return (Enumeration<String>)(this.attributeValueRefs != null ? Collections.enumeration(new ArrayList<>(this.attributeValueRefs.keySet())) : new EmptyEnumeration<>());
	}

	public void appendContent(String content) {
		if (content.length() > 0) {
			this.contentParts.add(content);
		}
	}

	public boolean renameAttribute(String oldName, String newName) {
		if (this.attributeValueRefs != null) {
			AtomicReference<String> attributeValueRef = this.attributeValueRefs.remove(oldName);
			if (attributeValueRef != null) {
				this.attributeValueRefs.put(newName, attributeValueRef);
				this.attributeNameRefs.get(oldName).set(newName);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public AtomicReference<String> getAttributeValueRef(String name) {
		return this.attributeValueRefs != null ? this.attributeValueRefs.get(name) : null;
	}

	public void addAttribute(String value) {
		if (this.currentNameRef == null) {
			throw new XMLParseException("Element or attribute has no name : '" + value + "'");
		} else {
			if (Options.ZKM_TRIM_XML_ATTRIBUTE_VALUES) {
				value = value.trim();
			}

			AtomicReference<String> valueRef = new AtomicReference<>(value);
			String attributeName = this.currentNameRef.get();
			if (this.attributeNameRefs == null) {
				this.attributeNameRefs = GlobalUtils.newHashMap(13);
				this.attributeValueRefs = GlobalUtils.newHashMap(13);
			}

			this.attributeNameRefs.put(attributeName, this.currentNameRef);
			this.attributeValueRefs.put(attributeName, valueRef);
			this.contentParts.add(valueRef);
		}
	}

	public String getOriginalName() {
		return this.originalName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		int contentPartCount = this.contentParts.size();

		for (int i = 0; i < contentPartCount; i++) {
			Object contentPart = this.contentParts.get(i);
			if (contentPart instanceof AtomicReference) {
				builder.append((String)((AtomicReference)contentPart).get());
			} else {
				builder.append(contentPart);
			}
		}

		return builder.toString();
	}
}
