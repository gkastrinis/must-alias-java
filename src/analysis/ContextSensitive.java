package analysis;

import java.util.ArrayList;
import java.util.List;

public class ContextSensitive<T extends Comparable<T>, E extends Comparable<E>> implements Comparable<ContextSensitive<T,E>> {
	public static final int MAX_CONTEXT_DEPTH = 2;

	public List<T> context;
	public E val;

	public ContextSensitive(E e) {
		context = new ArrayList<T>(MAX_CONTEXT_DEPTH);
		val = e;
	}

	public ContextSensitive(ContextSensitive<T,E> other) {
		context = new ArrayList<T>(other.context);
		val = other.val;
	}

	public ContextSensitive(E e, List<T> ctx) {
		context = ctx;
		val = e;
	}

	public void push(T contextEntry) {
		context.add(0, contextEntry);
	}

	public void pop() {
		context.remove(0);
	}

	@Override
	public int compareTo(ContextSensitive<T,E> other) {
		int valCmp = val.compareTo(other.val);
		if (valCmp != 0) return valCmp;

		int mySize = context.size();
		int otherSize = other.context.size();
		if (mySize != otherSize)
			return mySize - otherSize;
		for (int i = 0 ; i < mySize ; i++) {
			T me = context.get(i);
			T it = other.context.get(i);
			int cmp = me.compareTo(it);
			if (cmp != 0) return cmp;
		}
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ContextSensitive))
			return false;
		return val.equals(((ContextSensitive)other).val) &&
			context.equals(((ContextSensitive)other).context);
	}

	@Override
	public int hashCode() {
		return val.hashCode() + context.hashCode();
	}

	@Override
	public String toString() {
		return val.toString() + " " + context.toString();
	}
}
