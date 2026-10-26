package com.zelix.utils.dataset;

public class Triplet {
	private Object first;
	private Object second;
	private Object third;

	public Triplet(Object first, Object second, Object third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public Object getThird() {
		return this.third;
	}

	public Object getSecond() {
		return this.second;
	}

	public Object setByIndex(Object value, int index) {
		Object oldValue;
		switch (index) {
			case 0:
				oldValue = this.first;
				this.first = value;
				break;
			case 1:
				oldValue = this.second;
				this.second = value;
				break;
			case 2:
				oldValue = this.third;
				this.third = value;
				break;
			default:
				throw new IllegalArgumentException("Index must be between 0 and 1 inclusive : " + index);
		}

		return oldValue;
	}

	public Object getByIndex(int index) {
		switch (index) {
			case 0:
				return this.first;
			case 1:
				return this.second;
			case 2:
				return this.third;
			default:
				throw new IllegalArgumentException("Index must be between 0 and 1 inclusive : " + index);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Triplet)) {
			return false;
		} else {
			Triplet triplet = (Triplet)o;
			return (this.first == null && triplet.first == null || this.first != null && triplet.first != null && this.first.equals(triplet.first))
				&& (this.second == null && triplet.second == null || this.second != null && triplet.second != null && this.second.equals(triplet.second))
				&& (this.third == null && triplet.third == null || this.third != null && triplet.third != null && this.third.equals(triplet.third));
		}
	}

	public Object setThird(Object third) {
		Object oldThird = this.third;
		this.third = third;
		return oldThird;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		if (this.first != null) {
			hash = 0 ^ this.first.hashCode();
		}

		if (this.second != null) {
			hash ^= this.second.hashCode();
		}

		if (this.third != null) {
			hash ^= this.third.hashCode();
		}

		return hash;
	}

	public Object setSecond(Object second) {
		Object oldSecond = this.second;
		this.second = second;
		return oldSecond;
	}

	public Object getFirst() {
		return this.first;
	}
}
