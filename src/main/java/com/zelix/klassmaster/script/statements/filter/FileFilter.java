package com.zelix.klassmaster.script.statements.filter;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.FileFilterComponentBase;
import com.zelix.klassmaster.script.statements.javaccInterface.FileFilterComponentAcceptor;
import com.zelix.klassmaster.script.statements.javaccInterface.PathFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FileFilter extends AbstractScriptNode implements PathFilter, FileFilterComponentAcceptor {
	private LinkedList<List<FileFilterComponentBase>> orGroups = new LinkedList<>();

	public FileFilter(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).parseArguments(this, scriptContext);
		}
	}

	@Override
	public boolean matches(String path) {
		boolean orGroupMatched = false;
		Iterator<List<FileFilterComponentBase>> orGroupIterator = this.orGroups.iterator();

		label20:
		while (orGroupIterator.hasNext()) {
			Iterator<FileFilterComponentBase> componentIterator = orGroupIterator.next().iterator();

			while (componentIterator.hasNext()) {
				if (!componentIterator.next().matches(path)) {
					continue label20;
				}
			}

			orGroupMatched = true;
			break;
		}

		return orGroupMatched;
	}

	public void addNewGroup(FileFilterComponentBase component) {
		this.addComponent(component);
	}

	public void appendToLastGroup(FileFilterComponentBase component) {
		this.orGroups.getLast().add(component);
	}

	@Override
	public void addComponent(FileFilterComponentBase component) {
		LinkedList<FileFilterComponentBase> newGroup = new LinkedList<>();
		newGroup.add(component);
		this.orGroups.add(newGroup);
	}

	@Override
	public String getDefinition() {
		StringBuilder definitionBuilder = new StringBuilder();
		definitionBuilder.append("{");
		boolean firstOrGroup = true;

		for (List<FileFilterComponentBase> orGroup : this.orGroups) {
			if (!firstOrGroup) {
				definitionBuilder.append(" || ");
			} else {
				firstOrGroup = false;
			}

			boolean firstComponent = true;

			for (FileFilterComponentBase component : orGroup) {
				if (!firstComponent) {
					definitionBuilder.append(" && ");
				} else {
					firstComponent = false;
				}

				definitionBuilder.append(component.getDefinition());
			}
		}

		definitionBuilder.append("}");
		return definitionBuilder.toString();
	}
}
