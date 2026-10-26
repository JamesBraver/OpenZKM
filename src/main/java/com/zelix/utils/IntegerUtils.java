package com.zelix.utils;

public class IntegerUtils {
	public static boolean isInteger(String text) {
		try {
			Integer.parseInt(text);
			return true;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}
}
