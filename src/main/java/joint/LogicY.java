package joint;

import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.stanford.nlp.ling.CoreLabel;
import structure.Node;
import structure.StanfordSchema;
import utils.Tools;

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
//		if(!node1.label.equals(node2.label)) {
//			return 1.0f;
//		}
		if(!node1.infRuleType.equals(node2.infRuleType)) {
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

	public static float getLossForParenthesis(Node node1, Node node2) {
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
//		if(!node1.label.equals(node2.label)) {
//			return 1.0f;
//		}
//		if(!node1.infRuleType.equals(node2.infRuleType)) {
//			return 1.0f;
//		}
		float loss;
		if(node1.label.equals("ADD") || node1.label.equals("MUL")) {
			loss = Math.min(
					getLossForParenthesis(node1.children.get(0), node2.children.get(0)) +
							getLossForParenthesis(node1.children.get(1), node2.children.get(1)),
					getLossForParenthesis(node1.children.get(0), node2.children.get(1)) +
							getLossForParenthesis(node1.children.get(1), node2.children.get(0)));
		} else {
			loss = getLossForParenthesis(node1.children.get(0), node2.children.get(0)) +
					getLossForParenthesis(node1.children.get(1), node2.children.get(1));
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
			expr.infRuleType = null;
			expr.key = null;
			return;
		}
		populateInfType(x, expr.children.get(0), false, rateAnnotations);
		populateInfType(x, expr.children.get(1), false, rateAnnotations);
		int quantIndex1 = expr.children.get(0).quantIndex;
		int quantIndex2 = expr.children.get(1).quantIndex;
		if(quantIndex1 < quantIndex2) {
			populateInfType(x, expr, quantIndex1, quantIndex2, isTopmost, rateAnnotations);
		} else {
			populateInfType(x, expr, quantIndex2, quantIndex1, isTopmost, rateAnnotations);
		}
	}

	// Here, quantIndex1 is the left child, quantIndex2 is the right child
	public void populateInfType(LogicX x, Node expr, int quantIndex1, int quantIndex2,
								boolean isTopmost, List<Integer> rateAnnotations)  {
		StanfordSchema num1 = x.schema.get(quantIndex1);
		StanfordSchema num2 = x.schema.get(quantIndex2);
		StanfordSchema ques= x.questionSchema;
		int mathIndex = -1;
		String mathInfType = Logic.getMathInfType(x.tokens, num1, num2, ques, isTopmost);
		if(mathInfType != null) {
			if((expr.label.equals("ADD") || expr.label.equals("SUB")) &&
					(mathInfType.contains("Add") || mathInfType.contains("Sub"))) {
				expr.quantIndex = mathInfType.charAt(4) == '1' ? quantIndex2 : quantIndex1;
				expr.infRuleType = mathInfType;
				return;
			}
			if((expr.label.equals("MUL") || expr.label.equals("DIV")) &&
					mathInfType.contains("Mul")) {
				expr.quantIndex = mathInfType.charAt(4) == '1' ? quantIndex2 : quantIndex1;
				expr.infRuleType = mathInfType;
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
			if(rateAnnotations.contains(quantIndex1)) expr.infRuleType = "Rate0";
			if(rateAnnotations.contains(quantIndex2)) expr.infRuleType = "Rate1";
			if(rateAnnotations.contains(-1)) expr.infRuleType = "RateQues";
			return;
		}
		if(expr.label.equals("ADD") || expr.label.equals("SUB")) {
			if(isPartitionOrVerb(x, expr.label, num1, num2)) {
				expr.infRuleType = "Partition";
				expr.quantIndex = quantIndex1;
				return;
			}
			expr.infRuleType = "Verb";
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
				if(j>=1 && tokens.get(i).get(j-1).lemma().equals("to")) {
					continue;
				}
				if(tokens.get(i).get(j).tag().startsWith("V") &&
						!tokens.get(i).get(j).lemma().equals("be") &&
						!tokens.get(i).get(j).lemma().equals("have")) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean nowPresent(List<List<CoreLabel>> tokens,
									 StanfordSchema num1,
									 StanfordSchema num2) {
		for(int i=0; i<tokens.get(num1.sentId).size(); ++i) {
			if(tokens.get(num1.sentId).get(i).word().toLowerCase().equals("now")) {
				return true;
			}
		}
		for(int i=0; i<tokens.get(num2.sentId).size(); ++i) {
			if(tokens.get(num2.sentId).get(i).word().toLowerCase().equals("now")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPartitionOrVerb(LogicX x,
											String label,
											StanfordSchema num1,
											StanfordSchema num2) {
		if (x.tokens.get(num1.sentId).get(num1.verb).lemma().equals(
				x.tokens.get(num2.sentId).get(num2.verb).lemma())) {
			boolean midVerb = midVerb(x.tokens, num1, num2);
			boolean nowPresent = nowPresent(x.tokens, num1, num2);
			if (!midVerb && !nowPresent) {
				return true;
			}
		}
		List<CoreLabel> tokens = x.tokens.get(num1.sentId);
		int tokenId = Tools.getTokenIdFromCharOffset(tokens, num1.qs.start);
		for (int i = tokenId + 1; i < tokens.size(); ++i) {
			if (tokens.get(i).word().equals("remaining") ||
					tokens.get(i).word().equals("rest") ||
					tokens.get(i).word().toLowerCase().equals("either")) {
				return true;
			}
		}
		tokens = x.tokens.get(num2.sentId);
		tokenId = Tools.getTokenIdFromCharOffset(tokens, num2.qs.start);
		for (int i = tokenId + 1; i < tokens.size(); ++i) {
			if (tokens.get(i).word().equals("remaining") ||
					tokens.get(i).word().equals("rest") ||
					tokens.get(i).word().toLowerCase().equals("either")) {
				return true;
			}
		}
		if(label.equals("ADD")) {
			for(int i=x.questionSpan.getFirst(); i<x.questionSpan.getSecond(); ++i) {
				CoreLabel token = x.tokens.get(x.questionSchema.sentId).get(i);
				if(token.word().equals("all") || token.word().equals("altogether") ||
						token.word().equals("overall") || token.word().equals("total") ||
						token.word().equals("sum")) {
					return true;
				}
			}
			for(int i=0; i<2; ++i) {
				CoreLabel token = x.tokens.get(x.questionSchema.sentId).get(i);
				if(token.word().equals("all") || token.word().equals("altogether") ||
						token.word().equals("overall") || token.word().equals("total")) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean midNumber(List<List<CoreLabel>> tokens,
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
				if(tokens.get(i).get(j).tag().startsWith("CD")) {
					return true;
				}
			}
		}
		return false;
	}
}
