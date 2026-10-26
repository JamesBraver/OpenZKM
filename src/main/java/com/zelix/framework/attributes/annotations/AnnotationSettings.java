package com.zelix.framework.attributes.annotations;

import java.util.HashMap;

public class AnnotationSettings {
	private HashMap<String, String> annotationMap = new HashMap<>(13);
	public RuntimeInvisibleAnnotationsAttribute attribute;

	public AnnotationSettings(RuntimeInvisibleAnnotationsAttribute attribute) {
		this.attribute = attribute;
	}

	public RuntimeInvisibleAnnotationsAttribute getAttribute() {
		return this.attribute;
	}

	public String get(String parameterName) {
		return this.annotationMap.get(parameterName);
	}

	public void put(String parameterName, String value) {
		this.annotationMap.put(parameterName, value);
	}

	public boolean containsKey(String parameterName) {
		return this.annotationMap.containsKey(parameterName);
	}
}
