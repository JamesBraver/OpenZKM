package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.exceptions.LexicalException;
import com.zelix.exceptions.ParsingException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.ScriptParser;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.utils.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;

public class TrimExcludeStatement extends ExclusionInclusionStatement {
	public TrimExcludeStatement(int nodeType) {
		super(nodeType);
	}

	public static String buildStatementText(List<?> ruleStrings, int indent) {
		int ruleCount = ruleStrings.size();
		if (ruleCount > 0) {
			String continuationIndent = StringUtils.justificateString("", 76, indent, 32);
			StringBuffer textBuffer = new StringBuffer(100);
			textBuffer.append(Options.lineSeparator + StringUtils.justificateString("trimExclude", 76, indent, 32));

			for (int ruleIndex = 0; ruleIndex < ruleCount; ruleIndex++) {
				String ruleText = (String)ruleStrings.get(ruleIndex);
				if (ruleIndex > 0) {
					textBuffer.append(continuationIndent);
				}

				textBuffer.append(ruleText.toString());
				if (ruleIndex < ruleCount - 1) {
					textBuffer.append(" and" + Options.lineSeparator);
				} else {
					textBuffer.append(";" + Options.lineSeparator);
				}
			}

			return textBuffer.toString();
		} else {
			return null;
		}
	}

	public static ExclusionInclusionStatement parseTrimExcludeStatement(String statementText, ScriptContext scriptContext) throws IOException {
		BufferedReader statementReader = new BufferedReader(new StringReader(statementText));

		try {
			AbstractScriptNode inputNode = new ScriptParser(statementReader).parseDefaultTrimExcludeInput();
			inputNode.parseArguments(null, scriptContext);
			return ((DefaultTrimExcludeInput)inputNode).getExclusionStatement();
		} catch (ParsingException parsingException) {
			throw new ProcessingException(parsingException.toString());
		} catch (LexicalException lexicalException) {
			throw new ProcessingException(lexicalException.toString());
		}
	}

	@Override
	public String getCommandName() {
		return "trimExclude";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
		String message = AbstractScriptNode.getTimestampPrefix() + " Setting trim exclusions...";
		PrintWriter writer = scriptContext.getWriter();
		writer.println(message);
		System.out.println(message);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addTrimExcludeStatement(this);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}
