package com.zelix.ui.components.panels;

import com.zelix.ui.base.BasePanel;
import javax.swing.BorderFactory;

public class BorderedPanel extends BasePanel {
	public BorderedPanel(boolean isRaised) {
		if (isRaised) {
			this.setBorder(BorderFactory.createRaisedBevelBorder());
		} else {
			this.setBorder(BorderFactory.createLoweredBevelBorder());
		}
	}

	@Override
	public void removeAll() {
		super.removeAll();
		this.repaint();
	}
}
