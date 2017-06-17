package constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pair.PairDriver;
import pair.PairX;
import rate.RateDriver;
import rate.RateX;
import relevance.RelDriver;
import relevance.RelX;
import run.Annotations;
import run.RunDriver;
import run.RunX;
import structure.Node;
import structure.PairComparator;
import structure.Problem;
import structure.QuantSpan;
import utils.Params;
import utils.Tools;

import com.google.common.collect.MinMaxPriorityQueue;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.sl.core.SLModel;

public class ConsInfSolver {

	public static double wRel, wRun, wRate;
	
	public static Node constrainedInf(Problem prob, SLModel relModel, SLModel pairModel,
			SLModel runModel, SLModel rateModel) throws Exception {
		Map<String, Double> rateScores = new HashMap<>();
		Map<String, Double> runScores = new HashMap<>();
		Map<String, Double> pairScores = new HashMap<>();
		Map<String, Double> relScores = new HashMap<>();
		int numQuantities = prob.quantities.size();
		for(int i=0; i<numQuantities; ++i) {
			for(int j=i+1; j<numQuantities; ++j) {
				pairScores.putAll(PairDriver.getLabelsWithScores(
						new PairX(prob, i, j), pairModel));
				if(!Params.noUDG) runScores.putAll(RunDriver.getLabelsWithScores(
						new RunX(prob, i, j), runModel));
			}
			if(!Params.noUDG) runScores.putAll(RunDriver.getLabelsWithScores(
					new RunX(prob, i, -1), runModel));
		}
		for(int i=0; i<numQuantities; ++i) {
			relScores.putAll(RelDriver.getLabelsWithScores(new RelX(prob, i), relModel));
		}
		for(int i=-1; i<numQuantities; ++i) {
			if(!Params.noUDG) {
				rateScores.putAll(RateDriver.getLabelsWithScores(new RateX(prob, i), rateModel));
			}
		}
		return ConsInfSolver.getBestStructure(
				prob.ta, prob.quantities, relScores, pairScores, runScores, rateScores);
	}
	
	public static double constrainedInf(List<Problem> testData, SLModel relModel, 
			SLModel pairModel, SLModel runModel, SLModel rateModel, boolean isTune) throws Exception {
		double correct = 0.0, total = 0.0;
		double siCorrect = 0.0, siTotal = 0.0;
		for(Problem prob : testData) {
			total += 1.0;
			if(Params.simpleInterest && prob.id >= 110000 && prob.id < 110100) {
				siTotal += 1.0;
			}
			Node node = ConsInfSolver.constrainedInf(prob, relModel, pairModel,
					runModel, rateModel);
			double ans = node.getValue();
			boolean corr = false;
			if(Tools.safeEquals(ans, prob.answer)) {
				correct += 1.0;
				corr = true;
				if(Params.simpleInterest && prob.id >= 110000 && prob.id < 110100) {
					siCorrect += 1.0;
				}
			}
			if(!isTune && ((corr && Params.printCorrect) ||
					(!corr && Params.printMistakes))) {
				System.out.println(prob.id+" : "+prob.ta.getText());
				System.out.println("Gold : "+prob.expr);
				System.out.println("Predicted : "+node);
				System.out.println();
			}
		}
		if(Params.printMistakes) System.out.print("Final ");
		System.out.println("Constrained Inference : "+correct+" / "+
				total+" = "+(correct/total));
		if(Params.simpleInterest) System.out.println("SI Inference : "+siCorrect+" / "+siTotal
				+" = "+(siCorrect/siTotal));
		return (correct/total);
	}
	
	public static Node getBestStructure(TextAnnotation ta, List<QuantSpan> quantities, 
			Map<String, Double> relScores, Map<String, Double> pairScores, 
			Map<String, Double> runScores, Map<String, Double> rateScores) throws Exception {
		PairComparator<List<Node>> nodePairComparator = 
				new PairComparator<List<Node>>() {};
		MinMaxPriorityQueue<Pair<List<Node>, Double>> beam1 = 
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
				.maximumSize(200).create();
		MinMaxPriorityQueue<Pair<List<Node>, Double>> beam2 = 
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
				.maximumSize(200).create();
		int n = quantities.size();
		List<Node> init = new ArrayList<>();
		for(int i=0; i<quantities.size(); ++i) {
			init.add(new Node(i, quantities.get(i), "NUM"));
		}
		beam1.addAll(enumerateIrrelevantQuants(init, relScores));
		for(int i=0; i<n-1; ++i) {
			for(Pair<List<Node>, Double> state : beam1) {
				beam2.addAll(enumerateSingleMerge(state, pairScores));
			}
			beam1.clear();
			beam1.addAll(beam2);
			beam2.clear();
		}
		// Constraint scores
		for(Pair<List<Node>, Double> state : beam1) {
			boolean isPositive = Constraints.isPositive(state.getFirst().get(0).getValue());
			if(!isPositive) continue;
//			boolean isInteger = Constraints.isInteger(ta, state.getFirst().get(0).getValue());
//			if(!isInteger) continue;
			beam2.add(new Pair<>(state.getFirst(), state.getSecond()));
		}
		// Adding Run scores
		if(!Params.noUDG) {
			beam1.clear();
			beam1.addAll(beam2);
			beam2.clear();
			for (Pair<List<Node>, Double> state : beam1) {
				beam2.add(new Pair<>(state.getFirst(), state.getSecond() +
						getRunScore(quantities, state.getFirst().get(0), new ArrayList<Integer>(), runScores)));
				for (int i = -1; i < quantities.size(); ++i) {
					if (isRateListAllowable(state.getFirst().get(0), Arrays.asList(i))) {
						beam2.add(new Pair<>(state.getFirst(), state.getSecond() +
								getRunScore(quantities, state.getFirst().get(0), Arrays.asList(i), runScores) +
								getRateScore(quantities, state.getFirst().get(0), Arrays.asList(i), rateScores)));
					}
					for (int j = i + 1; j < quantities.size(); ++j) {
						if (isRateListAllowable(state.getFirst().get(0), Arrays.asList(i, j))) {
							beam2.add(new Pair<>(state.getFirst(), state.getSecond() +
									getRunScore(quantities, state.getFirst().get(0), Arrays.asList(i, j), runScores) +
									getRateScore(quantities, state.getFirst().get(0), Arrays.asList(i, j), rateScores)));
						}
					}
				}
			}
		}
		if(beam2.size() == 0) return beam1.element().getFirst().get(0);
		return beam2.element().getFirst().get(0);
	}
	
	public static List<Pair<List<Node>, Double>> enumerateIrrelevantQuants(
			List<Node> init, Map<String, Double> scores) {
		PairComparator<List<Node>> nodePairComparator = 
				new PairComparator<List<Node>>() {};
		MinMaxPriorityQueue<Pair<List<Node>, Double>> beam1 = 
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
				.maximumSize(200).create();
		MinMaxPriorityQueue<Pair<List<Node>, Double>> beam2 = 
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
				.maximumSize(200).create();
		List<Pair<List<Node>, Double>> nextStates = new ArrayList<>();
		beam1.add(new Pair<List<Node>, Double>(new ArrayList<Node>(), 0.0));
		for(int i=0; i<init.size(); ++i) {
			for(Pair<List<Node>, Double> pair : beam1) {
				List<Node> nodeList = new ArrayList<>();
				nodeList.addAll(pair.getFirst());
				beam2.add(new Pair<>(
						nodeList, pair.getSecond()+getIrrelevanceScore(i, scores)));
				nodeList = new ArrayList<>();
				nodeList.addAll(pair.getFirst());
				nodeList.add(init.get(i));
				beam2.add(new Pair<>(nodeList, pair.getSecond()));
			}
			beam1.clear();
			beam1.addAll(beam2);
			beam2.clear();
		}
		while(beam1.size() > 0) {
			Pair<List<Node>, Double> pair = beam1.poll();
			if(pair.getFirst().size() > 1) {
				nextStates.add(pair);
			}
		}
		return nextStates;
	}
	
	public static List<Pair<List<Node>, Double>> enumerateSingleMerge(
			Pair<List<Node>, Double> state, Map<String, Double> pairScores) {
		List<Pair<List<Node>, Double>> nextStates = new ArrayList<>();
		List<Node> nodeList = state.getFirst();
		if(nodeList.size() == 1) {
			List<Pair<List<Node>, Double>> tmpNodeList = new ArrayList<>();
			tmpNodeList.add(state);
			return tmpNodeList;
		}
		double initScore = state.getSecond();
		for(int i=0; i<nodeList.size(); ++i) {
			for(int j=i+1; j<nodeList.size(); ++j) {
				List<Node> tmpNodeList = new ArrayList<>();
				tmpNodeList.addAll(nodeList);
				tmpNodeList.remove(i);
				tmpNodeList.remove(j-1);
				for(Pair<Node, Double> pair : enumerateMerge(
						nodeList.get(i), nodeList.get(j), pairScores)) {
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
	
	public static List<Pair<Node, Double>> enumerateMerge(
			Node node1, Node node2, Map<String, Double> pairScores) {
		List<Pair<Node, Double>> nextStates = new ArrayList<>();
		List<String> labels = Arrays.asList("ADD", "SUB", "SUB_REV","MUL", "DIV", "DIV_REV");
		double mergeScore;
		for(String label : labels) {
			if((node1.label.startsWith("SUB") || node2.label.startsWith("SUB")) &&
					(label.startsWith("ADD") || label.startsWith("SUB"))) {
				continue;
			}
			if((node1.label.startsWith("DIV") || node2.label.startsWith("DIV")) &&
					(label.startsWith("MUL") || label.startsWith("DIV"))) {
				continue;
			}
			if(label.endsWith("REV")) {
				label = label.substring(0,3);
				mergeScore = getScore(node2, node1, label, pairScores);
				Node node = new Node(label, Arrays.asList(node2, node1));
				nextStates.add(new Pair<>(node, mergeScore));
			} else {
				mergeScore = getScore(node1, node2, label, pairScores);
				Node node = new Node(label, Arrays.asList(node1, node2));
				nextStates.add(new Pair<>(node, mergeScore));
			}
		}
		return nextStates;
	}
	
	public static double getScore(Node node1, Node node2, String label, 
			Map<String, Double> pairScores) {
		double mergeScore = 0.0;
		for(Node leaf1 : node1.getLeaves()) {
			for(Node leaf2 : node2.getLeaves()) {
				String pairKey = getPairKey(leaf1.quantIndex, leaf2.quantIndex, label);
				mergeScore += pairScores.get(pairKey);
			}
		}
		return mergeScore;
	}
	
	public static double getIrrelevanceScore(
			int index, Map<String, Double> relScore) {
		return relScore.get(index+"_IRR")*wRel;
	}
	
	// Input label does not have REV in it
	public static String getPairKey(int index1, int index2, String label) {
		String key = null;
		if(index1 > index2 && (label.equals("SUB") || label.equals("DIV"))) {
			key = index2+"_"+index1+"_"+label+"_REV";
		} else if(index1 > index2) {
			key = index2+"_"+index1+"_"+label;
		} else {
			key = index1+"_"+index2+"_"+label;
		}
		return key;
	}
	
	public static double getRunScore(List<QuantSpan> quantities, Node expr, 
			List<Integer> rates, Map<String, Double> runScores) {
		double score = 0.0;
		for(int i=0; i<quantities.size(); ++i) {
			if(!expr.hasLeaf(i)) continue;
			for(int j=i+1; j<quantities.size(); ++j) {
				if(!expr.hasLeaf(j)) continue;
				// Relation with j
				List<String> path = expr.getPath(i, j);
				String label = Annotations.getLabel(path, rates.contains(i), rates.contains(j));
				score += runScores.get(i+"_"+j+"_"+label);
			}
			// Relation with question
			List<String> path = expr.getPathToRoot(i);
			String label = Annotations.getLabel(path, rates.contains(i), rates.contains(-1));
			score += runScores.get(i+"_-1_"+label);
		}
		return score*wRun;
	}
	
	public static double getRateScore(List<QuantSpan> quantities, Node expr, 
			List<Integer> rates, Map<String, Double> rateScores) {
		double score = 0.0;
		for(int i=0; i<quantities.size(); ++i) {
			if(!expr.hasLeaf(i)) continue;
			if(rates.contains(i)) {
				score += rateScores.get(i+"_RATE");
			}
		}
		if(rates.contains(-1)) {
			score += rateScores.get(-1+"_RATE");
		}
		return score*wRate;
	}
	
	public static boolean isRateListAllowable(Node expr, List<Integer> rateIndices) {
        if(rateIndices.size() == 1) {
			if(rateIndices.get(0) == -1) {
				boolean onlyAddSubPathFound = false;
				for(Node node : expr.getLeaves()) {
					List<String> path = expr.getPathToRoot(node.quantIndex);
					if(!path.contains("MUL") && !path.contains("DIV") && !path.contains("DIV_REV")) {
						onlyAddSubPathFound = true;
						break;
					}
				}
				if(onlyAddSubPathFound) return false;
				else return true;
			} else {
				List<String> path = expr.getPathToRoot(rateIndices.get(0));
				if(!path.contains("MUL") && !path.contains("DIV") && !path.contains("DIV_REV")) {
					return false;
				} else {
					return true;
				}
			}
		}
        if(rateIndices.size() == 2) {
			if(rateIndices.get(1) == -1) return true;
			List<String> path1 = expr.getPathToRoot(rateIndices.get(0));
			List<String> path2 = expr.getPathToRoot(rateIndices.get(1));
			if(!path1.contains("MUL") && !path1.contains("DIV") && !path1.contains("DIV_REV")) {
				return false;
            }	
			if(!path2.contains("MUL") && !path2.contains("DIV") && !path2.contains("DIV_REV")) {
				return false;
			}
            return true;
		}
		return false;
	}
}