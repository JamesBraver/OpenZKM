package com.zelix.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256Utils {
	private final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
	private byte[] digestBytes;

	public SHA256Utils(BufferedInputStream inputStream) throws NoSuchAlgorithmException, IOException {
		this.computeDigest(inputStream);
	}

	public SHA256Utils(String filePath) throws NoSuchAlgorithmException, IOException {
		BufferedInputStream inputStream = null;

		try {
			inputStream = new BufferedInputStream(new FileInputStream(filePath), 32768);
			this.computeDigest(inputStream);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}

	private void computeDigest(BufferedInputStream inputStream) throws IOException {
		byte[] buffer = new byte[32768];

		int bytesRead;
		do {
			bytesRead = inputStream.read(buffer);
			if (bytesRead > 0) {
				this.messageDigest.update(buffer, 0, bytesRead);
			}
		} while (bytesRead != -1);

		inputStream.close();
		this.digestBytes = this.messageDigest.digest();
	}

	public String getHexDigest() {
		byte[] hashBytes = this.getDigestBytes();
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < hashBytes.length; i++) {
			String hexByte = Integer.toHexString(hashBytes[i] & 255);
			if (hexByte.length() == 1) {
				hexByte = "0" + hexByte;
			}

			builder.append(hexByte);
		}

		return builder.toString();
	}

	public byte[] getDigestBytes() {
		return (byte[])this.digestBytes.clone();
	}
}
