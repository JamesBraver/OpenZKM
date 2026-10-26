package com.zelix.utils;

import com.zelix.exceptions.DebugAssertFailedException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class DebugUtils {
	public static final Runtime RUNTIME = Runtime.getRuntime();
	public static final long START_TIME_MS = System.currentTimeMillis();
	public static long unusedTimestamp1 = START_TIME_MS;
	public static long unusedTimestamp2 = START_TIME_MS;
	public static long unusedTimestamp3 = START_TIME_MS;
	public static long unusedTimestamp4 = START_TIME_MS;
	public static final String LINE_SEPARATOR = GlobalUtils.LINE_SEPARATOR;
	public static Map<Object, Object> trackingMap = new LinkedHashMap<>();
	public static long INITIAL_USED_MEMORY = RUNTIME.totalMemory() - RUNTIME.freeMemory();
	public static PrintWriter STD_ERR = new PrintWriter(System.err, true);
	public static PrintWriter STD_OUT = new PrintWriter(System.out, true);

	public static void assertTrue(boolean value, String[] messages) {
		if (!value) {
			if (messages.length == 1) {
				throw new DebugAssertFailedException("Assert Failed: " + messages[0]);
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append('\'');

				for (int i = 0; i < messages.length; i++) {
					String message = messages[i];
					sb.append(message);
					if (i < messages.length - 2) {
						sb.append("', '");
					} else if (i == messages.length - 2) {
						sb.append("' and '");
					} else {
						sb.append('\'');
					}
				}

				throw new DebugAssertFailedException("Assert Failed: " + sb);
			}
		}
	}

	public static void assertNotNullObject(Object object, Object[] messages) {
		if (object == null) {
			StringBuilder sb = new StringBuilder();

			for (Object message : messages) {
				if (sb.length() > 0) {
					sb.append(", ");
				}

				sb.append(message.toString());
			}

			throw new DebugAssertFailedException("Assert Failed: " + sb.toString());
		}
	}

	public static void assertNull(Object object, String message) {
		if (object != null) {
			throw new DebugAssertFailedException("Assert Failed: " + message);
		}
	}

	public static String getSimpleName(Object object) {
		if (object == null) {
			return "null";
		} else {
			String className = object.getClass().getName().replace('/', '.');
			int lastDotIndex = className.lastIndexOf(".");
			return lastDotIndex > -1 ? className.substring(lastDotIndex + 1) : className;
		}
	}

	public static void noop(String message) {
	}

	public static String formatArray(Object[] array, boolean multiLine) {
		if (array != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("{");

			for (int i = 0; i < array.length; i++) {
				sb.append("#" + i + ": " + array[i] + (i < array.length - 1 ? "," + (multiLine ? LINE_SEPARATOR : "") : ""));
			}

			return sb.toString() + "}";
		} else {
			return "{null array}";
		}
	}

	public static String formatArrayMultiLine(Object[] array) {
		return formatArray(array, true);
	}

	public static void assertTrue(boolean value, int message) {
		if (!value) {
			throw new DebugAssertFailedException("Assert Failed: " + String.valueOf(message));
		}
	}

	public static void assertNotNullString(Object object, String[] messages) {
		if (object == null) {
			StringBuilder sb = new StringBuilder();

			for (String message : messages) {
				sb.append(message.toString());
			}

			throw new DebugAssertFailedException("Assert Failed: " + sb.toString());
		}
	}

	public static void assertNotNull(Object object, String message) {
		if (object == null) {
			throw new DebugAssertFailedException("Assert Not Null Failed: " + message);
		}
	}
}
