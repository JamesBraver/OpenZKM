package com.zelix.ui.components.panels.filter;

import com.zelix.framework.AccessFlags;
import com.zelix.ui.components.PlatformAdaptiveList;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.panels.base.ContainingPanel;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ContainingMethodPanel extends ContainingPanel implements ActionListener, FocusListener, ItemListener, ListSelectionListener {
	public static String[] layoutStyles = new String[]{
		"layout.height=nameFld.defaultHeight*8",
		"column1Width=max(accessModifierComboBox.defaultWidth, modifierList.defaultWidth)",
		"accessModifierComboBox.left=5",
		"accessModifierComboBox.top=5",
		"accessModifierComboBox.width=column1Width",
		"modifierList.left=5",
		"modifierList.top=accessModifierComboBox.bottom+5",
		"modifierList.width=column1Width",
		"modifierList.bottom=container.bottom-hintLbl.defaultHeight-20",
		"labelWidth=max(nameLbl.defaultWidth, typeLbl.defaultWidth, annotatedByLbl.defaultWidth)",
		"nameLbl.centerY=nameFld.centerY",
		"nameLbl.left=accessModifierComboBox.right+15",
		"nameLbl.width=labelWidth",
		"nameFld.left=nameLbl.right+1",
		"nameFld.top=5",
		"nameFld.right=container.right-5",
		"typeLbl.centerY=argumentsTypeFld.centerY",
		"typeLbl.left=accessModifierComboBox.right+15",
		"typeLbl.width=labelWidth",
		"argumentsTypeFld.left=typeLbl.right+1",
		"argumentsTypeFld.top=nameFld.bottom+5",
		"argumentsTypeFld.right=container.right-5",
		"throwsLbl.centerY=throwsFld.centerY",
		"throwsLbl.left=accessModifierComboBox.right+15",
		"throwsLbl.width=labelWidth",
		"throwsFld.left=throwsLbl.right+1",
		"throwsFld.top=argumentsTypeFld.bottom+5",
		"throwsFld.right=container.right-5",
		"annotatedByLbl.centerY=annotatedByFld.centerY",
		"annotatedByLbl.left=accessModifierComboBox.right+15",
		"annotatedByLbl.width=labelWidth",
		"annotatedByFld.left=annotatedByLbl.right+1",
		"annotatedByFld.top=throwsFld.bottom+5",
		"annotatedByFld.right=container.right-5",
		"hintLbl.left=5",
		"hintLbl.bottom=container.bottom-5",
		"hintLbl.right=container.right-5"
	};
	public JTextField annotatedByField;
	public DefaultListModel<String> modifierListModel;
	public DefaultComboBoxModel<String> accessModifierModel;
	public JTextField throwsField;
	public JComboBox<String> accessModifierComboBox;
	public PlatformAdaptiveList<String> modifierList;
	public JTextField argumentsTypeField;
	public JTextField nameField;

	public ContainingMethodPanel(JFrame parent, ParameterModel parameterModel, int parameterKind) {
		super(parent, parameterModel, parameterKind);
		this.initComponents();
	}

	public void loadAccessFlags() {
		AccessFlags memberAccessFlags = super.parameterModel.getMemberAccessFlags();
		if (memberAccessFlags != null) {
			if (memberAccessFlags.isPositivePublic()) {
				this.accessModifierComboBox.setSelectedIndex(1);
			} else if (memberAccessFlags.isPositiveProtected()) {
				this.accessModifierComboBox.setSelectedIndex(2);
			} else if (memberAccessFlags.isPositivePrivate()) {
				this.accessModifierComboBox.setSelectedIndex(3);
			} else if (memberAccessFlags.isDefaultAccess()) {
				this.accessModifierComboBox.setSelectedIndex(4);
			} else {
				this.accessModifierComboBox.setSelectedIndex(0);
			}

			if (memberAccessFlags.isPositiveStatic()) {
				this.modifierList.addSelectionInterval(0, 0);
			}

			if (memberAccessFlags.isPositiveFinal()) {
				this.modifierList.addSelectionInterval(1, 1);
			}

			if (memberAccessFlags.isPositiveSynchronized()) {
				this.modifierList.addSelectionInterval(2, 2);
			}

			if (memberAccessFlags.isPositiveNative()) {
				this.modifierList.addSelectionInterval(3, 3);
			}

			if (memberAccessFlags.isPositiveAbstract()) {
				this.modifierList.addSelectionInterval(4, 4);
			}

			if (memberAccessFlags.isNegativeStatic()) {
				this.modifierList.addSelectionInterval(5, 5);
			}

			if (memberAccessFlags.isNegativeFinal()) {
				this.modifierList.addSelectionInterval(6, 6);
			}

			if (memberAccessFlags.isNegativeSynchronized()) {
				this.modifierList.addSelectionInterval(7, 7);
			}

			if (memberAccessFlags.isNegativeNative()) {
				this.modifierList.addSelectionInterval(8, 8);
			}

			if (memberAccessFlags.isNegativeAbstract()) {
				this.modifierList.addSelectionInterval(9, 9);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		this.commitFieldEdit(eventSource);
	}

	@Override
	public void initComponents() {
		PowerLayout powerLayout = new PowerLayout(this);
		this.setLayout(powerLayout);
		this.accessModifierModel = new DefaultComboBoxModel<>();
		this.accessModifierComboBox = new JComboBox<>(this.accessModifierModel);
		this.modifierListModel = new DefaultListModel<>();
		this.modifierList = new PlatformAdaptiveList<>(this.modifierListModel);
		this.modifierList.setSelectionMode(2);
		JLabel nameLabel = new JLabel("Name:", 2);
		this.nameField = new JTextField();
		JLabel argumentsTypeLabel = new JLabel("Arguments:", 2);
		this.argumentsTypeField = new JTextField();
		JLabel throwsLabel = new JLabel("Throws:", 2);
		this.throwsField = new JTextField();
		JLabel annotatedByLabel = new JLabel("Annotated by:", 2);
		this.annotatedByField = new JTextField();
		super.hintLabel = new JLabel(" ");
		this.add(this.accessModifierComboBox, "accessModifierComboBox");
		this.add(new PlatformAdaptiveScrollPanel(this.modifierList), "modifierList");
		this.add(this.nameField, "nameFld");
		this.add(this.argumentsTypeField, "argumentsTypeFld");
		this.add(this.throwsField, "throwsFld");
		this.add(this.annotatedByField, "annotatedByFld");
		this.add(nameLabel, "nameLbl");
		this.add(argumentsTypeLabel, "typeLbl");
		this.add(throwsLabel, "throwsLbl");
		this.add(annotatedByLabel, "annotatedByLbl");
		this.add(super.hintLabel, "hintLbl");
		powerLayout.setProperties(layoutStyles);
		this.accessModifierModel.addElement("<access not specified>");
		this.accessModifierModel.addElement("public");
		this.accessModifierModel.addElement("protected");
		this.accessModifierModel.addElement("private");
		this.accessModifierModel.addElement("package");
		this.modifierListModel.addElement("static");
		this.modifierListModel.addElement("final");
		this.modifierListModel.addElement("synchronized");
		this.modifierListModel.addElement("native");
		this.modifierListModel.addElement("abstract");
		this.modifierListModel.addElement("!static");
		this.modifierListModel.addElement("!final");
		this.modifierListModel.addElement("!synchronized");
		this.modifierListModel.addElement("!native");
		this.modifierListModel.addElement("!abstract");
		this.loadAccessFlags();
		this.nameField.setText(super.parameterModel.getMethodNamePattern());
		this.argumentsTypeField.setText(super.parameterModel.getMethodArgumentTypeNames());
		this.throwsField.setText(super.parameterModel.getThrowsClassNames());
		this.annotatedByField.setText(super.parameterModel.getMemberAnnotationName());
		this.accessModifierComboBox.addItemListener(this);
		this.modifierList.addListSelectionListener(this);
		this.nameField.addFocusListener(this);
		this.argumentsTypeField.addFocusListener(this);
		this.throwsField.addFocusListener(this);
		this.annotatedByField.addFocusListener(this);
		this.nameField.addActionListener(this);
		this.argumentsTypeField.addActionListener(this);
		this.throwsField.addActionListener(this);
		this.annotatedByField.addActionListener(this);
	}

	@Override
	public void focusGained(FocusEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.nameField) {
			super.hintLabel.setText("Enter name of the method. May contain the \"*\" wildcard.");
		} else if (eventSource == this.argumentsTypeField) {
			super.hintLabel.setText("Enter comma separated fully qualified class names or primitive types.");
		} else if (eventSource == this.throwsField) {
			super.hintLabel.setText("Enter comma separated list of fully qualified class names. May not contain wildcards.");
		} else if (eventSource == this.annotatedByField) {
			super.hintLabel.setText("Enter a fully qualified class name. May contain the \"*\" wildcard.");
		}
	}

	@Override
	public void focusLost(FocusEvent e) {
		super.hintLabel.setText(" ");
		Object eventSource = e.getSource();
		this.commitFieldEdit(eventSource);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == this.accessModifierComboBox && e.getStateChange() == 1) {
			switch (this.accessModifierComboBox.getSelectedIndex()) {
				case 0:
					super.parameterModel.clearMemberPackagePrivate(3);
					break;
				case 1:
					super.parameterModel.markMemberPublic(3);
					break;
				case 2:
					super.parameterModel.markMemberProtected(3);
					break;
				case 3:
					super.parameterModel.markMemberPrivate(3);
					break;
				case 4:
					super.parameterModel.markMemberPackagePrivate(3);
			}
		}
	}

	public void commitFieldEdit(Object source) {
		if (source == this.nameField) {
			String methodNamePattern = this.nameField.getText().trim();
			if (methodNamePattern.length() == 0) {
				this.nameField.setText(super.parameterModel.getMethodNamePattern());
				CopyablePrompt.show(super.parentFrame, "Error", "Method name must be specified. Field restored.");
			} else {
				super.parameterModel.setMethodNamePattern(methodNamePattern);
			}
		} else if (source == this.argumentsTypeField) {
			String argumentTypeNames = this.argumentsTypeField.getText().trim();
			if (argumentTypeNames.length() > 1 && argumentTypeNames.indexOf("*") != -1) {
				this.argumentsTypeField.setText(super.parameterModel.getMethodArgumentTypeNames());
				CopyablePrompt.show(super.parentFrame, "Error", "Argument type must either be a single \"*\" or else cannot contain wildcards. Field restored.");
			} else {
				super.parameterModel.setMethodArgumentTypeNames(argumentTypeNames);
			}
		} else if (source == this.throwsField) {
			String throwsClassNames = this.throwsField.getText().trim();
			if (throwsClassNames.indexOf("*") != -1) {
				this.throwsField.setText(super.parameterModel.getThrowsClassNames());
				CopyablePrompt.show(super.parentFrame, "Error", "Throws class names cannot contain wildcards. Field restored.");
			} else {
				super.parameterModel.setThrowsClassNames(throwsClassNames);
			}
		} else if (source == this.annotatedByField) {
			super.parameterModel.setMemberAnnotationName(this.annotatedByField.getText().trim());
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		PlatformAdaptiveList<?> sourceList = (PlatformAdaptiveList<?>)e.getSource();
		int modifierCount = this.modifierListModel.getSize();

		for (int modifierIndex = 0; modifierIndex < modifierCount; modifierIndex++) {
			switch (modifierIndex) {
				case 0:
					super.parameterModel.setMemberStatic(sourceList.isSelectedIndex(modifierIndex), 3);
					break;
				case 1:
					super.parameterModel.setMemberFinal(sourceList.isSelectedIndex(modifierIndex), 3);
					break;
				case 2:
					super.parameterModel.setMemberSynchronized(sourceList.isSelectedIndex(modifierIndex), 3);
					break;
				case 3:
					super.parameterModel.setMemberNative(sourceList.isSelectedIndex(modifierIndex), 3);
					break;
				case 4:
					super.parameterModel.setMemberAbstract(sourceList.isSelectedIndex(modifierIndex), 3);
					break;
				case 5:
					super.parameterModel.setMemberNotStatic(sourceList.isSelectedIndex(modifierIndex), 3);
					break;
				case 6:
					super.parameterModel.setMemberNotFinal(sourceList.isSelectedIndex(modifierIndex), 3);
					break;
				case 7:
					super.parameterModel.setMemberNotSynchronized(sourceList.isSelectedIndex(modifierIndex), 3);
					break;
				case 8:
					super.parameterModel.setMemberNotNative(sourceList.isSelectedIndex(modifierIndex), 3);
					break;
				case 9:
					super.parameterModel.setMemberNotAbstract(sourceList.isSelectedIndex(modifierIndex), 3);
			}
		}
	}
}
