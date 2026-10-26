package com.zelix.ui.components.panels;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class BeveledPanel extends JPanel {
	public BeveledPanel(boolean isRaised, boolean isEtched) {
		this(isRaised, isEtched, 1, 1);
	}

	public BeveledPanel(boolean isRaised, boolean isEtched, int horizontalPadding, int verticalPadding) {
		if (isRaised) {
			this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createEmptyBorder(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding)));
		} else if (isEtched) {
			this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding)));
		} else {
			this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding)));
		}
	}
}
