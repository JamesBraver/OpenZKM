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
import javax.swing.InputMap;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public abstract class BasePrompt extends JDialog {
	protected static final String separator = GuiUtils.LINE_SEPARATOR;
	protected JFrame parentFrame;

	public BasePrompt(JFrame parent, String title, boolean modal) {
		super(parent, title, modal);
		this.parentFrame = parent;
		BasePrompt.WindowClosingListener windowClosingListener = new BasePrompt.WindowClosingListener(this);
		this.addWindowListener(windowClosingListener);
	}

	public void closeDialog() {
		this.setVisible(false);
		this.dispose();
	}

	public Action createEscapeAction() {
		return new BasePrompt.EscapeAction(this);
	}

	public final void positionPrompt(int x, int y, boolean centerOnParent) {
		Dimension promptSize = this.getSize();
		int locationX;
		int locationY;
		if (centerOnParent && this.parentFrame.isVisible()) {
			Point parentFrameLocation = this.parentFrame.getLocationOnScreen();
			Dimension parentFrameSize = this.parentFrame.getSize();
			locationX = parentFrameSize.width / 2 - promptSize.width / 2 + parentFrameLocation.x;
			locationY = parentFrameSize.height / 2 - promptSize.height / 2 + parentFrameLocation.y;
			locationX = Math.max(0, locationX) + x;
			locationY = Math.max(0, locationY) + y;
		} else {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			locationX = screenSize.width / 2 - promptSize.width / 2;
			locationY = screenSize.height / 2 - promptSize.height / 2;
			locationX = Math.max(0, locationX) + x;
			locationY = Math.max(0, locationY) + y;
		}

		this.setLocation(locationX, locationY);
		GuiUtils.setVisible(this, true);
	}

	@Override
	protected JRootPane createRootPane() {
		JRootPane jRootPane = new JRootPane();
		KeyStroke keyStroke = KeyStroke.getKeyStroke("ESCAPE");
		Action action = this.createEscapeAction();
		InputMap inputMap = jRootPane.getInputMap(2);
		inputMap.put(keyStroke, "ESCAPE");
		jRootPane.getActionMap().put("ESCAPE", action);
		return jRootPane;
	}

	public final void centerOnParent() {
		this.positionPrompt(0, 0, true);
	}

	public static class WindowClosingListener extends WindowAdapter {
		public final BasePrompt basePrompt;

		public WindowClosingListener(BasePrompt basePrompt) {
			this.basePrompt = basePrompt;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			this.basePrompt.closeDialog();
		}
	}

	public static class EscapeAction extends AbstractAction {
		public final BasePrompt basePrompt;

		public EscapeAction(BasePrompt basePrompt) {
			this.basePrompt = basePrompt;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (this.basePrompt.isEnabled()) {
				this.basePrompt.closeDialog();
			}
		}
	}
}
