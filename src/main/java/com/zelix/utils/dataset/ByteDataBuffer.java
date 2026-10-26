package com.zelix.utils.dataset;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ByteDataBuffer extends DataInputStream {
	private ByteArrayInputStream underlyingStream;
	private byte[] sha1Hash;

	private ByteDataBuffer(ByteArrayInputStream underlyingStream, byte[] originalData, boolean calculateHash) {
		super(underlyingStream);
		this.underlyingStream = underlyingStream;
		if (calculateHash) {
			try {
				MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
				this.sha1Hash = sha1.digest(originalData);
			} catch (NoSuchAlgorithmException ignored) {
				this.sha1Hash = new byte[0];
			}
		}
	}

	public static ByteDataBuffer fromBytes(byte[] bytes, boolean calculateHash) {
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		return new ByteDataBuffer(stream, bytes, calculateHash);
	}

	public static ByteDataBuffer fromStream(InputStream is, int size, boolean calculateHash) throws IOException {
		ByteDataBuffer dataBuffer;
		try {
			byte[] streamBytes = new byte[size];
			int bytesRead = 0;

			int readCount;
			while (bytesRead < size && (readCount = is.read(streamBytes, bytesRead, Math.min(8192, size - bytesRead))) != -1) {
				bytesRead += readCount;
			}

			ByteArrayInputStream stream = new ByteArrayInputStream(streamBytes);
			dataBuffer = new ByteDataBuffer(stream, streamBytes, calculateHash);
		} finally {
			is.close();
		}

		return dataBuffer;
	}

	public static ByteDataBuffer fromStream(InputStream is, int size) throws IOException {
		return fromStream(is, size, true);
	}

	public byte[] getSha1Hash() {
		return this.sha1Hash != null ? (byte[])this.sha1Hash.clone() : null;
	}

	@Override
	public void reset() {
		this.underlyingStream.reset();
	}
}
