package analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import utils.OutputWriter;
import utils.Variables;
import input.*;

public class MustAnalysis {
	Facts                          _facts;
	Set<CSInstruction>             _reachableInstr;
	Set<String>                    _reachableMethods;
	Map<CSInstruction, AliasGraph> _instrToGraph;
	Variables                      _calleeToVariables;

	Map<CSInstruction, Set<CSInstruction>> _csinstrToPrev;
	Map<CSInstruction, Set<CSInstruction>> _csinstrToNext;
	Map<CSInstruction, Set<CSInstruction>> _fromFirstCSInstrToAll;

	public MustAnalysis(String inputFactsDir) throws IOException {
		_facts           	= new Facts(inputFactsDir);
		_reachableInstr  	= new TreeSet<>();
		_reachableMethods	= new HashSet<>(_facts.rootMethods);
		_instrToGraph     	= new HashMap<>();
		_calleeToVariables	= new Variables();

		_csinstrToPrev = new HashMap<>();
		_csinstrToNext = new HashMap<>();
		_fromFirstCSInstrToAll = new HashMap<>();

		for (String method : _facts.rootMethods) {
			Map<Instruction, CSInstruction> mapping = new HashMap<>();
			for (Instruction i : _facts.methodToInstr.get(method)) {
				CSInstruction csinstr = new CSInstruction(i);
				mapping.put(i, csinstr);
				_reachableInstr.add(csinstr);
				_instrToGraph.put(csinstr, new AliasGraph());
			}
			gatherPrevAndNext(mapping);
		}
	}

	void gatherPrevAndNext(Map<Instruction, CSInstruction> mapping) {
		for (CSInstruction csinstr : mapping.values()) {
			List<Instruction> prevSet = _facts.instrToPrevious.get(csinstr.val);
			if (prevSet != null) {
				Set<CSInstruction> csPrevSet = new HashSet<>();
				for (Instruction prev : prevSet) {
					CSInstruction csPrev = mapping.get(prev);
					csPrevSet.add(csPrev);
				}
				_csinstrToPrev.put(csinstr, csPrevSet);
			}

			List<Instruction> nextSet = _facts.instrToNext.get(csinstr.val);
			if (nextSet != null) {
				Set<CSInstruction> csNextSet = new HashSet<>();
				for (Instruction next : nextSet) {
					CSInstruction csNext = mapping.get(next);
					csNextSet.add(csNext);
				}
				_csinstrToNext.put(csinstr, csNextSet);
			}
		}
	}

	static final boolean exportMustPointsTo = true;
	static final boolean exportMustAliasPairs = false;
	static final boolean exportAccessPathPairs = false;

	Set<CSInstruction> _newlyReachable;
	Set<CSInstruction> _currentCallInstructions;
	Set<CSInstruction> _nextInstructions;

	// highly inefficient in many ways, and currently not really up to fixpoint
	//  - replaces the old graphs every time, does not just add deltas
	//  - does not optimize order of instructions to reach fixpoint
	public void applyToFixpoint() {
		Set<CSInstruction> instructionsToProcess = _reachableInstr;
		for (int loop = 0; !instructionsToProcess.isEmpty() ; loop++) {
			//System.out.println("\u001B[33m"+"-- APPLY to Fixpoint loop: " + loop + " -- R: " + _reachableInstr.size() + ", toProcess: " + instructionsToProcess.size() + "\u001B[0m");
			Set<CSInstruction> deltaInstructions = new TreeSet<>();
			_newlyReachable = new TreeSet<>();

			for (CSInstruction csinstr : instructionsToProcess) {
				Instruction instr = csinstr.val;
				List<Instruction> currentCtx = csinstr.context;
				AliasGraph g = _instrToGraph.get(csinstr);
				AliasGraph oldGraph = new AliasGraph(g);
				_nextInstructions = new TreeSet<>();

				// Don't propagate information from predecessors in stores and unresolved calls
				if (instr.kind != Instruction.Kind.STORE_INSTANCE && instr.kind != Instruction.Kind.UNRESOLVED_CALL) {

					//don't copy graph from call instruction to callee
					if (instr.kind == Instruction.Kind.RESOLVED_CALL)
						g = new AliasGraph();

					Set<CSInstruction> predecessors = _csinstrToPrev.get(csinstr);
					if (predecessors != null) {
						if (predecessors.size() == 1) {
							Iterator<CSInstruction> curAncestor = predecessors.iterator();
							AliasGraph predecessorGraph = _instrToGraph.get(curAncestor.next());
							g = new AliasGraph(predecessorGraph);
						}
						else if (predecessors.size() > 1) {
							Iterator<CSInstruction> curAncestor = predecessors.iterator();
							CSInstruction firstAncestor = curAncestor.next();
							CSInstruction secondAncestor = curAncestor.next();
							AliasGraph currentGraph = new AliasGraph();
							currentGraph.intersect(_instrToGraph.get(firstAncestor), _instrToGraph.get(secondAncestor));
							while (curAncestor.hasNext()) {
								AliasGraph newGraph = new AliasGraph();
								newGraph.intersect(currentGraph, _instrToGraph.get(curAncestor.next()));
								currentGraph = newGraph;
							}
							g = currentGraph;
						}
					}
				}

				boolean hasChange = applyInstruction(csinstr, g);
				//boolean areEqual = g.equals(oldGraph);
				boolean origHasChange = hasChange;
				//if (instr.kind != Instruction.Kind.RESOLVED_CALL && hasChange) {
				//	hasChange = !areEqual;
				//}
				//else if (instr.kind == Instruction.Kind.RESOLVED_CALL) {
				//	hasChange = true;
				//}
				if (!hasChange && instr.kind != Instruction.Kind.RESOLVED_CALL) {
				//else if (instr.kind != Instruction.Kind.RESOLVED_CALL) {
					boolean areEqual = g.equals(oldGraph);
					hasChange = !areEqual;
				}

				if (hasChange) {
					Set<CSInstruction> nextSet = _csinstrToNext.get(csinstr);
					if (nextSet != null && instr.kind != Instruction.Kind.RESOLVED_CALL)
						_nextInstructions.addAll(nextSet);

					deltaInstructions.addAll(_nextInstructions);
				}
			}
			instructionsToProcess = deltaInstructions;
			_reachableInstr.addAll(_newlyReachable);
		}
		System.out.println("-- DONE --");
	}

	// Apply instruction semantics on the graph right before the instruction
	// and return whether there was any change
	boolean applyInstruction(CSInstruction csinstr, AliasGraph graph) {
		switch (csinstr.val.kind) {
			case MOVE:
				return handleMoveInstruction(csinstr, graph);
			case PHI:
				return handlePhiInstruction(csinstr, graph);
			case LOAD_INSTANCE:
				return handleLoadInstanceInstruction(csinstr, graph);
			case STORE_INSTANCE:
				return handleStoreInstanceInstruction(csinstr, graph);

			case RESOLVED_CALL:
				return handleResolvedCall(csinstr, graph);
			case RETURN:
				return handleReturn(csinstr, graph);

			case IRRELEVANT:
				_instrToGraph.put(csinstr, graph);
				return false;

			case UNRESOLVED_CALL:
				// An unresolved call invalidates everything
				if (graph.getNodes().isEmpty())
					return false;
				else {
					_instrToGraph.put(csinstr, new AliasGraph());
					return true;
				}
			default:
				System.out.println("!!!!!!!!!!!!!!!!!!!!");
				assert false;
				return false;
		}
	}

	boolean handleMoveInstruction(CSInstruction csinstr, AliasGraph graph) {
		_instrToGraph.put(csinstr, graph);
		MoveInstruction instr = (MoveInstruction) csinstr.val;

		AliasNode right = graph.lookupVar(instr.fromVar);
		if (right == null) {
			right = new AliasNode();
			right.addVariable(instr.fromVar);
			graph.addNode(right);
		}
		AliasNode left = graph.lookupVar(instr.toVar);
		// from and to are already in the same node
		if (left == right) {
			return false;
		}

		if (left != null) {
			graph.removeVariableFromNode(left, instr.toVar);
		}
		graph.addVariableToNode(right, instr.toVar);

		graph.gcNodes();
		return true;
	}

	boolean handlePhiInstruction(CSInstruction csinstr, AliasGraph graph) {
		_instrToGraph.put(csinstr, graph);
		PhiInstruction instr = (PhiInstruction) csinstr.val;

		Iterator<String> curElem = instr.phiVars.iterator();
		AliasNode firstNode = graph.lookupVar(curElem.next());
		if (firstNode == null) {
			return false;
		}
		AliasNode resultNode = firstNode;

		boolean hasChange = false;
		while (curElem.hasNext()) {
			AliasNode curNode = graph.lookupVar(curElem.next());
			// only two cases: either the from vars of the phi instruction are all in
			// the same node, or they are in disjoint nodes, so there are no produced
			// aliases. But the resulting toVar node needs to be computed because aliased
			// access paths may be starting from it.
			if (firstNode != curNode) {
				resultNode = new AliasNode(); // empty intersection of fromVar nodes!
				hasChange = true;
				break;
			}
		}

		AliasNode left = graph.lookupVar(instr.toVar);
		// from and to are already in the same node
		if (left == resultNode) {
			return false;
		}

		if (left != null) {
			graph.removeVariableFromNode(left, instr.toVar);
		}

		if (firstNode == resultNode)
			graph.addVariableToNode(resultNode, instr.toVar);

		graph.gcNodes();
		return true;
	}

	boolean handleLoadInstanceInstruction(CSInstruction csinstr, AliasGraph graph) {
		_instrToGraph.put(csinstr, graph);
		LoadInstruction instr = (LoadInstruction) csinstr.val;

		AliasNode base = graph.lookupVar(instr.baseVar);
		if (base == null) {
			base = new AliasNode();
			base.addVariable(instr.baseVar);
			graph.addNode(base);
		}
		AliasNode node = base.getOutEdgeMap().get(instr.fld);
		if (node == null) {
			node = new AliasNode();
			graph.addNode(node);
			base.addOutEdge(instr.fld, node);
		}
		AliasNode left = graph.lookupVar(instr.toVar);
		// base.fld and to are already in the same node
		if (left == node) {
			int numNodesBefore = graph.getNodes().size();
			graph.gcNodes();
			return (numNodesBefore != graph.getNodes().size());
		}

		if (left != null) {
			graph.removeVariableFromNode(left, instr.toVar);
		}
		graph.addVariableToNode(node, instr.toVar);

		graph.gcNodes();
		return true;
	}

	boolean handleStoreInstanceInstruction(CSInstruction csinstr, AliasGraph graph) {
		_instrToGraph.put(csinstr, graph);
		StoreInstruction instr = (StoreInstruction) csinstr.val;

		//assert graph.sanityEdges2way();

		AliasNode base = graph.lookupVar(instr.baseVar);
		if (base == null) {
			base = new AliasNode();
			base.addVariable(instr.baseVar);
			graph.addNode(base);
		}

		AliasNode right = graph.lookupVar(instr.fromVar);
		if (right == null) {
			right = new AliasNode();
			right.addVariable(instr.fromVar);
			graph.addNode(right);
		}

		AliasNode oldNode = base.getOutEdgeMap().get(instr.fld);
		// base.fld and from are already in the same node
		if (oldNode == right) {
			return false;
		}

		base.removeOutEdge(instr.fld);
		base.addOutEdge(instr.fld, right);

		graph.gcNodes();
		return true;
	}

	// A call instruction doesn't affect its graph directly. This will
	// happen from a subsequent return instruction.
	boolean handleResolvedCall(CSInstruction csinstr, AliasGraph graph) {
		ResolvedCallInstruction instr = (ResolvedCallInstruction) csinstr.val;
		List<Instruction> currentCtx = csinstr.context;
		List<Instruction> methodInstructions = _facts.methodToInstr.get(instr.toMethod);
		Instruction firstInMethod = _facts.firstInstructions.get(instr.toMethod);

		boolean hasChange = false;

		_currentCallInstructions = new HashSet<>();
		if (currentCtx.size() < CSInstruction.MAX_CONTEXT_DEPTH && methodInstructions != null) {
			List<Instruction> newCtx = new ArrayList<>(currentCtx);
			newCtx.add(0, instr);

			if (!_reachableMethods.contains(instr.toMethod)) {
				_reachableMethods.add(instr.toMethod);
				hasChange = true;
			}

			CSInstruction firstCSInstr = new CSInstruction((Instruction) firstInMethod.clone(), newCtx);
			Set<CSInstruction> csInstructions = _fromFirstCSInstrToAll.get(firstCSInstr);
			// not reachable under newCtx
			if (csInstructions == null) {
				csInstructions = new HashSet<>();
				Map<Instruction, CSInstruction> mapping = new HashMap<>();
				for(Instruction curInstruction : methodInstructions) {
					CSInstruction newInstruction = new CSInstruction((Instruction) curInstruction.clone(), newCtx);
					mapping.put(curInstruction, newInstruction);
					csInstructions.add(newInstruction);
					_newlyReachable.add(newInstruction);
					_nextInstructions.add(newInstruction);
					_instrToGraph.put(newInstruction, new AliasGraph());
				}
				_fromFirstCSInstrToAll.put(firstCSInstr, csInstructions);
				gatherPrevAndNext(mapping);
				hasChange = true;
			}
			_currentCallInstructions.addAll(csInstructions);

			// Don't propagate information from predecessors in stores and unresolved calls
			AliasGraph newGraph;
			if (firstCSInstr.val.kind != Instruction.Kind.STORE_INSTANCE && firstCSInstr.val.kind != Instruction.Kind.UNRESOLVED_CALL) {
				newGraph = new AliasGraph(graph, true);
				Map<String, List<String>> argMapping = _facts.actualToFormal.get(instr);
				// Has (non-primitive) args
				if (argMapping != null) {
					newGraph.varsOnCall(argMapping);
				}
				AliasGraph oldGraph = _instrToGraph.get(firstCSInstr);
				newGraph.union(oldGraph);
			}
			else {
				newGraph = new AliasGraph();
			}

			_instrToGraph.put(firstCSInstr, newGraph);
			return hasChange;
		}
		else {
			// Keep all aliases from local variables and discard all from heap
			AliasGraph newGraph = new AliasGraph(graph);
			newGraph.removeHeapAliases();
			newGraph.gcNodes();
			_instrToGraph.put(csinstr, newGraph);
			return true;
		}
	}

	boolean handleReturn(CSInstruction csinstr, AliasGraph graph) {
		_instrToGraph.put(csinstr, graph);
		ReturnInstruction instr = (ReturnInstruction) csinstr.val;
		List<Instruction> currentCtx = csinstr.context;

		if (currentCtx.isEmpty())
			return false;

		List<Instruction> callerCtx = new ArrayList<>(currentCtx);
		Instruction caller = callerCtx.get(0);
		callerCtx.remove(0);
		CSInstruction csCaller = new CSInstruction(caller, callerCtx);

		boolean hasChange = false;
		AliasGraph newGraph = new AliasGraph(graph);

		//	// Rename arguments (reverse mapping)
		//	Map<String, List<String>> origMapping = _facts.actualToFormal.get(caller);
		//	// Has (non-primitive) args
		//	if (origMapping != null) {
		//		for (Entry<String, List<String>> origMappingEntry : origMapping.entrySet()) {
		//			String actualVar = origMappingEntry.getKey();
		//			// We don't care about the case of multiple actuals applying to the
		//			// same formal. Either they will continue to be completely aliased
		//			// at the point of the return instruction, or they won't be at all,
		//			// due to SSA (no further assignment to locals).
		//			for (String formalVar : origMappingEntry.getValue()) {
		//				if (mapping.containsKey(formalVar)) {
		//					List<String> tempList = new ArrayList<>();
		//					tempList.addAll(mapping.get(formalVar));
		//					tempList.add(actualVar);
		//					mapping.put(formalVar, tempList);
		//				}
		//				else {
		//					mapping.put(formalVar, Arrays.asList(actualVar));
		//				}
		//			}
		//		}
		//	}

		newGraph.varsOnReturn();
		AliasGraph oldGraph = _instrToGraph.get(csCaller);
		if (!newGraph.equals(oldGraph)) {
			_instrToGraph.put(csCaller, newGraph);
			hasChange = true;
			Set<CSInstruction> nextSet = _csinstrToNext.get(csCaller);
			if (nextSet != null)
				_nextInstructions.addAll(nextSet);
		}

		return hasChange;
	}


	// METRICS
	public int computeMustPointTo(BufferedWriter br) throws IOException {
		// Instruction to Method mapping
		Map<Instruction, String> instrToMethod = new HashMap<>();
		for (Entry<String, List<Instruction>> entry : _facts.methodToInstr.entrySet()) {
			for (Instruction instr : entry.getValue()) {
				instrToMethod.put(instr, entry.getKey());
			}
		}
		// Reachable Allocation Vars with Context
		Set<CSVar> reachableAllocationCSVars = new HashSet<>();
		for (CSInstruction csinstr : _reachableInstr) {
			Instruction instr = csinstr.val;
			String method = instrToMethod.get(instr);
			//if (method.contains("Config"))
			//	System.out.println(csinstr.context);
			Set<String> methodAllocationVars = _facts.methodToAllocationVars.get(method);
			if (methodAllocationVars != null) {
				for (String var : methodAllocationVars) {
					reachableAllocationCSVars.add(new CSVar(var, csinstr.context));
				}
			}
		}

		Set<List<Instruction>> allContexts = new HashSet<>();
		// Var with Context to Node mapping
		Map<CSVar, Set<AliasNode>> csvarToAllNodes = new HashMap<>();
		for (Entry<CSInstruction, AliasGraph> graphEntry : _instrToGraph.entrySet()) {
			CSInstruction csinstr = graphEntry.getKey();
			List<Instruction> context = csinstr.context;
			allContexts.add(context);
			AliasGraph graph = graphEntry.getValue();

			for (Entry<String, AliasNode> entry : graph._varToNode.entrySet()) {
				CSVar csvar = new CSVar(entry.getKey(), context);
				Set<AliasNode> nodesForCSVar = csvarToAllNodes.get(csvar);
				if (nodesForCSVar == null) {
					nodesForCSVar = new HashSet<>();
				}
				nodesForCSVar.add(entry.getValue());
				csvarToAllNodes.put(csvar, nodesForCSVar);
			}
		}

		// Map from a context to all contexts that are expanding it
		Map<List<Instruction>, Set<List<Instruction>>> fromContextToBiggerContexts = new HashMap<>();
		for (List<Instruction> context : allContexts) {
			for (int i = 0 ; i <= ContextSensitive.MAX_CONTEXT_DEPTH ; i++) {
				if (context.size() < i) break;
				List<Instruction> subContext = new ArrayList<>(context.subList(i, context.size()));
				Set<List<Instruction>> biggerContexts = fromContextToBiggerContexts.get(subContext);
				if (biggerContexts == null) {
					biggerContexts = new HashSet<>();
					fromContextToBiggerContexts.put(subContext, biggerContexts);
				}
				biggerContexts.add(subContext);
				biggerContexts.add(context);
			}
				Set<List<Instruction>> biggerContexts = fromContextToBiggerContexts.get(context);
				if (biggerContexts == null) {
					biggerContexts = new HashSet<>();
					fromContextToBiggerContexts.put(context, biggerContexts);
				}
				biggerContexts.add(context);
		}

		Set<CSVar> csvarPointsToAllocation = new HashSet<>(reachableAllocationCSVars);
		Set<CSVar> deltaCSVarPointsToAllocation = new HashSet<>(reachableAllocationCSVars);
		while (!deltaCSVarPointsToAllocation.isEmpty()) {
			Set<CSVar> newDeltaCSVarPointsToAllocation = new HashSet<>();
			for (CSVar heapCSVar : deltaCSVarPointsToAllocation) {
				String heapVar = heapCSVar.val;
				List<Instruction> context = heapCSVar.context;
				Set<List<Instruction>> biggerContexts = fromContextToBiggerContexts.get(context);

				String prefix = "", suffix = "";
			   	for (int i = 0 ; i <= ContextSensitive.MAX_CONTEXT_DEPTH ; i++) {
					String markedHeapVar = prefix + heapVar + suffix;
					if (biggerContexts != null) {
						for (List<Instruction> biggerContext : biggerContexts) {
							if (biggerContext.size() == context.size() + i) {
								CSVar markedCSVar = new CSVar(markedHeapVar, biggerContext);
								Set<AliasNode> nodesForCSVar = csvarToAllNodes.get(markedCSVar);
								if (nodesForCSVar != null) {
									for (AliasNode nodeForCSVar : nodesForCSVar) {
										for (String nodeVar : nodeForCSVar.getVariables()) {
											newDeltaCSVarPointsToAllocation.add(new CSVar(nodeVar, biggerContext));
										}
									}
								}
							}
						}
					}
					prefix += "[";
					suffix += "]";
				}
			}
			newDeltaCSVarPointsToAllocation.removeAll(csvarPointsToAllocation);
			csvarPointsToAllocation.addAll(newDeltaCSVarPointsToAllocation);
			deltaCSVarPointsToAllocation = newDeltaCSVarPointsToAllocation;
		}
		Set<String> resultVars = new HashSet<>();
		for (CSVar csvar : csvarPointsToAllocation) {
			if (!csvar.val.startsWith("[")) {
				resultVars.add(csvar.val);
			}
		}
		for (String var : resultVars) {
			br.write(var + "\n");
		}

		return resultVars.size();
	}

	Set<String> expandVars(Set<String> vars) {
		Set<String> result = new HashSet<>();
		for (String var : vars) {
			String temp = var;
			result.add(temp);
			while (temp.startsWith("[")) {
				temp = temp.substring(1, temp.length() -1);
				result.add(temp);
			}
			result.add(temp);
		}
		return result;
	}

	@Override
	public String toString() {
		String graphs = "";
		for (CSInstruction instruction: _reachableInstr) {
			graphs += instruction + ":\n";
			graphs += _instrToGraph.get(instruction).toString();
		}
		return graphs;
	}

	public void getResults(String dir) throws IOException {
		String path = "out/" + dir;
		BufferedWriter br;

		if (exportMustPointsTo) {
			br = OutputWriter.openOutputFile(path + "MustPointsTo.facts");
			int total = computeMustPointTo(br);
			OutputWriter.closeOutputFile(br);
			System.out.println("#MustPointTo: " + total);
		}
	}
}
