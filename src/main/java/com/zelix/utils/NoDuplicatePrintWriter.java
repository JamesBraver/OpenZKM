package com.zelix.utils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;

public class NoDuplicatePrintWriter extends PrintWriter {
	private HashMap<String, String> printed = GlobalUtils.newHashMap();

	public NoDuplicatePrintWriter(OutputStream out) {
		super(out);
	}

	public NoDuplicatePrintWriter(Writer out, boolean autoFlush) {
		super(out, autoFlush);
	}

	@Override
	public void println(String x) {
		if (this.printed.put(x, x) == null) {
			super.println(x);
		}
	}
}
