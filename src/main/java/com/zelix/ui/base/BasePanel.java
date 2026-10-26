package com.zelix.ui.base;

import com.zelix.event.ChangeListener;
import com.zelix.event.INotifier;
import java.awt.Font;
import javax.swing.JPanel;

public class BasePanel extends JPanel implements ChangeListener {
	public BasePanel() {
		this.setFont(new Font("Dialog", Font.PLAIN, 12));
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
	}
}
