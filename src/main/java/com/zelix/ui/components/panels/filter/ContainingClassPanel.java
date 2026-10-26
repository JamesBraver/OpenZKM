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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ContainingClassPanel extends ContainingPanel implements ActionListener, FocusListener, ItemListener, ListSelectionListener {
	public static String layoutStyles = "layout.height=nameFld.defaultHeight*8;column1Width=max(accessModifierComboBox.defaultWidth, modifierList.defaultWidth);accessModifierComboBox.left=5;accessModifierComboBox.top=5;accessModifierComboBox.width=column1Width;modifierList.left=5;modifierList.top=accessModifierComboBox.bottom+5;modifierList.width=column1Width;modifierList.bottom=container.bottom-hintLbl.defaultHeight-20;labelWidth=max(nameLbl.defaultWidth, extendsLbl.defaultWidth, implementsLbl.defaultWidth, annotatedByLbl.defaultWidth);nameLbl.centerY=nameFld.centerY;nameLbl.left=accessModifierComboBox.right+15;nameLbl.width=labelWidth;nameFld.left=nameLbl.right+1;nameFld.top=5;nameFld.right=container.right-5;extendsLbl.centerY=extendsFld.centerY;extendsLbl.left=accessModifierComboBox.right+15;extendsLbl.width=labelWidth;extendsFld.left=extendsLbl.right+1;extendsFld.top=nameFld.bottom+5;extendsFld.right=container.right-5;implementsLbl.centerY=implementsFld.centerY;implementsLbl.left=accessModifierComboBox.right+15;implementsLbl.width=labelWidth;implementsFld.left=implementsLbl.right+1;implementsFld.top=extendsFld.bottom+5;implementsFld.right=container.right-5;annotatedByLbl.centerY=annotatedByFld.centerY;annotatedByLbl.left=accessModifierComboBox.right+15;annotatedByLbl.width=labelWidth;annotatedByFld.left=annotatedByLbl.right+1;annotatedByFld.top=implementsFld.bottom+5;annotatedByFld.right=container.right-5;hintLbl.left=5;hintLbl.bottom=container.bottom-5;hintLbl.right=container.right-5;";
	public static String higherLevelExcludeStyle = "higherLevelExcludeChk.left = annotatedByLbl.left;higherLevelExcludeChk.top  = annotatedByLbl.bottom+5";
	public boolean higherLevelExcludeEnabled;
	public DefaultComboBoxModel<String> accessModifierModel;
	public JTextField implementsField;
	public JTextField extendsField;
	public JCheckBox higherLevelExcludeCheckBox;
	public JTextField nameField;
	public JComboBox<String> accessModifierComboBox;
	public JTextField annotatedByField;
	private DefaultListModel<String> modifierListModel;
	private PlatformAdaptiveList<String> modifierList;

	public ContainingClassPanel(JFrame parent, ParameterModel parameterModel, boolean higherLevelExcludeEnabled, int parameterKind) {
		super(parent, parameterModel, parameterKind);
		this.higherLevelExcludeEnabled = higherLevelExcludeEnabled;
		this.initComponents();
	}

	public void commitFieldEdit(Object source) {
		if (source == this.nameField) {
			String classNamePattern = this.nameField.getText().trim();
			if (classNamePattern.length() == 0) {
				this.nameField.setText(super.parameterModel.getClassNamePattern());
				CopyablePrompt.show(super.parentFrame, "Error", "Class name must be specified. Field restored.");
			} else if (classNamePattern.indexOf(46) == -1 && classNamePattern.indexOf(47) == -1) {
				if (classNamePattern.endsWith("^")) {
					classNamePattern = classNamePattern.substring(0, classNamePattern.length() - 1).trim();
					this.nameField.setText(classNamePattern);
				}

				super.parameterModel.setClassNamePattern(classNamePattern);
			} else {
				this.nameField.setText(super.parameterModel.getClassNamePattern());
				CopyablePrompt.show(super.parentFrame, "Error", "Class name must be unqualified. Enter package qualifiers in the \"Containing package\" panel. Field restored.");
			}
		} else if (source == this.extendsField) {
			String extendsClassName = this.extendsField.getText().trim();
			if (extendsClassName.indexOf("*") != -1) {
				this.extendsField.setText(super.parameterModel.getExtendsClassName());
				CopyablePrompt.show(super.parentFrame, "Error", "Extends class name cannot contain wildcards. Field restored.");
			} else {
				super.parameterModel.setExtendsClassName(extendsClassName);
			}
		} else if (source == this.implementsField) {
			String implementsClassNames = this.implementsField.getText().trim();
			if (implementsClassNames.indexOf("*") != -1) {
				this.implementsField.setText(super.parameterModel.getImplementsClassNames());
				CopyablePrompt.show(super.parentFrame, "Error", "Implements class names cannot contain wildcards. Field restored.");
			} else {
				super.parameterModel.setImplementsClassNames(implementsClassNames);
			}
		} else if (source == this.annotatedByField) {
			super.parameterModel.setClassAnnotationName(this.annotatedByField.getText().trim());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		this.commitFieldEdit(eventSource);
	}

	public void loadAccessFlags() {
		AccessFlags classAccessFlags = super.parameterModel.getClassAccessFlags();
		if (classAccessFlags != null) {
			if (classAccessFlags.isPositivePublic()) {
				this.accessModifierComboBox.setSelectedIndex(1);
			} else if (classAccessFlags.isDefaultAccess()) {
				this.accessModifierComboBox.setSelectedIndex(2);
			} else {
				this.accessModifierComboBox.setSelectedIndex(0);
			}

			if (classAccessFlags.isPositiveAbstract()) {
				this.modifierList.addSelectionInterval(0, 0);
			}

			if (classAccessFlags.isPositiveFinal()) {
				this.modifierList.addSelectionInterval(1, 1);
			}

			if (classAccessFlags.isPositiveInterface()) {
				this.modifierList.addSelectionInterval(2, 2);
			}

			if (classAccessFlags.isNegativeAbstract()) {
				this.modifierList.addSelectionInterval(3, 3);
			}

			if (classAccessFlags.isNegativeFinal()) {
				this.modifierList.addSelectionInterval(4, 4);
			}

			if (classAccessFlags.isNegativeInterface()) {
				this.modifierList.addSelectionInterval(5, 5);
			}
		}
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
		JLabel extendsLabel = new JLabel("Extends:", 2);
		this.extendsField = new JTextField();
		JLabel implementsLabel = new JLabel("Implements:", 2);
		this.implementsField = new JTextField();
		JLabel annotatedByLabel = new JLabel("Annotated by:", 2);
		this.annotatedByField = new JTextField();
		super.hintLabel = new JLabel(" ");
		this.add(this.accessModifierComboBox, "accessModifierComboBox");
		this.add(new PlatformAdaptiveScrollPanel(this.modifierList), "modifierList");
		this.add(this.nameField, "nameFld");
		this.add(this.extendsField, "extendsFld");
		this.add(this.implementsField, "implementsFld");
		this.add(this.annotatedByField, "annotatedByFld");
		this.add(nameLabel, "nameLbl");
		this.add(extendsLabel, "extendsLbl");
		this.add(implementsLabel, "implementsLbl");
		this.add(annotatedByLabel, "annotatedByLbl");
		this.add(super.hintLabel, "hintLbl");
		StringBuffer stylesBuffer = new StringBuffer(layoutStyles);
		if (this.higherLevelExcludeEnabled) {
			switch (super.parameterKind) {
				case 1:
					this.higherLevelExcludeCheckBox = new JCheckBox("Exclude class name as well", super.parameterModel.isClassNameRecursive());
					break;
				case 2:
					this.higherLevelExcludeCheckBox = new JCheckBox("Exclude class as well", super.parameterModel.isClassNameRecursive());
			}

			this.higherLevelExcludeCheckBox.addItemListener(this);
			this.add(this.higherLevelExcludeCheckBox, "higherLevelExcludeChk");
			stylesBuffer.append(higherLevelExcludeStyle);
		}

		powerLayout.applyStyles(stylesBuffer.toString());
		this.accessModifierModel.addElement("<access not specified>");
		this.accessModifierModel.addElement("public");
		this.accessModifierModel.addElement("package");
		this.modifierListModel.addElement("abstract");
		this.modifierListModel.addElement("final");
		this.modifierListModel.addElement("interface");
		this.modifierListModel.addElement("!abstract");
		this.modifierListModel.addElement("!final");
		this.modifierListModel.addElement("!interface");
		this.loadAccessFlags();
		this.nameField.setText(super.parameterModel.getClassNamePattern());
		this.extendsField.setText(super.parameterModel.getExtendsClassName());
		this.implementsField.setText(super.parameterModel.getImplementsClassNames());
		this.annotatedByField.setText(super.parameterModel.getClassAnnotationName());
		this.accessModifierComboBox.addItemListener(this);
		this.modifierList.addListSelectionListener(this);
		this.nameField.addFocusListener(this);
		this.extendsField.addFocusListener(this);
		this.implementsField.addFocusListener(this);
		this.annotatedByField.addFocusListener(this);
		this.nameField.addActionListener(this);
		this.extendsField.addActionListener(this);
		this.implementsField.addActionListener(this);
		this.annotatedByField.addActionListener(this);
	}

	@Override
	public void focusGained(FocusEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.nameField) {
			super.hintLabel.setText("Enter name of the class. May contain the \"*\" wildcard.");
		} else if (eventSource == this.extendsField) {
			super.hintLabel.setText("Enter fully qualified class name. May not contain wildcards.");
		} else if (eventSource == this.implementsField) {
			super.hintLabel.setText("Enter comma separated list of fully qualified class names. May not contain wildcards.");
		} else if (eventSource == this.annotatedByField) {
			super.hintLabel.setText("Enter fully qualified class name.  May contain the \"*\" wildcard.");
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
		Object eventSource = e.getSource();
		if (eventSource == this.accessModifierComboBox) {
			if (e.getStateChange() == 1) {
				switch (this.accessModifierComboBox.getSelectedIndex()) {
					case 0:
						super.parameterModel.clearClassPackagePrivate();
						break;
					case 1:
						super.parameterModel.markClassPublic();
						break;
					case 2:
						super.parameterModel.markClassPackagePrivate();
				}
			}
		} else if (eventSource == this.higherLevelExcludeCheckBox) {
			if (e.getStateChange() == 1) {
				super.parameterModel.setClassNameRecursive(true);
			} else {
				super.parameterModel.setClassNameRecursive(false);
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		PlatformAdaptiveList<String> sourceList = (PlatformAdaptiveList<String>)e.getSource();
		int[] selectedIndices = sourceList.getSelectedIndices();
		StringBuffer selectedIndicesBuffer = new StringBuffer();

		for (int i = 0; i < selectedIndices.length; i++) {
			selectedIndicesBuffer.append(selectedIndices[i] + ",");
		}

		int modifierCount = this.modifierListModel.getSize();

		for (int modifierIndex = 0; modifierIndex < modifierCount; modifierIndex++) {
			switch (modifierIndex) {
				case 0:
					super.parameterModel.setClassAbstract(sourceList.isSelectedIndex(modifierIndex));
					break;
				case 1:
					super.parameterModel.setClassFinal(sourceList.isSelectedIndex(modifierIndex));
					break;
				case 2:
					super.parameterModel.setClassInterface(sourceList.isSelectedIndex(modifierIndex));
					break;
				case 3:
					super.parameterModel.setClassNotAbstract(sourceList.isSelectedIndex(modifierIndex));
					break;
				case 4:
					super.parameterModel.setClassNotFinal(sourceList.isSelectedIndex(modifierIndex));
					break;
				case 5:
					super.parameterModel.setClassNotInterface(sourceList.isSelectedIndex(modifierIndex));
			}
		}
	}
}
