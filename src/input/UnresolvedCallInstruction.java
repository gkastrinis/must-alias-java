package input;

public class UnresolvedCallInstruction extends Instruction {

	public UnresolvedCallInstruction(String id) {
		super(id, Instruction.Kind.UNRESOLVED_CALL);
	}

	@Override
	public Object clone() {
		return new UnresolvedCallInstruction(id);
	}
}

