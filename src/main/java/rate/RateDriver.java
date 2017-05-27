package rate;

import java.util.*;

import structure.Problem;
import utils.Folds;
import utils.Params;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLParameters;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class RateDriver {

	public static void crossVal(List<Problem> probs, List<List<Integer>> foldIndices)
			throws Exception {
		double acc1 = 0.0, acc2 = 0.0;
		for(int i=0;i<foldIndices.size(); i++) {
			List<Integer> train = new ArrayList<>();
			List<Integer> test = new ArrayList<>();
			for(int j=0; j<foldIndices.size(); ++j) {
				if(i==j) test.addAll(foldIndices.get(j));
				else train.addAll(foldIndices.get(j));
			}
			Pair<Double, Double> pair = doTrainTest(probs, train, test, i);
			acc1 += pair.getFirst();
			acc2 += pair.getSecond();
		}
		System.out.println("CV : " + (acc1/foldIndices.size()) + " " + (acc2/foldIndices.size()));
	}

	public static Pair<Double, Double> doTrainTest(
			List<Problem> probs, List<Integer> trainIndices,
			List<Integer> testIndices, int id) throws Exception {
		List<List<Problem>> split = Folds.getDataSplit(probs, trainIndices, testIndices, 0.0);
		List<Problem> trainProbs = split.get(0);
		List<Problem> testProbs = split.get(2);
		SLProblem train = getSP(trainProbs);
		SLProblem test = getSP(testProbs);
		System.out.println("Train : "+train.instanceList.size()+" Test : "+test.instanceList.size());
		trainModel(Params.modelDir+Params.ratePrefix+id+Params.modelSuffix, train);
		return testModel(Params.modelDir+Params.ratePrefix+id+Params.modelSuffix, test);
	}
	
	public static Pair<Double, Double> testModel(String modelPath, SLProblem sp)
			throws Exception {
		SLModel model = SLModel.loadModel(modelPath);
		Set<Integer> incorrect = new HashSet<>();
		Set<Integer> total = new HashSet<>();
		double acc = 0.0;
		double rateDetectedByRule = 0.0;
		double totalRates = 0.0;
		Map<Pair<String, String>, Integer> counts = new HashMap<>(); 
		for (int i = 0; i < sp.instanceList.size(); i++) {
			RateX prob = (RateX) sp.instanceList.get(i);
			RateY gold = (RateY) sp.goldStructureList.get(i);
			RateY pred = (RateY) model.infSolver.getBestStructure(model.wv, prob);
			total.add(prob.problemId);
			if(!counts.containsKey(new Pair<>(gold.label, pred.label))) {
				counts.put(new Pair<>(gold.label, pred.label), 1);
			} else {
				counts.put(new Pair<>(gold.label, pred.label),
						counts.get(new Pair<>(gold.label, pred.label))+1);
			}
			if(prob.quantIndex != -1 && gold.label.equalsIgnoreCase("RATE")) {
				totalRates += 1.0;
				if(prob.schema.quantSchemas.get(prob.quantIndex).rateUnit != null) {
					rateDetectedByRule += 1.0;
				}
			}
			boolean correct = false;
			if(RateY.getLoss(gold, pred) < 0.0001) {
				acc += 1;
				correct = true;
			} else {
				incorrect.add(prob.problemId);
			}
			if((correct && Params.printCorrect) ||
					(!correct && Params.printMistakes)){
				System.out.println(prob.problemId+" : "+prob.ta.getText());
				System.out.println();
				System.out.println("Schema : "+prob.schema);
				System.out.println();
				System.out.println("Quantities : "+prob.quantities);
				System.out.println("Quant of Interest: "+prob.quantIndex);
				System.out.println("Gold : "+gold);
				System.out.println("Pred : "+pred);
				System.out.println("Loss : "+RateY.getLoss(gold, pred));
				System.out.println("Labels : "+Arrays.asList(getLabelsWithScores(prob, model)));
				System.out.println();
			}
		}
		System.out.println("Accuracy : = " + acc + " / " + sp.instanceList.size() 
				+ " = " + (acc/sp.instanceList.size()));
		for(Pair<String, String> key : counts.keySet()) {
			double tot = 0, count = 0;
			for(Pair<String, String> key1 : counts.keySet()) {
				if(key1.getFirst().equals(key.getFirst())) {
					tot += counts.get(key1);
					if(key1.getFirst().equals(key1.getSecond())) {
						count += counts.get(key1);
					}
				}
			}
			System.out.println(key.getFirst()+" : "+count+" "+tot+" "+(count/tot));
		}
		System.out.println("Strict Accuracy : ="+ (1-1.0*incorrect.size()/total.size()));
		System.out.println("Rates Detected : ="+ rateDetectedByRule+" out of "+totalRates);
		return new Pair<Double, Double>(acc/sp.instanceList.size(),
				1-1.0*incorrect.size()/total.size());
	}
	
	public static void trainModel(String modelPath, SLProblem train) 
			throws Exception {
		SLModel model = new SLModel();
		Lexiconer lm = new Lexiconer();
		lm.setAllowNewFeatures(true);
		model.lm = lm;
		AbstractFeatureGenerator fg = new RateFeatGen(lm);
		model.featureGenerator = fg;
		model.infSolver = new RateInfSolver(fg);
		SLParameters para = new SLParameters();
		para.loadConfigFile(Params.spConfigFile);
		para.MAX_NUM_ITER = 5;
		Learner learner = LearnerFactory.getLearner(model.infSolver, fg, para);
		model.wv = learner.train(train);
		lm.setAllowNewFeatures(false);
		model.saveModel(modelPath);
	}
	
	public static Map<String, Double> getLabelsWithScores(RateX prob, SLModel model) {
		List<String> labels = Arrays.asList("RATE", "NOT_RATE");
		Map<String, Double> labelsWithScores = new HashMap<String, Double>();
		for(String label : labels) {
			labelsWithScores.put(prob.quantIndex+"_"+label,
					1.0*model.wv.dotProduct(model.featureGenerator.getFeatureVector(
							prob,
							new RateY(label))));
		}
		return labelsWithScores;
	}
	
	public static SLProblem getSP(List<Problem> problemList) throws Exception{
		SLProblem problem = new SLProblem();
		for(Problem prob : problemList) {
			for(int i=-1; i<prob.quantities.size(); ++i) {
				if(i>=0 && !prob.expr.hasLeaf(i)) continue;
				RateX x = new RateX(prob, i);
				String label = (prob.rates.contains(i)) ? "RATE" : "NOT_RATE";
				RateY y = new RateY(label);
				problem.addExample(x, y);
			}
			
		}
		return problem;
	}

}