package com.zelix.utils.collections;

import com.zelix.exceptions.LookupException;
import com.zelix.utils.GlobalUtils;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;

public class RingQueue<E> {
	private int head = 0;
	private int tail = 0;
	private int size = 0;
	private Object[] buffer;
	private Set<E> elementSet;

	public RingQueue() {
		this(10);
	}

	public RingQueue(int capacity) {
		if (capacity <= 1) {
			throw new IllegalArgumentException("Queue size must be greater than 1 : " + capacity);
		} else {
			this.buffer = new Object[capacity];
			this.elementSet = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(capacity));
		}
	}

	public void addAll(Collection<E> collection) {
		for (E element : collection) {
			this.push(element);
		}
	}

	private void grow() {
		int oldCapacity = this.buffer.length;
		Object[] newBuffer = new Object[oldCapacity * 2];

		for (int i = 0; i < this.size; i++) {
			int sourceIndex = (this.head + i) % oldCapacity;
			newBuffer[i] = this.buffer[sourceIndex];
		}

		this.head = 0;
		this.tail = this.size;
		this.buffer = newBuffer;
	}

	@SuppressWarnings("unchecked")
	public E pop() {
		if (this.size == 0) {
			throw new LookupException("Empty Queue");
		} else {
			int capacity = this.buffer.length;
			E element = (E)this.buffer[this.head];
			this.head = (this.head + 1) % capacity;
			this.size--;
			this.elementSet.remove(element);
			return element;
		}
	}

	public int size() {
		return this.size;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof RingQueue) {
			RingQueue<?> other = (RingQueue<?>)obj;
			if (this.size != other.size) {
				return false;
			} else {
				int capacity = this.buffer.length;
				int otherCapacity = other.buffer.length;

				for (int i = 0; i < this.size; i++) {
					int index = (this.head + i) % capacity;
					int otherIndex = (other.head + i) % otherCapacity;
					if (!this.buffer[index].equals(other.buffer[otherIndex])) {
						return false;
					}
				}

				return true;
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = 1;
		int capacity = this.buffer.length;

		for (int i = 0; i < this.size; i++) {
			int index = (this.head + i) % capacity;
			hash = 31 * hash + this.buffer[index].hashCode();
		}

		return hash;
	}

	public boolean push(E element) {
		if (!this.elementSet.add(element)) {
			return false;
		} else {
			int capacity = this.buffer.length;
			if (this.size == capacity) {
				this.grow();
				capacity = this.buffer.length;
			}

			this.buffer[this.tail] = element;
			this.tail = (this.tail + 1) % capacity;
			this.size++;
			return true;
		}
	}

	public void addAll(Enumeration<E> enumeration) {
		while (enumeration.hasMoreElements()) {
			this.push(enumeration.nextElement());
		}
	}

	public boolean isEmpty() {
		return this.size == 0;
	}

	public boolean contains(Object element) {
		return this.elementSet.contains(element);
	}
}
