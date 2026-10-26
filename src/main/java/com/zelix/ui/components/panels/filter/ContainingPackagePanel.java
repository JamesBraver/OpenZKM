package com.zelix.ui.components.panels.filter;

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
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class ContainingPackagePanel extends ContainingPanel implements ActionListener, FocusListener, ItemListener {
	public static String layoutStyles = "layout.height=nameFld.defaultHeight*8;nameLbl.centerY=nameFld.centerY;nameLbl.left=5;nameFld.left=nameLbl.right+5;nameFld.top=5;nameFld.right=container.right-5;hintLbl.left=5;hintLbl.bottom=container.bottom-5;hintLbl.right=container.right-5;";
	public static String higherLevelExcludeStyle = "higherLevelExcludeChk.left=nameLbl.left;higherLevelExcludeChk.top = nameLbl.bottom+5";
	public boolean higherLevelExcludeEnabled;
	public JCheckBox higherLevelExcludeCheckBox;

	public ContainingPackagePanel(JFrame parent, ParameterModel parameterModel, boolean higherLevelExcludeEnabled, int parameterKind) {
		super(parent, parameterModel, parameterKind);
		this.higherLevelExcludeEnabled = higherLevelExcludeEnabled;
		this.initComponents();
	}

	public void commitFieldEdit(Object source) {
		if (source == super.packageNameField) {
			String packageName = super.packageNameField.getText().trim();
			if (super.parameterModel.getTargetType() == 1 && packageName.length() == 0) {
				super.packageNameField.setText(super.parameterModel.getPackageNamePrefix());
				CopyablePrompt.show(super.parentFrame, "Error", "Package name must be entered. Field restored.");
			} else if (!packageName.startsWith("!") && packageName.indexOf("(") == -1 && packageName.indexOf(")") == -1) {
				if (packageName.endsWith("^")) {
					packageName = packageName.substring(0, packageName.length() - 1).trim();
					super.packageNameField.setText(packageName);
				}

				super.parameterModel.setPackageNameParts(packageName);
			} else {
				super.packageNameField.setText(super.parameterModel.getPackageNamePrefix());
				CopyablePrompt.show(super.parentFrame, "Error", "Package name must be a simple package name. Field restored.");
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
		JLabel nameLabel = new JLabel("Name:", 2);
		super.packageNameField = new JTextField();
		StringBuffer stylesBuffer = new StringBuffer(layoutStyles);
		if (this.higherLevelExcludeEnabled) {
			switch (super.parameterKind) {
				case 1:
					this.higherLevelExcludeCheckBox = new JCheckBox("Exclude package name as well", super.parameterModel.isPackageRecursive());
					break;
				case 2:
					this.higherLevelExcludeCheckBox = new JCheckBox("Exclude package as well", super.parameterModel.isPackageRecursive());
			}

			this.higherLevelExcludeCheckBox.addItemListener(this);
			this.add(this.higherLevelExcludeCheckBox, "higherLevelExcludeChk");
			stylesBuffer.append(higherLevelExcludeStyle);
		}

		super.hintLabel = new JLabel(" ");
		this.add(nameLabel, "nameLbl");
		this.add(super.packageNameField, "nameFld");
		this.add(super.hintLabel, "hintLbl");
		powerLayout.applyStyles(stylesBuffer.toString());
		super.packageNameField.setText(super.parameterModel.getPackageNamePrefix());
		super.packageNameField.addFocusListener(this);
		super.packageNameField.addActionListener(this);
	}

	@Override
	public void focusGained(FocusEvent e) {
		if (e.getSource() == super.packageNameField) {
			super.hintLabel.setText("Enter name of the package. May contain the \"*\" wildcard.");
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
		if (e.getStateChange() == 1) {
			super.parameterModel.setPackageRecursive(true);
		} else {
			super.parameterModel.setPackageRecursive(false);
		}
	}
}
