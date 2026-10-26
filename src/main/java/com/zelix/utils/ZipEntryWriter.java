package com.zelix.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipEntryWriter {
	private ZipOutputStream zipOutputStream;
	private String entryName;
	private boolean isDeflated;
	private File sourceFile;
	private InputStream sourceInputStream;
	private int sourceInputStreamSize;
	private String sourceType;
	private ByteArrayOutputStream byteArrayOutputStream;
	private BufferedOutputStream bufferedOutputStream;

	public ZipEntryWriter(ZipOutputStream zipOutputStream, String entryName, boolean isDeflated, InputStream sourceInputStream, int sourceInputStreamSize) {
		this.zipOutputStream = zipOutputStream;
		this.entryName = entryName;
		this.isDeflated = isDeflated;
		this.sourceInputStream = sourceInputStream;
		this.sourceInputStreamSize = sourceInputStreamSize;
		this.sourceType = "IS";
	}

	public ZipEntryWriter(ZipOutputStream zipOutputStream, String entryName, boolean isDeflated) {
		this.zipOutputStream = zipOutputStream;
		this.entryName = entryName;
		this.isDeflated = isDeflated;
		this.sourceType = "BAOS";
	}

	public ZipEntryWriter(ZipOutputStream zipOutputStream, String entryName, boolean isDeflated, File sourceFile) {
		this.zipOutputStream = zipOutputStream;
		this.entryName = entryName;
		this.isDeflated = isDeflated;
		this.sourceFile = sourceFile;
		this.sourceType = "FILE";
	}

	public OutputStream getOutputStream() {
		if (this.bufferedOutputStream == null) {
			this.byteArrayOutputStream = new ByteArrayOutputStream();
			CheckedOutputStream cos = new CheckedOutputStream(this.byteArrayOutputStream, new CRC32());
			this.bufferedOutputStream = new BufferedOutputStream(cos);
		}

		this.sourceType = this.sourceType + "<";
		return this.bufferedOutputStream;
	}

	public CRC32 writeEntry(Long time) throws IOException {
		return this.writeEntry(time, null);
	}

	public CRC32 writeEntryInternal(Long time, Long extra, String comment) throws IOException {
		CRC32 crc32 = null;
		ZipEntry ze = new ZipEntry(this.entryName);
		if (time != null) {
			ze.setTime(time);
		}

		if (extra != null) {
			byte[] extraData = extra.toString().getBytes("UTF-8");
			ze.setExtra(extraData);
			if (comment != null && comment.length() > 0) {
				ze.setComment(comment);
			}
		}

		ze.setMethod(this.isDeflated ? ZipEntry.DEFLATED : ZipEntry.STORED);
		if (this.isDeflated && this.byteArrayOutputStream == null) {
			this.zipOutputStream.putNextEntry(ze);

			try {
				if (this.sourceInputStream == null) {
					DebugUtils.assertNotNull(
						this.sourceFile,
						"(" + (this.sourceInputStream == null) + "," + (this.byteArrayOutputStream == null) + ",'" + this.sourceType + "'," + this.isDeflated + ")"
					);
					this.sourceInputStream = new BufferedInputStream(new FileInputStream(this.sourceFile));
				}

				crc32 = new CRC32();
				byte[] block = new byte[1024];

				int len;
				while ((len = this.sourceInputStream.read(block)) >= 0) {
					this.zipOutputStream.write(block, 0, len);
					crc32.update(block, 0, len);
				}
			} finally {
				if (this.sourceInputStream != null) {
					try {
						this.sourceInputStream.close();
					} catch (IOException ignored) {
					}
				}
			}
		} else if (this.sourceFile != null) {
			BufferedInputStream bis = null;

			try {
				bis = new BufferedInputStream(new FileInputStream(this.sourceFile));
				byte[] fileBlock = new byte[1024];
				long size = 0L;
				crc32 = new CRC32();

				int fileBytesRead;
				while ((fileBytesRead = bis.read(fileBlock)) >= 0) {
					crc32.update(fileBlock, 0, fileBytesRead);
					size += fileBytesRead;
				}

				bis.close();
				ze.setCrc(crc32.getValue());
				ze.setSize(size);
				this.zipOutputStream.putNextEntry(ze);
				bis = new BufferedInputStream(new FileInputStream(this.sourceFile));

				while ((fileBytesRead = bis.read(fileBlock)) >= 0) {
					this.zipOutputStream.write(fileBlock, 0, fileBytesRead);
				}
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException fileCloseIgnored) {
					}
				}
			}
		} else {
			byte[] ba;
			if (this.byteArrayOutputStream != null) {
				this.bufferedOutputStream.flush();
				ba = this.byteArrayOutputStream.toByteArray();
				this.byteArrayOutputStream.close();
			} else {
				try {
					ba = new byte[this.sourceInputStreamSize];
					int index = 0;

					int bytesRead;
					while (index < this.sourceInputStreamSize && (bytesRead = this.sourceInputStream.read(ba, index, Math.min(8192, this.sourceInputStreamSize - index))) != -1) {
						index += bytesRead;
					}
				} finally {
					if (this.sourceInputStream != null) {
						try {
							this.sourceInputStream.close();
						} catch (IOException entryCloseIgnored) {
						}
					}
				}
			}

			if (!this.isDeflated) {
				crc32 = new CRC32();
				crc32.update(ba);
				ze.setCrc(crc32.getValue());
				ze.setSize(ba.length);
			}

			this.zipOutputStream.putNextEntry(ze);
			this.zipOutputStream.write(ba, 0, ba.length);
		}

		this.zipOutputStream.flush();
		this.zipOutputStream.closeEntry();
		return crc32;
	}

	public CRC32 writeEntry() throws IOException {
		return this.writeEntry(null, null);
	}

	public CRC32 writeEntry(Long time, Long extra) throws IOException {
		return this.writeEntryInternal(time, extra, null);
	}
}
