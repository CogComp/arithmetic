package joint;

import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.stanford.nlp.ling.CoreLabel;
import structure.Node;
import structure.StanfordSchema;

import java.util.List;


public class LogicY implements IStructure {
	
	public Node expr;
	
	public LogicY(LogicX x,
				  Node expr,
				  List<Integer> rateAnnotations) {
		this.expr = expr;
		populateInfType(x, expr, true, rateAnnotations);
	}

	public LogicY(Node expr) {
		this.expr = expr;
	}
	
	public LogicY(LogicY other) {
		this.expr = other.expr;
	}
	
	@Override
	public String toString() {
		return expr.toString();
	}
	
	public static float getLoss(LogicY gold, LogicY pred) {
		if(gold == null || pred == null) {
			return 1.0f;
		}
		return getLossForLogic(gold.expr, pred.expr);
	}

	public static float getLossForLogic(Node node1, Node node2) {
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
					getLossForLogic(node1.children.get(0), node2.children.get(0)) +
							getLossForLogic(node1.children.get(1), node2.children.get(1)),
					getLossForLogic(node1.children.get(0), node2.children.get(1)) +
							getLossForLogic(node1.children.get(1), node2.children.get(0)));
		} else {
			loss = getLossForLogic(node1.children.get(0), node2.children.get(0)) +
					getLossForLogic(node1.children.get(1), node2.children.get(1));
		}
		if(loss > 0.5) {
			return 1.0f;
		}
		return 0.0f;
	}

	public void populateInfType(LogicX x,
								Node expr,
								boolean isTopmost,
								List<Integer> rateAnnotations) {
		if(expr.children.size() == 0) {
			expr.infRuleType = -1;
			expr.key = null;
			return;
		}
		populateInfType(x, expr.children.get(0), false, rateAnnotations);
		populateInfType(x, expr.children.get(1), false, rateAnnotations);
		int quantIndex1 = expr.children.get(0).quantIndex;
		int quantIndex2 = expr.children.get(1).quantIndex;
		populateInfType(x, expr, quantIndex1, quantIndex2, isTopmost, rateAnnotations);
	}

	// Here, quantIndex1 is the left child, quantIndex2 is the right child
	public void populateInfType(LogicX x, Node expr, int quantIndex1, int quantIndex2,
								boolean isTopmost, List<Integer> rateAnnotations)  {
		StanfordSchema num1 = x.schema.get(quantIndex1);
		StanfordSchema num2 = x.schema.get(quantIndex2);
		StanfordSchema ques= x.questionSchema;
		int mathIndex = -1;
		String mathToken = null;
		if(num1.math != -1) {
			mathIndex = 1;
			mathToken = x.tokens.get(num1.sentId).get(num1.math).word();
		}
		if(mathIndex == -1 && num2.math != -1) {
			mathIndex = 2;
			mathToken = x.tokens.get(num2.sentId).get(num2.math).word();
		}
		if(mathIndex == -1 && ques.math != -1 && isTopmost) {
			mathIndex = 0;
			mathToken = x.tokens.get(ques.sentId).get(ques.math).word();
		}
		if(mathToken != null) {
			if((expr.label.equals("ADD") || expr.label.equals("SUB")) &&
					(Logic.addTokens.contains(mathToken) ||
							Logic.subTokens.contains(mathToken))) {
				expr.quantIndex = mathIndex == 2 ? quantIndex2 : quantIndex1;
				expr.infRuleType = 2;
				return;
			}
			if((expr.label.equals("MUL") || expr.label.equals("DIV")) &&
					Logic.mulTokens.contains(mathToken)) {
				expr.quantIndex = mathIndex == 2 ? quantIndex2 : quantIndex1;
				expr.infRuleType = 2;
				return;
			}
		}
		if(expr.label.equals("MUL") || expr.label.equals("DIV")) {
			if(rateAnnotations.contains(quantIndex1) &&
					!rateAnnotations.contains(quantIndex2)) {
				expr.quantIndex = quantIndex2;
			} else {
				expr.quantIndex = quantIndex1;
			}
			expr.infRuleType = 3;
			return;
		}
		if(expr.label.equals("ADD") || expr.label.equals("SUB")) {
			if(x.tokens.get(num1.sentId).get(num1.verb).lemma().equals(
					x.tokens.get(num2.sentId).get(num2.verb).lemma())) {
				boolean midVerb = midVerb(x.tokens, num1, num2);
				if(!midVerb) {
					expr.infRuleType = 1;
					expr.quantIndex = quantIndex1;
					return;
				}
			}
			expr.infRuleType = 0;
			expr.quantIndex = quantIndex1;
			return;
		}
	}

	public static boolean midVerb(List<List<CoreLabel>> tokens,
								  StanfordSchema num1,
								  StanfordSchema num2) {
		int start1 = num1.sentId < num2.sentId ? num1.sentId : num2.sentId;
		int start2 = num1.sentId < num2.sentId ? num1.verb : num2.verb;
		int end1 = num1.sentId >= num2.sentId ? num1.sentId : num2.sentId;
		int end2 = num1.sentId >= num2.sentId ? num1.verb : num2.verb;
		for(int i=start1; i<=end1; ++i) {
			int start = 0, end = tokens.get(i).size();
			if(i==start1) start = start2+1;
			if(i==end1) end = end2;
			for(int j=start; j<end; ++j) {
				if(tokens.get(i).get(j).tag().startsWith("V") &&
						!tokens.get(i).get(j).lemma().equals("be")) {
					return true;
				}
			}
		}
		return false;
	}
}
