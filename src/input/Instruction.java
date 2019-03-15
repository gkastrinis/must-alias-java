package input;

public abstract class Instruction implements Comparable<Instruction> {
	public final String id;

	// Not the best approach, but should be more efficient compared to
	// instanceof or double dispatching (a la Visitor pattern).
	public enum Kind {
		IRRELEVANT,
		LOAD_INSTANCE,
		STORE_INSTANCE,
		MOVE,
		PHI,
		RESOLVED_CALL,
		UNRESOLVED_CALL,
		RETURN
	}

	public final Kind kind;

	public Instruction(String id, Kind kind) {
		this.id = id;
		this.kind = kind;
	}

	@Override
	public int compareTo(Instruction i) {
		return id.compareTo(i.id);
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public abstract Object clone();

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Instruction))
			return false;
		return id.equals(((Instruction) o).id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
