package input;

import java.util.HashSet;
import java.util.Set;

public class PhiInstruction extends Instruction {
	public String toVar;
	public final Set<String> phiVars;

	public PhiInstruction(String id) {
		super(id, Instruction.Kind.PHI);
		this.phiVars = new HashSet<>();
	}

	public void addToVarToPhi(String toVar) {
		this.toVar = toVar;
	}

	public void addFromVarToPhi(String fromVar) {
		this.phiVars.add(fromVar);
	}

	@Override
	public Object clone() {
		PhiInstruction p = new PhiInstruction(id);
		p.toVar = toVar;
		p.phiVars.addAll(phiVars);
		return p;
	}
}
