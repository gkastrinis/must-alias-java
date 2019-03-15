package input;

public class MoveInstruction extends Instruction {
	public final String toVar;
	public final String fromVar;

	public MoveInstruction(String id, String toVar, String fromVar) {
		super(id, Instruction.Kind.MOVE);
		this.toVar = toVar;
		this.fromVar = fromVar;
	}

	@Override
	public Object clone() {
		return new MoveInstruction(id, toVar, fromVar);
	}
}
