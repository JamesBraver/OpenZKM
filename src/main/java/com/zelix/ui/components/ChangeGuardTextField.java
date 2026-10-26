package com.zelix.ui.components;

import javax.swing.JTextField;

public class ChangeGuardTextField extends JTextField {
	public boolean changing;

	public boolean isChanging() {
		return this.changing;
	}

	@Override
	public void setText(String t) {
		this.changing = true;
		super.setText(t);
		this.changing = false;
	}
}
