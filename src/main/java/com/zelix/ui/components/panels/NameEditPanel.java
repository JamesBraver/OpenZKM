package com.zelix.ui.components.panels;

import com.zelix.event.EventRegistry;
import com.zelix.event.INotifier;
import com.zelix.exceptions.ProcessingException;
import com.zelix.exceptions.ValidationException;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.ui.GUI;
import com.zelix.ui.components.PlatformAdaptiveList;
import com.zelix.ui.components.panels.base.BaseEditPanel;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.Workspace;
import java.util.HashMap;
import javax.swing.DefaultListModel;

public class NameEditPanel extends BaseEditPanel {
	public ClassNode classNode;
	public String packagePrefix;

	public NameEditPanel(ClassNode classNode, Workspace workspace, GUI gui, EventRegistry eventRegistry) {
		super(workspace, gui);
		this.classNode = classNode;
		eventRegistry.subscribeDefault(this.classNode, this);
		this.classNode.onChange(this);
	}

	@Override
	public boolean hasModifierChanges() {
		return super.accessComboBox.getSelectedIndex() != super.savedAccessIndex
			|| super.modifierList.isSelectedIndex(super.abstractIndex) != super.savedModifierIndexes.contains(super.abstractIndexObj)
			|| super.modifierList.isSelectedIndex(super.finalIndex) != super.savedModifierIndexes.contains(super.finalIndexObj)
			|| super.modifierList.isSelectedIndex(super.interfaceIndex) != super.savedModifierIndexes.contains(super.interfaceIndexObj)
			|| super.modifierList.isSelectedIndex(super.superIndex) != super.savedModifierIndexes.contains(super.superIndexObj);
	}

	public void applyClassAccessFlags(int accessFlags) {
		int oldAccessFlags = this.classNode.getAccessFlags();
		ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(this.classNode.getInternalName());
		if ((accessFlags & 1024) != 0 && (accessFlags & 16) != 0) {
			throw new ValidationException("An abstract class cannot also be final");
		} else {
			if ((accessFlags & 512) != 0 && (oldAccessFlags & 512) == 0) {
				if (!this.classNode.getSuperclassName().equals("java/lang/Object")) {
					throw new ValidationException("An interface can only have Object as it's superclass");
				}

				if (hierarchyNode.hasSubclasses()) {
					throw new ValidationException("This class has subclasses. It cannot be changed to an interface.");
				}
			}

			if ((accessFlags & 512) == 0 && (oldAccessFlags & 512) != 0 && hierarchyNode.hasImplementers()) {
				throw new ValidationException("This class is implemented by other classes. It must remain an interface.");
			} else {
				synchronized (super.workspace) {
					this.classNode.setClassAccessFlags(accessFlags);
				}
			}
		}
	}

	@Override
	public void addAccessModifiers() {
		super.accessComboBoxModel.addElement("public");
		super.accessComboBoxModel.addElement("package");
	}

	@Override
	public final void updateEnabledState() {
		super.accessComboBox.setEnabled(Options.ENABLE_PATCHES);
		super.modifierList.setEnabled(Options.ENABLE_PATCHES);
		super.propertyEditField.setEnabled(Options.ENABLE_PATCHES);
	}

	@Override
	public void addModifiers(PlatformAdaptiveList<String> modifierList) {
		DefaultListModel<String> modifierModel = (DefaultListModel<String>)modifierList.getModel();
		modifierModel.addElement("abstract");
		modifierModel.addElement("final");
		modifierModel.addElement("interface");
		modifierModel.addElement("super");
	}

	@Override
	public void onChanged(INotifier notifier, Object changeCode, Object changedObject, Object unused) {
		if (changeCode == null || changedObject != null && changedObject instanceof ClassNode && changeCode instanceof AtomicInt && ((AtomicInt)changeCode).get() == 0) {
			super.existingName = this.classNode.getConstructorDisplayName();
			this.packagePrefix = this.classNode.getPackageNameBinary();
			if (!this.packagePrefix.equals("")) {
				this.packagePrefix = this.packagePrefix + ".";
			}

			super.existingNameLabel.setText(this.packagePrefix + super.existingName);
			super.propertyEditField.setText(super.existingName);
		}

		if (changeCode == null || changeCode instanceof AtomicInt && ((AtomicInt)changeCode).get() == 1) {
			if (this.classNode.isPublic()) {
				super.accessComboBox.setSelectedIndex(super.publicIndex);
				super.savedAccessIndex = super.publicIndex;
			} else {
				super.accessComboBox.setSelectedIndex(super.packageIndex);
				super.savedAccessIndex = super.packageIndex;
			}

			super.savedModifierIndexes = GlobalUtils.newNamedSet();
			if (this.classNode.isAbstract()) {
				if (!super.modifierList.isSelectedIndex(super.abstractIndex)) {
					super.modifierList.addSelectionInterval(super.abstractIndex, super.abstractIndex);
				}

				super.savedModifierIndexes.add(super.abstractIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.abstractIndex, super.abstractIndex);
			}

			if (this.classNode.isFinal()) {
				if (!super.modifierList.isSelectedIndex(super.finalIndex)) {
					super.modifierList.addSelectionInterval(super.finalIndex, super.finalIndex);
				}

				super.savedModifierIndexes.add(super.finalIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.finalIndex, super.finalIndex);
			}

			if (this.classNode.isInterface()) {
				if (!super.modifierList.isSelectedIndex(super.interfaceIndex)) {
					super.modifierList.addSelectionInterval(super.interfaceIndex, super.interfaceIndex);
				}

				super.savedModifierIndexes.add(super.interfaceIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.interfaceIndex, super.interfaceIndex);
			}

			if (this.classNode.isSuper()) {
				if (!super.modifierList.isSelectedIndex(super.superIndex)) {
					super.modifierList.addSelectionInterval(super.superIndex, super.superIndex);
				}

				super.savedModifierIndexes.add(super.superIndexObj);
			} else {
				super.modifierList.removeSelectionInterval(super.superIndex, super.superIndex);
			}
		}

		this.updateEnabledState();
		super.listSelectionListener.setPreviousSelectedIndices(super.modifierList.getSelectedIndices());
		this.updateButtonState();
		GuiUtils.requestFocusInWindow(super.propertyEditField);
	}

	@Override
	public void applyChanges() {
		String validatedInternalName = null;
		int newAccessFlags = -1;
		String newClassName = null;
		String newInternalName = null;
		String enteredName = super.propertyEditField.getText().trim();
		if (!enteredName.equals(super.existingName)) {
			if (this.classNode.hasInnerClasses()) {
				new CopyablePrompt(
					super.gui,
					"Error",
					"Cannot rename a class with multi-release versions : " + this.classNode.getRawSourcePath() + "' : '" + this.classNode.getInnerClasses().get(0).getRawSourcePath() + "'"
				);
			} else if (enteredName.length() > 0) {
				newClassName = this.packagePrefix + enteredName;
				newInternalName = newClassName.replace('.', '/');
				if (!ClassHierarchyNode.getNodeByInternalName(this.classNode.getInternalName()).hasSuperClass()) {
					if (!ClassHierarchyNode.isWorkspaceClassByInternalName(newInternalName)) {
						validatedInternalName = newInternalName;
					} else {
						new CopyablePrompt(super.gui, "Error", "Class " + newClassName + " already exists.");
					}
				} else {
					new CopyablePrompt(super.gui, "Error", "Cannot rename inner classes in this version : " + this.classNode.getRawSourcePath() + "'");
				}
			} else {
				new CopyablePrompt(super.gui, "Error", "Class name cannot be blank");
			}
		}

		if (this.hasModifierChanges()) {
			if (this.classNode.hasInnerClasses()) {
				new CopyablePrompt(
					super.gui,
					"Error",
					"Cannot modify a class with multi-release versions : " + this.classNode.getRawSourcePath() + "' : '" + this.classNode.getInnerClasses().get(0).getRawSourcePath() + "'"
				);
			} else {
				newAccessFlags = this.classNode.getAccessFlags() & -1586;
				if (super.accessComboBox.getSelectedIndex() == super.publicIndex) {
					newAccessFlags |= 1;
				}

				if (super.modifierList.isSelectedIndex(super.abstractIndex)) {
					newAccessFlags |= 1024;
				}

				if (super.modifierList.isSelectedIndex(super.finalIndex)) {
					newAccessFlags |= 16;
				}

				if (super.modifierList.isSelectedIndex(super.superIndex)) {
					newAccessFlags |= 32;
				}

				if (super.modifierList.isSelectedIndex(super.interfaceIndex)) {
					newAccessFlags |= 512;
				}
			}
		}

		if (validatedInternalName != null) {
			synchronized (super.workspace) {
				super.existingNameLabel.setText(newClassName);
				HashMap<String, String> reverseRenameMap = GlobalUtils.newHashMap();
				HashMap<String, String> renameMap = GlobalUtils.newHashMap();
				this.classNode.remapInternalName(newInternalName, reverseRenameMap, renameMap);

				try {
					super.workspace.processReferences(renameMap, null, null);
				} catch (ProcessingException processingException) {
					new CopyablePrompt(super.gui, "Error", processingException.getMessage());
				}

				super.workspace.applyClassNameMappings(renameMap);
				super.workspace.rebuildAllRegistries();
			}
		}

		if (newAccessFlags != -1) {
			try {
				this.applyClassAccessFlags(newAccessFlags);
				super.savedAccessIndex = super.accessComboBox.getSelectedIndex();
			} catch (ValidationException validationException) {
				new CopyablePrompt(super.gui, "Error", validationException.getMessage());
			}
		}
	}

	@Override
	public void initModifierIndexes() {
		super.publicIndex = 0;
		super.packageIndex = 1;
		super.abstractIndex = 0;
		super.finalIndex = 1;
		super.interfaceIndex = 2;
		super.superIndex = 3;
	}
}
