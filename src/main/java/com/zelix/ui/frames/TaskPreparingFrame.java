package com.zelix.ui.frames;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.klassmaster.config.FilterBaseConfig;
import com.zelix.klassmaster.config.base.ConfigBase;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.ui.GUI;
import com.zelix.ui.components.cell.AbstractListCellRenderer;
import com.zelix.ui.components.cell.ListCellRenderer;
import com.zelix.ui.components.panels.BeveledPanel;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.utils.collections.OrderedMap;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

public abstract class TaskPreparingFrame extends StatementListFrame implements ItemListener {
	public static String mainMethodPattern = "public static main(java.lang.String[])";
	public FilterBaseConfig filterConfig;
	public List<ClassNodeEntry> mainClassEntries;
	public boolean ignoreMainClassEvent;
	public OrderedMap<String, ParameterModel> frameworkParams;
	public boolean ignoreApplicationTypeEvent;
	public DefaultComboBoxModel<Object> mainClassModel;
	public OrderedMap<String, ParameterModel> midletParams;
	public OrderedMap<String, ParameterModel> libraryParams;
	public OrderedMap<String, ParameterModel> selfContainedParams;
	public DefaultComboBoxModel<String> applicationTypeModel;
	public JComboBox<String> applicationTypeComboBox;
	public JComboBox<Object> mainClassComboBox;

	public TaskPreparingFrame(String title, GUI gui, List<ClassNodeEntry> mainClassEntries, FilterBaseConfig filterConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer, int parameterCategory) {
		super(title, gui, scriptContext, resultConsumer, parameterCategory);
		this.filterConfig = filterConfig;
		this.mainClassEntries = mainClassEntries;
	}

	@Override
	public ConfigBase getConfig() {
		return this.filterConfig;
	}

	@Override
	public void applyToConfig() {
		ArrayList<String> ruleStrings = new ArrayList<>();
		Enumeration<ListCellRenderer> cellRendererEnumeration = super.paramListModel.elements();

		while (cellRendererEnumeration.hasMoreElements()) {
			ListCellRenderer cellRenderer = cellRendererEnumeration.nextElement();
			if (!cellRenderer.isDisabled()) {
				ruleStrings.add(((String)cellRenderer.getValue()).toString());
			}
		}

		this.filterConfig.setParameters(ruleStrings);
		switch (this.mainClassComboBox.getSelectedIndex()) {
			case -1:
			case 0:
				this.filterConfig.setExcludeMainClass(false, null, false);
				break;
			default:
				ClassNodeEntry selectedMainClassEntry = (ClassNodeEntry)this.mainClassComboBox.getSelectedItem();
				this.filterConfig.setExcludeMainClass(true, selectedMainClassEntry.getBinaryName(), selectedMainClassEntry.isMainClass());
		}
	}

	public abstract String[] getExtraLayoutProperties();

	public abstract String getEntryRuleString(ClassNodeEntry entry);

	public final void addMainClassSelector(Container container, String labelText, String toolTipText) {
		JLabel label = new JLabel(labelText);
		container.add(label, "excludeMainClassLbl");
		this.mainClassModel = new DefaultComboBoxModel<>();
		this.mainClassComboBox = new JComboBox<>(this.mainClassModel);
		container.add(this.mainClassComboBox, "mainClassComboBox");
		this.mainClassComboBox.addItem("<None>");
		if (this.mainClassEntries != null && this.mainClassEntries.size() != 0) {
			for (int i = 0; i < this.mainClassEntries.size(); i++) {
				this.mainClassComboBox.addItem(this.mainClassEntries.get(i));
			}
		} else {
			this.mainClassComboBox.setEnabled(false);
		}

		if (this.filterConfig.isExcludeMainClass() && this.filterConfig.isPersisted()) {
			String mainClassName = this.filterConfig.getMainClassName();
			int mainClassIndex = this.findMainClassIndex(mainClassName);
			if (mainClassIndex > -1) {
				this.mainClassComboBox.setSelectedIndex(mainClassIndex);
			} else {
				this.removeParameter(this.getRuleString(mainClassName, this.filterConfig.isExcludeMainMethod()));
			}
		}

		this.mainClassComboBox.addItemListener(this);
		this.mainClassComboBox.setToolTipText(toolTipText);
	}

	@Override
	@SneakyThrows
	public final void initScreenContent(Container container) {
		this.loadApplicationTypeParams();
		BeveledPanel extraPanel = new BeveledPanel(false, true);
		PowerLayout extraLayout = new PowerLayout(extraPanel);
		extraPanel.setLayout(extraLayout);
		extraPanel.add(new JLabel("Application type:"), "applicationTypeLbl");
		this.applicationTypeModel = new DefaultComboBoxModel<>();
		this.applicationTypeComboBox = new JComboBox<>(this.applicationTypeModel);
		this.applicationTypeComboBox.addItem("Non-extensible class library");
		this.applicationTypeComboBox.addItem("Extensible framework");
		this.applicationTypeComboBox.addItem("J2ME MIDlet");
		this.applicationTypeComboBox.addItem("Self contained application or applet");
		extraPanel.add(this.applicationTypeComboBox, "applicationTypeComboBox");
		this.applicationTypeComboBox.setToolTipText(HelpManager.getToolTip("EXCLUSION_APPLICATION_TYPE"));
		Enumeration<String> ruleStringEnumeration = this.selfContainedParams.keysEnumeration();

		while (ruleStringEnumeration.hasMoreElements()) {
			String selfContainedRuleString = ruleStringEnumeration.nextElement();

			try {
				if (this.parseParameter(selfContainedRuleString).getRuleString().equals(selfContainedRuleString)) {
					this.addParameter(selfContainedRuleString, true);
				}
			} catch (DebugAssertFailedException debugAssertFailedDefault) {
				throw debugAssertFailedDefault;
			} catch (Throwable ignoredDefault) {
			}
		}

		if (this.filterConfig.isPersisted()) {
			ruleStringEnumeration = this.filterConfig.getParameterEnumeration();

			while (ruleStringEnumeration.hasMoreElements()) {
				String configRuleString = ruleStringEnumeration.nextElement();

				try {
					if (this.parseParameter(configRuleString).getRuleString().equals(configRuleString)) {
						this.addParameter(configRuleString);
					}
				} catch (DebugAssertFailedException debugAssertFailedConfig) {
					throw debugAssertFailedConfig;
				} catch (Throwable ignoredConfig) {
				}
			}
		}

		this.syncApplicationType();
		this.applicationTypeComboBox.addItemListener(this);
		this.addExtraComponents(extraLayout, extraPanel);
		extraLayout.setProperties(this.getExtraLayoutProperties());
		container.add(extraPanel, "extraPnl");
	}

	private void addParams(OrderedMap<String, ParameterModel> params) {
		for (int paramIndex = 0; paramIndex < params.size(); paramIndex++) {
			String paramName = params.getKeyAt(paramIndex);
			if (!super.paramMap.containsKey(paramName)) {
				try {
					if (this.parseParameter(paramName).getRuleString().equals(paramName)) {
						ListCellRenderer cellRenderer = new ListCellRenderer(paramName, false);
						super.paramMap.put(paramName, cellRenderer);
						super.paramListModel.addElement(cellRenderer);
					}
				} catch (DebugAssertFailedException debugAssertFailedException) {
					throw debugAssertFailedException;
				} catch (Throwable ignored) {
				}
			}
		}

		if (super.paramList.getSelectedIndex() == -1 && super.previousSelection != null && super.paramMap.containsKey(super.previousSelection)) {
			super.paramList.setSelectedIndex(super.paramMap.indexOfKey(super.previousSelection));
		}

		super.previousSelection = null;
		if (super.paramListModel.size() > 0) {
			super.paramList.ensureIndexIsVisible(super.paramListModel.size() - 1);
		}
	}

	private void loadApplicationTypeParams() throws IOException {
		this.selfContainedParams = new OrderedMap<>();
		ExclusionInclusionStatement exclusionStatement;
		if (super.parameterCategory == 1) {
			exclusionStatement = DefaultExcludes.loadDefaultExclusions(super.scriptContext);
		} else {
			exclusionStatement = TrimExcludes.loadDefaultExclusions(super.scriptContext);
		}

		if (exclusionStatement != null) {
			Enumeration<RenameFilterParameter> selfContainedParamEnumeration = exclusionStatement.getRenameFilterParameters();

			while (selfContainedParamEnumeration.hasMoreElements()) {
				RenameFilterParameter selfContainedParam = selfContainedParamEnumeration.nextElement();
				ParameterModel selfContainedParamModel = new ParameterModel(selfContainedParam);
				this.selfContainedParams.put(selfContainedParam.toString(), selfContainedParamModel);
			}
		}

		this.libraryParams = new OrderedMap<>();
		if (super.parameterCategory == 1) {
			exclusionStatement = DefaultExcludes.getClasslibExclusions(super.scriptContext);
		} else {
			exclusionStatement = TrimExcludes.getClasslibExclusions(super.scriptContext);
		}

		if (exclusionStatement != null) {
			Enumeration<RenameFilterParameter> libraryParamEnumeration = exclusionStatement.getRenameFilterParameters();

			while (libraryParamEnumeration.hasMoreElements()) {
				RenameFilterParameter libraryParam = libraryParamEnumeration.nextElement();
				ParameterModel libraryParamModel = new ParameterModel(libraryParam);
				this.libraryParams.put(libraryParam.toString(), libraryParamModel);
			}
		}

		this.frameworkParams = new OrderedMap<>();
		if (super.parameterCategory == 1) {
			exclusionStatement = DefaultExcludes.getFrameworkExclusions(super.scriptContext);
		} else {
			exclusionStatement = TrimExcludes.getFrameworkExclusions(super.scriptContext);
		}

		if (exclusionStatement != null) {
			Enumeration<RenameFilterParameter> frameworkParamEnumeration = exclusionStatement.getRenameFilterParameters();

			while (frameworkParamEnumeration.hasMoreElements()) {
				RenameFilterParameter frameworkParam = frameworkParamEnumeration.nextElement();
				ParameterModel frameworkParamModel = new ParameterModel(frameworkParam);
				this.frameworkParams.put(frameworkParam.toString(), frameworkParamModel);
			}
		}

		this.midletParams = new OrderedMap<>();
		if (super.parameterCategory == 1) {
			exclusionStatement = DefaultExcludes.getMidletExclusions(super.scriptContext);
		} else {
			exclusionStatement = TrimExcludes.getMidletExclusions(super.scriptContext);
		}

		if (exclusionStatement != null) {
			Enumeration<RenameFilterParameter> midletParamEnumeration = exclusionStatement.getRenameFilterParameters();

			while (midletParamEnumeration.hasMoreElements()) {
				RenameFilterParameter midletParam = midletParamEnumeration.nextElement();
				ParameterModel midletParamModel = new ParameterModel(midletParam);
				this.midletParams.put(midletParam.toString(), midletParamModel);
			}
		}
	}

	@Override
	public void onParametersChanged() {
		this.syncApplicationType();
		this.validateMainClassSelection();
	}

	public void syncApplicationType() {
		int paramCount = super.paramListModel.getSize();
		boolean allParamsPresent;
		if (this.frameworkParams.size() <= paramCount) {
			allParamsPresent = true;

			for (int frameworkParamIndex = 0; frameworkParamIndex < this.frameworkParams.size(); frameworkParamIndex++) {
				if (!super.paramMap.containsKey(this.frameworkParams.getKeyAt(frameworkParamIndex))) {
					allParamsPresent = false;
					break;
				}
			}
		} else {
			allParamsPresent = false;
		}

		if (allParamsPresent) {
			this.ignoreApplicationTypeEvent = true;
			this.applicationTypeComboBox.setSelectedIndex(1);
			this.filterConfig.setApplicationType(1);
			this.ignoreApplicationTypeEvent = false;
		} else {
			if (this.libraryParams.size() <= paramCount) {
				allParamsPresent = true;

				for (int libraryParamIndex = 0; libraryParamIndex < this.libraryParams.size(); libraryParamIndex++) {
					if (!super.paramMap.containsKey(this.libraryParams.getKeyAt(libraryParamIndex))) {
						allParamsPresent = false;
						break;
					}
				}
			} else {
				allParamsPresent = false;
			}

			if (allParamsPresent) {
				this.ignoreApplicationTypeEvent = true;
				this.applicationTypeComboBox.setSelectedIndex(0);
				this.filterConfig.setApplicationType(0);
				this.ignoreApplicationTypeEvent = false;
			} else {
				if (this.midletParams.size() <= paramCount) {
					allParamsPresent = true;

					for (int midletParamIndex = 0; midletParamIndex < this.midletParams.size(); midletParamIndex++) {
						if (!super.paramMap.containsKey(this.midletParams.getKeyAt(midletParamIndex))) {
							allParamsPresent = false;
							break;
						}
					}
				} else {
					allParamsPresent = false;
				}

				if (allParamsPresent) {
					this.ignoreApplicationTypeEvent = true;
					this.applicationTypeComboBox.setSelectedIndex(2);
					this.filterConfig.setApplicationType(2);
					this.ignoreApplicationTypeEvent = false;
				} else {
					this.ignoreApplicationTypeEvent = true;
					this.applicationTypeComboBox.setSelectedIndex(3);
					this.filterConfig.setApplicationType(3);
					this.ignoreApplicationTypeEvent = false;
				}
			}
		}
	}

	private void removeParams(OrderedMap<String, ParameterModel> params) {
		if (!super.paramList.isSelectionEmpty()) {
			super.previousSelection = super.paramList.getSelectedValue().toString();
		}

		for (int paramIndex = 0; paramIndex < params.size(); paramIndex++) {
			String paramName = params.getKeyAt(paramIndex);
			if (super.paramMap.containsKey(paramName)) {
				AbstractListCellRenderer cellRenderer = super.paramMap.remove(paramName);
				super.updatingSelection = true;
				super.paramListModel.removeElement(cellRenderer);
				super.updatingSelection = false;
			}
		}

		if (super.paramList.getSelectedIndex() == -1) {
			super.modifyButton.setEnabled(false);
			super.deleteButton.setEnabled(false);
			super.explanationArea.setText("");
		}
	}

	@Override
	public boolean validateParameters() {
		return true;
	}

	public abstract String getRuleString(String binaryName, boolean hasMainMethod);

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == this.applicationTypeComboBox) {
			if (this.ignoreApplicationTypeEvent) {
				return;
			}

			int applicationTypeIndex = this.applicationTypeModel.getIndexOf(e.getItem());
			if (e.getStateChange() == 1) {
				this.filterConfig.setApplicationType(applicationTypeIndex);
			}

			switch (applicationTypeIndex) {
				case 0:
					if (e.getStateChange() == 1) {
						this.addParams(this.libraryParams);
						this.syncApplicationType();
					} else {
						this.removeParams(this.libraryParams);
					}
					break;
				case 1:
					if (e.getStateChange() == 1) {
						this.addParams(this.frameworkParams);
						this.syncApplicationType();
					} else {
						this.removeParams(this.frameworkParams);
					}
					break;
				case 2:
					if (e.getStateChange() == 1) {
						this.addParams(this.midletParams);
						this.syncApplicationType();
					} else {
						this.removeParams(this.midletParams);
					}
				case 3:
			}
		} else if (e.getSource() == this.mainClassComboBox) {
			if (this.ignoreMainClassEvent) {
				return;
			}

			int mainClassIndex = this.mainClassModel.getIndexOf(e.getItem());
			switch (mainClassIndex) {
				case -1:
				case 0:
					break;
				default:
					String ruleString = this.getEntryRuleString((ClassNodeEntry)this.mainClassModel.getElementAt(mainClassIndex));
					if (e.getStateChange() == 1) {
						this.addParameter(ruleString);
					} else {
						this.ignoreMainClassEvent = true;
						this.removeParameter(ruleString);
						this.ignoreMainClassEvent = false;
					}
			}
		}
	}

	public int findMainClassIndex(String binaryName) {
		int itemCount = this.mainClassModel.getSize();

		for (int i = 1; i < itemCount; i++) {
			if (((ClassNodeEntry)this.mainClassModel.getElementAt(i)).getClassNode().getBinaryName().equals(binaryName)) {
				return i;
			}
		}

		return -1;
	}

	public final void validateMainClassSelection() {
		if (!this.ignoreMainClassEvent && this.mainClassComboBox != null) {
			switch (this.mainClassComboBox.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				default:
					if (!super.paramMap.containsKey(this.getEntryRuleString((ClassNodeEntry)this.mainClassComboBox.getSelectedItem()))) {
						this.mainClassComboBox.setSelectedIndex(0);
					}
			}
		}
	}

	@Override
	public void initToolTips() {
		super.addButton.setToolTipText(HelpManager.getToolTip("ADD_EXCLUSION_PARAM"));
		super.modifyButton.setToolTipText(HelpManager.getToolTip("MODIFY_EXCLUSION_PARAM"));
		super.deleteButton.setToolTipText(HelpManager.getToolTip("DELETE_EXCLUSION_PARAM"));
		super.paramList.setToolTipText(HelpManager.getToolTip("CURRENT_EXCLUSION_PARAMS"));
		super.explanationArea.setToolTipText(HelpManager.getToolTip("EXPLAIN_SELECTED_EXCLUSION_PARAM"));
	}

	public abstract void addExtraComponents(PowerLayout layout, Container container);
}
