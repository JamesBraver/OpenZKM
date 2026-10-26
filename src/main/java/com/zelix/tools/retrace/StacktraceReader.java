package com.zelix.tools.retrace;

import com.zelix.exceptions.StacktraceAnalysisException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class StacktraceReader {
	private String stacktraceText;
	private List<StacktraceElement> lines;

	public StacktraceReader(String stacktraceText) {
		this.stacktraceText = stacktraceText;
		BufferedReader lineReader = new BufferedReader(new StringReader(this.stacktraceText));
		this.lines = new ArrayList<>();

		try {
			String line;
			while ((line = lineReader.readLine()) != null) {
				if (line.trim().length() > 0) {
					this.lines.add(new StacktraceElement(line));
				}
			}
		} catch (IOException ioException) {
			throw new StacktraceAnalysisException(ioException.toString());
		}
	}

	public Enumeration<StacktraceElement> getLinesReversed() {
		ArrayList<StacktraceElement> reversedLines = new ArrayList<>(this.lines);
		Collections.reverse(reversedLines);
		return Collections.enumeration(reversedLines);
	}

	public int getLineCount() {
		return this.lines.size();
	}
}
