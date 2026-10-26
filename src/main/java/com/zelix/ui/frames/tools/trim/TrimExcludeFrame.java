package com.zelix.ui.frames.tools.trim;

import com.zelix.HelpManager;
import com.zelix.klassmaster.config.RuleFilterConfig;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.dataset.ParameterModel;
import com.zelix.ui.dialogs.AddElementsDialog;
import com.zelix.ui.dialogs.filter.AddClassFilterDialog;
import com.zelix.ui.dialogs.filter.AddFieldFilterDialog;
import com.zelix.ui.dialogs.filter.AddMethodFilterDialog;
import com.zelix.ui.frames.TaskPreparingFrame;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.Workspace;
import java.awt.Container;
import java.util.Enumeration;
import java.util.List;

public abstract class TrimExcludeFrame extends TaskPreparingFrame {
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
	public static String[] extraLayoutProperties = new String[]{
		"startSecondColumn=max(applicationTypeLbl.defaultWidth, excludeMainClassLbl.defaultWidth)+10",
		"applicationTypeLbl.centerY=applicationTypeComboBox.centerY",
		"applicationTypeLbl.left=5",
		"applicationTypeComboBox.top=5",
		"applicationTypeComboBox.left=startSecondColumn",
		"applicationTypeComboBox.right=container.right-5",
		"excludeMainClassLbl.left=5",
		"excludeMainClassLbl.centerY=mainClassComboBox.centerY",
		"mainClassComboBox.left=startSecondColumn",
		"mainClassComboBox.top=applicationTypeComboBox.bottom+5",
		"mainClassComboBox.right=container.right-5",
		"container.defaultRightPad=5",
		"container.defaultBottomPad=5"
	};
	public Workspace workspace;
	public TrimConfig trimConfig;

	public TrimExcludeFrame(
		String title, GUI gui, List<ClassNodeEntry> classNodes, RuleFilterConfig ruleFilterConfig, Workspace workspace, TrimConfig trimConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer, int parameterCategory
	) {
		super(title, gui, classNodes, ruleFilterConfig, scriptContext, resultConsumer, parameterCategory);
		this.initFrame(title);
		this.workspace = workspace;
		this.trimConfig = trimConfig;
		GuiUtils.requestFocusInWindow(super.okButton);
	}

	@Override
	public String[] getExtraLayoutProperties() {
		return extraLayoutProperties;
	}

	@Override
	public void modifyParameter(ParameterModel parameterModel, String ruleString, ZKMConsumer consumer) {
		switch (parameterModel.getTargetType()) {
			case 2:
				this.showParameterDialog("2", ruleString, consumer);
				break;
			case 3:
				this.showParameterDialog("3", ruleString, consumer);
				break;
			case 4:
				this.showParameterDialog("4", ruleString, consumer);
		}
	}

	@Override
	public String[] getLayoutProperties() {
		return layoutProperties;
	}

	@Override
	public final String getEntryRuleString(ClassNodeEntry entry) {
		if (entry != null) {
			String binaryName = entry.getBinaryName();
			return this.getRuleString(binaryName, entry.isMainClass());
		} else {
			return null;
		}
	}

	@Override
	public final String getRuleString(String binaryName, boolean hasMainMethod) {
		String ruleString = null;
		if (binaryName != null) {
			if (hasMainMethod) {
				ruleString = binaryName + "^ " + TaskPreparingFrame.mainMethodPattern;
			} else {
				ruleString = binaryName;
			}
		}

		return ruleString;
	}

	@Override
	public boolean validateParameters() {
		if (super.paramMap.size() == 0) {
			new CopyablePrompt(this, "Error - No trim exclusion parameters", "If there are no Trim exclusion parameters then every class will be trimmed.");
			return false;
		} else {
			boolean classExcluded = false;
			Enumeration<String> ruleStrings = super.paramMap.keysEnumeration();

			while (ruleStrings.hasMoreElements()) {
				String ruleString = ruleStrings.nextElement();
				if (this.parseParameter(ruleString).excludesClass()) {
					classExcluded = true;
				}
			}

			if (classExcluded) {
				return true;
			} else {
				new CopyablePrompt(
					this, "Error - No trim exclusion parameters exclude a class", "If no Trim exclusion parameters can exclude a class then every class will be trimmed."
				);
				return false;
			}
		}
	}

	@Override
	public void showParameterDialog(Object targetType, String ruleString, ZKMConsumer consumer) {
		String targetTypeString = (String)targetType;
		if (targetTypeString.equals("2")) {
			if (ruleString == null) {
				ruleString = "*.*";
			}

			new AddClassFilterDialog(this, CopyrightConstants.NAME + " Trim Class Exclude Parameter", this.parseParameter(ruleString), 2, consumer);
		} else if (targetTypeString.equals("3")) {
			if (ruleString == null) {
				ruleString = "*.* *";
			}

			new AddFieldFilterDialog(this, CopyrightConstants.NAME + " Trim Field Exclude Parameter", this.parseParameter(ruleString), 2, consumer);
		} else if (targetTypeString.equals("4")) {
			if (ruleString == null) {
				ruleString = "*.* *(*)";
			}

			new AddMethodFilterDialog(this, CopyrightConstants.NAME + " Trim Method Exclude Parameter", this.parseParameter(ruleString), 2, consumer);
		}
	}

	@Override
	public final void showHelp() {
		HelpManager.showHelp("020");
	}

	@Override
	public void addExtraComponents(PowerLayout layout, Container container) {
		this.addMainClassSelector(container, "Application entry point class:", HelpManager.getToolTip("TRIM_EXCLUDE_MAIN_CLASS_LIST"));
	}

	@Override
	public void showParameterTypeDialog(ZKMConsumer consumer) {
		new AddElementsDialog(this, CopyrightConstants.NAME + " Trim Exclude Parameter Type", consumer);
	}
}
