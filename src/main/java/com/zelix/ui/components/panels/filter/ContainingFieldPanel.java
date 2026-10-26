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

public class ContainingFieldPanel extends ContainingPanel implements ActionListener, FocusListener, ItemListener, ListSelectionListener {
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
		"typeLbl.centerY=typeFld.centerY",
		"typeLbl.left=accessModifierComboBox.right+15",
		"typeLbl.width=labelWidth",
		"typeFld.left=typeLbl.right+1",
		"typeFld.top=nameFld.bottom+5",
		"typeFld.right=container.right-5",
		"annotatedByLbl.centerY=annotatedByFld.centerY",
		"annotatedByLbl.left=accessModifierComboBox.right+15",
		"annotatedByLbl.width=labelWidth",
		"annotatedByFld.left=annotatedByLbl.right+1",
		"annotatedByFld.top=typeFld.bottom+5",
		"annotatedByFld.right=container.right-5",
		"hintLbl.left=5",
		"hintLbl.bottom=container.bottom-5",
		"hintLbl.right=container.right-5"
	};
	public DefaultComboBoxModel<String> accessModifierModel;
	public JComboBox<String> accessModifierComboBox;
	public JTextField annotatedByField;
	public JTextField typeField;
	public JTextField nameField;
	public PlatformAdaptiveList<String> modifierList;
	public DefaultListModel<String> modifierListModel;

	public ContainingFieldPanel(JFrame parent, ParameterModel parameterModel, int parameterKind) {
		super(parent, parameterModel, parameterKind);
		this.initComponents();
	}

	public void commitFieldEdit(Object source) {
		if (source == this.nameField) {
			String fieldNamePattern = this.nameField.getText().trim();
			if (fieldNamePattern.length() == 0) {
				this.nameField.setText(super.parameterModel.getFieldNamePattern());
				CopyablePrompt.show(super.parentFrame, "Error", "Field name must be specified. Field restored.");
			} else {
				super.parameterModel.setFieldNamePattern(fieldNamePattern);
			}
		} else if (source == this.typeField) {
			String fieldTypeDescriptor = this.typeField.getText().trim();
			if (fieldTypeDescriptor.indexOf("*") != -1) {
				this.typeField.setText(super.parameterModel.getFieldTypeDescriptor());
				CopyablePrompt.show(super.parentFrame, "Error", "Field type cannot contain wildcards. Field restored.");
			} else {
				super.parameterModel.setFieldTypeDescriptor(fieldTypeDescriptor);
			}
		} else if (source == this.annotatedByField) {
			super.parameterModel.setMemberAnnotationName(this.annotatedByField.getText().trim());
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
		JLabel typeLabel = new JLabel("Type:", 2);
		this.typeField = new JTextField();
		JLabel annotatedByLabel = new JLabel("Annotated by:", 2);
		this.annotatedByField = new JTextField();
		super.hintLabel = new JLabel(" ");
		this.add(this.accessModifierComboBox, "accessModifierComboBox");
		this.add(new PlatformAdaptiveScrollPanel(this.modifierList), "modifierList");
		this.add(this.nameField, "nameFld");
		this.add(this.typeField, "typeFld");
		this.add(this.annotatedByField, "annotatedByFld");
		this.add(super.hintLabel, "hintLbl");
		this.add(nameLabel, "nameLbl");
		this.add(typeLabel, "typeLbl");
		this.add(annotatedByLabel, "annotatedByLbl");
		powerLayout.setProperties(layoutStyles);
		this.accessModifierModel.addElement("<access not specified>");
		this.accessModifierModel.addElement("public");
		this.accessModifierModel.addElement("protected");
		this.accessModifierModel.addElement("private");
		this.accessModifierModel.addElement("package");
		this.modifierListModel.addElement("static");
		this.modifierListModel.addElement("final");
		this.modifierListModel.addElement("volatile");
		this.modifierListModel.addElement("transient");
		this.modifierListModel.addElement("!static");
		this.modifierListModel.addElement("!final");
		this.modifierListModel.addElement("!volatile");
		this.modifierListModel.addElement("!transient");
		this.loadAccessFlags();
		this.nameField.setText(super.parameterModel.getFieldNamePattern());
		this.typeField.setText(super.parameterModel.getFieldTypeDescriptor());
		this.annotatedByField.setText(super.parameterModel.getMemberAnnotationName());
		this.accessModifierComboBox.addItemListener(this);
		this.modifierList.addListSelectionListener(this);
		this.nameField.addFocusListener(this);
		this.typeField.addFocusListener(this);
		this.annotatedByField.addFocusListener(this);
		this.nameField.addActionListener(this);
		this.typeField.addActionListener(this);
		this.annotatedByField.addActionListener(this);
	}

	@Override
	public void focusGained(FocusEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.nameField) {
			super.hintLabel.setText("Enter name of the field. May contain the \"*\" wildcard.");
		} else if (eventSource == this.typeField) {
			super.hintLabel.setText("Enter fully qualified class name or a primitive type. No wildcards.");
		} else if (eventSource == this.annotatedByField) {
			super.hintLabel.setText("Enter fully qualified class name. May contain the \"*\" wildcard.");
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
					super.parameterModel.clearMemberPackagePrivate(2);
					break;
				case 1:
					super.parameterModel.markMemberPublic(2);
					break;
				case 2:
					super.parameterModel.markMemberProtected(2);
					break;
				case 3:
					super.parameterModel.markMemberPrivate(2);
					break;
				case 4:
					super.parameterModel.markMemberPackagePrivate(2);
			}
		}
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

			if (memberAccessFlags.isPositiveVolatile()) {
				this.modifierList.addSelectionInterval(2, 2);
			}

			if (memberAccessFlags.isPositiveTransient()) {
				this.modifierList.addSelectionInterval(3, 3);
			}

			if (memberAccessFlags.isNegativeStatic()) {
				this.modifierList.addSelectionInterval(4, 4);
			}

			if (memberAccessFlags.isNegativeFinal()) {
				this.modifierList.addSelectionInterval(5, 5);
			}

			if (memberAccessFlags.isNegativeVolatile()) {
				this.modifierList.addSelectionInterval(6, 6);
			}

			if (memberAccessFlags.isNegativeTransient()) {
				this.modifierList.addSelectionInterval(7, 7);
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		PlatformAdaptiveList<String> sourceList = (PlatformAdaptiveList<String>)e.getSource();
		int modifierCount = this.modifierListModel.getSize();

		for (int modifierIndex = 0; modifierIndex < modifierCount; modifierIndex++) {
			switch (modifierIndex) {
				case 0:
					super.parameterModel.setMemberStatic(sourceList.isSelectedIndex(modifierIndex), 2);
					break;
				case 1:
					super.parameterModel.setMemberFinal(sourceList.isSelectedIndex(modifierIndex), 2);
					break;
				case 2:
					super.parameterModel.setMemberVolatile(sourceList.isSelectedIndex(modifierIndex), 2);
					break;
				case 3:
					super.parameterModel.setMemberTransient(sourceList.isSelectedIndex(modifierIndex), 2);
					break;
				case 4:
					super.parameterModel.setMemberNotStatic(sourceList.isSelectedIndex(modifierIndex), 2);
					break;
				case 5:
					super.parameterModel.setMemberNotFinal(sourceList.isSelectedIndex(modifierIndex), 2);
					break;
				case 6:
					super.parameterModel.setMemberNotVolatile(sourceList.isSelectedIndex(modifierIndex), 2);
					break;
				case 7:
					super.parameterModel.setMemberNotTransient(sourceList.isSelectedIndex(modifierIndex), 2);
			}
		}
	}
}
