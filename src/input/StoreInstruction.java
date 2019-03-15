package input;

public class StoreInstruction extends Instruction {
	public final String baseVar;
	public final String fld;
	public final String fromVar;

	public StoreInstruction(String id, String baseVar, String fld, String fromVar) {
		super(id, Instruction.Kind.STORE_INSTANCE);
		this.baseVar = baseVar;
		this.fld = fld;
		this.fromVar = fromVar;
	}

	@Override
	public Object clone() {
		return new StoreInstruction(id, baseVar, fld, fromVar);
	}
}
