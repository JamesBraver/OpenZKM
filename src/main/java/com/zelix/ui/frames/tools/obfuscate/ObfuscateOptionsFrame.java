package com.zelix.ui.frames.tools.obfuscate;

import com.zelix.HelpManager;
import com.zelix.enums.MixCaseType;
import com.zelix.klassmaster.config.FilterConfig;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.frames.base.BaseChangeLogInputFrame;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.PackageManager;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class ObfuscateOptionsFrame extends BaseChangeLogInputFrame {
	private static String layoutProperties = "container.defaultBottomPad=10;column2X=max(changeLogInChk.defaultWidth,changeLogOutChk.defaultWidth,obfuscateFlowLbl.defaultWidth,encryptStringsLbl.defaultWidth,collapsePackagesChk.defaultWidth,aggressiveMethodRenamingChk.defaultWidth,randomizeChk.defaultWidth,keepInnerClassesLbl.defaultWidth,keepGenericsLbl.defaultWidth,removeLineNumbersLbl.defaultWidth)+12;logFileTextWidth=max(logFileInTxt.defaultWidth, logFileOutTxt.defaultWidth);changeLogInChk.centerY=logFileInTxt.centerY;changeLogInChk.left=10;logFileInTxt.top=10;logFileInTxt.left=column2X;logFileInTxt.right=container.right-changeLogInBrowseBtn.defaultWidth-10;logFileInTxt.minWidth=logFileTextWidth;changeLogInBrowseBtn.centerY=logFileInTxt.centerY;changeLogInBrowseBtn.left=logFileInTxt.right+5;changeLogInBrowseBtn.right=container.right-10;changeLogOutChk.centerY=logFileOutTxt.centerY;changeLogOutChk.left=10;logFileOutTxt.top=logFileInTxt.bottom+10;logFileOutTxt.left=column2X;logFileOutTxt.right=logFileInTxt.right;logFileOutTxt.minWidth=logFileTextWidth;changeLogOutBrowseBtn.centerY=logFileOutTxt.centerY;changeLogOutBrowseBtn.left=changeLogInBrowseBtn.left;changeLogOutBrowseBtn.right=container.right-10;obfuscateFlowLbl.centerY=obfuscateFlowComboBox.centerY;obfuscateFlowLbl.left=10;obfuscateFlowComboBox.top=logFileOutTxt.bottom+10;obfuscateFlowComboBox.left=column2X;obfuscateFlowComboBox.right=container.right-10;exceptionObfuscationLbl.centerY=exceptionObfuscationComboBox.centerY;exceptionObfuscationLbl.left=10;exceptionObfuscationComboBox.top=obfuscateFlowComboBox.bottom+10;exceptionObfuscationComboBox.left=column2X;exceptionObfuscationComboBox.right=container.right-10;encryptStringsLbl.centerY=encryptStringsComboBox.centerY;encryptStringsLbl.left=10;encryptStringsComboBox.top=exceptionObfuscationComboBox.bottom+10;encryptStringsComboBox.left=column2X;encryptStringsComboBox.right=container.right-10;encryptIntegersLbl.centerY=encryptIntegersComboBox.centerY;encryptIntegersLbl.left=10;encryptIntegersComboBox.top=encryptStringsComboBox.bottom+10;encryptIntegersComboBox.left=column2X;encryptIntegersComboBox.right=container.right-10;encryptLongsLbl.centerY=encryptLongsComboBox.centerY;encryptLongsLbl.left=10;encryptLongsComboBox.top=encryptIntegersComboBox.bottom+10;encryptLongsComboBox.left=column2X;encryptLongsComboBox.right=container.right-10;mixedCaseLbl.centerY=mixedCaseComboBox.centerY;mixedCaseLbl.left=10;mixedCaseComboBox.top=encryptLongsComboBox.bottom+10;mixedCaseComboBox.left=column2X;mixedCaseComboBox.right=container.right-10;defaultCollapsePackageTxt.top=mixedCaseComboBox.bottom+10;collapsePackagesChk.centerY=defaultCollapsePackageTxt.centerY;collapsePackagesChk.left=10;defaultCollapsePackageTxt.left=column2X;defaultCollapsePackageTxt.right=container.right-10;aggressiveMethodRenamingChk.top=defaultCollapsePackageTxt.bottom+10;aggressiveMethodRenamingChk.left=10;randomizeChk.top=aggressiveMethodRenamingChk.bottom+10;randomizeChk.left=10;keepInnerClassesLbl.centerY=keepInnerClassesComboBox.centerY;keepInnerClassesLbl.left=10;keepInnerClassesComboBox.top=randomizeChk.bottom+10;keepInnerClassesComboBox.left=column2X;keepInnerClassesComboBox.right=container.right-10;keepGenericsLbl.centerY=keepGenericsComboBox.centerY;keepGenericsLbl.left=10;keepGenericsComboBox.top=keepInnerClassesComboBox.bottom+10;keepGenericsComboBox.left=column2X;keepGenericsComboBox.right=container.right-10;localVariablesLbl.centerY=localVariablesComboBox.centerY;localVariablesLbl.left=10;localVariablesComboBox.top=keepGenericsComboBox.bottom+10;localVariablesComboBox.left=column2X;localVariablesComboBox.right=container.right-10;removeLineNumbersLbl.centerY=removeLineNumbersComboBox.centerY;removeLineNumbersLbl.left=10;removeLineNumbersComboBox.top=localVariablesComboBox.bottom+10;removeLineNumbersComboBox.left=column2X;removeLineNumbersComboBox.right=container.right-10;obfuscateReferencesChk.top=removeLineNumbersLbl.bottom+10;obfuscateReferencesChk.left=10;autoReflectionChk.top=obfuscateReferencesChk.bottom+10;autoReflectionChk.left=10;methodParameterChangesLbl.centerY=methodParameterChangesComboBox.centerY;methodParameterChangesLbl.left=10;methodParameterChangesComboBox.top=autoReflectionChk.bottom+10;methodParameterChangesComboBox.left=column2X;methodParameterChangesComboBox.right=container.right-10;obfuscateMethodParametersChk.top=methodParameterChangesComboBox.bottom+10;obfuscateMethodParametersChk.left=10;keepBalancedLocksChk.top=obfuscateMethodParametersChk.bottom+10;keepBalancedLocksChk.left=10;preverifyChk.top=keepBalancedLocksChk.bottom+10;preverifyChk.left=10;";
	public JComboBox<String> lineNumberTables;
	public JCheckBox keepBalancedLocks;
	public DefaultComboBoxModel<String> encryptStringLiteralsModel;
	public JCheckBox aggressivelyRenameMethods;
	public DefaultComboBoxModel<String> exceptionObfuscationModel;
	public JComboBox<String> exceptionObfuscation;
	public JComboBox<String> mixedCaseClassNames;
	public JComboBox<String> encryptLongConstants;
	public JComboBox<String> encryptStringLiterals;
	public DefaultComboBoxModel<String> localVariableTablesModel;
	public JCheckBox preverify;
	public JComboBox<String> localVariableTables;
	public JComboBox<String> encryptIntegerConstants;
	public JComboBox<String> obfuscateControlFlow;
	public DefaultComboBoxModel<String> obfuscateControlFlowModel;
	public JTextField collapsePackagesText;
	public JComboBox<String> keepGenericsInformation;
	public JCheckBox randomizeObfuscation;
	public DefaultComboBoxModel<String> keepGenericsInformationModel;
	public DefaultComboBoxModel<String> encryptLongConstantsModel;
	public JCheckBox collapsePackagesCheckBox;
	public DefaultComboBoxModel<String> mixedCaseClassNamesModel;
	public DefaultComboBoxModel<String> encryptIntegerConstantsModel;
	public DefaultComboBoxModel<String> lineNumberTablesModel;

	public ObfuscateOptionsFrame(String title, String okButtonToolTip, GUI gui, ObfuscateConfig obfuscateConfig, FilterConfig filterConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer) {
		super(title, okButtonToolTip, gui, obfuscateConfig, filterConfig, scriptContext, resultConsumer);
	}

	@Override
	public void saveOptionsToConfig() {
		super.obfConfig.keepGenericsInformation = this.keepGenericsInformation.getSelectedIndex();
		super.obfConfig.lineNumbers = this.lineNumberTables.getSelectedIndex();
		super.obfConfig.exceptionObfuscation = this.exceptionObfuscation.getSelectedIndex();
		super.obfConfig.obfuscateControlFlow = this.obfuscateControlFlow.getSelectedIndex();
		super.obfConfig.encryptStringLiterals = this.encryptStringLiterals.getSelectedIndex();
		super.obfConfig.encryptIntegerConstants = this.encryptIntegerConstants.getSelectedIndex();
		super.obfConfig.encryptLongConstants = this.encryptLongConstants.getSelectedIndex();
		super.obfConfig.mixedCaseClassNames = MixCaseType.fromTypeID(this.mixedCaseClassNames.getSelectedIndex());
		super.obfConfig.localVariableTables = this.localVariableTables.getSelectedIndex();
		super.obfConfig.randomizeObfuscation = this.randomizeObfuscation.isSelected();
		super.obfConfig.keepBalancedLocks = this.keepBalancedLocks.isSelected();
		super.obfConfig.preverify = this.preverify.isSelected();
		super.obfConfig.collapsePackagesWithDefault = this.collapsePackagesCheckBox.isSelected();
		super.obfConfig.aggressiveMethodRenaming = this.aggressivelyRenameMethods.isSelected();
		super.obfConfig.collapseTargetPackage = this.collapsePackagesText.getText().replace('.', '/');
	}

	@Override
	public void loadOptionsFromConfig() {
		this.obfuscateControlFlow.setSelectedIndex(super.obfConfig.obfuscateControlFlow);
		this.encryptStringLiterals.setSelectedIndex(super.obfConfig.encryptStringLiterals);
		this.encryptIntegerConstants.setSelectedIndex(super.obfConfig.encryptIntegerConstants);
		this.encryptLongConstants.setSelectedIndex(super.obfConfig.encryptLongConstants);
		if (super.obfConfig.successReadConfigFromFile()) {
			if (super.filterConfig.getApplicationType() != 0 && super.filterConfig.getApplicationType() != 1) {
				this.aggressivelyRenameMethods.setSelected(super.obfConfig.aggressiveMethodRenaming);
			} else {
				this.aggressivelyRenameMethods.setSelected(false);
			}
		} else if (super.filterConfig.getApplicationType() != 0 && super.filterConfig.getApplicationType() != 1) {
			this.aggressivelyRenameMethods.setSelected(true);
		} else {
			this.aggressivelyRenameMethods.setSelected(false);
		}

		if (!super.obfConfig.successReadConfigFromFile()) {
			if (super.filterConfig.getApplicationType() != 2 && super.filterConfig.getApplicationType() != 3) {
				this.collapsePackagesCheckBox.setSelected(false);
			} else {
				this.collapsePackagesCheckBox.setSelected(true);
			}
		} else {
			this.collapsePackagesCheckBox.setSelected(super.obfConfig.collapsePackagesWithDefault);
		}

		if (super.obfConfig.collapseTargetPackage != null) {
			this.collapsePackagesText.setText(super.obfConfig.collapseTargetPackage.replace('/', '.'));
		} else {
			this.collapsePackagesText.setText("");
		}

		this.collapsePackagesText.setEnabled(this.collapsePackagesCheckBox.isSelected());
		this.randomizeObfuscation.setSelected(super.obfConfig.randomizeObfuscation);
		this.keepGenericsInformation.setSelectedIndex(super.obfConfig.keepGenericsInformation);
		this.mixedCaseClassNames.setSelectedIndex(super.obfConfig.mixedCaseClassNames.getType());
		this.localVariableTables.setSelectedIndex(super.obfConfig.localVariableTables);
		this.lineNumberTables.setSelectedIndex(super.obfConfig.lineNumbers);
		this.exceptionObfuscation.setSelectedIndex(super.obfConfig.exceptionObfuscation);
		this.preverify.setSelected(super.obfConfig.preverify);
		this.keepBalancedLocks.setSelected(super.obfConfig.keepBalancedLocks);
	}

	@Override
	public void buildLayout(StringBuffer layoutProperties) {
		JLabel obfuscateFlowLabel = new JLabel("Obfuscate control flow:");
		this.obfuscateControlFlowModel = new DefaultComboBoxModel<>();
		this.obfuscateControlFlow = new JComboBox<>(this.obfuscateControlFlowModel);
		this.obfuscateControlFlowModel.addElement("none");
		this.obfuscateControlFlowModel.addElement("light");
		this.obfuscateControlFlowModel.addElement("normal");
		this.obfuscateControlFlowModel.addElement("aggressive");
		JLabel encryptStringLiteralsLabel = new JLabel("Encrypt String literals:");
		this.encryptStringLiteralsModel = new DefaultComboBoxModel<>();
		this.encryptStringLiterals = new JComboBox<>(this.encryptStringLiteralsModel);
		this.encryptStringLiteralsModel.addElement("none");
		this.encryptStringLiteralsModel.addElement("normal");
		this.encryptStringLiteralsModel.addElement("aggressive");
		this.encryptStringLiteralsModel.addElement("flow obfuscate");
		this.encryptStringLiteralsModel.addElement("enhanced");
		JLabel encryptIntegerConstantsLabel = new JLabel("Encrypt Integer Constants:");
		this.encryptIntegerConstantsModel = new DefaultComboBoxModel<>();
		this.encryptIntegerConstants = new JComboBox<>(this.encryptIntegerConstantsModel);
		this.encryptIntegerConstantsModel.addElement("none");
		this.encryptIntegerConstantsModel.addElement("normal");
		this.encryptIntegerConstantsModel.addElement("aggressive");
		JLabel encryptLongConstantsLabel = new JLabel("Encrypt Long Constants:");
		this.encryptLongConstantsModel = new DefaultComboBoxModel<>();
		this.encryptLongConstants = new JComboBox<>(this.encryptLongConstantsModel);
		this.encryptLongConstantsModel.addElement("none");
		this.encryptLongConstantsModel.addElement("normal");
		JLabel mixedCaseClassNamesLabel = new JLabel("Mixed case class names:");
		this.mixedCaseClassNamesModel = new DefaultComboBoxModel<>();
		this.mixedCaseClassNames = new JComboBox<>(this.mixedCaseClassNamesModel);
		this.mixedCaseClassNamesModel.addElement("true");
		this.mixedCaseClassNamesModel.addElement("false");
		this.mixedCaseClassNamesModel.addElement("if only in archive");
		this.collapsePackagesCheckBox = new JCheckBox("collapse packages. Default name:", false);
		this.collapsePackagesText = new JTextField();
		this.aggressivelyRenameMethods = new JCheckBox("aggressively rename methods", false);
		this.randomizeObfuscation = new JCheckBox("randomize obfuscation", false);
		JLabel keepInnerClassInformationLabel = new JLabel("Keep inner class information:");
		super.keepInnerClassInformationModel = new DefaultComboBoxModel<>();
		super.keepInnerClassInformation = new JComboBox<>(super.keepInnerClassInformationModel);
		super.keepInnerClassInformationModel.addElement("true");
		super.keepInnerClassInformationModel.addElement("false");
		super.keepInnerClassInformationModel.addElement("if name not obfuscated");
		JLabel keepGenericsInformationLabel = new JLabel("Keep generics information:");
		this.keepGenericsInformationModel = new DefaultComboBoxModel<>();
		this.keepGenericsInformation = new JComboBox<>(this.keepGenericsInformationModel);
		this.keepGenericsInformationModel.addElement("true");
		this.keepGenericsInformationModel.addElement("false");
		JLabel localVariableTablesLabel = new JLabel("Local variable tables:");
		this.localVariableTablesModel = new DefaultComboBoxModel<>();
		this.localVariableTables = new JComboBox<>(this.localVariableTablesModel);
		this.localVariableTablesModel.addElement("delete");
		this.localVariableTablesModel.addElement("keep");
		this.localVariableTablesModel.addElement("obfuscate");
		this.localVariableTablesModel.addElement("keep visible method parameters");
		this.localVariableTablesModel.addElement("keep visible parameters if name not obfuscated");
		this.localVariableTablesModel.addElement("keep parameters if name not obfuscated");
		JLabel lineNumberTablesLabel = new JLabel("Line number tables:");
		this.lineNumberTablesModel = new DefaultComboBoxModel<>();
		this.lineNumberTables = new JComboBox<>(this.lineNumberTablesModel);
		this.lineNumberTablesModel.addElement("delete");
		this.lineNumberTablesModel.addElement("scramble");
		this.lineNumberTablesModel.addElement("keep");
		JLabel exceptionObfuscationLabel = new JLabel("Exception obfuscation:");
		this.exceptionObfuscationModel = new DefaultComboBoxModel<>();
		this.exceptionObfuscation = new JComboBox<>(this.exceptionObfuscationModel);
		this.exceptionObfuscationModel.addElement("none");
		this.exceptionObfuscationModel.addElement("light");
		this.exceptionObfuscationModel.addElement("heavy");
		super.obfuscateReferencesCheckBox = new JCheckBox("obfuscate references", true);
		JLabel methodParameterChangesLabel = new JLabel("Method parameter changes:");
		super.methodParameterChangesModel = new DefaultComboBoxModel<>();
		super.methodParameterChanges = new JComboBox<>(super.methodParameterChangesModel);
		super.methodParameterChangesModel.addElement("none");
		super.methodParameterChangesModel.addElement("normal");
		super.methodParameterChangesModel.addElement("random");
		super.methodParameterChangesModel.addElement("flowObfuscate");
		super.obfuscateMethodParameters = new JCheckBox("obfuscate method parameters", true);
		super.autoReflectionHandling = new JCheckBox("auto reflection handling", true);
		this.keepBalancedLocks = new JCheckBox("keep balanced locks", false);
		this.preverify = new JCheckBox("preverify", true);
		ObfuscateOptionsFrame.CollapsePackagesListener collapsePackagesListener = new ObfuscateOptionsFrame.CollapsePackagesListener(this);
		this.collapsePackagesCheckBox.addItemListener(collapsePackagesListener);
		super.mainPanel.add(super.useChangeLogFileCheckBox, "changeLogInChk");
		super.mainPanel.add(super.changeLogFileInField, "logFileInTxt");
		super.mainPanel.add(super.changeLogInBrowseButton, "changeLogInBrowseBtn");
		super.mainPanel.add(super.produceChangeLogFileCheckBox, "changeLogOutChk");
		super.mainPanel.add(super.changeLogFileOutField, "logFileOutTxt");
		super.mainPanel.add(super.changeLogOutBrowseButton, "changeLogOutBrowseBtn");
		super.mainPanel.add(obfuscateFlowLabel, "obfuscateFlowLbl");
		super.mainPanel.add(this.obfuscateControlFlow, "obfuscateFlowComboBox");
		super.mainPanel.add(exceptionObfuscationLabel, "exceptionObfuscationLbl");
		super.mainPanel.add(this.exceptionObfuscation, "exceptionObfuscationComboBox");
		super.mainPanel.add(encryptStringLiteralsLabel, "encryptStringsLbl");
		super.mainPanel.add(this.encryptStringLiterals, "encryptStringsComboBox");
		super.mainPanel.add(encryptIntegerConstantsLabel, "encryptIntegersLbl");
		super.mainPanel.add(this.encryptIntegerConstants, "encryptIntegersComboBox");
		super.mainPanel.add(encryptLongConstantsLabel, "encryptLongsLbl");
		super.mainPanel.add(this.encryptLongConstants, "encryptLongsComboBox");
		super.mainPanel.add(mixedCaseClassNamesLabel, "mixedCaseLbl");
		super.mainPanel.add(this.mixedCaseClassNames, "mixedCaseComboBox");
		super.mainPanel.add(this.collapsePackagesCheckBox, "collapsePackagesChk");
		super.mainPanel.add(this.collapsePackagesText, "defaultCollapsePackageTxt");
		super.mainPanel.add(this.randomizeObfuscation, "randomizeChk");
		super.mainPanel.add(this.aggressivelyRenameMethods, "aggressiveMethodRenamingChk");
		super.mainPanel.add(keepInnerClassInformationLabel, "keepInnerClassesLbl");
		super.mainPanel.add(super.keepInnerClassInformation, "keepInnerClassesComboBox");
		super.mainPanel.add(keepGenericsInformationLabel, "keepGenericsLbl");
		super.mainPanel.add(this.keepGenericsInformation, "keepGenericsComboBox");
		super.mainPanel.add(localVariableTablesLabel, "localVariablesLbl");
		super.mainPanel.add(this.localVariableTables, "localVariablesComboBox");
		super.mainPanel.add(lineNumberTablesLabel, "removeLineNumbersLbl");
		super.mainPanel.add(this.lineNumberTables, "removeLineNumbersComboBox");
		super.mainPanel.add(super.obfuscateReferencesCheckBox, "obfuscateReferencesChk");
		super.mainPanel.add(super.autoReflectionHandling, "autoReflectionChk");
		super.mainPanel.add(methodParameterChangesLabel, "methodParameterChangesLbl");
		super.mainPanel.add(super.methodParameterChanges, "methodParameterChangesComboBox");
		super.mainPanel.add(super.obfuscateMethodParameters, "obfuscateMethodParametersChk");
		super.mainPanel.add(this.keepBalancedLocks, "keepBalancedLocksChk");
		super.mainPanel.add(this.preverify, "preverifyChk");
		layoutProperties.append(ObfuscateOptionsFrame.layoutProperties);
	}

	@Override
	protected final void showHelp() {
		HelpManager.showHelp("033");
	}

	@Override
	public Dimension getMinFrameSize() {
		return new Dimension(250, 250);
	}

	@Override
	public boolean validateOptions() {
		if (this.collapsePackagesCheckBox.isSelected()) {
			String enteredPackageName = this.collapsePackagesText.getText();
			AtomicReference<String> validationError = new AtomicReference<>();
			String normalizedPackageName = PackageManager.normalizePackageName(enteredPackageName, validationError);
			if (!validationError.isNull()) {
				new CopyablePrompt(this, "Error", "Invalid default package name '" + enteredPackageName + "' : \"" + validationError.get() + "\"");
				GuiUtils.requestFocusInWindow(this.collapsePackagesText);
				return false;
			}

			this.collapsePackagesText.setText(normalizedPackageName.replace('/', '.'));
		}

		if (this.lineNumberTables.getSelectedItem().equals("scramble") && !super.produceChangeLogFileCheckBox.isSelected()) {
			new CopyablePrompt(this, "Error", "Must produce a change log if scramble line numbers chosen");
			GuiUtils.requestFocusInWindow(super.produceChangeLogFileCheckBox);
			return false;
		} else {
			return super.validateOptions();
		}
	}

	public static class CollapsePackagesListener implements ItemListener {
		public final ObfuscateOptionsFrame obfuscateOptionsFrame;

		public CollapsePackagesListener(ObfuscateOptionsFrame obfuscateOptionsFrame) {
			this.obfuscateOptionsFrame = obfuscateOptionsFrame;
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource() == this.obfuscateOptionsFrame.collapsePackagesCheckBox) {
				this.obfuscateOptionsFrame.collapsePackagesText.setEnabled(this.obfuscateOptionsFrame.collapsePackagesCheckBox.isSelected());
				if (!this.obfuscateOptionsFrame.collapsePackagesCheckBox.isSelected()) {
					this.obfuscateOptionsFrame.collapsePackagesText.select(0, 0);
				}
			}
		}
	}
}
