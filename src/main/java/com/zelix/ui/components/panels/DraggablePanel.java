package com.zelix.ui.components.panels;

import com.zelix.ui.layouts.SplitLayout;
import java.awt.Cursor;
import java.awt.SystemColor;
import javax.swing.JPanel;

public class DraggablePanel extends JPanel {
	public boolean dragging = false;
	public SplitLayout splitLayout;
	public Cursor savedCursor;

	public DraggablePanel(SplitLayout splitLayout) {
		this.splitLayout = splitLayout;
		this.setBackground(SystemColor.control.darker());
		this.addMouseListener(new DraggablePanel_Listener(this));
		this.addMouseMotionListener(new DraggablePanel_Listener2(this));
	}

	public void updateCursor() {
		Cursor cursor;
		if (this.splitLayout.splitType == 0) {
			if (this.splitLayout.splitRatio < 0.5) {
				cursor = new Cursor(11);
			} else {
				cursor = new Cursor(10);
			}
		} else if (this.splitLayout.splitRatio < 0.5) {
			cursor = new Cursor(9);
		} else {
			cursor = new Cursor(8);
		}

		this.setCursor(cursor);
	}
}
