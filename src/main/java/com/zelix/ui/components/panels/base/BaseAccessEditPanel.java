package com.zelix.ui.components.panels.base;

import com.zelix.event.EventRegistry;
import com.zelix.event.INotifier;
import com.zelix.exceptions.DuplicateFieldException;
import com.zelix.exceptions.FileException;
import com.zelix.exceptions.ValidationException;
import com.zelix.framework.MemberNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.ui.GUI;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.workspace.Workspace;
import javax.swing.JList;

public abstract class BaseAccessEditPanel extends BaseEditPanel {
	public MemberNode memberNode;

	public BaseAccessEditPanel(MemberNode memberNode, Workspace workspace, JList<String> membersList, GUI gui, EventRegistry eventRegistry) {
		super(workspace, gui);
		this.memberNode = memberNode;
		eventRegistry.subscribe(memberNode, this, "DescListObservers");
	}

	@Override
	public final boolean hasModifierChanges() {
		return super.accessComboBox.getSelectedIndex() != super.savedAccessIndex
			|| super.modifierList.isSelectedIndex(super.abstractIndex) != super.savedModifierIndexes.contains(super.abstractIndexObj)
			|| super.modifierList.isSelectedIndex(super.staticIndex) != super.savedModifierIndexes.contains(super.staticIndexObj)
			|| super.modifierList.isSelectedIndex(super.finalIndex) != super.savedModifierIndexes.contains(super.finalIndexObj)
			|| super.modifierList.isSelectedIndex(super.synchronizedIndex) != super.savedModifierIndexes.contains(super.synchronizedIndexObj)
			|| super.modifierList.isSelectedIndex(super.nativeIndex) != super.savedModifierIndexes.contains(super.nativeIndexObj)
			|| super.modifierList.isSelectedIndex(super.volatileIndex) != super.savedModifierIndexes.contains(super.volatileIndexObj)
			|| super.modifierList.isSelectedIndex(super.transientIndex) != super.savedModifierIndexes.contains(super.transientIndexObj);
	}

	public abstract void changeName(String newName);

	@Override
	public void addAccessModifiers() {
		super.accessComboBoxModel.addElement("public");
		super.accessComboBoxModel.addElement("protected");
		super.accessComboBoxModel.addElement("private");
		super.accessComboBoxModel.addElement("package");
	}

	public abstract int getPreservedAccessMask();

	@Override
	public final void updateEnabledState() {
		super.accessComboBox.setEnabled(Options.ENABLE_PATCHES);
		super.modifierList.setEnabled(Options.ENABLE_PATCHES);
		super.propertyEditField.setEnabled(Options.ENABLE_PATCHES);
	}

	@Override
	public void onChanged(INotifier notifier, Object changeCode, Object changedObject, Object unused) {
		if (changeCode == null || changeCode != null && changeCode instanceof AtomicInt && ((AtomicInt)changeCode).get() == 0) {
			super.existingName = this.memberNode.getNameDisplay();
			super.existingNameLabel.setText(super.existingName);
			super.propertyEditField.setText(super.existingName);
		}

		if (changeCode == null || changeCode != null && changeCode instanceof AtomicInt && ((AtomicInt)changeCode).get() == 1) {
			super.savedModifierIndexes = GlobalUtils.newNamedSet();
			if (this.memberNode.isPublic()) {
				super.accessComboBox.setSelectedIndex(super.publicIndex);
				super.savedAccessIndex = super.publicIndex;
			} else if (this.memberNode.isProtected()) {
				super.accessComboBox.setSelectedIndex(super.protectedIndex);
				super.savedAccessIndex = super.protectedIndex;
			} else if (this.memberNode.isPrivate()) {
				super.accessComboBox.setSelectedIndex(super.privateIndex);
				super.savedAccessIndex = super.privateIndex;
			} else {
				super.accessComboBox.setSelectedIndex(super.packageIndex);
				super.savedAccessIndex = super.packageIndex;
			}

			if (this.memberNode.isAbstract()) {
				if (!super.modifierList.isSelectedIndex(super.abstractIndex)) {
					super.modifierList.addSelectionInterval(super.abstractIndex, super.abstractIndex);
				}

				super.savedModifierIndexes.add(super.abstractIndexObj);
			} else if (super.modifierList.isSelectedIndex(super.abstractIndex)) {
				super.modifierList.removeSelectionInterval(super.abstractIndex, super.abstractIndex);
			}

			if (this.memberNode.isStatic()) {
				if (!super.modifierList.isSelectedIndex(super.staticIndex)) {
					super.modifierList.addSelectionInterval(super.staticIndex, super.staticIndex);
				}

				super.savedModifierIndexes.add(super.staticIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.staticIndex, super.staticIndex);
			}

			if (this.memberNode.isFinal()) {
				if (!super.modifierList.isSelectedIndex(super.finalIndex)) {
					super.modifierList.addSelectionInterval(super.finalIndex, super.finalIndex);
				}

				super.savedModifierIndexes.add(super.finalIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.finalIndex, super.finalIndex);
			}

			if (this.memberNode.isSynchronized()) {
				if (!super.modifierList.isSelectedIndex(super.synchronizedIndex)) {
					super.modifierList.addSelectionInterval(super.synchronizedIndex, super.synchronizedIndex);
				}

				super.savedModifierIndexes.add(super.synchronizedIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.synchronizedIndex, super.synchronizedIndex);
			}

			if (this.memberNode.isNative()) {
				if (!super.modifierList.isSelectedIndex(super.nativeIndex)) {
					super.modifierList.addSelectionInterval(super.nativeIndex, super.nativeIndex);
				}

				super.savedModifierIndexes.add(super.nativeIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.nativeIndex, super.nativeIndex);
			}

			if (this.memberNode.isVolatile()) {
				if (!super.modifierList.isSelectedIndex(super.volatileIndex)) {
					super.modifierList.addSelectionInterval(super.volatileIndex, super.volatileIndex);
				}

				super.savedModifierIndexes.add(super.volatileIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.volatileIndex, super.volatileIndex);
			}

			if (this.memberNode.isTransient()) {
				if (!super.modifierList.isSelectedIndex(super.transientIndex)) {
					super.modifierList.addSelectionInterval(super.transientIndex, super.transientIndex);
				}

				super.savedModifierIndexes.add(super.transientIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.transientIndex, super.transientIndex);
			}
		}

		this.updateEnabledState();
		super.listSelectionListener.setPreviousSelectedIndices(super.modifierList.getSelectedIndices());
		this.updateButtonState();
		GuiUtils.requestFocusInWindow(super.propertyEditField);
	}

	@Override
	public void applyChanges() {
		String newName = null;
		int updatedAccessFlags = -1;
		String editedName = super.propertyEditField.getText().trim();
		if (!editedName.equals(super.existingName)) {
			if (editedName.length() > 0) {
				newName = editedName;
			} else {
				new CopyablePrompt(super.gui, "Error", "Name cannot be blank");
			}
		}

		if (this.hasModifierChanges()) {
			updatedAccessFlags = this.memberNode.getAccessFlags() & this.getPreservedAccessMask();
			int selectedAccessIndex = super.accessComboBox.getSelectedIndex();
			if (selectedAccessIndex == super.publicIndex) {
				updatedAccessFlags |= 1;
			} else if (selectedAccessIndex == super.protectedIndex) {
				updatedAccessFlags |= 4;
			} else if (selectedAccessIndex == super.privateIndex) {
				updatedAccessFlags |= 2;
			}

			if (super.modifierList.isSelectedIndex(super.abstractIndex)) {
				updatedAccessFlags |= 1024;
			}

			if (super.modifierList.isSelectedIndex(super.staticIndex)) {
				updatedAccessFlags |= 8;
			}

			if (super.modifierList.isSelectedIndex(super.finalIndex)) {
				updatedAccessFlags |= 16;
			}

			if (super.modifierList.isSelectedIndex(super.synchronizedIndex)) {
				updatedAccessFlags |= 32;
			}

			if (super.modifierList.isSelectedIndex(super.nativeIndex)) {
				updatedAccessFlags |= 256;
			}

			if (super.modifierList.isSelectedIndex(super.volatileIndex)) {
				updatedAccessFlags |= 64;
			}

			if (super.modifierList.isSelectedIndex(super.transientIndex)) {
				updatedAccessFlags |= 128;
			}
		}

		if (newName != null) {
			try {
				this.changeName(editedName);
			} catch (DuplicateFieldException duplicateFieldException) {
				new CopyablePrompt(super.gui, "Error", duplicateFieldException.getMessage());
			} catch (ValidationException nameValidationException) {
				new CopyablePrompt(super.gui, "Error", nameValidationException.getMessage());
			} catch (FileException fileException) {
				new CopyablePrompt(super.gui, "Error", fileException.getMessage());
			}
		}

		if (updatedAccessFlags != -1) {
			try {
				this.applyAccessFlags(updatedAccessFlags);
			} catch (ValidationException accessFlagsValidationException) {
				new CopyablePrompt(super.gui, "Error", accessFlagsValidationException.getMessage());
			}
		}
	}

	public abstract void applyAccessFlags(int accessFlags);

	@Override
	public void initModifierIndexes() {
		super.publicIndex = 0;
		super.protectedIndex = 1;
		super.privateIndex = 2;
		super.packageIndex = 3;
	}
}
