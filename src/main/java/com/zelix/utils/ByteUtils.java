package com.zelix.utils;

public class ByteUtils {
	public static byte[] unpackLong(long value) {
		return new byte[]{
			(byte)(value >>> 56), (byte)(value >>> 48), (byte)(value >>> 40), (byte)(value >>> 32), (byte)(value >>> 24), (byte)(value >>> 16), (byte)(value >>> 8), (byte)value
		};
	}

	public static final int toUnsigned(int value) {
		return value < 0 ? 256 + value : value;
	}

	public static byte[] unpackInt(int value) {
		return new byte[]{(byte)(value >>> 24), (byte)(value >>> 16), (byte)(value >>> 8), (byte)value};
	}

	public static final int getHighByte(int value) {
		return (value & 0xFF00) >> 8;
	}

	public static final int getLowByte(int value) {
		return value & 0xFF;
	}

	public static long packLong(byte[] bytes) {
		return (bytes[0] & 255L) << 56
			| (bytes[1] & 255L) << 48
			| (bytes[2] & 255L) << 40
			| (bytes[3] & 255L) << 32
			| (bytes[4] & 255L) << 24
			| (bytes[5] & 255L) << 16
			| (bytes[6] & 255L) << 8
			| bytes[7] & 255L;
	}

	public static final int combineBytes(int highByte, int lowByte) {
		return (highByte & 0xFF) << 8 | lowByte & 0xFF;
	}
}
