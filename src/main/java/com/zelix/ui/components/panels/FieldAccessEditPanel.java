package com.zelix.ui.components.panels;

import com.zelix.event.EventRegistry;
import com.zelix.exceptions.ValidationException;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.ui.GUI;
import com.zelix.ui.components.PlatformAdaptiveList;
import com.zelix.ui.components.panels.base.BaseAccessEditPanel;
import com.zelix.workspace.Workspace;
import javax.swing.DefaultListModel;

public class FieldAccessEditPanel extends BaseAccessEditPanel {
	public FieldAccessEditPanel(FieldNode fieldNode, Workspace workspace, PlatformAdaptiveList<String> membersList, GUI gui, EventRegistry eventRegistry) {
		super(fieldNode, workspace, membersList, gui, eventRegistry);
	}

	@Override
	public void changeName(String newName) {
		synchronized (super.workspace) {
			super.workspace.renameField((FieldNode)super.memberNode, newName);
		}
	}

	@Override
	public int getPreservedAccessMask() {
		return -224;
	}

	@Override
	public void addModifiers(PlatformAdaptiveList<String> modifierList) {
		DefaultListModel<String> modifierModel = (DefaultListModel<String>)modifierList.getModel();
		modifierModel.addElement("static");
		modifierModel.addElement("final");
		modifierModel.addElement("volatile");
		modifierModel.addElement("transient");
	}

	@Override
	public void applyAccessFlags(int accessFlags) {
		if ((accessFlags & 16) != 0 && (accessFlags & 64) != 0) {
			throw new ValidationException("An field cannot be final and volatile");
		} else if (!super.memberNode.getClassNode().isInterface() || (accessFlags & 8) != 0 && (accessFlags & 16) != 0 && (accessFlags & 1) != 0) {
			synchronized (super.workspace) {
				super.memberNode.setAccessFlags(accessFlags);
			}
		} else {
			throw new ValidationException("Fields in Interfaces must be public, static and final");
		}
	}

	@Override
	public void initModifierIndexes() {
		super.initModifierIndexes();
		super.staticIndex = 0;
		super.finalIndex = 1;
		super.volatileIndex = 2;
		super.transientIndex = 3;
	}
}
