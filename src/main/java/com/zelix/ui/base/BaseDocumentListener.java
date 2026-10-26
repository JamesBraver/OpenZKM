package com.zelix.ui.base;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class BaseDocumentListener implements DocumentListener {
	public abstract void onUpdate(DocumentEvent e);

	@Override
	public void changedUpdate(DocumentEvent e) {
		this.onUpdate(e);
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		this.onUpdate(e);
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		this.onUpdate(e);
	}
}
