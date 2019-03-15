package input;

public class IrrelevantInstruction extends Instruction {

	public IrrelevantInstruction(String id) {
		super(id, Instruction.Kind.IRRELEVANT);
	}

	@Override
	public Object clone() {
		return new IrrelevantInstruction(id);
	}
}
