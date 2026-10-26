package com.zelix.ui.components.panels.base;

import com.zelix.HelpManager;
import com.zelix.event.INotifier;
import com.zelix.event.ResourceChangeDispatcher;
import com.zelix.event.ResourceChangedListener;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.ui.GUI;
import com.zelix.ui.base.BaseDocumentListener;
import com.zelix.ui.base.BasePanel;
import com.zelix.ui.components.ChangeGuardTextField;
import com.zelix.ui.components.PlatformAdaptiveList;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.interfaces.CallbackInputActionListener;
import com.zelix.ui.interfaces.EditPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.prompts.ErrorPrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.NamedSet;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.Workspace;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class BaseEditPanel extends BasePanel implements EditPanel, ResourceChangedListener, CallbackInputActionListener {
	public static String[] layoutProperties = new String[]{
		"layout.minWidth=150",
		"layout.minHeight=120",
		"existingNameLbl.top=1",
		"existingNameLbl.left=5",
		"existingNameLbl.right=container.right-5",
		"propertyEditFld.top=existingNameLbl.bottom+1",
		"propertyEditFld.left=5",
		"propertyEditFld.right=container.right-5",
		"accessComboBox.top=propertyEditFld.bottom+3",
		"accessComboBox.left=5",
		"accessComboBox.right=container.right-btnWidth-10",
		"modifierLst.top=accessComboBox.bottom+3",
		"modifierLst.left=5",
		"modifierLst.right=accessComboBox.right",
		"modifierLst.bottom=container.bottom-3",
		"btnWidth=max(chngBtn.defaultWidth, undoBtn.defaultWidth)",
		"btnHeight=max(chngBtn.defaultHeight, undoBtn.defaultHeight)",
		"btnAreaHeight=container.height-propertyEditFld.bottom",
		"chngBtn.width=btnWidth",
		"chngBtn.height=btnHeight",
		"chngBtn.centerY=btnAreaHeight*30/100 + propertyEditFld.bottom",
		"chngBtn.right=container.right-5",
		"undoBtn.width=btnWidth",
		"undoBtn.height=btnHeight",
		"undoBtn.centerY=btnAreaHeight*70/100 + propertyEditFld.bottom",
		"undoBtn.right=container.right-5"
	};
	public int publicIndex = 99;
	public int protectedIndex = 99;
	public int privateIndex = 99;
	public int packageIndex = 99;
	public int abstractIndex = 99;
	public int staticIndex = 99;
	public int finalIndex = 99;
	public int synchronizedIndex = 99;
	public int nativeIndex = 99;
	public int volatileIndex = 99;
	public int transientIndex = 99;
	public int interfaceIndex = 99;
	public int superIndex = 99;
	public Workspace workspace;
	public GUI gui;
	public Integer abstractIndexObj;
	public Integer staticIndexObj;
	public Integer finalIndexObj;
	public Integer synchronizedIndexObj;
	public Integer nativeIndexObj;
	public Integer volatileIndexObj;
	public Integer transientIndexObj;
	public Integer interfaceIndexObj;
	public Integer superIndexObj;
	public JLabel existingNameLabel;
	public ChangeGuardTextField propertyEditField;
	public DefaultComboBoxModel<String> accessComboBoxModel;
	public JComboBox<String> accessComboBox;
	public final PlatformAdaptiveList<String> modifierList;
	public final DefaultListModel<String> modifierListModel;
	public EditPanelSelectListener listSelectionListener;
	public JButton changeButton;
	public JButton restoreButton;
	public String existingName;
	public NamedSet<Integer> savedModifierIndexes;
	public int savedAccessIndex;

	public BaseEditPanel(Workspace workspace, GUI gui) {
		this.workspace = workspace;
		this.gui = gui;
		this.initModifierIndexes();
		this.abstractIndexObj = this.abstractIndex;
		this.staticIndexObj = this.staticIndex;
		this.finalIndexObj = this.finalIndex;
		this.synchronizedIndexObj = this.synchronizedIndex;
		this.nativeIndexObj = this.nativeIndex;
		this.volatileIndexObj = this.volatileIndex;
		this.transientIndexObj = this.transientIndex;
		this.interfaceIndexObj = this.interfaceIndex;
		this.superIndexObj = this.superIndex;
		CallbackInputActionListener.ActionForwardingListener actionForwardingListener = new CallbackInputActionListener.ActionForwardingListener(this);
		PowerLayout panelLayout = new PowerLayout(this);
		this.setLayout(panelLayout);
		this.existingNameLabel = new JLabel();
		this.propertyEditField = new ChangeGuardTextField();
		this.propertyEditField.addActionListener(actionForwardingListener);
		this.propertyEditField.getDocument().addDocumentListener(new InputListener(this, this.propertyEditField));
		this.add(this.propertyEditField, "propertyEditFld");
		this.add(this.existingNameLabel, "existingNameLbl");
		this.accessComboBoxModel = new DefaultComboBoxModel<>();
		this.accessComboBox = new JComboBox<>(this.accessComboBoxModel);
		this.accessComboBox.setToolTipText(HelpManager.getToolTip("ACCESS_MODIFIERS"));
		this.addAccessModifiers();
		this.accessComboBox.addItemListener(new ComboBoxSelectionListener(this));
		this.add(this.accessComboBox, "accessComboBox");
		this.modifierList = new PlatformAdaptiveList<>(new DefaultListModel<>());
		this.modifierListModel = (DefaultListModel<String>)this.modifierList.getModel();
		this.addModifiers(this.modifierList);
		this.modifierList.setSelectionMode(2);
		this.listSelectionListener = new EditPanelSelectListener(this);
		this.modifierList.addListSelectionListener(this.listSelectionListener);
		this.add(new PlatformAdaptiveScrollPanel(this.modifierList), "modifierLst");
		this.changeButton = new JButton("Change");
		this.changeButton.setToolTipText(HelpManager.getToolTip("CHANGE_DESC"));
		this.changeButton.addActionListener(actionForwardingListener);
		this.add(this.changeButton, "chngBtn");
		this.restoreButton = new JButton("Restore");
		this.restoreButton.setToolTipText(HelpManager.getToolTip("RESTORE_DESC"));
		this.restoreButton.addActionListener(actionForwardingListener);
		this.add(this.restoreButton, "undoBtn");
		panelLayout.setProperties(layoutProperties);
	}

	public final void updateButtonState() {
		if (!this.hasChanges()) {
			this.changeButton.setEnabled(Options.ENABLE_PATCHES);
			this.restoreButton.setEnabled(Options.ENABLE_PATCHES);
		}
	}

	public abstract boolean hasModifierChanges();

	public abstract void addAccessModifiers();

	@Override
	public final void onSelectionChanged(int[] selected, int[] notSelected) {
		for (int newlySelectedIndex = 0; newlySelectedIndex < selected.length; newlySelectedIndex++) {
			if (!this.savedModifierIndexes.contains(selected[newlySelectedIndex])) {
				this.enableButtons();
				return;
			}
		}

		for (int deselectedIndex = 0; deselectedIndex < notSelected.length; deselectedIndex++) {
			if (this.savedModifierIndexes.contains(notSelected[deselectedIndex])) {
				this.enableButtons();
				return;
			}
		}

		this.updateButtonState();
	}

	public final boolean hasChanges() {
		return !this.propertyEditField.getText().trim().equals(this.existingName) || this.hasModifierChanges();
	}

	public void updateEnabledState() {
	}

	public abstract void addModifiers(PlatformAdaptiveList<String> modifierList);

	@Override
	public final void onAccessChanged(int accessIndex) {
		if (this.savedAccessIndex != accessIndex) {
			this.enableButtons();
		} else {
			this.updateButtonState();
		}
	}

	public final void enableButtons() {
		this.changeButton.setEnabled(true);
		this.restoreButton.setEnabled(true);
	}

	@Override
	public abstract void onChanged(INotifier notifier, Object changeCode, Object changedObject, Object unused);

	public abstract void applyChanges();

	@Override
	public final void onTextChanged(String text) {
		if (!text.equals(this.existingName)) {
			this.enableButtons();
		} else {
			this.updateButtonState();
		}
	}

	public abstract void initModifierIndexes();

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		new ResourceChangeDispatcher(this, notifier, changeKind, changedNode, changeArg);
	}

	@Override
	public final void onAction(ActionEvent event) {
		Component sourceComponent = (Component)event.getSource();
		if (sourceComponent == this.changeButton) {
			if (this.workspace.isClasspathChanged()) {
				new CopyablePrompt(this.gui, "Classpath changed", "Classpath has been changed but the classes have not been reopened. You must reopen the classes. (G)");
				return;
			}

			if (!this.workspace.isErrorEmpty()) {
				new ErrorPrompt(
					this.gui,
					"Opened classes not useable",
					"Opened classes not useable for the reason shown below.",
					this.workspace.getError() + " : Please adjust your " + CopyrightConstants.NAME + " classpath or class file selections."
				);
				return;
			}

			this.gui.setupWaitCursorSafety();
			this.applyChanges();
			this.gui.resetCursorSafety();
		} else if (sourceComponent == this.restoreButton) {
			this.propertyEditField.setText(this.existingName);
			this.accessComboBox.setSelectedIndex(this.savedAccessIndex);
			int modifierCount = this.modifierListModel.getSize();

			for (int i = 0; i < modifierCount; i++) {
				if (this.savedModifierIndexes.contains(i)) {
					if (!this.modifierList.isSelectedIndex(i)) {
						this.modifierList.addSelectionInterval(i, i);
					}
				} else {
					this.modifierList.removeSelectionInterval(i, i);
				}
			}

			this.changeButton.setEnabled(Options.ENABLE_PATCHES);
			this.restoreButton.setEnabled(Options.ENABLE_PATCHES);
			GuiUtils.requestFocusInWindow(this.propertyEditField);
		} else {
			if (this.workspace.isClasspathChanged()) {
				new CopyablePrompt(this.gui, "Classpath changed", "Classpath has been changed but the classes have not been reopened. You must reopen the classes. (G)");
				return;
			}

			if (!this.workspace.isErrorEmpty()) {
				new ErrorPrompt(
					this.gui,
					"Opened classes not useable",
					"Opened classes not useable for the reason shown below.",
					this.workspace.getError() + " : Please adjust your " + CopyrightConstants.NAME + " classpath or class file selections."
				);
				return;
			}

			this.gui.setupWaitCursorSafety();
			this.applyChanges();
			this.gui.resetCursorSafety();
		}
	}

	public static class ComboBoxSelectionListener implements ItemListener {
		public EditPanel editPanel;

		public ComboBoxSelectionListener(EditPanel editPanel) {
			this.editPanel = editPanel;
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				JComboBox<?> comboBox = (JComboBox<?>)e.getSource();
				this.editPanel.onAccessChanged(comboBox.getSelectedIndex());
			}
		}
	}

	public static class EditPanelSelectListener implements ListSelectionListener {
		private int[] previousSelectedIndices = new int[0];
		private EditPanel editPanel;

		public EditPanelSelectListener(EditPanel editPanel) {
			this.editPanel = editPanel;
		}

		public void setPreviousSelectedIndices(int[] previousSelectedIndices) {
			this.previousSelectedIndices = previousSelectedIndices;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			int[] selectedIndices = ((PlatformAdaptiveList<?>)e.getSource()).getSelectedIndices();
			AtomicReference<int[]> newlySelectedIndices = new AtomicReference<>();
			AtomicReference<int[]> deselectedIndices = new AtomicReference<>();
			GlobalUtils.computeSortedSetDifferences(this.previousSelectedIndices, selectedIndices, newlySelectedIndices, deselectedIndices);
			this.editPanel.onSelectionChanged(newlySelectedIndices.get(), deselectedIndices.get());
		}
	}

	public static class InputListener extends BaseDocumentListener {
		public CallbackInputActionListener callbackListener;
		public ChangeGuardTextField textField;

		public InputListener(CallbackInputActionListener callbackListener, ChangeGuardTextField textField) {
			this.callbackListener = callbackListener;
			this.textField = textField;
		}

		@Override
		public void onUpdate(DocumentEvent e) {
			e.getDocument();
			if (!this.textField.isChanging()) {
				this.callbackListener.onTextChanged(this.textField.getText().trim());
			}
		}
	}
}
