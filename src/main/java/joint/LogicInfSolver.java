package joint;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
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
				getTopExpressions(x, weight, 200);
		Node node = topExpressions.element().getFirst().get(0);
		LogicY y = new LogicY(node);
		return y;
	}

	@Override
	public float getLoss(IInstance ins, IStructure gold, IStructure pred) {
		return LogicY.getLoss((LogicY)gold, (LogicY)pred);
	}

	public LogicY getLatentBestStructure(LogicX x, LogicY gold, WeightVector weight) {
		MinMaxPriorityQueue<Pair<Node, Double>> beam =
				populateInfRuleType(x, gold.expr, weight, true, 200);
		if(beam.size() == 0) {
			System.out.println("Prob: "+ Arrays.asList(x.tokens));
			System.out.println();
			for(StanfordSchema schema : x.schema) {
				System.out.println(schema);
			}
			System.out.println(x.questionSchema);
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
		List<StanfordSchema> schemas = new ArrayList<>();
		schemas.addAll(x.schema);
		schemas.add(x.questionSchema);
		for(int i=0; i<n; ++i) {
			Node node = new Node(i, x.quantities.get(i), "NUM");
			node.infRuleType = -1;
			if(!Logic.irrelevance(schemas, i, x.tokens)) {
				init.add(node);
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
			WeightVector wv) {
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
												   boolean isTopmost) {
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
		for(int infRuleType=0; infRuleType<LogicNew.maxNumInferenceTypes; ++infRuleType) {
			mathOp = null;
			if(num1.math != -1 || num2.math != -1 || (isTopmost && ques.math != -1)) {
				mathOp = LogicNew.getMathOp(x.tokens, num1, num2, ques);
			}
			if(infRuleType == 2 && mathOp == null) continue;
			if(infRuleType != 2 && mathOp != null) continue;
			if(num1.rate.getFirst()>=0 || num2.rate.getFirst()>=0 || (isTopmost && ques.rate.getFirst()>=0)) {
				if(infRuleType != 3) continue;
			}
			for(String key : LogicNew.getRelevantKeys(infRuleType, isTopmost, mathOp)) {
				if(num1.rate.getFirst()>=0 || num2.rate.getFirst()>=0 || ques.rate.getFirst()>=0) {
					if(key.startsWith("0") && num1.rate.getFirst() == -1) continue;
					if(key.startsWith("1") && num2.rate.getFirst() == -1) continue;
					if(key.startsWith("QUES") && ques.rate.getFirst() == -1) continue;
				}
				label = null;
				if(infRuleType == 0) {
					label = LogicNew.verb(
							x.tokens.get(num1.sentId).get(num1.verb).lemma(),
							x.tokens.get(num2.sentId).get(num2.verb).lemma(),
							Tools.spanToLemmaList(x.tokens.get(num1.sentId), num1.unit),
							Tools.spanToLemmaList(x.tokens.get(num2.sentId), num2.unit),
							key);
				}
				if(infRuleType == 1) {
					label = LogicNew.partition(key);
				}
				if(infRuleType == 2) {
					label = LogicNew.math(mathOp, key);
				}
				if(infRuleType == 3) {
					label = LogicNew.unitDependency(key);
				}
				if(label == null) continue;
//				System.out.println("Infrule: "+infRuleType+" Label: "+label+
//						" Mathop: "+mathOp+" Key: "+key);
				if((label.equals("ADD") || label.startsWith("SUB")) &&
						(l.label.startsWith("SUB") || r.label.startsWith("SUB"))) {
					continue;
				}
				if((label.equals("MUL") || label.startsWith("DIV")) &&
						(l.label.startsWith("DIV") || r.label.startsWith("DIV"))) {
					continue;
				}
				double score = wv.dotProduct(featGen.getCombinationFeatureVector(
						x, num1, num2, ques, infRuleType, key, isTopmost));

				Node node = new Node();
				node.infRuleType = infRuleType;
				node.key = key;
				if(infRuleType==0 || infRuleType==1) {
					if(label.endsWith("REV")) {
						node.quantIndex = r.quantIndex;
					} else {
						node.quantIndex = l.quantIndex;
					}
				}
				if(infRuleType==2) {
					if(num1.math != -1) {
						node.quantIndex = r.quantIndex;
					} else {
						node.quantIndex = l.quantIndex;
					}
				}
				if(infRuleType==3) {
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
				beam.add(new Pair<>(node, score));
			}
		}
		return beam;
	}

	public MinMaxPriorityQueue<Pair<Node, Double>> populateInfRuleType(
			LogicX x, Node expr, WeightVector wv, boolean isTopmost, int beamSize) {
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
					if(expr.label.equals("SUB") || expr.label.equals("DIV")) {
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