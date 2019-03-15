package input;

public class LoadInstruction extends Instruction {
	public final String toVar;
	public final String baseVar;
	public final String fld;

	public LoadInstruction(String id, String toVar, String baseVar, String fld) {
		super(id, Instruction.Kind.LOAD_INSTANCE);
		this.toVar = toVar;
		this.baseVar = baseVar;
		this.fld = fld;
	}

	@Override
	public Object clone() {
		return new LoadInstruction(id, toVar, baseVar, fld);
	}
}
