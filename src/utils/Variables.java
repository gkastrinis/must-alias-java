package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import analysis.AliasNode;
import analysis.CSInstruction;

public class Variables {
	public final Map<CSInstruction, Set<String>> varsFromCallerToCallee;
	
	public Variables() {
		varsFromCallerToCallee = new HashMap<CSInstruction, Set<String>>();
	}
	
	@Override
	public String toString() {
		String ret = "";
		Set<Entry<CSInstruction, Set<String>>> entrySet = new HashSet<>(varsFromCallerToCallee.entrySet());
		for (Entry<CSInstruction, Set<String>> e : entrySet) {
			ret += e.getKey().val.toString() + " -> " + e.getValue().toString();
		}
		return ret;
	}
}
