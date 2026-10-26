package com.zelix.ui.components.panels.filter;

import com.zelix.ui.components.panels.base.ContainingPanel;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class ContainingClassSuffixPanel extends ContainingPanel implements ActionListener, FocusListener {
	public static String[] layoutStyles = new String[]{
		"layout.height=nameFld.defaultHeight*8",
		"labelWidth=max(nameLbl.defaultWidth, extendsLbl.defaultWidth, implementsLbl.defaultWidth, annotatedByLbl.defaultWidth)",
		"nameLbl.centerY=nameFld.centerY",
		"nameLbl.left=5",
		"nameLbl.width=labelWidth",
		"nameFld.left=nameLbl.right+1",
		"nameFld.top=5",
		"nameFld.right=container.right-5",
		"extendsLbl.centerY=extendsFld.centerY",
		"extendsLbl.left=5",
		"extendsLbl.width=labelWidth",
		"extendsFld.left=extendsLbl.right+1",
		"extendsFld.top=nameFld.bottom+5",
		"extendsFld.right=container.right-5",
		"implementsLbl.centerY=implementsFld.centerY",
		"implementsLbl.left=5",
		"implementsLbl.width=labelWidth",
		"implementsFld.left=implementsLbl.right+1",
		"implementsFld.top=extendsFld.bottom+5",
		"implementsFld.right=container.right-5",
		"annotatedByLbl.centerY=annotatedByFld.centerY",
		"annotatedByLbl.left=5",
		"annotatedByLbl.width=labelWidth",
		"annotatedByFld.left=annotatedByLbl.right+1",
		"annotatedByFld.top=implementsFld.bottom+5",
		"annotatedByFld.right=container.right-5",
		"hintLbl.left=5",
		"hintLbl.bottom=container.bottom-5",
		"hintLbl.right=container.right-5"
	};
	public JTextField suffixField;
	public JTextField implementsField;
	public JTextField extendsField;
	public JTextField annotatedByField;

	public ContainingClassSuffixPanel(JFrame parent, ParameterModel parameterModel, int parameterKind) {
		super(parent, parameterModel, parameterKind);
		this.initComponents();
	}

	public void commitFieldEdit(Object source) {
		if (source == this.suffixField) {
			String classNameSuffix = this.suffixField.getText().trim();
			if (classNameSuffix.length() == 0) {
				this.suffixField.setText(super.parameterModel.getClassNameSuffix());
				CopyablePrompt.show(super.parentFrame, "Error", "Class suffix must be specified. Field restored.");
			} else if (classNameSuffix.indexOf("*") != -1) {
				this.suffixField.setText(super.parameterModel.getClassNameSuffix());
				CopyablePrompt.show(super.parentFrame, "Error", "Class suffix cannot contain wildcards. Field restored.");
			} else {
				super.parameterModel.setClassNameSuffix(classNameSuffix);
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

	@Override
	public void initComponents() {
		PowerLayout powerLayout = new PowerLayout(this);
		this.setLayout(powerLayout);
		JLabel suffixLabel = new JLabel("Suffix:", 2);
		this.suffixField = new JTextField();
		JLabel extendsLabel = new JLabel("Extends:", 2);
		this.extendsField = new JTextField();
		JLabel implementsLabel = new JLabel("Implements:", 2);
		this.implementsField = new JTextField();
		JLabel annotatedByLabel = new JLabel("Annotated by:", 2);
		this.annotatedByField = new JTextField();
		super.hintLabel = new JLabel(" ");
		this.add(this.suffixField, "nameFld");
		this.add(this.extendsField, "extendsFld");
		this.add(this.implementsField, "implementsFld");
		this.add(this.annotatedByField, "annotatedByFld");
		this.add(suffixLabel, "nameLbl");
		this.add(extendsLabel, "extendsLbl");
		this.add(implementsLabel, "implementsLbl");
		this.add(annotatedByLabel, "annotatedByLbl");
		this.add(super.hintLabel, "hintLbl");
		powerLayout.setProperties(layoutStyles);
		this.suffixField.setText(super.parameterModel.getClassNameSuffix());
		this.extendsField.setText(super.parameterModel.getExtendsClassName());
		this.implementsField.setText(super.parameterModel.getImplementsClassNames());
		this.annotatedByField.setText(super.parameterModel.getClassAnnotationName());
		this.suffixField.addFocusListener(this);
		this.extendsField.addFocusListener(this);
		this.implementsField.addFocusListener(this);
		this.annotatedByField.addFocusListener(this);
		this.suffixField.addActionListener(this);
		this.extendsField.addActionListener(this);
		this.implementsField.addActionListener(this);
		this.annotatedByField.addActionListener(this);
	}

	@Override
	public void focusGained(FocusEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.suffixField) {
			super.hintLabel.setText("Enter the class name suffix. No wildcards.");
		} else if (eventSource == this.extendsField) {
			super.hintLabel.setText("Enter fully qualified class name. May not contain wildcards.");
		} else if (eventSource == this.implementsField) {
			super.hintLabel.setText("Enter comma separated list of fully qualified class names. May not contain wildcards.");
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
}
