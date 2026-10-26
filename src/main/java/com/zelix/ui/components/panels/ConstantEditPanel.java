package com.zelix.ui.components.panels;

import com.zelix.event.EventRegistry;
import com.zelix.event.INotifier;
import com.zelix.event.ResourceChangeDispatcher;
import com.zelix.event.ResourceChangedListener;
import com.zelix.exceptions.ValidationException;
import com.zelix.framework.constant.ConstantNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.writer.base.ConstantWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.ui.base.BaseDocumentListener;
import com.zelix.ui.base.BasePanel;
import com.zelix.ui.components.PlatformAdaptiveList;
import com.zelix.ui.interfaces.CallbackInputActionListener;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.workspace.Workspace;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

public class ConstantEditPanel extends BasePanel implements ResourceChangedListener, CallbackInputActionListener {
	public static String[] panelStyles = new String[]{
		"layout.minWidth=150",
		"layout.minHeight=120",
		"entryDetailsLbl.top=5",
		"entryDetailsLbl.left=5",
		"existingValueLbl.top=entryDetailsLbl.bottom+3",
		"existingValueLbl.left=5",
		"existingValueLbl.right=container.right-5",
		"propertyEditFld.top=existingValueLbl.bottom+2",
		"propertyEditFld.left=5",
		"propertyEditFld.right=container.right-5",
		"btnWidth=max(chngBtn.defaultWidth, undoBtn.defaultWidth)",
		"btnHeight=max(chngBtn.defaultHeight, undoBtn.defaultHeight)",
		"chngBtn.centerX=container.width*25/100",
		"chngBtn.top=propertyEditFld.bottom+15",
		"undoBtn.centerX=container.width*75/100",
		"undoBtn.top=chngBtn.top"
	};
	public Frame parentFrame;
	public ConstantNode constantNode;
	private ReferenceGraph referenceGraph;
	public Workspace workspace;
	public PlatformAdaptiveList<String> membersList;
	public String existingValue;
	public JLabel entryDetailsLabel;
	public JLabel existingValueLabel;
	public JTextField propertyEditField;
	public JButton changeButton;
	public JButton restoreButton;

	public ConstantEditPanel(ConstantWriter constantWriter, Workspace workspace, PlatformAdaptiveList<String> membersList, Frame parentFrame, EventRegistry eventRegistry, ReferenceGraph referenceGraph) {
		this.parentFrame = parentFrame;
		this.constantNode = constantWriter;
		this.referenceGraph = referenceGraph;
		this.workspace = workspace;
		this.membersList = membersList;
		eventRegistry.subscribe(constantWriter, this, "DescListObservers");
		CallbackInputActionListener.ActionForwardingListener actionForwardingListener = new CallbackInputActionListener.ActionForwardingListener(this);
		PowerLayout panelLayout = new PowerLayout(this);
		this.setLayout(panelLayout);
		this.existingValue = this.constantNode.getDisplayValue();
		String entryDetailsText = this.constantNode.getTypeName() + " Pool Entry #" + ((TypeWriter)this.constantNode).getTag();
		this.entryDetailsLabel = new JLabel(entryDetailsText);
		this.add(this.entryDetailsLabel, "entryDetailsLbl");
		this.existingValueLabel = new JLabel(this.existingValue);
		this.add(this.existingValueLabel, "existingValueLbl");
		this.propertyEditField = new JTextField(this.existingValue);
		this.propertyEditField.addActionListener(actionForwardingListener);
		this.propertyEditField.getDocument().addDocumentListener(new ConstantEditPanel.InputListener(this));
		this.add(this.propertyEditField, "propertyEditFld");
		this.propertyEditField.setEnabled(Options.ENABLE_PATCHES);
		this.changeButton = new JButton("Change");
		this.changeButton.setEnabled(Options.ENABLE_PATCHES);
		this.changeButton.addActionListener(actionForwardingListener);
		this.add(this.changeButton, "chngBtn");
		this.restoreButton = new JButton("Restore");
		this.restoreButton.addActionListener(actionForwardingListener);
		this.restoreButton.setEnabled(Options.ENABLE_PATCHES);
		this.add(this.restoreButton, "undoBtn");
		panelLayout.setProperties(panelStyles);
	}

	@Override
	public void onChanged(INotifier notifier, Object changeCode, Object changedObject, Object unused) {
		this.existingValue = this.constantNode.getDisplayValue();
		String entryDetailsText = this.constantNode.getTypeName() + " Pool Entry #" + ((TypeWriter)this.constantNode).getTag();
		this.entryDetailsLabel.setText(entryDetailsText);
		this.existingValueLabel.setText(this.existingValue);
		this.propertyEditField.setText(this.existingValue);
		this.restoreButton.setEnabled(false);
		this.changeButton.setEnabled(false);
		GuiUtils.requestFocusInWindow(this.propertyEditField);
	}

	@Override
	public void onTextChanged(String text) {
		if (!text.equals(this.existingValue)) {
			this.changeButton.setEnabled(true);
			this.restoreButton.setEnabled(true);
		} else {
			this.changeButton.setEnabled(false);
			this.restoreButton.setEnabled(false);
		}
	}

	public void applyNewValue() {
		String newValue = this.propertyEditField.getText();
		if (!newValue.equals(this.existingValue)) {
			try {
				synchronized (this.workspace) {
					this.constantNode.setValueFromString(newValue);
					Set<StringNode> references = this.referenceGraph.getReferences(this.constantNode);
					if (references != null) {
						for (StringNode reference : references) {
							reference.setValueFromString(newValue);
						}
					}

					this.existingValueLabel.setText(newValue);
					this.changeButton.setEnabled(false);
					this.restoreButton.setEnabled(false);
					this.existingValue = newValue;
					int selectedIndex = this.membersList.getSelectedIndex();
					((DefaultListModel)this.membersList.getModel()).setElementAt(newValue, selectedIndex);
					this.membersList.setSelectedIndex(selectedIndex);
					this.membersList.ensureIndexIsVisible(selectedIndex);
					GuiUtils.requestFocusInWindow(this.propertyEditField);
				}
			} catch (ValidationException validationException) {
				new CopyablePrompt(this.parentFrame, "Error", validationException.getMessage());
			}
		}
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		new ResourceChangeDispatcher(this, notifier, changeKind, changedNode, changeArg);
	}

	@Override
	public void onAction(ActionEvent event) {
		Component eventSource = (Component)event.getSource();
		if (eventSource == this.changeButton) {
			this.applyNewValue();
		} else if (eventSource == this.restoreButton) {
			this.propertyEditField.setText(this.existingValue);
			this.changeButton.setEnabled(false);
			this.restoreButton.setEnabled(false);
			GuiUtils.requestFocusInWindow(this.propertyEditField);
		} else {
			this.applyNewValue();
		}
	}

	public static class InputListener extends BaseDocumentListener {
		public final ConstantEditPanel constantEditPanel;

		public InputListener(ConstantEditPanel constantEditPanel) {
			this.constantEditPanel = constantEditPanel;
		}

		@Override
		public void onUpdate(DocumentEvent e) {
			this.constantEditPanel.onTextChanged(this.constantEditPanel.propertyEditField.getText());
		}
	}
}
