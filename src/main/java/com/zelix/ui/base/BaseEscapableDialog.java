package com.zelix.ui.base;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public abstract class BaseEscapableDialog extends JDialog {
	public BaseEscapableDialog(Frame frame, String title, boolean modal) {
		super(frame, title, modal);
	}

	public Action getAction() {
		return new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (BaseEscapableDialog.this.isEnabled()) {
					BaseEscapableDialog.this.onEscapePressed();
				}
			}
		};
	}

	@Override
	protected JRootPane createRootPane() {
		JRootPane jRootPane = new JRootPane();
		KeyStroke keyStroke = KeyStroke.getKeyStroke("ESCAPE");
		Action action = this.getAction();
		InputMap inputMap = jRootPane.getInputMap(2);
		inputMap.put(keyStroke, "ESCAPE");
		jRootPane.getActionMap().put("ESCAPE", action);
		return jRootPane;
	}

	protected abstract void onEscapePressed();
}
