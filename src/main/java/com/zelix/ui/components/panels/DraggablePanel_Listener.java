package com.zelix.ui.components.panels;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DraggablePanel_Listener extends MouseAdapter {
	public final DraggablePanel draggablePanel;

	public DraggablePanel_Listener(DraggablePanel draggablePanel) {
		this.draggablePanel = draggablePanel;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		this.draggablePanel.savedCursor = this.draggablePanel.getCursor();
		this.draggablePanel.updateCursor();
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (this.draggablePanel.savedCursor == null) {
			this.draggablePanel.savedCursor = new Cursor(0);
		}

		this.draggablePanel.setCursor(this.draggablePanel.savedCursor);
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (this.draggablePanel.dragging) {
			this.draggablePanel.dragging = false;
			this.draggablePanel.splitLayout.endDividerDrag(e.getPoint());
		}
	}
}
