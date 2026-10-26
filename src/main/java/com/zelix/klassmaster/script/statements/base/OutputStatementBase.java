package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.token.StringLiteral;
import com.zelix.utils.GlobalUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class OutputStatementBase extends ScriptCommand {
	protected Map<String, String> parameterValues = GlobalUtils.newHashMap();
	protected List<String> stringArguments = new ArrayList<>();

	public OutputStatementBase(int nodeType) {
		super(nodeType);
	}

	@Override
	public final void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();
		int initialMessageCount = scriptContext.getMessageCount();
		int initialWarningCount = scriptContext.getWarningCount();
		int initialErrorCount = scriptContext.getErrorCount();

		for (int i = 0; i < childCount; i++) {
			ScriptNode child = this.getChild(i);
			if (child instanceof StringLiteral) {
				String literalValue = ((StringLiteral)child).getText();
				if (this.stringArguments.contains(literalValue)) {
					scriptContext.logWarning(
						"\""
							+ literalValue
							+ "\" appears more than once in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ ". First occurrence will be used."
					);
				} else {
					this.stringArguments.add(literalValue);
				}
			} else if (child instanceof ConfigurationParameter) {
				ScriptNode parameterNode = ((ConfigurationParameter)child).getChild(0);
				parameterNode.parseArguments(this, scriptContext);
				ConfigurationParameter parameter = (ConfigurationParameter)parameterNode;
				String previousValue = this.parameterValues.put(parameter.getParameterName(), parameter.getValue(0));
				if (previousValue != null) {
					scriptContext.logWarning(
						"\""
							+ parameter.getParameterName()
							+ "\" appears more than once in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ ".  First occurrence will be used."
					);
					this.parameterValues.put(parameter.getParameterName(), previousValue);
				}
			} else {
				scriptContext.logFatalError(this.getClass().getName() + " had " + child.getClass().getName() + " as child " + i + " at line " + this.getLineNumber() + ".");
			}
		}

		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}
}
