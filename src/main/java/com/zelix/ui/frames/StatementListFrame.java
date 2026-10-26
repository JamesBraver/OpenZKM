package com.zelix.ui.frames;

import lombok.SneakyThrows;

import com.zelix.exceptions.LexicalException;
import com.zelix.exceptions.LookupException;
import com.zelix.exceptions.ParsingException;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.config.base.ConfigBase;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.ScriptParser;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.exclusion.SingleRenameFilterParameter;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.ui.GUI;
import com.zelix.ui.base.BaseFrame;
import com.zelix.ui.components.ClassList;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.cell.ListCellRenderer;
import com.zelix.ui.components.panels.BorderedPanel;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.collections.OrderedMap;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class StatementListFrame extends BaseFrame {
	public BorderedPanel mainPanel = new BorderedPanel(true);
	public GUI gui;
	public ZKMConsumer resultConsumer;
	public ScriptContext scriptContext;
	public int parameterCategory;
	public JButton addButton;
	public boolean skipCloseCallback;
	public DefaultListModel<ListCellRenderer> paramListModel;
	public JTextArea explanationArea;
	public JButton cancelButton;
	public JButton deleteButton;
	public int editIndex;
	public JButton okButton;
	public JButton helpButton;
	protected String previousSelection;
	public JButton modifyButton;
	public OrderedMap<String, ListCellRenderer> paramMap;
	public ClassList<ListCellRenderer> paramList;
	public boolean updatingSelection;

	public StatementListFrame(String title, GUI gui, ScriptContext scriptContext, ZKMConsumer resultConsumer, int parameterCategory) {
		this.gui = gui;
		this.resultConsumer = resultConsumer;
		this.scriptContext = scriptContext;
		this.parameterCategory = parameterCategory;
	}

	public final void setWaitCursor() {
		Cursor waitCursor = new Cursor(3);
		this.setCursor(waitCursor);
		this.explanationArea.setCursor(waitCursor);
	}

	public abstract ConfigBase getConfig();

	public void applyToConfig() {
		ArrayList<String> ruleStrings = new ArrayList<>();
		Enumeration<ListCellRenderer> cellRendererEnumeration = this.paramListModel.elements();

		while (cellRendererEnumeration.hasMoreElements()) {
			ListCellRenderer cellRenderer = cellRendererEnumeration.nextElement();
			if (!cellRenderer.isDisabled()) {
				ruleStrings.add(((String)cellRenderer.getValue()).toString());
			}
		}

		this.getConfig().setParameters(ruleStrings);
	}

	public final void onCancel() {
		this.skipCloseCallback = true;
		this.close();
		this.resultConsumer.accept();
	}

	public abstract void modifyParameter(ParameterModel parameterModel, String ruleString, ZKMConsumer consumer);

	public final void updateEditedParameter(String ruleString) {
		ListCellRenderer cellRenderer = new ListCellRenderer(ruleString, false);
		this.paramListModel.setElementAt(cellRenderer, this.editIndex);
		if (this.paramMap.containsKey(ruleString) && this.paramMap.indexOfKey(ruleString) != this.editIndex) {
			this.updatingSelection = true;
			this.paramListModel.remove(this.editIndex);
			this.updatingSelection = false;
			this.paramMap.removeAt(this.editIndex);
			this.paramList.setSelectedIndex(this.paramListModel.indexOf(cellRenderer));
		} else {
			this.paramMap.setEntryAt(this.editIndex, ruleString, cellRenderer);
			String explanation = this.parseParameter(ruleString).getExplanation(this.parameterCategory);
			this.explanationArea.setText(explanation);
			this.explanationArea.setCaretPosition(0);
		}
	}

	public abstract String[] getLayoutProperties();

	public abstract void initScreenContent(Container container);

	public void onParametersChanged() {
	}

	private void removeParameterAt(int index) {
		if (index > -1) {
			this.updatingSelection = true;
			this.paramListModel.remove(index);
			this.updatingSelection = false;
			this.paramMap.removeAt(index);
			int paramCount = this.paramListModel.getSize();
			if (index < paramCount) {
				this.paramList.setSelectedIndex(index);
			} else if (paramCount > 0) {
				this.paramList.setSelectedIndex(paramCount - 1);
			}
		}

		int selectedIndex = this.paramList.getSelectedIndex();
		if (selectedIndex == -1) {
			this.modifyButton.setEnabled(false);
			this.deleteButton.setEnabled(false);
			this.explanationArea.setText("");
		} else {
			if (this.paramList.getSelectedValue().isDisabled()) {
				this.modifyButton.setEnabled(false);
				this.deleteButton.setEnabled(false);
			} else {
				this.modifyButton.setEnabled(true);
				this.deleteButton.setEnabled(true);
			}

			String ruleString = (String)this.paramListModel.getElementAt(selectedIndex).getValue();
			String explanation = this.parseParameter(ruleString).getExplanation(this.parameterCategory);
			this.explanationArea.setText(explanation);
			this.explanationArea.setCaretPosition(0);
		}

		this.onParametersChanged();
	}

	public final void onAdd() {
		StatementListFrame.AddParameterListener addParameterListener = new StatementListFrame.AddParameterListener(this);
		this.showParameterTypeDialog(addParameterListener);
	}

	public final void onOk() {
		if (this.validateParameters()) {
			this.skipCloseCallback = true;
			this.close();
			this.applyToConfig();
			this.resultConsumer.accept(1);
		}
	}

	public final void addParameter(String ruleString) {
		this.addParameter(ruleString, false);
	}

	public final void initFrame(String title) {
		this.setTitle(title);
		Container contentPane = this.getContentPane();
		PowerLayout layout = new PowerLayout(contentPane);
		contentPane.setLayout(layout);
		StatementListFrame.WindowClosingListener windowListener = new StatementListFrame.WindowClosingListener(this);
		this.addWindowListener(windowListener);
		contentPane.add(this.mainPanel, "mainPnl");
		this.createComponents();
		this.initScreenContent(contentPane);
		this.addFrameButtons(layout, contentPane);
		this.initToolTips();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setIconImage(KlassMaster.getIcon(this));
		this.setSize(Math.min(600, screenSize.width), Math.min(500, screenSize.height));
		Dimension frameSize = this.getSize();
		Point guiLocation = this.gui.getLocationOnScreen();
		Dimension guiSize = this.gui.getSize();
		int x = guiSize.width / 2 - frameSize.width / 2 + guiLocation.x;
		int y = guiSize.height / 2 - frameSize.height / 2 + guiLocation.y;
		x = Math.max(0, x);
		y = Math.max(0, y);
		this.setLocation(x, y);
		this.gui.setEnabled(false);
		GuiUtils.setVisible(this, true);
	}

	public boolean validateParameters() {
		return true;
	}

	public final void addParameter(String ruleString, boolean disabled) {
		if (!this.paramMap.containsKey(ruleString)) {
			ListCellRenderer cellRenderer = new ListCellRenderer(ruleString, disabled);
			this.paramListModel.addElement(cellRenderer);
			this.paramMap.put(ruleString, cellRenderer);
			this.paramList.setSelectedIndex(this.paramListModel.indexOf(cellRenderer));
			if (this.paramList.getSelectedIndex() == -1) {
				this.modifyButton.setEnabled(false);
				this.deleteButton.setEnabled(false);
				this.explanationArea.setText("");
			} else {
				this.paramList.ensureIndexIsVisible(this.paramList.getSelectedIndex());
			}
		}
	}

	public abstract void showParameterDialog(Object targetType, String ruleString, ZKMConsumer consumer);

	public abstract void showHelp();

	public final void resetCursor() {
		Cursor defaultCursor = Cursor.getDefaultCursor();
		this.setCursor(defaultCursor);
		this.explanationArea.setCursor(defaultCursor);
	}

	@SneakyThrows
	public final ParameterModel parseParameter(String ruleString) {
		BufferedReader reader = new BufferedReader(new StringReader(ruleString + ";"));
		ScriptParser scriptParser = new ScriptParser(reader);

		AbstractScriptNode parsedNode;
		try {
			parsedNode = scriptParser.parseSingleRenameFilterParameter();
			parsedNode.parseArguments(null, this.scriptContext);
		} catch (ParsingException ignoredParsing) {
			throw new LookupException("Bad parameter '" + ruleString + "' (A)");
		} catch (LexicalException ignoredLexical) {
			throw new LookupException("Bad parameter '" + ruleString + "' (B)");
		}

		RenameFilterParameter filterParameter = ((SingleRenameFilterParameter)parsedNode).getFilterParameter();
		return new ParameterModel(filterParameter);
	}

	public final void onModify() {
		this.editIndex = this.paramList.getSelectedIndex();
		if (this.editIndex != -1) {
			String ruleString = (String)this.paramList.getSelectedValue().getValue();
			ParameterModel parameterModel = this.parseParameter(ruleString);
			StatementListFrame.ModifyParameterListener modifyParameterListener = new StatementListFrame.ModifyParameterListener(this);
			this.modifyParameter(parameterModel, ruleString, modifyParameterListener);
		}
	}

	@Override
	public final Action getAction() {
		return new StatementListFrame.EscapeAction(this);
	}

	public final void createComponents() {
		PowerLayout layout = new PowerLayout(this.mainPanel);
		this.mainPanel.setLayout(layout);
		this.addButton = new JButton("Add");
		this.modifyButton = new JButton("Modify");
		this.deleteButton = new JButton("Delete");
		this.paramListModel = new DefaultListModel<>();
		this.paramList = new ClassList<>(this.paramListModel);
		this.paramList.setSelectionMode(0);
		this.paramList.setFont(this.getFont());
		this.paramMap = new OrderedMap<>();
		this.explanationArea = new JTextArea();
		this.explanationArea.setEditable(false);
		this.mainPanel.add(this.addButton, "mainAddBtn");
		this.mainPanel.add(this.modifyButton, "mainModifyBtn");
		this.mainPanel.add(this.deleteButton, "mainDeleteBtn");
		this.mainPanel.add(new PlatformAdaptiveScrollPanel(this.paramList), "mainParamList");
		this.mainPanel.add(new PlatformAdaptiveScrollPanel(this.explanationArea), "explanationArea");
		layout.setProperties(this.getLayoutProperties());
		this.modifyButton.setEnabled(false);
		this.deleteButton.setEnabled(false);
		StatementListFrame.ParameterSelectionListener parameterSelectionListener = new StatementListFrame.ParameterSelectionListener(this);
		this.paramList.addListSelectionListener(parameterSelectionListener);
		StatementListFrame.ButtonActionListener buttonActionListener = new StatementListFrame.ButtonActionListener(this);
		this.addButton.addActionListener(buttonActionListener);
		this.modifyButton.addActionListener(buttonActionListener);
		this.deleteButton.addActionListener(buttonActionListener);
		this.paramList.addMouseListener(new StatementListFrame.DoubleClickModifyListener(this));
		StatementListFrame.ButtonKeyListener buttonKeyListener = new StatementListFrame.ButtonKeyListener(this);
		this.addButton.addKeyListener(buttonKeyListener);
		this.modifyButton.addKeyListener(buttonKeyListener);
		this.deleteButton.addKeyListener(buttonKeyListener);
		this.paramList.addKeyListener(buttonKeyListener);
	}

	public abstract void initToolTips();

	@Override
	public final void close() {
		super.close();
		if (!this.skipCloseCallback) {
			this.resultConsumer.accept();
		}
	}

	public abstract void showParameterTypeDialog(ZKMConsumer consumer);

	public final void onDelete() {
		int selectedIndex = this.paramList.getSelectedIndex();
		this.removeParameterAt(selectedIndex);
	}

	public abstract void addFrameButtons(PowerLayout layout, Container container);

	public final void removeParameter(String ruleString) {
		this.removeParameterAt(this.paramMap.indexOfKey(ruleString));
	}

	public static class WindowClosingListener extends WindowAdapter {
		public final StatementListFrame statementListFrame;

		public WindowClosingListener(StatementListFrame statementListFrame) {
			this.statementListFrame = statementListFrame;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			this.statementListFrame.close();
			this.statementListFrame.resultConsumer.accept();
		}
	}

	public static class ButtonActionListener implements ActionListener {
		public final StatementListFrame statementListFrame;

		public ButtonActionListener(StatementListFrame statementListFrame) {
			this.statementListFrame = statementListFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object eventSource = e.getSource();
			if (eventSource == this.statementListFrame.addButton) {
				this.statementListFrame.onAdd();
			} else if (eventSource == this.statementListFrame.modifyButton) {
				this.statementListFrame.onModify();
			} else if (eventSource == this.statementListFrame.deleteButton) {
				this.statementListFrame.onDelete();
			}
		}
	}

	public static class ButtonKeyListener extends KeyAdapter {
		public final StatementListFrame statementListFrame;

		public ButtonKeyListener(StatementListFrame statementListFrame) {
			this.statementListFrame = statementListFrame;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			Object eventSource = e.getSource();
			if (e.getKeyCode() == 10) {
				if (eventSource == this.statementListFrame.addButton) {
					this.statementListFrame.onAdd();
				} else if (eventSource == this.statementListFrame.modifyButton) {
					this.statementListFrame.onModify();
				} else if (eventSource == this.statementListFrame.paramList) {
					if (!this.statementListFrame.paramList.getSelectedValue().isDisabled()) {
						this.statementListFrame.onModify();
					}
				} else if (eventSource == this.statementListFrame.deleteButton) {
					this.statementListFrame.onDelete();
				}
			} else if (e.getKeyCode() == 127) {
				if (eventSource == this.statementListFrame.paramList) {
					if (!this.statementListFrame.paramList.getSelectedValue().isDisabled()) {
						this.statementListFrame.onDelete();
					}
				} else if (eventSource == this.statementListFrame.deleteButton) {
					this.statementListFrame.onDelete();
				}
			}
		}
	}

	public static class EscapeAction extends AbstractAction {
		public final StatementListFrame statementListFrame;

		public EscapeAction(StatementListFrame statementListFrame) {
			this.statementListFrame = statementListFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (this.isEnabled()) {
				this.statementListFrame.onCancel();
			}
		}
	}

	public static class DoubleClickModifyListener extends MouseAdapter {
		public final StatementListFrame statementListFrame;

		public DoubleClickModifyListener(StatementListFrame statementListFrame) {
			this.statementListFrame = statementListFrame;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				int clickedIndex = this.statementListFrame.paramList.locationToIndex(e.getPoint());
				if (clickedIndex > -1 && !this.statementListFrame.paramListModel.getElementAt(clickedIndex).isDisabled()) {
					this.statementListFrame.onModify();
				}
			}
		}
	}

	public static class AddParameterListener extends OptionalConsumer {
		public final StatementListFrame statementListFrame;

		public AddParameterListener(StatementListFrame statementListFrame) {
			this.statementListFrame = statementListFrame;
		}

		public void acceptParameterType(String typeCode) {
			AddParameterConsumer addParameterConsumer = new AddParameterConsumer(this);
			this.statementListFrame.showParameterDialog(typeCode, null, addParameterConsumer);
		}

		@Override
		public void accept(Object arg) {
			this.acceptParameterType((String)arg);
		}

		@Override
		public void accept() {
		}

		public static class AddParameterConsumer extends OptionalConsumer {
			public final StatementListFrame.AddParameterListener addParameterListener;

			public AddParameterConsumer(StatementListFrame.AddParameterListener addParameterListener) {
				this.addParameterListener = addParameterListener;
			}

			public void addParameter(String ruleString) {
				this.addParameterListener.statementListFrame.addParameter(ruleString);
				this.addParameterListener.statementListFrame.onParametersChanged();
				this.addParameterListener.statementListFrame.paramList.ensureIndexIsVisible(this.addParameterListener.statementListFrame.paramListModel.getSize() - 1);
				this.addParameterListener.statementListFrame.toFront();
			}

			@Override
			public void accept(Object arg) {
				this.addParameter((String)arg);
			}

			@Override
			public void accept() {
				this.addParameterListener.statementListFrame.toFront();
			}
		}
	}

	public static class ModifyParameterListener extends OptionalConsumer {
		public final StatementListFrame statementListFrame;

		public ModifyParameterListener(StatementListFrame statementListFrame) {
			this.statementListFrame = statementListFrame;
		}

		public void acceptParameter(String parameter) {
			this.statementListFrame.updateEditedParameter(parameter);
			this.statementListFrame.onParametersChanged();
			this.statementListFrame.toFront();
		}

		@Override
		public void accept(Object arg) {
			this.acceptParameter((String)arg);
		}

		@Override
		public void accept() {
			this.statementListFrame.toFront();
		}
	}

	public static class ParameterSelectionListener implements ListSelectionListener {
		public final StatementListFrame statementListFrame;

		public ParameterSelectionListener(StatementListFrame statementListFrame) {
			this.statementListFrame = statementListFrame;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (!this.statementListFrame.updatingSelection && this.statementListFrame.paramList.getSelectedIndex() > -1) {
				ListCellRenderer cellRenderer = this.statementListFrame.paramList.getSelectedValue();
				if (!cellRenderer.isDisabled()) {
					this.statementListFrame.modifyButton.setEnabled(true);
					this.statementListFrame.deleteButton.setEnabled(true);
				} else {
					this.statementListFrame.modifyButton.setEnabled(false);
					this.statementListFrame.deleteButton.setEnabled(false);
				}

				String ruleString = (String)cellRenderer.getValue();
				String explanation = this.statementListFrame.parseParameter(ruleString).getExplanation(this.statementListFrame.parameterCategory);
				this.statementListFrame.explanationArea.setText(explanation);
				this.statementListFrame.explanationArea.setCaretPosition(0);
			}
		}
	}
}
