package utils;

public class Pair <L,R> {
	public final L left;
	public final R right;
	
	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) return false;
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Pair<L,R> pair = (Pair) o;
		return this.left.equals(pair.left) && this.right.equals(pair.right);
	}

	@Override
	public int hashCode() {
		return left.hashCode() + right.hashCode();
	}
	
}
