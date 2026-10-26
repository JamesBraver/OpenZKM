package com.zelix.ui.base;

import com.zelix.event.ChangeListener;
import com.zelix.event.INotifier;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public class BaseFrame extends JFrame implements ChangeListener {
	public BaseFrame() {
		this.init();
	}

	public BaseFrame(String title) {
		super(title);
		this.init();
	}

	private void init() {
	}

	protected void onClose() {
	}

	@Override
	protected JRootPane createRootPane() {
		JRootPane jRootPane = new JRootPane();
		KeyStroke keyStroke = KeyStroke.getKeyStroke("ESCAPE");
		Action action = this.getAction();
		InputMap inputMap = jRootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(keyStroke, "ESCAPE");
		jRootPane.getActionMap().put("ESCAPE", action);
		return jRootPane;
	}

	@Override
	public void doLayout() {
		super.doLayout();
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	public Action getAction() {
		return new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (this.isEnabled()) {
					BaseFrame.this.close();
				}
			}
		};
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
	}

	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if (b) {
			this.onClose();
		}
	}

	public void close() {
		this.setVisible(false);
		this.dispose();
	}

	@Override
	public void validate() {
		super.validate();
	}

	@Override
	public void validateTree() {
		super.validateTree();
	}
}
