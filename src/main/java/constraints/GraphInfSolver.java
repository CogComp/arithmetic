package constraints;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import rate.RateDriver;
import rate.RateX;
import rate.RateY;
import run.Annotations;
import run.RunDriver;
import run.RunX;
import run.RunY;
import structure.PairComparator;
import structure.Problem;
import utils.Params;

import java.util.*;

public class GraphInfSolver {

	public static double wRun;

	public static List<String> constrainedInf(
			Problem prob, SLModel runModel, SLModel rateModel, boolean useConstraints)
			throws Exception {
		Map<String, Double> rateScores = new HashMap<String, Double>();
		Map<String, Double> runScores = new HashMap<String, Double>();
		int numQuantities = prob.quantities.size();
		for(int i=0; i<numQuantities; ++i) {
			for(int j=i+1; j<numQuantities; ++j) {
				runScores.putAll(RunDriver.getLabelsWithScores(
						new RunX(prob, i, j), runModel));
			}
			runScores.putAll(RunDriver.getLabelsWithScores(
					new RunX(prob, i, -1), runModel));
		}
		for(int i=-1; i<numQuantities; ++i) {
			rateScores.putAll(RateDriver.getLabelsWithScores(new RateX(prob, i), rateModel));
		}
		return GraphInfSolver.getBestStructure(prob, runScores, rateScores, useConstraints);
	}

	public static double constrainedInf(List<Problem> testData,
										SLModel runModel,
										SLModel rateModel) throws Exception {
		double correct = 0.0, total = 0.0;
		for(Problem prob : testData) {
			total += 1.0;
			int n = prob.quantities.size();
			List<Integer> indices = new ArrayList<>();
			for(int i=0; i<n; ++i) {
				if(!prob.expr.hasLeaf(i)) continue;
				indices.add(i);
			}
			indices.add(-1);
			List<String> predLabels = GraphInfSolver.constrainedInf(
					prob, runModel, rateModel, true);
			List<String> goldLabels = GraphInfSolver.getGoldLabels(prob);
			assert predLabels.size() == goldLabels.size();
			if((Arrays.asList(predLabels)+"").equals((Arrays.asList(goldLabels)+""))) {
				correct += 1.0;
			}
		}
		if(Params.printMistakes) System.out.print("Final ");
		System.out.println("Constrained Inference : "+correct+" / "+
				total+" = "+(correct/total));
		return (correct/total);
	}

	public static List<String> getGoldLabels(Problem prob) throws Exception {
		SLProblem rateSp = RateDriver.getSP(Arrays.asList(prob),
				Annotations.readRateAnnotations(Params.allArithDir+"rateAnnotations.txt"));
		SLProblem runSp = Annotations.getSP(Arrays.asList(prob),
				Annotations.readRateAnnotations(Params.allArithDir+"rateAnnotations.txt"));
		List<String> labels = new ArrayList<>();
		for(int i=1; i<rateSp.size(); i++) {
			labels.add(((RateY)rateSp.goldStructureList.get(i)).label);
		}
		labels.add(((RateY)rateSp.goldStructureList.get(0)).label);
		for(int i=0; i<runSp.size(); i++) {
			labels.add(((RunY)runSp.goldStructureList.get(i)).label);
		}
		return labels;
	}

	public static List<String> getBestStructure(Problem prob,
												Map<String, Double> runScores,
												Map<String, Double> rateScores,
												boolean useConstraints)
			throws Exception {
		if(!useConstraints) wRun = 1.0; // Things are independent
		PairComparator<List<String>> nodePairComparator =
				new PairComparator<List<String>>() {};
		MinMaxPriorityQueue<Pair<List<String>, Double>> beam1 =
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
				.maximumSize(200).create();
		MinMaxPriorityQueue<Pair<List<String>, Double>> beam2 =
				MinMaxPriorityQueue.orderedBy(nodePairComparator)
				.maximumSize(200).create();
		List<Integer> indices = createRelevantQuantIndexList(prob);
		int n = indices.size();
		List<String> init = new ArrayList<>();
		for(int i=0; i<n; ++i) {
			init.add("NOT_RATE");
		}
		// Labels only for relevant quantities, and for question
		// Order : Vertex label for each quantity, followed by vertex label of question,
		// next, edge labels for all edges (following order of for loop on the above
		// vertex order)
		List<String> labels = new ArrayList<>();
		labels.addAll(init);
		beam1.add(new Pair<>(labels, 0.0));
		for(int i=0; i<n; ++i) {
			labels = new ArrayList<>();
			labels.addAll(init);
			labels.set(i, "RATE");
			beam1.add(new Pair<>(labels, rateScores.get(indices.get(i)+"_RATE")));
			for(int j=i+1; j<n; ++j) {
				labels = new ArrayList<>();
				labels.addAll(init);
				labels.set(i, "RATE");
				labels.set(j, "RATE");
				beam1.add(new Pair<>(labels, rateScores.get(indices.get(i)+"_RATE") +
						rateScores.get(indices.get(j)+"_RATE")));
			}
		}
		for(int i=0; i<n-1; ++i) {
			for(int j=i+1; j<n; ++j) {
				for(String label : Arrays.asList(
						"SAME_UNIT", "1_RATE_1", "1_RATE_2",
						"2_RATE_1", "2_RATE_2", "NO_REL")) {
					for(Pair<List<String>, Double> pair : beam1) {
						labels = new ArrayList<>();
						labels.addAll(pair.getFirst());
						labels.add(label);
						if(useConstraints && !satisfyConstraints(
								labels.get(i),
								labels.get(j),
								label
						)) continue;
						beam2.add(new Pair<>(labels, pair.getSecond() +
						wRun*runScores.get(indices.get(i)+"_"+indices.get(j)+"_"+label)));
					}
				}
				beam1.clear();
				beam1.addAll(beam2);
				beam2.clear();
			}
		}
		assert beam1.element().getFirst().size() == n+n*(n-1)/2;
		return beam1.element().getFirst();
	}

	public static boolean satisfyConstraints(String vertex1, String vertex2, String edge) {
		if(vertex1.equals(vertex2) && edge.equals("SAME_UNIT")) return true;
		if(vertex1.equals("RATE") && vertex2.equals("NOT_RATE") &&
				edge.startsWith("1_RATE")) return true;
		if(vertex1.equals("NOT_RATE") && vertex2.equals("RATE") &&
				edge.startsWith("2_RATE")) return true;
		if(edge.equals("NO_REL")) return true;
		return false;
	}

	public static List<Integer> createRelevantQuantIndexList(Problem prob) {
		List<Integer> indices = new ArrayList<>();
		for(int i=0; i<prob.quantities.size(); ++i) {
			if(!prob.expr.hasLeaf(i)) continue;
			indices.add(i);
		}
		indices.add(-1);
		return indices;
	}


}