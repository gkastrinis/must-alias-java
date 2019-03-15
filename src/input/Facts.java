package input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.InputReader;

public class Facts {
	public final static boolean PRINT = false;
	public final Map<Instruction, List<Instruction>> instrToPrevious;
	public final Map<Instruction, List<Instruction>> instrToNext;
	public final Map<String, List<Instruction>> methodToInstr;
	public final Map<String, Instruction> firstInstructions;
	public final Map<ResolvedCallInstruction, Map<String, List<String>>> actualToFormal; // also map receiver var to "this"
	public final Map<ReturnInstruction, Set<ResolvedCallInstruction>> calleeToCallers;
	public final Set<String> rootMethods;
	public final Set<String> allocationVars;
	public final Map<String, Set<String>> methodToAllocationVars; // auxiliary

	public Facts(String pathPrefix) throws IOException {
		instrToPrevious        = new HashMap<>();
		instrToNext            = new HashMap<>();
		methodToInstr          = new HashMap<>();
		firstInstructions      = new HashMap<>();
		actualToFormal         = new HashMap<>();
		calleeToCallers        = new HashMap<>();
		rootMethods            = new HashSet<>();
		allocationVars         = new HashSet<>();
		methodToAllocationVars = new HashMap<>();

		Map<String, Instruction> idToInstr = new HashMap<>();


		////////////////// Instructions //////////////////
		String f_phiNode             = pathPrefix + "PhiNodeHead.facts";
		String f_assignLocal         = pathPrefix + "AssignLocal.facts";
		String f_assignCast          = pathPrefix + "AssignCast.facts";
		String f_loadInstance        = pathPrefix + "LoadInstanceField.facts";
 		String f_storeInstance       = pathPrefix + "StoreInstanceField.facts";
		String f_unresolvedCall      = pathPrefix + "UnresolvedCall.facts";
		String f_resolvedCall        = pathPrefix + "ResolvedCall.facts";
		String f_validReturn         = pathPrefix + "ReturnTo.facts";
		String f_instructionInMethod = pathPrefix + "Instruction_Method.facts";

		Set<String[]> lines;

		Map<String, PhiInstruction> idToPhiInstr = new HashMap<>();
		lines = InputReader.readFile(f_phiNode);
		for (String[] line : lines) {
			String id = line[0], headId = line[1];

			PhiInstruction phi = idToPhiInstr.get(headId);
			if (phi == null) {
				phi = new PhiInstruction(headId);
				idToPhiInstr.put(headId, phi);
				idToInstr.put(headId, phi);
			}
			idToPhiInstr.put(id, phi);
			idToInstr.put(id, phi);
		}

		// AssignLocal and AssignCast are treated in the same way
		lines = InputReader.readFile(f_assignLocal);
		lines.addAll(InputReader.readFile(f_assignCast));
		for (String[] line : lines) {
			String id = line[0], fromVar = line[2], toVar = line[3];

			// Assign of a Phi node
			PhiInstruction phi = idToPhiInstr.get(id);
			if (phi != null) {
				phi.addToVarToPhi(toVar);
				phi.addFromVarToPhi(fromVar);
			}
			// Normal move
			else {
				idToInstr.put(id, new MoveInstruction(id, toVar, fromVar));
			}
		}
		idToPhiInstr = null;

		lines = InputReader.readFile(f_loadInstance);
		for (String[] line : lines) {
			String id = line[0], toVar = line[2], baseVar = line[3], fld = line[4];

			idToInstr.put(id, new LoadInstruction(id, toVar, baseVar, fld));
		}

		lines = InputReader.readFile(f_storeInstance);
		for (String[] line : lines) {
			String id = line[0], fromVar = line[2], baseVar = line[3], fld = line[4];

			idToInstr.put(id, new StoreInstruction(id, baseVar, fld, fromVar));
		}

		lines = InputReader.readFile(f_unresolvedCall);
		for (String[] line : lines) {
			String id = line[0];

			idToInstr.put(id, new UnresolvedCallInstruction(id));
		}

		lines = InputReader.readFile(f_resolvedCall);
		for (String[] line : lines) {
			String toMethod = line[0], id = line[1];

			idToInstr.put(id, new ResolvedCallInstruction(id, toMethod));
		}

		lines = InputReader.readFile(f_validReturn);
		for (String[] line : lines) {
			String var = line[0], callerId = line[1], retVar = line[2], returnId = line[3];
			if (var.equals("@nil")) {
				var = null;
				retVar = null;
			}

			ResolvedCallInstruction caller = (ResolvedCallInstruction) idToInstr.get(callerId);
			caller.setResultVar(var);

			ReturnInstruction returnInstr = (ReturnInstruction) idToInstr.get(returnId);
			if (returnInstr == null) {
				returnInstr = new ReturnInstruction(returnId, retVar);
				idToInstr.put(returnId, returnInstr);
			}

			Set<ResolvedCallInstruction> callers = calleeToCallers.get(returnInstr);
			if (callers == null) {
				callers = new HashSet<>();
			}
			callers.add(caller);
			calleeToCallers.put(returnInstr, callers);
		}

		lines = InputReader.readFile(f_instructionInMethod);
		for (String[] line : lines) {
			String id = line[0], method = line[1];

			Instruction instr = idToInstr.get(id);
			// Any instruction not handled so far, is deemed to be irrelevant
			if (instr == null) {
				instr = new IrrelevantInstruction(id);
				idToInstr.put(id, instr);
			}
			List<Instruction> list = methodToInstr.get(method);
			if (list == null) {
				list = new ArrayList<>();
			}
			list.add(instr);
			methodToInstr.put(method, list);
		}


		////////////////// CFG //////////////////
		String f_successor           = pathPrefix + "Successor.facts";
		String f_firstInMethod       = pathPrefix + "FirstInstructionInMethod.facts";
		String f_actualToFormal      = pathPrefix + "ActualToFormalArg.facts";
		String f_rootMethod          = pathPrefix + "RootMethod.facts";
		String f_allocationVar       = pathPrefix + "AssignHeapAllocationVar.facts";

		lines = InputReader.readFile(f_successor);
		for (String[] line : lines) {
			String nextInstrId = line[0], prevInstrId = line[1];

			Instruction nextInstr = idToInstr.get(nextInstrId);
			Instruction prevInstr = idToInstr.get(prevInstrId);
			assert nextInstr != null && prevInstr != null;

			// When handling an instruction that's part of a Phi node multiple
			// assign local exist but all correspond to the same Phi.
			// nextInstrId will map to the Phi instruction, and as a result
			// prevInstr and nextInstr will be the same. We ignore successor
			// relations amongst all of them.
			if (prevInstr.compareTo(nextInstr) != 0) {
				List<Instruction> prevInstructions = instrToPrevious.get(nextInstr);
				if (prevInstructions == null) {
					prevInstructions = new ArrayList<>();
				}
				prevInstructions.add(prevInstr);
				instrToPrevious.put(nextInstr, prevInstructions);

				List<Instruction> nextInstructions = instrToNext.get(prevInstr);
				if (nextInstructions == null) {
					nextInstructions = new ArrayList<>();
				}
				nextInstructions.add(nextInstr);
				instrToNext.put(prevInstr, nextInstructions);
			}
		}

		lines = InputReader.readFile(f_firstInMethod);
		for (String[] line : lines) {
			String id = line[0], method = line[1];

			Instruction instr = idToInstr.get(id);
			assert instr != null;
			firstInstructions.put(method, instr);
		}

		lines = InputReader.readFile(f_actualToFormal);
		for (String[] line : lines) {
			String actual = line[0], id = line[1], formal = line[2];

			ResolvedCallInstruction instr = (ResolvedCallInstruction) idToInstr.get(id);
			// Can be null on weird cases with native implicit method calls
			// <java.io.FileSystem: java.io.FileSystem getFileSystem()>/native <java.io.UnixFileSystem: void <init>()>
			// <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>/native <java.security.PrivilegedExceptionAction: java.lang.Object run()>
			if (instr != null) {
				Map<String, List<String>> mapping = actualToFormal.get(instr);
				if (mapping == null) {
					mapping = new HashMap<>();
				}
				List<String> formalList = mapping.get(actual);
				if (formalList == null) {
					formalList = new ArrayList<>();
				}
				formalList.add(formal);
				mapping.put(actual, formalList);
				actualToFormal.put(instr, mapping);
			}
		}

		lines = InputReader.readFile(f_rootMethod);
		for (String[] line : lines) {
			String method = line[0];

			rootMethods.add(method);
		}

		lines = InputReader.readFile(f_allocationVar);
		for (String[] line : lines) {
			String var = line[0], inMethod = line[1];

			allocationVars.add(var);
			Set<String> otherVars = methodToAllocationVars.get(inMethod);
			if (otherVars == null) {
				otherVars = new HashSet<>();
			}
			otherVars.add(var);
			methodToAllocationVars.put(inMethod, otherVars);
		}
	}
}
