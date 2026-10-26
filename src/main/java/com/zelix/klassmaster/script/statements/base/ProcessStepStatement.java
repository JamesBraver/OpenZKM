package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.utils.collections.MultiMap;
import java.io.IOException;

public abstract class ProcessStepStatement extends ScriptCommand {
	protected MultiMap<String, String> parameterValues = new MultiMap<>(11, 3);

	public ProcessStepStatement(int nodeType) {
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
			if (child instanceof ActionParameter) {
				ActionParameter actionParameter = (ActionParameter)child;
				actionParameter.parseArguments(this, scriptContext);
				String parameterName = actionParameter.getParameterName();
				if (this.parameterValues.containsKey(parameterName)) {
					scriptContext.logWarning(
						"\""
							+ parameterName
							+ "\" appears more than once in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ ".  First occurrence will be used."
					);
				} else if (this.validateParameter(actionParameter, scriptContext)) {
					int valueCount = actionParameter.getValueCount();

					for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
						this.parameterValues.put(parameterName, actionParameter.getValue(valueIndex));
					}
				}
			} else {
				scriptContext.logFatalError(this.getClass().getName() + " had " + child.getClass().getName() + " as child " + i + " at line " + this.getLineNumber() + ".");
			}
		}

		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}

	public boolean validateParameter(ActionParameter parameter, ScriptContext scriptContext) {
		return true;
	}
}
