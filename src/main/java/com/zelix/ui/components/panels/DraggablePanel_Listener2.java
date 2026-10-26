package com.zelix.ui.components.panels;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class DraggablePanel_Listener2 extends MouseMotionAdapter {
	public final DraggablePanel draggablePanel;

	public DraggablePanel_Listener2(DraggablePanel draggablePanel) {
		this.draggablePanel = draggablePanel;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		this.draggablePanel.dragging = true;
		this.draggablePanel.splitLayout.dragDivider(e.getPoint());
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}
}
