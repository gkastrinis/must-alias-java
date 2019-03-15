package input;

public class ResolvedCallInstruction extends Instruction {
	public final String toMethod;
	public String resultVar;

	public ResolvedCallInstruction(String id, String toMethod) {
		super(id, Instruction.Kind.RESOLVED_CALL);
		this.toMethod = toMethod;
	}

	public void setResultVar(String resultVar) {
		this.resultVar = resultVar;
	}

	@Override
	public Object clone() {
		return new ResolvedCallInstruction(id, toMethod);
	}
}
