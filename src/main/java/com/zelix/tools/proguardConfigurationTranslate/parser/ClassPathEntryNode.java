package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.ClassPathFilterAcceptor;
import com.zelix.utils.collections.OrderedSet;
import java.io.File;
import java.util.Set;

public class ClassPathEntryNode extends ProguardNodeBase implements ClassPathFilterAcceptor {
	private Set<String> jarPaths = new OrderedSet<>();
	private Set<String> archiveFilters = new OrderedSet<>();
	private Set<String> fileFilters = new OrderedSet<>();

	public ClassPathEntryNode(int commandId) {
		super(commandId);
	}

	public Set<String> getFileFilters() {
		return new OrderedSet<>(this.fileFilters);
	}

	@Override
	public boolean addFileFilter(String filter) {
		return this.fileFilters.add(filter);
	}

	public String buildJarPathsAndFilters() {
		StringBuilder pathBuilder = new StringBuilder();
		int itemCount = 0;

		for (String jarPath : this.jarPaths) {
			if (itemCount > 0) {
				pathBuilder.append(File.pathSeparatorChar);
			}

			pathBuilder.append('"');
			pathBuilder.append(jarPath);
			pathBuilder.append('"');
			itemCount++;
		}

		if (!this.fileFilters.isEmpty()) {
			pathBuilder.append('(');
			if (!this.archiveFilters.isEmpty()) {
				itemCount = 0;

				for (String archiveFilter : this.archiveFilters) {
					if (itemCount > 0) {
						pathBuilder.append(',');
					}

					pathBuilder.append('"');
					pathBuilder.append(archiveFilter);
					pathBuilder.append('"');
					itemCount++;
				}

				pathBuilder.append(File.pathSeparatorChar);
			}

			itemCount = 0;

			for (String fileFilter : this.fileFilters) {
				if (itemCount > 0) {
					pathBuilder.append(',');
				}

				pathBuilder.append('"');
				pathBuilder.append(fileFilter);
				pathBuilder.append('"');
				itemCount++;
			}

			pathBuilder.append(')');
		}

		return pathBuilder.toString();
	}

	@Override
	public boolean addArchiveFilter(String filter) {
		return this.archiveFilters.add(filter);
	}

	public boolean hasArchiveFilters() {
		return !this.archiveFilters.isEmpty();
	}

	public Set<String> getJarPaths() {
		return new OrderedSet<>(this.jarPaths);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		for (int i = 0; i < this.getChildrenCount(); i++) {
			ProguardNode childNode = this.getChild(i);
			childNode.parseArguments(this, scriptTranslator);
			if (childNode instanceof QuotedStringNode) {
				String jarPath = ((QuotedStringNode)childNode).getValue();
				this.jarPaths.add(jarPath.trim());
			} else if (childNode instanceof ClassPathFiltersNode) {
			}
		}
	}

	public String buildArchiveFilterString() {
		StringBuilder filterBuilder = new StringBuilder();
		if (!this.archiveFilters.isEmpty()) {
			int itemCount = 0;

			for (String archiveFilter : this.archiveFilters) {
				if (itemCount > 0) {
					filterBuilder.append(',');
				}

				filterBuilder.append('"');
				filterBuilder.append(archiveFilter);
				filterBuilder.append('"');
				itemCount++;
			}

			filterBuilder.append(File.pathSeparatorChar);
		}

		return filterBuilder.toString();
	}

	public boolean hasFileFilters() {
		return !this.fileFilters.isEmpty();
	}
}
