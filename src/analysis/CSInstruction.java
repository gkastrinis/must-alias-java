package analysis;

import java.util.List;
import input.Instruction;

public class CSInstruction extends ContextSensitive<Instruction, Instruction> {
	public CSInstruction(Instruction i, List<Instruction> context) {
		super(i, context);
	}

	public CSInstruction(Instruction i) {
		super(i);
	}
}
