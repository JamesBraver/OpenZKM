package com.zelix;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

public class AntPathElement extends Path {
	private String path;

	public AntPathElement(Project project) {
		super(project);
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void addText(String path) {
		this.path = path;
	}
}
