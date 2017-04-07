package joint;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import logic.Logic;
import logic.LogicInput;
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
		LogicX logicX = (LogicX) ins;
		List<StanfordSchema> extractionCandidates = new ArrayList<>();
		// Get top candidate extractions
		for(int i=0; i<=logicX.quantities.size(); ++i) {
			extractionCandidates.add(getBestStanfordSchemas(logicX, weight, i));
		}
		// Now find best tree construction from the extractions
		Node node = getBestStructure(logicX, extractionCandidates, weight);
		LogicY y = new LogicY(node, extractionCandidates);
		return y;
	}

	@Override
	public float getLoss(IInstance ins, IStructure gold, IStructure pred) {
		return LogicY.getLoss((LogicY)gold, (LogicY)pred);
	}

	public LogicY getLatentBestStructure(LogicX ins, LogicY gold, WeightVector weight) {
		gold.extractions = new ArrayList<>();
		// Get top candidate extractions
		for(int i=0; i<=ins.quantities.size(); ++i) {
			gold.extractions.add(getBestStanfordSchemas(ins, weight, i));
		}
		// Now find best tree construction from the extractions
		populateInfRuleType(ins, gold.expr, gold.extractions, weight, true);
		return gold;
	}

	public StanfordSchema getBestStanfordSchemas(LogicX x, WeightVector wv, int index) {
		PairComparator<StanfordSchema> pairComparator =
				new PairComparator<StanfordSchema>() {};
		MinMaxPriorityQueue<Pair<StanfordSchema, Double>> inputs =
				MinMaxPriorityQueue.orderedBy(pairComparator).maximumSize(5).create();
		StanfordSchema schema;
		if(index == x.quantities.size()) {
			schema = x.questionSchema;
		} else {
			schema = x.schema.get(index);
		}
		inputs.add(new Pair<>(schema, 1.0*wv.dotProduct(
				featGen.getExtractionFeatureVector(x, schema))));
		return inputs.element().getFirst();
	}

	public Node getBestStructure(LogicX x,
								 List<StanfordSchema> extractionCandidates,
								 WeightVector wv) throws Exception {
		PairComparator<List<Node>> nodePairComparator =
				new PairComparator<List<Node>>() {};
		MinMaxPriorityQueue<Pair<List<Node>, Double>> beam1 =
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
						.maximumSize(200).create();
		MinMaxPriorityQueue<Pair<List<Node>, Double>> beam2 =
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
						.maximumSize(200).create();
		int n = extractionCandidates.size() - 1;
		List<Node> init = new ArrayList<>();
		for(int i=0; i<n; ++i) {
			Node node = new Node(i, x.quantities.get(i), "NUM");
			node.infRuleType = -1;
			if(!Logic.irrelevance(extractionCandidates, i, x.tokens)) {
				init.add(node);
			}
		}
		n = init.size();
		beam1.add(new Pair<>(init, 0.0));
		for(int i=0; i<n-1; ++i) {
			for(Pair<List<Node>, Double> state : beam1) {
				beam2.addAll(enumerateSingleMerge(
						x, state, extractionCandidates, wv));
			}
			beam1.clear();
			beam1.addAll(beam2);
			beam2.clear();
		}
		if(beam2.size() == 0) return beam1.element().getFirst().get(0);
		return beam2.element().getFirst().get(0);
	}

	public List<Pair<List<Node>, Double>> enumerateSingleMerge(
			LogicX x,
			Pair<List<Node>, Double> state,
			List<StanfordSchema> extractionCandidates,
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
						x, nodeList.get(i), nodeList.get(j),
						extractionCandidates, wv, isTopmost)) {
					List<Node> newNodeList = new ArrayList<>();
					newNodeList.addAll(tmpNodeList);
					newNodeList.add(pair.getFirst());
					nextStates.add(new Pair<>(newNodeList, initScore + pair.getSecond()));
				}
			}
		}
		return nextStates;
	}

	public List<Pair<Node, Double>> enumerateMerge(
			LogicX x, Node node1, Node node2,
			List<StanfordSchema> extractionCandidates,
			WeightVector wv,
			boolean isTopmost) {
		List<Pair<Node, Double>> nextStates = new ArrayList<>();
		double mergeScore;
		LogicInput num1 = new LogicInput(
				1,
				extractionCandidates.get(node1.quantIndex),
				x.tokens.get(extractionCandidates.get(node1.quantIndex).sentId));
		LogicInput num2 = new LogicInput(
				2,
				extractionCandidates.get(node2.quantIndex),
				x.tokens.get(extractionCandidates.get(node2.quantIndex).sentId));
		LogicInput ques = new LogicInput(
				0,
				extractionCandidates.get(x.quantities.size()),
				x.tokens.get(extractionCandidates.get(x.quantities.size()).sentId));
		Map<Pair<String, Integer>, Double> scores =
				Logic.logicSolver(num1, num2, ques, isTopmost);
		for(Pair<String, Integer> key : scores.keySet()) {
			double score = scores.get(key);
			if(score < -100.0) continue;
			String label = key.getFirst();
			int infRuleType = key.getSecond();
			if((label.equals("ADD") || label.startsWith("SUB")) &&
					(node1.label.startsWith("SUB") || node2.label.startsWith("SUB"))) {
				continue;
			}
			if((label.equals("MUL") || label.startsWith("DIV")) &&
					(node1.label.startsWith("DIV") || node2.label.startsWith("DIV"))) {
				continue;
			}
			Node node;
			if(label.endsWith("REV")) {
				label = label.substring(0, 3);
				node = new Node(label, Arrays.asList(node2, node1));
				mergeScore = wv.dotProduct(featGen.getInfTypeFeatureVector(
						x,
						extractionCandidates.get(node1.quantIndex),
						extractionCandidates.get(node2.quantIndex),
						extractionCandidates.get(x.quantities.size()),
						infRuleType));
			} else {
				node = new Node(label, Arrays.asList(node1, node2));
				mergeScore = wv.dotProduct(featGen.getInfTypeFeatureVector(
						x,
						extractionCandidates.get(node1.quantIndex),
						extractionCandidates.get(node2.quantIndex),
						extractionCandidates.get(x.quantities.size()),
						infRuleType));
			}
			node.infRuleType = infRuleType;
			if(infRuleType==0 || infRuleType==1) {
				node.quantIndex = node.children.get(0).quantIndex;
			}
			if(infRuleType==2) {
				if(extractionCandidates.get(node2.quantIndex).math != -1) {
					node.quantIndex = node.children.get(1).quantIndex;
				} else {
					node.quantIndex = node.children.get(0).quantIndex;
				}
			}
			if(infRuleType==3) {
				if(extractionCandidates.get(node2.quantIndex).rate != null &&
						extractionCandidates.get(node2.quantIndex).rate.getFirst() >= 0) {
					node.quantIndex = node.children.get(1).quantIndex;
				} else {
					node.quantIndex = node.children.get(0).quantIndex;
				}
			}
			nextStates.add(new Pair<>(node, mergeScore));

		}
		return nextStates;
	}

	public void populateInfRuleType(LogicX x,
									Node expr,
									List<StanfordSchema> extractions,
									WeightVector wv,
									boolean isTopmost) {
		if (expr.children.size() == 0) return;
		populateInfRuleType(x, expr.children.get(0), extractions, wv, false);
		populateInfRuleType(x, expr.children.get(1), extractions, wv, false);
		expr.infRuleType = -1;
		double bestScore = -Double.MAX_VALUE;
		StanfordSchema num1 = extractions.get(expr.children.get(0).quantIndex);
		StanfordSchema num2 = extractions.get(expr.children.get(1).quantIndex);
		StanfordSchema ques = extractions.get(extractions.size()-1);
		for(int i=0; i<Logic.maxNumInferenceTypes; ++i) {
			String opLogic = Logic.logicSolver(
					new LogicInput(0, num1, x.tokens.get(num1.sentId)),
					new LogicInput(0, num2, x.tokens.get(num2.sentId)),
					new LogicInput(0, ques, x.tokens.get(ques.sentId)),
					i,
					isTopmost);
			if(!opLogic.equals(expr.label)) continue;
			float score = wv.dotProduct(featGen.getInfTypeFeatureVector(
					x, num1, num2, ques, i));
			if(score > bestScore) {
				bestScore = score;
				expr.infRuleType = i;
				if(i==0 || i==1) {
					expr.quantIndex = expr.children.get(0).quantIndex;
				}
				if(i==2) {
					if(num2.math != -1) {
						expr.quantIndex = expr.children.get(1).quantIndex;
					} else {
						expr.quantIndex = expr.children.get(0).quantIndex;
					}
				}
				if(i==3) {
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