package analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import utils.StringJoiner;

public class AliasNode {
	Set<String> _vars;
	Map<String, AliasNode> _labelToNode;   // labeled outgoing edge
	Map<String, AliasNode> _labelFromNode; // labeled incoming edge

	public AliasNode() {
		_vars          = new HashSet<>();
		_labelToNode   = new HashMap<>();
		_labelFromNode = new HashMap<>();
	}

	public boolean isEmpty() {
		return _vars.isEmpty();
	}

	public Set<String> getVariables() {
		return _vars;
	}

	public void addVariable(String variable) {
		_vars.add(variable);
	}

	public void removeVariable(String variable) {
		_vars.remove(variable);
	}

	public void addOutEdge(String label, AliasNode node) {
		this._labelToNode.put(label, node);
		node._labelFromNode.put(label, this);
	}

	public void removeOutEdge(String label) {
		AliasNode toNode = this._labelToNode.remove(label);
		if (toNode != null) {
			toNode._labelFromNode.remove(label);
		}
	}

	public void removeInEdge(String label) {
		AliasNode fromNode = this._labelFromNode.remove(label);
		if (fromNode != null) {
			fromNode._labelToNode.remove(label);
		}
	}

	public Map<String, AliasNode> getOutEdgeMap() {
		return _labelToNode;
	}

	public Map<String, AliasNode> getInEdgeMap() {
		return _labelFromNode;
	}

	String varsToString() {
		StringJoiner joiner = new StringJoiner(",");
		for (String var : _vars) joiner.add(var);
		return "{ " + joiner.toString() + " }";
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner("\n");
		joiner.add(varsToString());
		for (Entry<String, AliasNode> outEdge : _labelToNode.entrySet()) {
			joiner.add("--" + outEdge.getKey() + "--> " + outEdge.getValue().varsToString());
		}
		for (Entry<String, AliasNode> inEdge : _labelFromNode.entrySet()) {
			joiner.add("<--" + inEdge.getKey() + "-- " + inEdge.getValue().varsToString());
		}
		joiner.add("\n");
		return joiner.toString();
	}

	public int printNodeMustAliasPairs(BufferedWriter br, String instruction) throws IOException {
		int num = 0;
		Set<String> nodeVars = new HashSet<>(_vars);
		for (String var: _vars) {
			nodeVars.remove(var);
			for (String var2: nodeVars) {
				br.write(instruction + "\t" + var + "\t" + var2 + "\n");
				num++;
			}
			if (nodeVars.size() <= 1) break;
		}
		return num;
	}

	public void printNodeOutEdges(BufferedWriter br, String instruction, int nodeID1) throws IOException {
		for (Entry<String, AliasNode> labelAndNode: _labelToNode.entrySet()) {
			int nodeID2 = labelAndNode.getValue().getNodeId();
			String label = labelAndNode.getKey();
			br.write(instruction + "\t" + nodeID1 + "\t" + nodeID2 + "\t" + label + "\n");
		}
	}

	public static AliasNode intersectVars(AliasNode n1, AliasNode n2) {
		AliasNode newOne = new AliasNode();
		boolean set1IsLarger = n1._vars.size() > n2._vars.size();
		newOne._vars.addAll(set1IsLarger ? n2._vars : n1._vars);
		newOne._vars.retainAll(set1IsLarger ? n1._vars : n2._vars);
		return newOne;
	}

	public static AliasNode cloneVarsOnly(AliasNode other) {
		AliasNode newOne = new AliasNode();
		newOne._vars = new HashSet<>(other._vars);
		return newOne;
	}

	public static AliasNode cloneAndMarkVarsOnly(AliasNode other) {
		AliasNode newOne = new AliasNode();
		newOne._vars = new HashSet<>();
		for (String var : other._vars)
			newOne._vars.add("[" + var + "]");
		return newOne;
	}

	public int getNodeId() {
		return _vars.hashCode();
	}
}
