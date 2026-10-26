package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;

public class GroupingsStatement extends ExclusionInclusionStatement {
	public GroupingsStatement(int nodeType) {
		super(nodeType);
	}

	public ArrayList<ArrayList<RenameFilterParameter>> getGroupingParameters() {
		ArrayList<ArrayList<RenameFilterParameter>> filterParametersByGrouping = new ArrayList<>();
		if (super.filterParameters != null) {
			for (int i = 0; i < super.filterParameters.size(); i++) {
				Grouping grouping = (Grouping)super.filterParameters.get(i);
				ArrayList<RenameFilterParameter> groupFilterParameters = new ArrayList<>();
				Enumeration<RenameFilterParameter> filterParameterEnumeration = grouping.getFilterParameters();

				while (filterParameterEnumeration.hasMoreElements()) {
					RenameFilterParameter filterParameter = filterParameterEnumeration.nextElement();
					groupFilterParameters.add(filterParameter);
				}

				filterParametersByGrouping.add(groupFilterParameters);
			}
		}

		return filterParametersByGrouping;
	}

	@Override
	public String getCommandName() {
		return "groupings";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
		PrintWriter writer = scriptContext.getWriter();
		String message = AbstractScriptNode.getTimestampPrefix() + " Setting groupings...";
		writer.println(message);
		System.out.println(message);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addGroupingsStatement(this);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}
