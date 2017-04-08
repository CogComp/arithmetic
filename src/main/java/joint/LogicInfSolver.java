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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
		List<StanfordSchema> extractionCandidates = new ArrayList<>();
		// Get top candidate extractions
		for(int i=0; i<x.quantities.size(); ++i) {
			extractionCandidates.add(x.schema.get(i));
		}
		extractionCandidates.add(x.questionSchema);
		// Now find best tree construction from the extractions
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
		populateInfRuleType(x, gold.expr, weight, true);
		return gold;
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

	public List<Pair<Node, Double>> enumerateMerge(LogicX x, Node node1, Node node2,
			WeightVector wv, boolean isTopmost) {
		List<Pair<Node, Double>> nodeList = new ArrayList<>();
		LogicInput num1 = new LogicInput(
				1,
				x.schema.get(node1.quantIndex),
				x.tokens.get(x.schema.get(node1.quantIndex).sentId));
		LogicInput num2 = new LogicInput(
				2,
				x.schema.get(node2.quantIndex),
				x.tokens.get(x.schema.get(node2.quantIndex).sentId));
		LogicInput ques = new LogicInput(
				0,
				x.questionSchema,
				x.tokens.get(x.questionSchema.sentId));
		Map<Pair<String, Integer>, Double> scores =
				Logic.logicSolver(num1, num2, ques, isTopmost);
		for(Pair<String, Integer> key : scores.keySet()) {
			if(scores.get(key) < -100.0) continue;
			int infRuleType = key.getSecond();
			String label = key.getFirst();
			Node node;
			double mergeScore;
			if (label.endsWith("REV")) {
				label = label.substring(0, 3);
				node = new Node(label, Arrays.asList(node2, node1));
				mergeScore = wv.dotProduct(featGen.getInfTypeFeatureVector(
						x,
						x.schema.get(node1.quantIndex),
						x.schema.get(node2.quantIndex),
						x.questionSchema,
						infRuleType));
			} else {
				node = new Node(label, Arrays.asList(node1, node2));
				mergeScore = wv.dotProduct(featGen.getInfTypeFeatureVector(
						x,
						x.schema.get(node1.quantIndex),
						x.schema.get(node2.quantIndex),
						x.questionSchema,
						infRuleType));
			}
			node.infRuleType = infRuleType;
			if (infRuleType == 0 || infRuleType == 1) {
				node.quantIndex = node.children.get(0).quantIndex;
			}
			if (infRuleType == 2) {
				if (x.schema.get(node2.quantIndex).math != -1) {
					node.quantIndex = node.children.get(1).quantIndex;
				} else {
					node.quantIndex = node.children.get(0).quantIndex;
				}
			}
			if (infRuleType == 3) {
				if (x.schema.get(node2.quantIndex).rate != null &&
						x.schema.get(node2.quantIndex).rate.getFirst() >= 0) {
					node.quantIndex = node.children.get(1).quantIndex;
				} else {
					node.quantIndex = node.children.get(0).quantIndex;
				}
			}
			nodeList.add(new Pair<>(node, mergeScore));
		}
		return nodeList;
	}

	public void populateInfRuleType(LogicX x,
									Node expr,
									WeightVector wv,
									boolean isTopmost) {
		if (expr.children.size() == 0) return;
		populateInfRuleType(x, expr.children.get(0), wv, false);
		populateInfRuleType(x, expr.children.get(1), wv, false);
		expr.infRuleType = -1;
		double bestScore = -Double.MAX_VALUE;
		StanfordSchema num1 = x.schema.get(expr.children.get(0).quantIndex);
		StanfordSchema num2 = x.schema.get(expr.children.get(1).quantIndex);
		StanfordSchema ques = x.questionSchema;

		Map<Pair<String, Integer>, Double> scores =
				Logic.logicSolver(
						new LogicInput(0, num1, x.tokens.get(num1.sentId)),
						new LogicInput(0, num2, x.tokens.get(num2.sentId)),
						new LogicInput(0, ques, x.tokens.get(ques.sentId)),
						isTopmost);
		for(Pair<String, Integer> key : scores.keySet()) {
			double score = scores.get(key);
			if(score < -100.0) continue;
			String label = key.getFirst();
			int infRuleType = key.getSecond();
			if(!label.equals(expr.label)) continue;
			score = wv.dotProduct(featGen.getInfTypeFeatureVector(
					x, num1, num2, ques, infRuleType));
			if(score > bestScore) {
				bestScore = score;
				expr.infRuleType = infRuleType;
				if(infRuleType==0 || infRuleType==1) {
					expr.quantIndex = expr.children.get(0).quantIndex;
				}
				if(infRuleType==2) {
					if(num2.math != -1) {
						expr.quantIndex = expr.children.get(1).quantIndex;
					} else {
						expr.quantIndex = expr.children.get(0).quantIndex;
					}
				}
				if(infRuleType==3) {
					if(num2.rate != null && num2.rate.getFirst() >= 0) {
						expr.quantIndex = expr.children.get(1).quantIndex;
					} else {
						expr.quantIndex = expr.children.get(0).quantIndex;
					}
				}
			}
		}
	}

}