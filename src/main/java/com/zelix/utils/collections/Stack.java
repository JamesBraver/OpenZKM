package com.zelix.utils.collections;

public class Stack<E> {
	private java.util.Stack<E> stack = new java.util.Stack<>();

	public int search(Object element) {
		return this.stack.search(element);
	}

	public E push(E item) {
		this.stack.push(item);
		return item;
	}

	public boolean empty() {
		return this.stack.empty();
	}

	public int size() {
		return this.stack.size();
	}

	public E pop() {
		return this.stack.pop();
	}

	public E peek(int depth) {
		return this.stack.get(this.stack.size() - 1 - depth);
	}

	public E peek() {
		return this.stack.peek();
	}
}
