package com.zelix.ui.frames.tools.obfuscate.references;

import com.zelix.HelpManager;
import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.klassmaster.config.ReferenceObfuscateConfig;
import com.zelix.klassmaster.config.base.ConfigBase;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.dialogs.AddMembersDialog;
import com.zelix.ui.dialogs.filter.AddFieldFilterDialog;
import com.zelix.ui.dialogs.filter.AddMethodFilterDialog;
import com.zelix.ui.frames.StatementListFrame;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import javax.swing.JButton;

public class ObfuscateReferencesFrame extends StatementListFrame {
	private static String emptyParamListExplanation = "You have selected \"obfuscate references\".\nYou now need to specify which references to obfuscate.\nOtherwise your selection will have no affect.\nClick on the <Add> button.";
	public static String[] layoutProperties = new String[]{
		"mainParamList.top=5",
		"mainParamList.left=5",
		"mainParamList.right=container.right-btnWidth-10",
		"mainParamList.bottom=container.height*50/100",
		"explanationArea.left=5",
		"explanationArea.top=mainParamList.bottom+7",
		"explanationArea.right=container.right-5",
		"explanationArea.bottom=container.bottom-5",
		"btnWidth=max(mainAddBtn.defaultWidth, mainModifyBtn.defaultWidth, mainDeleteBtn.defaultWidth)",
		"mainAddBtn.width=btnWidth",
		"mainAddBtn.left=mainParamList.right+5",
		"mainAddBtn.centerY=container.height*20/200",
		"mainModifyBtn.width=btnWidth",
		"mainModifyBtn.left=mainAddBtn.left",
		"mainModifyBtn.centerY=container.height*50/200",
		"mainDeleteBtn.width=btnWidth",
		"mainDeleteBtn.left=mainAddBtn.left",
		"mainDeleteBtn.centerY=container.height*80/200"
	};
	public static String[] frameLayoutProperties = new String[]{
		"layout.minWidth=btnWidth*4+25",
		"layout.minHeight=250",
		"mainPnl.left=5",
		"mainPnl.top=5",
		"mainPnl.right=container.right-5",
		"mainPnl.bottom=container.bottom-btnHeight-20",
		"btnWidth=max(previousBtn.defaultWidth, okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(previousBtn.defaultHeight, okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"previousBtn.top=mainPnl.bottom+5",
		"previousBtn.centerX=container.width*12/100",
		"previousBtn.width=btnWidth",
		"previousBtn.height=btnHeight",
		"okBtn.top=previousBtn.top",
		"okBtn.centerX=container.width*37/100",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"cancelBtn.top=previousBtn.top",
		"cancelBtn.centerX=container.width*62/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"helpBtn.top=previousBtn.top",
		"helpBtn.centerX=container.width*87/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight"
	};
	private ReferenceObfuscateConfig referenceObfuscateConfig;
	private JButton previousButton;

	public ObfuscateReferencesFrame(String title, GUI gui, ReferenceObfuscateConfig referenceObfuscateConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer, int parameterCategory) {
		super(title, gui, scriptContext, resultConsumer, parameterCategory);
		this.referenceObfuscateConfig = referenceObfuscateConfig;
		this.initFrame(title);
		GuiUtils.requestFocusInWindow(super.okButton);
	}

	@Override
	public ConfigBase getConfig() {
		return this.referenceObfuscateConfig;
	}

	@Override
	public void modifyParameter(ParameterModel parameterModel, String ruleString, ZKMConsumer consumer) {
		switch (parameterModel.getTargetType()) {
			case 3:
				this.showParameterDialog("1", ruleString, consumer);
				break;
			case 4:
				this.showParameterDialog("2", ruleString, consumer);
		}
	}

	@Override
	public String[] getLayoutProperties() {
		return layoutProperties;
	}

	@Override
	public void initScreenContent(Container container) {
		if (this.referenceObfuscateConfig.isPersisted()) {
			Enumeration<?> ruleStringEnumeration = this.referenceObfuscateConfig.getParameterEnumeration();

			while (ruleStringEnumeration.hasMoreElements()) {
				String configRuleString = (String)ruleStringEnumeration.nextElement();

				try {
					if (this.parseParameter(configRuleString).getRuleString().equals(configRuleString)) {
						this.addParameter(configRuleString);
					}
				} catch (DebugAssertFailedException debugAssertFailedConfig) {
					throw debugAssertFailedConfig;
				} catch (Throwable ignored) {
				}
			}
		}
	}

	@Override
	public void showParameterDialog(Object targetType, String ruleString, ZKMConsumer consumer) {
		String parameterType = (String)targetType;
		if (parameterType.equals("1")) {
			if (ruleString == null) {
				ruleString = "*.* *";
			}

			new AddFieldFilterDialog(this, CopyrightConstants.NAME + " - Field Include Parameter", this.parseParameter(ruleString), 3, consumer);
		} else if (parameterType.equals("2")) {
			if (ruleString == null) {
				ruleString = "*.* *(*)";
			}

			new AddMethodFilterDialog(this, CopyrightConstants.NAME + " - Method Include Parameter", this.parseParameter(ruleString), 3, consumer);
		}
	}

	@Override
	public void showHelp() {
		HelpManager.showHelp("041");
	}

	@Override
	public void initToolTips() {
		super.addButton.setToolTipText(HelpManager.getToolTip("ADD_INCLUSION_PARAM"));
		super.modifyButton.setToolTipText(HelpManager.getToolTip("MODIFY_INCLUSION_PARAM"));
		super.deleteButton.setToolTipText(HelpManager.getToolTip("DELETE_INCLUSION_PARAM"));
		super.paramList.setToolTipText(HelpManager.getToolTip("CURRENT_INCLUSION_PARAMS"));
		super.explanationArea.setToolTipText(HelpManager.getToolTip("EXPLAIN_SELECTED_INCLUSION_PARAM"));
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		super.okButton.setToolTipText(HelpManager.getToolTip("OK_INCLUDE"));
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
	}

	public final void onPrevious() {
		super.skipCloseCallback = true;
		this.close();
		this.applyToConfig();
		super.resultConsumer.accept(2);
	}

	@Override
	public void showParameterTypeDialog(ZKMConsumer consumer) {
		new AddMembersDialog(this, CopyrightConstants.NAME + " Include Parameter Type", consumer);
	}

	@Override
	public void addFrameButtons(PowerLayout layout, Container container) {
		this.previousButton = new JButton("Previous");
		super.okButton = new JButton("Next");
		super.cancelButton = new JButton("Cancel");
		super.helpButton = new JButton("Help");
		ObfuscateReferencesFrame.ButtonActionListener buttonActionListener = new ObfuscateReferencesFrame.ButtonActionListener(
			this
		);
		this.previousButton.addActionListener(buttonActionListener);
		super.okButton.addActionListener(buttonActionListener);
		super.cancelButton.addActionListener(buttonActionListener);
		super.helpButton.addActionListener(buttonActionListener);
		ObfuscateReferencesFrame.ButtonEnterKeyListener buttonKeyListener = new ObfuscateReferencesFrame.ButtonEnterKeyListener(
			this
		);
		this.previousButton.addKeyListener(buttonKeyListener);
		super.okButton.addKeyListener(buttonKeyListener);
		super.cancelButton.addKeyListener(buttonKeyListener);
		super.helpButton.addKeyListener(buttonKeyListener);
		container.add(this.previousButton, "previousBtn");
		container.add(super.okButton, "okBtn");
		container.add(super.cancelButton, "cancelBtn");
		container.add(super.helpButton, "helpBtn");
		if (this.referenceObfuscateConfig.getParameterCount() == 0) {
			super.explanationArea.setText(emptyParamListExplanation);
		}

		layout.setProperties(frameLayoutProperties);
	}

	public static JButton getPreviousButton(ObfuscateReferencesFrame frame) {
		return frame.previousButton;
	}

	public static class ButtonActionListener implements ActionListener {
		public final ObfuscateReferencesFrame obfuscateReferencesOptionsFrame;

		public ButtonActionListener(ObfuscateReferencesFrame obfuscateReferencesOptionsFrame) {
			this.obfuscateReferencesOptionsFrame = obfuscateReferencesOptionsFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object eventSource = e.getSource();
			if (eventSource == ObfuscateReferencesFrame.getPreviousButton(this.obfuscateReferencesOptionsFrame)) {
				this.obfuscateReferencesOptionsFrame.onPrevious();
			} else if (eventSource == this.obfuscateReferencesOptionsFrame.okButton) {
				this.obfuscateReferencesOptionsFrame.onOk();
			} else if (eventSource == this.obfuscateReferencesOptionsFrame.cancelButton) {
				this.obfuscateReferencesOptionsFrame.onCancel();
			} else if (eventSource == this.obfuscateReferencesOptionsFrame.helpButton) {
				this.obfuscateReferencesOptionsFrame.showHelp();
			}
		}
	}

	public static class ButtonEnterKeyListener extends KeyAdapter {
		public final ObfuscateReferencesFrame outerFrame;

		public ButtonEnterKeyListener(ObfuscateReferencesFrame obfuscateReferencesOptionsFrame) {
			this.outerFrame = obfuscateReferencesOptionsFrame;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == ObfuscateReferencesFrame.getPreviousButton(this.outerFrame)) {
					this.outerFrame.onPrevious();
				} else if (e.getSource() == this.outerFrame.okButton) {
					this.outerFrame.onOk();
				} else if (e.getSource() == this.outerFrame.cancelButton) {
					this.outerFrame.onCancel();
				} else if (e.getSource() == this.outerFrame.helpButton) {
					this.outerFrame.showHelp();
				}
			}
		}
	}
}
