package com.zelix.ui.components.panels;

import com.zelix.event.EventRegistry;
import com.zelix.exceptions.ValidationException;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.ui.GUI;
import com.zelix.ui.components.PlatformAdaptiveList;
import com.zelix.ui.components.panels.base.BaseAccessEditPanel;
import com.zelix.workspace.Workspace;
import javax.swing.DefaultListModel;

public class MethodAccessEditPanel extends BaseAccessEditPanel {
	public MethodAccessEditPanel(MethodNode methodNode, Workspace workspace, PlatformAdaptiveList<String> membersList, GUI gui, EventRegistry eventRegistry) {
		super(methodNode, workspace, membersList, gui, eventRegistry);
	}

	@Override
	public void changeName(String newName) {
	}

	@Override
	public int getPreservedAccessMask() {
		return -1344;
	}

	@Override
	public void addModifiers(PlatformAdaptiveList<String> modifierList) {
		DefaultListModel<String> modifierModel = (DefaultListModel<String>)modifierList.getModel();
		modifierModel.addElement("abstract");
		modifierModel.addElement("static");
		modifierModel.addElement("final");
		modifierModel.addElement("synchronized");
		modifierModel.addElement("native");
	}

	@Override
	public void applyAccessFlags(int accessFlags) {
		if ((accessFlags & 1024) != 0) {
			if ((accessFlags & 2) != 0) {
				throw new ValidationException("An abstract method cannot also be private");
			}

			if ((accessFlags & 8) != 0) {
				throw new ValidationException("An abstract method cannot also be static");
			}

			if ((accessFlags & 16) != 0) {
				throw new ValidationException("An abstract method cannot also be final");
			}

			if ((accessFlags & 256) != 0) {
				throw new ValidationException("An abstract method cannot also be native");
			}

			if ((accessFlags & 32) != 0) {
				throw new ValidationException("An abstract method cannot also be synchronized");
			}
		}

		synchronized (super.workspace) {
			super.memberNode.setAccessFlags(accessFlags);
		}
	}

	@Override
	public void initModifierIndexes() {
		super.initModifierIndexes();
		super.abstractIndex = 0;
		super.staticIndex = 1;
		super.finalIndex = 2;
		super.synchronizedIndex = 3;
		super.nativeIndex = 4;
	}
}
