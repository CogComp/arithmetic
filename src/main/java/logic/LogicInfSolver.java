package logic;

import com.google.common.collect.MinMaxPriorityQueue;
import coref.CorefX;
import coref.CorefY;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import structure.Node;
import structure.PairComparator;
import structure.StanfordSchema;
import utils.Tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogicInfSolver extends AbstractInferenceSolver implements Serializable {

	private static final long serialVersionUID = 5253748728743334706L;
	private LogicFeatGen featGen;

	public LogicInfSolver(LogicFeatGen featGen) throws Exception {
		this.featGen = featGen;
	}

	@Override
	public IStructure getBestStructure(WeightVector weight, IInstance ins)
			throws Exception {
		return getLossAugmentedBestStructure(weight, ins, null);
	}

	@Override
	public IStructure getLossAugmentedBestStructure(WeightVector weight,
			IInstance ins, IStructure goldStructure) throws Exception {
		LogicX x = (LogicX) ins;
		MinMaxPriorityQueue<Pair<List<Node>, Double>> topExpressions =
				getTopExpressions(x, weight, 1000);
		if(topExpressions.size() == 0 || topExpressions.element().getFirst().size() == 0) {
			System.out.println("Prob: "+ Arrays.asList(x.tokens));
			for(int i=x.questionSpan.getFirst(); i<x.questionSpan.getSecond(); ++i) {
				System.out.print(x.tokens.get(x.questionSchema.sentId).get(i)+" ");
			}
			System.out.println();
			for(StanfordSchema schema : x.schema) {
				System.out.println(schema);
			}
			System.out.println(x.questionSchema);
			return goldStructure;
		}
		Node node = topExpressions.element().getFirst().get(0);
		LogicY y = new LogicY(node);
		return y;
	}

	@Override
	public float getLoss(IInstance ins, IStructure gold, IStructure pred) {
		return LogicY.getLoss((LogicY)gold, (LogicY)pred);
	}

	public LogicY getLatentBestStructure(LogicX x, LogicY gold, WeightVector weight)
			throws Exception {
		MinMaxPriorityQueue<Pair<Node, Double>> beam =
				populateInfRuleType(x, gold.expr, weight, true, 2000);
		if(beam.size() == 0) {
			System.out.println("Prob: "+ Arrays.asList(x.tokens));
			for(int i=x.questionSpan.getFirst(); i<x.questionSpan.getSecond(); ++i) {
				System.out.print(x.tokens.get(x.questionSchema.sentId).get(i)+" ");
			}
			System.out.println();
			for(StanfordSchema schema : x.schema) {
				System.out.println(schema);
			}
			System.out.println(x.questionSchema);
			System.out.println("Gold:" + gold);
		}
		LogicY y = new LogicY(beam.element().getFirst());
		return y;
	}

	public MinMaxPriorityQueue<Pair<List<Node>, Double>> getTopExpressions(
			LogicX x, WeightVector wv, int beamSize) throws Exception {
		PairComparator<List<Node>> nodePairComparator =
				new PairComparator<List<Node>>() {};
		MinMaxPriorityQueue<Pair<List<Node>, Double>> beam1 =
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
						.maximumSize(beamSize).create();
		MinMaxPriorityQueue<Pair<List<Node>, Double>> beam2 =
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
						.maximumSize(beamSize).create();
		int n = x.quantities.size();
		List<Node> init = new ArrayList<>();
		for(int i=0; i<n; ++i) {
			Node node = new Node(i, x.quantities.get(i), "NUM");
			node.infRuleType = null;
			if(i == (n-1) && init.size() == 1) {
				init.add(node);
				continue;
			}
			if(i == (n-2) && init.size() == 0) {
				init.add(node);
				continue;
			}
			if(LogicDriver.useGoldRelevance) {
				if(x.relevantQuantIndices.contains(i)) {
					init.add(node);
				}
			} else {
				if (!Relevance.irrelevance(x, i)) {
					init.add(node);
				}
			}
		}
		n = init.size();
		beam1.add(new Pair<>(init, 0.0));
		for(int i=0; i<n-1; ++i) {
			for(Pair<List<Node>, Double> state : beam1) {
				beam2.addAll(enumerateSingleMerge(x, state, wv));
			}
			beam1.clear();
			beam1.addAll(beam2);
			beam2.clear();
		}
		return beam1;
	}

	public List<Pair<List<Node>, Double>> enumerateSingleMerge(
			LogicX x,
			Pair<List<Node>, Double> state,
			WeightVector wv) throws Exception {
		List<Pair<List<Node>, Double>> nextStates = new ArrayList<>();
		List<Node> nodeList = state.getFirst();
		if(nodeList.size() == 1) {
			List<Pair<List<Node>, Double>> tmpNodeList = new ArrayList<>();
			tmpNodeList.add(state);
			return tmpNodeList;
		}
		boolean isTopmost = nodeList.size() <= 2;
		double initScore = state.getSecond();
		for(int i=0; i<nodeList.size(); ++i) {
			for(int j=i+1; j<nodeList.size(); ++j) {
				List<Node> tmpNodeList = new ArrayList<>();
				tmpNodeList.addAll(nodeList);
				tmpNodeList.remove(i);
				tmpNodeList.remove(j-1);
				for(Pair<Node, Double> pair : enumerateMerge(
						x, nodeList.get(i), nodeList.get(j), wv, isTopmost)) {
					List<Node> newNodeList = new ArrayList<>();
					newNodeList.addAll(tmpNodeList);
					newNodeList.add(pair.getFirst());
					nextStates.add(new Pair<>(newNodeList,
							initScore + pair.getSecond()));
				}
			}
		}
		return nextStates;
	}

	public List<Pair<Node, Double>> enumerateMerge(LogicX x,
												   Node l,
												   Node r,
												   WeightVector wv,
												   boolean isTopmost) throws Exception {
//		System.out.println("Enumerating Merge ....");
		List<Pair<Node, Double>> beam = new ArrayList<>();
		if(l.quantIndex > r.quantIndex) {
			Node tmp = l;
			l = r;
			r = tmp;
		}
		StanfordSchema num1 = x.schema.get(l.quantIndex);
		StanfordSchema num2 = x.schema.get(r.quantIndex);
		StanfordSchema ques = x.questionSchema;
		String label, mathOp;
		for(String infRuleType : Logic.inferenceTypes) {
			mathOp = null;
			if(num1.math != -1 || num2.math != -1 || (isTopmost && ques.math != -1)) {
				mathOp = Logic.getMathInfType(x.tokens, num1, num2, ques, isTopmost);
			}
			if(infRuleType.contains("Ques") && !isTopmost) continue;
			if(mathOp != null /* && !mathOp.contains("Mul") */ && !infRuleType.equals(mathOp)) continue;
			if(mathOp == null && infRuleType.startsWith("Math")) continue;
			if(num1.rate.getFirst()>=0 || num2.rate.getFirst()>=0 ||
					(isTopmost && ques.rate.getFirst()>=0)) {
				if(!infRuleType.startsWith("Math") && !infRuleType.startsWith("Rate")) continue;
				if(infRuleType.startsWith("Rate")) {
					if (num1.rate.getFirst() == -1 && infRuleType.contains("0")) continue;
					if (num2.rate.getFirst() == -1 && infRuleType.contains("1")) continue;
					if (ques.rate.getFirst() == -1 && infRuleType.contains("Ques")) continue;
				}
			}
			if(l.children.size() > 0 && (infRuleType.startsWith("Rate0") ||
					infRuleType.startsWith("Math0"))) continue;
			if(r.children.size() > 0 && (infRuleType.startsWith("Rate1") ||
					infRuleType.startsWith("Math1"))) continue;
			if(LogicDriver.useInfModel) {
				SLModel infModel = LogicDriver.corefModel;
				CorefY y = (CorefY) LogicDriver.corefModel.
						infSolver.getBestStructure(
						infModel.wv,
						new CorefX(x, l.quantIndex, r.quantIndex, infRuleType));
				if(l.getValue() < r.getValue() && y.label.equals("SUB")) {
					y.label += "_REV";
				}
				if((y.label.equals("ADD") || y.label.startsWith("SUB")) &&
						(l.label.startsWith("SUB") || r.label.startsWith("SUB"))) {
					continue;
				}
				if((y.label.equals("MUL") || y.label.startsWith("DIV")) &&
						(l.label.startsWith("DIV") || r.label.startsWith("DIV"))) {
					continue;
				}
				if(y.label.startsWith("ADD") || y.label.startsWith("SUB")) {
					if(infRuleType.equals("Verb") &&
							LogicY.isPartitionOrVerb(x, y.label, num1, num2)) continue;
					if(infRuleType.equals("Partition") &&
							!LogicY.isPartitionOrVerb(x, y.label, num1, num2)) continue;
				}
//				if(y.label.equals("ADD") && r.label.equals("ADD")) continue;
				Node node = populateNode(l, r, num2, infRuleType, y.key, y.label);
				double score = wv.dotProduct(featGen.getCombinationFeatureVector(
						x, node, num1, num2, ques, infRuleType, y.key, isTopmost));
				beam.add(new Pair<>(node, score));
				continue;
			}
			for(String key : Logic.getRelevantKeys(infRuleType)) {
				label = null;
				if(infRuleType.equals("Verb")) {
					label = Logic.verb(x.tokens, num1, num2, key);
				}
				if(infRuleType.equals("Partition")) {
					label = Logic.partition(key);
				}
				if(infRuleType.startsWith("Math")) {
					label = Logic.math(mathOp, key);
				}
				if(infRuleType.startsWith("Rate")) {
					label = Logic.unitDependency(infRuleType, key);
				}
				if(label.equals("SUB") && l.getValue() < r.getValue()) {
					label = label + "_REV";
				}
				if(label == null) continue;
				if((label.equals("ADD") || label.startsWith("SUB")) &&
						(l.label.startsWith("SUB") || r.label.startsWith("SUB"))) {
					continue;
				}
				if((label.equals("MUL") || label.startsWith("DIV")) &&
						(l.label.startsWith("DIV") || r.label.startsWith("DIV"))) {
					continue;
				}
				Node node = populateNode(l, r, num2, infRuleType, key, label);
				double score = wv.dotProduct(featGen.getCombinationFeatureVector(
						x, node, num1, num2, ques, infRuleType, key, isTopmost));
				beam.add(new Pair<>(node, score));
			}
		}
		return beam;
	}

	public static Node populateNode(Node l, Node r, StanfordSchema num2,
									String infRuleType, String key, String label) {
		Node node = new Node();
		node.infRuleType = infRuleType;
		node.key = key;
		if(infRuleType.startsWith("Verb") || infRuleType.startsWith("Partition")) {
			if(label.endsWith("REV")) {
				node.quantIndex = r.quantIndex;
			} else {
				node.quantIndex = l.quantIndex;
			}
		}
		if(infRuleType.startsWith("Math")) {
			if(infRuleType.contains("Math0")) {
				node.quantIndex = r.quantIndex;
			} else {
				node.quantIndex = l.quantIndex;
			}
		}
		if(infRuleType.startsWith("Rate")) {
			if(num2.rate != null && num2.rate.getFirst() >= 0) {
				node.quantIndex = l.quantIndex;
			} else {
				node.quantIndex = r.quantIndex;
			}
		}
		if(label.endsWith("REV")) {
			label = label.substring(0, 3);
			node.children.add(r);
			node.children.add(l);
		} else {
			node.children.add(l);
			node.children.add(r);
		}
		node.label = label;
		return node;
	}

	public MinMaxPriorityQueue<Pair<Node, Double>> populateInfRuleType(
			LogicX x, Node expr, WeightVector wv, boolean isTopmost, int beamSize)
			throws Exception {
		PairComparator<Node> nodePairComparator = new PairComparator<Node>() {};
		MinMaxPriorityQueue<Pair<Node, Double>> beam =
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
						.maximumSize(beamSize).create();
		if (expr.children.size() == 0) {
			Node node = new Node(expr);
			beam.add(new Pair<>(node, 0.0));
			return beam;
		}
		MinMaxPriorityQueue<Pair<Node, Double>> left =
				populateInfRuleType(x, expr.children.get(0), wv, false, beamSize);
		MinMaxPriorityQueue<Pair<Node, Double>> right =
				populateInfRuleType(x, expr.children.get(1), wv, false, beamSize);
		for(Pair<Node, Double> l : left) {
			for(Pair<Node, Double> r : right) {
				List<Pair<Node, Double>> pairList = enumerateMerge(
						x, l.getFirst(), r.getFirst(), wv, isTopmost);
				for(Pair<Node, Double> pair : pairList) {
					if(!pair.getFirst().label.equals(expr.label)) continue;
					if(!pair.getFirst().infRuleType.equals(expr.infRuleType)) continue;
					if(expr.key != null && !expr.key.equals(pair.getFirst().key)) continue;
					if(expr.label.equals("DIV") || expr.label.equals("SUB")) {
						if(!(Tools.safeEquals(
								pair.getFirst().children.get(0).getValue(),
								expr.children.get(0).getValue()) &&
								Tools.safeEquals(
										pair.getFirst().children.get(1).getValue(),
										expr.children.get(1).getValue()))) {
							continue;
						}
					}
					beam.add(new Pair<>(pair.getFirst(),
							pair.getSecond() + l.getSecond() + r.getSecond()));
				}
			}
		}
		return beam;
	}

}