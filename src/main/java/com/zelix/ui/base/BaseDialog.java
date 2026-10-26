package com.zelix.ui.base;

import com.zelix.ui.utils.GuiUtils;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;

public abstract class BaseDialog extends BaseFrame {
	protected JFrame parentFrame;
	private boolean modal;

	public BaseDialog(JFrame parent, String title, Object initArg1, Object initArg2, Object initArg3, Object initArg4) {
		this(parent, title, true, initArg1, initArg2, initArg3, initArg4, null, null, null);
	}

	public BaseDialog(JFrame parent, String title, Object initArg1, Object initArg2) {
		this(parent, title, true, initArg1, initArg2, null, null, null, null, null);
	}

	public BaseDialog(JFrame parent, String title, Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5) {
		this(parent, title, true, initArg1, initArg2, initArg3, initArg4, initArg5, null, null);
	}

	public BaseDialog(JFrame parent, String title, Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6) {
		this(parent, title, true, initArg1, initArg2, initArg3, initArg4, initArg5, initArg6, null);
	}

	public BaseDialog(JFrame parent, String title, boolean modal, Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		super(title);
		this.parentFrame = parent;
		this.modal = modal;
		if (modal) {
			parent.setEnabled(false);
		}

		BaseDialog.WindowClosingListener windowClosingListener = new BaseDialog.WindowClosingListener(this);
		this.addWindowListener(windowClosingListener);
		this.initGui(initArg1, initArg2, initArg3, initArg4, initArg5, initArg6, initArg7);
	}

	public BaseDialog(String title) {
		this(null, title, false, null, null, null, null, null, null, null);
	}

	public BaseDialog(JFrame parent, String title) {
		this(parent, title, true, null, null, null, null, null, null, null);
	}

	public BaseDialog(JFrame parent, String title, Object initArg1, Object initArg2, Object initArg3) {
		this(parent, title, true, initArg1, initArg2, initArg3, null, null, null, null);
	}

	public BaseDialog(JFrame parent, String title, Object initArg1) {
		this(parent, title, true, initArg1, null, null, null, null, null, null);
	}

	public void centerOnParent() {
		this.positionDialog(0, 0, true);
	}

	protected abstract void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7);

	public void onPositionCompleted() {
	}

	@Override
	public Action getAction() {
		return new BaseDialog.ConfirmAction(this);
	}

	protected abstract void onConfirm();

	@Override
	public void close() {
		super.close();
		if (this.modal) {
			this.parentFrame.setEnabled(true);
			this.parentFrame.toFront();
		}
	}

	public final void positionDialog(int x, int y, boolean centerOnParent) {
		Dimension dialogSize = this.getSize();
		int locationX;
		int locationY;
		if (centerOnParent) {
			Point parentFrameLocation = this.parentFrame.getLocationOnScreen();
			Dimension parentFrameSize = this.parentFrame.getSize();
			locationX = parentFrameSize.width / 2 - dialogSize.width / 2 + parentFrameLocation.x;
			locationY = parentFrameSize.height / 2 - dialogSize.height / 2 + parentFrameLocation.y;
			locationX = Math.max(0, locationX) + x;
			locationY = Math.max(0, locationY) + y;
		} else {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			locationX = screenSize.width / 2 - dialogSize.width / 2;
			locationY = screenSize.height / 2 - dialogSize.height / 2;
			locationX = Math.max(0, locationX) + x;
			locationY = Math.max(0, locationY) + y;
		}

		this.setLocation(locationX, locationY);
		GuiUtils.setVisible(this, true);
		this.toFront();
		this.onPositionCompleted();
	}

	public static class WindowClosingListener extends WindowAdapter {
		public final BaseDialog baseDialog;

		public WindowClosingListener(BaseDialog baseDialog) {
			this.baseDialog = baseDialog;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			this.baseDialog.close();
		}
	}

	public static class ConfirmAction extends AbstractAction {
		public final BaseDialog baseDialog;

		public ConfirmAction(BaseDialog baseDialog) {
			this.baseDialog = baseDialog;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (this.baseDialog.isEnabled()) {
				this.baseDialog.onConfirm();
			}
		}
	}
}
