package com.zelix.utils.collections;

public class CircularBuffer {
	private int writePosition = 0;
	private int currentSize = 0;
	private final Object[] buffer;

	public CircularBuffer(int capacity) {
		this.buffer = new Object[capacity];
	}

	public Object get(int offset) {
		if (offset >= this.currentSize) {
			throw new IllegalArgumentException(offset + ">=" + this.currentSize);
		} else {
			int bufferIndex = this.writePosition - offset - 1;
			if (bufferIndex < 0) {
				bufferIndex += this.buffer.length;
			}

			return this.buffer[bufferIndex];
		}
	}

	public void add(Object element) {
		this.buffer[this.writePosition] = element;
		this.writePosition = (this.writePosition + 1) % this.buffer.length;
		if (this.currentSize < this.buffer.length) {
			this.currentSize++;
		}
	}
}
