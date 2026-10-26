package com.zelix.workspace.resources.xml;

import com.zelix.exceptions.XMLParseException;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.Stack;
import java.util.List;
import java.util.Map;

public abstract class AbstractXmlTagHandler implements XmlHandler {
	protected Stack<XmlElement> elementStack = new Stack<>();
	protected final String fullResourcePath;
	protected final String resourceName;

	public AbstractXmlTagHandler(String fullResourcePath) {
		this.fullResourcePath = fullResourcePath;
		int bangIndex = fullResourcePath.lastIndexOf("!");
		if (bangIndex > 0) {
			this.resourceName = fullResourcePath.substring(bangIndex + 1);
		} else {
			this.resourceName = fullResourcePath;
		}
	}

	public static String extractSchemaLocation(String xmlContent) {
		int attributeStartIndex = xmlContent.indexOf(":schemaLocation=\"");
		if (attributeStartIndex > -1) {
			int valueEndIndex = xmlContent.indexOf("\"", attributeStartIndex + 17);
			if (valueEndIndex > attributeStartIndex) {
				return xmlContent.substring(attributeStartIndex + 17, valueEndIndex);
			}
		}

		return "";
	}

	public static String extractDocType(String xmlContent) {
		int docTypeStartIndex = xmlContent.indexOf("<!DOCTYPE");
		if (docTypeStartIndex > -1) {
			int docTypeEndIndex = xmlContent.indexOf(">", docTypeStartIndex);
			if (docTypeEndIndex > docTypeStartIndex) {
				return xmlContent.substring(docTypeStartIndex, docTypeEndIndex);
			}
		}

		return "";
	}

	protected String getTagNameFromStack(int depth) {
		int stackSize = this.elementStack.size();
		return !this.elementStack.empty() && depth < stackSize ? this.elementStack.peek(depth).getName() : null;
	}

	@Override
	public void initialize() {
	}

	@Override
	public void resolveDeferredReferences(
		Map<FieldNode, String> fieldReferences, Map<MethodNode, String> methodReferences, DualKeyMap<MethodNode, String, Object> methodAccessorReferences
	) {
	}

	protected XmlElement getTagFromStack(int depth) {
		int stackSize = this.elementStack.size();
		return !this.elementStack.empty() && depth < stackSize ? this.elementStack.peek(depth) : null;
	}

	@Override
	public boolean supportsHierarchy() {
		return true;
	}

	protected String getCurrentTagName() {
		return this.elementStack.empty() ? null : this.elementStack.peek().getName();
	}

	protected int findTagOnStack(String tagName) {
		for (int i = 0; i < this.elementStack.size(); i++) {
			if (this.elementStack.peek(i).getOriginalName().equals(tagName)) {
				return i;
			}
		}

		return -1;
	}

	@Override
	public final void handleEndTagByName(String tagName, List<Object> outputFragments) {
		int tagIndex = this.findTagOnStack(tagName);
		if (tagIndex != -1) {
			XmlElement matchedElement = null;
			int popCount = tagIndex + 1;

			for (int i = 0; i < popCount; i++) {
				matchedElement = this.elementStack.pop();
			}

			if (outputFragments != null) {
				matchedElement.getClass();
				outputFragments.add(new XmlTag(matchedElement));
			}

			this.onEndTag(tagName, matchedElement);
		}
	}

	protected void onEndTag(String tagName, XmlElement element) {
	}

	@Override
	public void handleCharacters(String text, List<Object> outputFragments) {
		outputFragments.add(text);
	}

	protected boolean isTagOnStack(String tagName) {
		return this.elementStack.empty() ? false : this.findTagOnStack(tagName) != -1;
	}

	@Override
	public void handleEndTag(XmlElement element, List<Object> outputFragments) {
		outputFragments.add(element);
	}

	@Override
	public void handleCharacterData(String characterData, String tagName, List<Object> outputFragments) {
		if (tagName == null) {
			throw new XMLParseException("Character data outside of tag : '" + StringUtils.escapeJavaString(characterData) + "' (1)");
		} else {
			outputFragments.add(characterData);
		}
	}

	@Override
	public void handleStartTag(XmlElement element, List<Object> outputFragments) {
		this.elementStack.push(element);
		outputFragments.add(element);
	}
}
