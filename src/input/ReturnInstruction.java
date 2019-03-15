package input;

public class ReturnInstruction extends Instruction {
	public final String var;

	public ReturnInstruction(String id, String var) {
		super(id, Instruction.Kind.RETURN);
		this.var = var;
	}

	@Override
	public Object clone() {
		return new ReturnInstruction(id, var);
	}
}
