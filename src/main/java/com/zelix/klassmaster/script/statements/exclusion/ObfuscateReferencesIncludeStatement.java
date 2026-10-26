package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.exceptions.LexicalException;
import com.zelix.exceptions.ParsingException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.ScriptParser;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.DetailedExclusionStatement;
import com.zelix.utils.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;

public class ObfuscateReferencesIncludeStatement extends DetailedExclusionStatement {
	public ObfuscateReferencesIncludeStatement(int nodeType) {
		super(nodeType);
	}

	public static String buildStatementText(List<?> ruleStrings, int indent) {
		int ruleCount = ruleStrings.size();
		if (ruleCount > 0) {
			String continuationIndent = StringUtils.justificateString("", 76, indent, 32);
			StringBuffer textBuffer = new StringBuffer(100);
			textBuffer.append(Options.lineSeparator + StringUtils.justificateString("obfuscateReferencesInclude", 76, indent, 32));

			for (int ruleIndex = 0; ruleIndex < ruleCount; ruleIndex++) {
				Object ruleText = ruleStrings.get(ruleIndex);
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

	public static ObfuscateReferencesIncludeStatement parseIncludeStatement(String statementText, ScriptContext scriptContext) throws IOException {
		BufferedReader statementReader = new BufferedReader(new StringReader(statementText));

		try {
			AbstractScriptNode inputNode = new ScriptParser(statementReader).parseSingleObfuscateReferencesIncludeInput();
			inputNode.parseArguments(null, scriptContext);
			return ((SingleObfuscateReferencesIncludeInput)inputNode).getParsedStatement();
		} catch (ParsingException parsingException) {
			throw new ProcessingException(parsingException.toString());
		} catch (LexicalException lexicalException) {
			throw new ProcessingException(lexicalException.toString());
		}
	}

	public static int getCommandNameLength() {
		return 26;
	}

	@Override
	public String getCommandName() {
		return "obfuscateReferencesInclude";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
		String message = AbstractScriptNode.getTimestampPrefix() + " Setting obfuscate reference inclusions...";
		PrintWriter writer = scriptContext.getWriter();
		writer.println(message);
		System.out.println(message);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addObfuscateReferencesIncludeStatement(this);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}
