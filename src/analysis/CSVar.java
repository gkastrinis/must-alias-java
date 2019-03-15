package analysis;

import java.util.List;
import input.Instruction;

public class CSVar extends ContextSensitive<Instruction, String> {
	public CSVar(String v, List<Instruction> context) {
		super(v, context);
	}
}
