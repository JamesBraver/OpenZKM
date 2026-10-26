package com.zelix.ui.dialogs.base;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.event.INotifier;
import com.zelix.exceptions.ProcessingException;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.ui.base.BaseDialog;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.panels.BorderedPanel;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.ScrollableMessagePrompt;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

public abstract class BaseAddFilterDialog extends BaseDialog implements ActionListener, KeyListener {
	public static String[] containerStyles = new String[]{
		"layout.minWidth=300",
		"layout.minHeight=300",
		"tabbedPane.top=5",
		"tabbedPane.left=5",
		"tabbedPane.right=container.right-5",
		"paramLbl.centerY=displayPnl.centerY",
		"paramLbl.left=10",
		"displayPnl.top=tabbedPane.bottom+10",
		"displayPnl.left=paramLbl.right+3",
		"displayPnl.right=container.right-5",
		"explanationArea.left=5",
		"explanationArea.top=displayPnl.bottom+5",
		"explanationArea.right=container.right-5",
		"explanationArea.bottom=container.bottom-btnHeight-10",
		"btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"okBtn.bottom=container.bottom-5",
		"okBtn.centerX=container.width*16/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"cancelBtn.top=okBtn.top",
		"cancelBtn.centerX=container.width*50/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight",
		"helpBtn.top=okBtn.top",
		"helpBtn.centerX=container.width*84/100"
	};
	public static String[] paramDisplayStyles = new String[]{"paramDisplay.top=1", "paramDisplay.left=2", "paramDisplay.right=container.right-2"};
	public ZKMConsumer resultConsumer;
	public JButton helpButton;
	public int targetType;
	public JTabbedPane tabbedPane;
	public JLabel ruleLabel;
	public ParameterModel parameterModel;
	public JButton cancelButton;
	public JTextArea explanationArea;
	public JButton okButton;

	public BaseAddFilterDialog(JFrame parent, String title, ParameterModel parameterModel, int targetType, ZKMConsumer resultConsumer) {
		super(parent, title, parameterModel, targetType);
		this.setFont(KlassMaster.getFont());
		this.resultConsumer = resultConsumer;
		this.setIconImage(KlassMaster.getIcon(this));
		this.centerOnParent();
		parameterModel.subscribe(this);
	}

	public final void initParamDisplay(JPanel displayPanel) {
		PowerLayout displayLayout = new PowerLayout(displayPanel);
		displayPanel.setLayout(displayLayout);
		this.ruleLabel = new JLabel();
		this.ruleLabel.setText(this.parameterModel.getRuleString());
		this.ruleLabel.setFont(this.getFont());
		this.ruleLabel.setToolTipText(HelpManager.getToolTip("CURRENT_EXCLUDE_PARAM"));
		displayPanel.add(this.ruleLabel, "paramDisplay");
		displayLayout.setProperties(paramDisplayStyles);
	}

	protected abstract void openHelpGUI();

	@Override
	protected final void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		this.parameterModel = (ParameterModel)initArg1;
		this.targetType = (Integer)initArg2;
		Container contentPane = this.getContentPane();
		PowerLayout contentLayout = new PowerLayout(contentPane);
		contentPane.setLayout(contentLayout);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(Math.min(500, screenSize.width), Math.min(450, screenSize.height));
		this.okButton = new JButton("OK");
		this.okButton.setToolTipText(HelpManager.getToolTip("PARAMETER_COMPLETE"));
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.okButton.addKeyListener(this);
		this.cancelButton.addKeyListener(this);
		this.helpButton.addKeyListener(this);
		contentPane.add(this.okButton, "okBtn");
		contentPane.add(this.cancelButton, "cancelBtn");
		contentPane.add(this.helpButton, "helpBtn");
		this.tabbedPane = new JTabbedPane();
		contentPane.add(this.tabbedPane, "tabbedPane");
		JLabel parameterLabel = new JLabel("Parameter so far:");
		BorderedPanel displayPanel = new BorderedPanel(false);
		this.initParamDisplay(displayPanel);
		contentPane.add(parameterLabel, "paramLbl");
		contentPane.add(displayPanel, "displayPnl");
		this.explanationArea = new JTextArea();
		this.explanationArea.setEditable(false);
		if (this.targetType == 3) {
			this.explanationArea.setToolTipText(HelpManager.getToolTip("EXPLAIN_INCLUSION_PARAM"));
		} else {
			this.explanationArea.setToolTipText(HelpManager.getToolTip("EXPLAIN_EXCLUSION_PARAM"));
		}

		contentPane.add(new PlatformAdaptiveScrollPanel(this.explanationArea), "explanationArea");
		this.explanationArea.setText(this.parameterModel.getExplanation(this.targetType));
		this.explanationArea.setCaretPosition(0);
		contentLayout.setProperties(containerStyles);
		this.initTabs();
	}

	@Override
	@SneakyThrows
	public final void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.okButton) {
			this.applyParameter();
		} else if (eventSource == this.cancelButton) {
			this.onConfirm();
		} else if (eventSource == this.helpButton) {
			this.openHelpGUI();
		}
	}

	public abstract void initTabs();

	@Override
	@SneakyThrows
	public final void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			if (e.getSource() == this.okButton) {
				this.applyParameter();
			} else if (e.getSource() == this.cancelButton) {
				this.onConfirm();
			} else if (e.getSource() == this.helpButton) {
				this.openHelpGUI();
			}
		}
	}

	@Override
	public final void keyReleased(KeyEvent e) {
	}

	@Override
	public final void keyTyped(KeyEvent e) {
	}

	@Override
	public final void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		this.ruleLabel.setText(this.parameterModel.getRuleString());
		this.explanationArea.setText(this.parameterModel.getExplanation(this.targetType));
		this.explanationArea.setCaretPosition(0);
	}

	@Override
	protected final void onConfirm() {
		this.close();
		this.resultConsumer.accept();
	}

	public final void applyParameter() throws IOException {
		try {
			String ruleString = this.parameterModel.getRuleString();
			ParameterModel.parseRuleString(this.parameterModel.getScriptContext(), ruleString);
			this.resultConsumer.accept(ruleString);
			this.close();
		} catch (ProcessingException processingException) {
			new ScrollableMessagePrompt(this, "Error", "Error while parsing the whole exclude parameter", processingException.getMessage(), false, true);
		}
	}
}
