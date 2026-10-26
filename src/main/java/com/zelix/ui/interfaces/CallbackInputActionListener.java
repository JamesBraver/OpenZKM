package com.zelix.ui.interfaces;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public interface CallbackInputActionListener {
	void onTextChanged(String text);

	void onAction(ActionEvent event);

	public static class ActionForwardingListener implements ActionListener {
		public CallbackInputActionListener callbackListener;

		public ActionForwardingListener(CallbackInputActionListener callbackListener) {
			this.callbackListener = callbackListener;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			this.callbackListener.onAction(e);
		}
	}
}
