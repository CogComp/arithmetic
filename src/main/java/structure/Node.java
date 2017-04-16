package structure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Node implements Serializable {
	
	private static final long serialVersionUID = -1127009463482561785L;
	public String label;
	public List<Node> children;
	public int quantIndex; // Assign this to max_quant+1 for question
	public QuantSpan qs;
	public double val;
	public int infRuleType;
	public String key;
	
	public Node() {
		children = new ArrayList<>();
	}

	// For internal nodes
	public Node(String label, List<Node> children) {
		this();
		this.label = label;
		this.children = children;
		this.infRuleType = -1;
	}
	
	// For leaves
	public Node(int quantindex, QuantSpan qs, String label) {
		this();
		this.quantIndex = quantindex;
		this.qs = qs;
		this.label = label;
		this.infRuleType = -1;
	}

	public Node(Node other) {
		this();
		this.quantIndex = other.quantIndex;
		this.qs = other.qs;
		this.label = other.label;
		this.val = other.val;
		this.infRuleType = other.infRuleType;
		this.key = other.key;
		for(Node child : other.children) {
			this.children.add(new Node(child));			
		}
	}
	
	@Override
	public String toString() {
		if(children.size() == 0) return "" + qs.val;
		String labelSym = null;
		switch(label) {
		case "ADD" : labelSym = "+"; break;
		case "SUB" : labelSym = "-"; break;
		case "MUL" : labelSym = "*"; break;
		case "DIV" : labelSym = "/"; break;
		}
		return "("+children.get(0).toString() + " " +
				label + "_" +
				infRuleType + "_" +
				key + " " +
				children.get(1).toString()+")";
	}

	public String toStringForPython() {
		if(children.size() == 0) return "" + qs.val + "||" + quantIndex;
		String labelSym = null;
		switch(label) {
			case "ADD" : labelSym = "+"; break;
			case "SUB" : labelSym = "-"; break;
			case "MUL" : labelSym = "*"; break;
			case "DIV" : labelSym = "/"; break;
		}
		return "("+children.get(0).toStringForPython() + labelSym +
				children.get(1).toStringForPython()+")";
	}

	public List<Node> getLeaves() {
		List<Node> leaves = new ArrayList<Node>();
		if(children.size() == 0) {
			leaves.add(this);
		} else {
			leaves.addAll(children.get(0).getLeaves());
			leaves.addAll(children.get(1).getLeaves());
		}
		return leaves;
	}
	
	public List<Node> getAllSubNodes() {
		List<Node> all = new ArrayList<Node>();
		all.add(this);
		if(children.size() == 2) {
			all.addAll(children.get(0).getAllSubNodes());
			all.addAll(children.get(1).getAllSubNodes());
		}
		return all;
	}
	
	// Input is quantIndex
	public boolean hasLeaf(int index) {
		if(label.equals("NUM")) {
			if(quantIndex == index) return true;
			else return false;
		} else {
			return (children.get(0).hasLeaf(index) || 
					children.get(1).hasLeaf(index));
		}
	}
	
	// Input are quantIndices
	public String findLabelofLCA(int i, int j) {
		for(Node node : getAllSubNodes()) {
			if(!node.label.equals("NUM") && node.children.get(0).hasLeaf(i)
					&& node.children.get(1).hasLeaf(j)) {
				return node.label;
			}
			if((node.label.equals("SUB") || node.label.equals("DIV")) 
					&& node.children.get(0).hasLeaf(j)
					&& node.children.get(1).hasLeaf(i)) {
				return node.label+"_REV";
			}
			if((node.label.equals("ADD") || node.label.equals("MUL")) 
					&& node.children.get(0).hasLeaf(j)
					&& node.children.get(1).hasLeaf(i)) {
				return node.label;
			}
		}
		return "NONE";
	}
	
	// Input are quantIndices
	public Node findLCAnode(int i, int j) {
		for(Node node : getAllSubNodes()) {
			if(!node.label.equals("NUM") && node.children.get(0).hasLeaf(i)
					&& node.children.get(1).hasLeaf(j)) {
				return node;
			}
			if((node.label.equals("SUB") || node.label.equals("DIV")) 
					&& node.children.get(0).hasLeaf(j)
					&& node.children.get(1).hasLeaf(i)) {
				return node;
			}
			if((node.label.equals("ADD") || node.label.equals("MUL")) 
					&& node.children.get(0).hasLeaf(j)
					&& node.children.get(1).hasLeaf(i)) {
				return node;
			}
		}
		return null;
	}
	
	// Input is quantIndex
	public String findRelevanceLabel(int i) {
		for(Node node : getLeaves()) {
			if(node.quantIndex == i) {
				return "REL";
			}
		}
		return "IRR";
	}
	
	public double getValue() {
		if(label.equals("NUM")) return qs.val;
		if(label.equals("ADD")) return children.get(0).getValue() + 
				children.get(1).getValue();
		if(label.equals("SUB")) return children.get(0).getValue() - 
						children.get(1).getValue();
		if(label.equals("MUL")) return children.get(0).getValue() * 
				children.get(1).getValue();
		if(label.equals("DIV")) return children.get(0).getValue() / 
				children.get(1).getValue();
		return 0.0;
	}
	
	public boolean isEqual(Node node) {
		if(!label.equals(node.label)) {
			return false;
		}
		if(label.equals("NUM")) {
			if(quantIndex == node.quantIndex) {
				return true;
			}
			else {
				return false;
			}
		}
		if(label.equals("ADD") || label.equals("MUL")) {
			return (children.get(0).isEqual(node.children.get(0)) && children.get(1).isEqual(node.children.get(1))) ||
					(children.get(0).isEqual(node.children.get(1)) && children.get(1).isEqual(node.children.get(0)));
		}
		return (children.get(0).isEqual(node.children.get(0)) && children.get(1).isEqual(node.children.get(1)));
	}
	
	public static Node parseNode(String eqString) {
		eqString = eqString.trim();
//		System.out.println("EqString : "+eqString);
		int index = eqString.indexOf("=");
		if(index > 0) eqString = eqString.substring(index+1).trim();
		if(eqString.charAt(0)=='(' && eqString.charAt(eqString.length()-1)==')') {
			eqString = eqString.substring(1, eqString.length()-1).trim();
		}
		index = indexOfMathOp(eqString, Arrays.asList('+', '-', '*', '/'));
		Node node = new Node();
		if(index > 0) {
			if(eqString.charAt(index) == '+') node.label = "ADD";
			else if(eqString.charAt(index) == '-') node.label = "SUB";
			else if(eqString.charAt(index) == '*') node.label = "MUL";
			else if(eqString.charAt(index) == '/') node.label = "DIV";
			else node.label = "ISSUE";
			node.children.add(parseNode(eqString.substring(0, index)));
			node.children.add(parseNode(eqString.substring(index+1)));
			return node;
		} else {
			node.label = "NUM";
			node.val = Double.parseDouble(eqString.trim());
		}
		return node;
	}
	
	public static int indexOfMathOp(String equationString, List<Character> keys) {
		for(int index=0; index<equationString.length(); ++index) {
			if(keys.contains(equationString.charAt(index))) {
				int open = 0, close = 0;
				for(int i=index; i>=0; --i) {
					if(equationString.charAt(i) == ')') close++;
					if(equationString.charAt(i) == '(') open++;
				}
				if(open==close) {
					return index;
				}
			}
		}
		return -1;
	}
	
	public Node getParent(Node child) {
		for(Node node : getAllSubNodes()) {
			if(node.children.size() == 2 && (node.children.get(0) == child 
					|| node.children.get(1) == child)) {
				return node;
			}
		}
		return null;
	}
	
	public List<String> getPath(int i, int j) {
		Node n1 = null, n2 = null;
		for(Node leaf : getLeaves()) {
			if(leaf.quantIndex == i) {
				n1 = leaf;
			}
		}
		for(Node leaf : getLeaves()) {
			if(leaf.quantIndex == j) {
				n2 = leaf;
			}
		}
		List<String> path1 = new ArrayList<>();
		List<String> path2 = new ArrayList<>();
		Node lcaNode = findLCAnode(i, j);
		Node parent = null;
		String postFix = "";
		while(n1 != lcaNode && n1 != null) {
			postFix = "";
			parent = getParent(n1);
			if(parent.label.equals("SUB") || parent.label.equals("DIV")) {
				if(parent.children.get(1) == n1) {
					postFix = "_REV";
				}
			}
			n1 = parent;
			path1.add(n1.label+postFix);
		}
		while(n2 != lcaNode && n2 != null) {
			postFix = "";
			parent = getParent(n2);
			if(parent.label.equals("SUB") || parent.label.equals("DIV")) {
				if(parent.children.get(1) == n2) {
					postFix = "_REV";
				}
			}
			n2 = parent;
			path2.add(n2.label+postFix);
		}
		Collections.reverse(path2);
		path2.remove(0);
		path1.addAll(path2);
		return path1;
	}
	
	public List<String> getPathToRoot(int i) {
		Node n = null;
		for(Node leaf : getLeaves()) {
			if(leaf.quantIndex == i) {
				n = leaf;
			}
		}
		List<String> path = new ArrayList<>();
		Node parent = null;
		String postFix = "";
		while(true) {
			postFix = "";
			parent = getParent(n);
			if(parent == null) break;
			if(parent.label.equals("SUB") || parent.label.equals("DIV")) {
				if(parent.children.get(1) == n) {
					postFix = "_REV";
				}
			}
			n = parent;
			path.add(n.label+postFix);
		}
		return path;
	}

	public static float getLoss(Node node1, Node node2) {
		if(node1.children.size() != node2.children.size()) {
			return 1.0f;
		}
		if(node1.children.size() == 0) {
			if(node1.quantIndex == node2.quantIndex) {
				return 0.0f;
			} else {
				return 1.0f;
			}
		}
		if(!node1.label.equals(node2.label)) {
			return 1.0f;
		}
		if(node1.infRuleType != node2.infRuleType) {
			return 1.0f;
		}
		if(node1.key != null && node2.key != null && !node1.key.equals(node2.key)) {
			return 1.0f;
		}
		float loss;
		if(node1.label.equals("ADD") || node1.label.equals("MUL")) {
			loss = Math.min(
					getLoss(node1.children.get(0), node2.children.get(0)) +
							getLoss(node1.children.get(1), node2.children.get(1)),
					getLoss(node1.children.get(0), node2.children.get(1)) +
							getLoss(node1.children.get(1), node2.children.get(0)));
		} else {
			loss = getLoss(node1.children.get(0), node2.children.get(0)) +
					getLoss(node1.children.get(1), node2.children.get(1));
		}
		if(loss > 0.5) {
			return 1.0f;
		}
		return 0.0f;
	}
}
