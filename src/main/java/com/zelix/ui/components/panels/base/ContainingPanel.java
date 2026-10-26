package com.zelix.ui.components.panels.base;

import com.zelix.ui.dataset.ParameterModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public abstract class ContainingPanel extends JPanel {
	public int parameterKind;
	public JFrame parentFrame;
	public ParameterModel parameterModel;
	public JLabel hintLabel;
	public JTextField packageNameField;

	public ContainingPanel(JFrame parent, ParameterModel parameterModel, int parameterKind) {
		this.parameterKind = parameterKind;
		this.parentFrame = parent;
		this.parameterModel = parameterModel;
	}

	public abstract void initComponents();
}
