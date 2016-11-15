package run;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import structure.Problem;
import utils.Folds;
import utils.Params;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.utilities.commands.CommandDescription;
import edu.illinois.cs.cogcomp.core.utilities.commands.InteractiveShell;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLParameters;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class RunDriver {
	
	@CommandDescription(description = "Params : train (true/false), dataset_folder")
	public static void crossVal(String train, String dataset) 
			throws Exception {
		double acc1 = 0.0, acc2 = 0.0;
		int numFolds = Folds.getNumFolds(dataset);
		for(int i=0; i<numFolds; i++) {
			Pair<Double, Double> pair = doTrainTest(i, train, dataset);
			acc1 += pair.getFirst();
			acc2 += pair.getSecond();
		}
		System.out.println("CV : " + (acc1/numFolds) + " " + (acc2/numFolds));
	}

	@CommandDescription(description = "Params : testFold, train (true/false), dataset_folder")
	public static Pair<Double, Double> doTrainTest(int testFold, String isTrain, String dataset) 
			throws Exception {
		List<List<Problem>> split = Folds.getDataSplit(dataset, testFold);
		List<Problem> trainProbs = split.get(0);
		List<Problem> testProbs = split.get(2);
		SLProblem train = Annotations.getSP(trainProbs, Annotations.readRateAnnotations(
				dataset+"rateAnnotations.txt"));
		SLProblem test = Annotations.getSP(testProbs, Annotations.readRateAnnotations(
				dataset+"rateAnnotations.txt"));
		System.out.println("Train : "+train.instanceList.size()+" Test : "+test.instanceList.size());
		if(isTrain.equalsIgnoreCase("true")) {
			trainModel("models/Run"+testFold+".save", train);
		}
		return testModel("models/Run"+testFold+".save", test);
	}
	
	public static Pair<Double, Double> testModel(String modelPath, SLProblem sp)
			throws Exception {
		SLModel model = SLModel.loadModel(modelPath);
		Set<Integer> incorrect = new HashSet<>();
		Set<Integer> total = new HashSet<>();
		double acc = 0.0;
		int countNoRel = 0;
		Map<Pair<String, String>, Integer> counts = new HashMap<>(); 
		for (int i = 0; i < sp.instanceList.size(); i++) {
			RunX prob = (RunX) sp.instanceList.get(i);
			RunY gold = (RunY) sp.goldStructureList.get(i);
			RunY pred = (RunY) model.infSolver.getBestStructure(model.wv, prob);
			if(pred.label.equalsIgnoreCase("NO_REL")) countNoRel++;
			total.add(prob.problemId);
			if(!counts.containsKey(new Pair<String, String>(gold.label, pred.label))) {
				counts.put(new Pair<String, String>(gold.label, pred.label), 1);
			} else {
				counts.put(new Pair<String, String>(gold.label, pred.label), 
						counts.get(new Pair<String, String>(gold.label, pred.label))+1);
			}
			if(RunY.getLoss(gold, pred) < 0.0001) {
				acc += 1;
			} else {
				incorrect.add(prob.problemId);
				System.out.println(prob.problemId+" : "+prob.ta.getText());
				System.out.println();
				System.out.println("Schema : "+prob.schema);
				System.out.println();
				System.out.println("Quantities : "+prob.quantities);
				System.out.println("Quant of Interest: "+prob.quantIndex1+" "+prob.quantIndex2);
				System.out.println("Gold : "+gold);
				System.out.println("Pred : "+pred);
				System.out.println("Loss : "+RunY.getLoss(gold, pred));
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
		System.out.println("NoRel : ="+ countNoRel);
		return new Pair<Double, Double>(acc/sp.instanceList.size(),
				1-1.0*incorrect.size()/total.size());
	}
	
	public static void trainModel(String modelPath, SLProblem train) 
			throws Exception {
		SLModel model = new SLModel();
		Lexiconer lm = new Lexiconer();
		lm.setAllowNewFeatures(true);
		model.lm = lm;
		AbstractFeatureGenerator fg = new RunFeatGen(lm);
		model.featureGenerator = fg;
		model.infSolver = new RunInfSolver(fg);
		SLParameters para = new SLParameters();
		para.loadConfigFile(Params.spConfigFile);
		para.MAX_NUM_ITER = 5;
		Learner learner = LearnerFactory.getLearner(model.infSolver, fg, para);
		model.wv = learner.train(train);
		lm.setAllowNewFeatures(false);
		model.saveModel(modelPath);
	}
	
	public static Map<String, Double> getLabelsWithScores(RunX prob, SLModel model) {
		List<String> labels = Arrays.asList("SAME_UNIT", "1_RATE_1", "1_RATE_2", 
				"2_RATE_1", "2_RATE_2", "NO_REL");
		Map<String, Double> labelsWithScores = new HashMap<String, Double>();
		for(String label : labels) {
			labelsWithScores.put(prob.quantIndex1+"_"+prob.quantIndex2+"_"+label,
					1.0*model.wv.dotProduct(model.featureGenerator.getFeatureVector(
							prob,
							new RunY(label))));
		}
		return labelsWithScores;
	}

	public static void main(String[] args) throws Exception {
		InteractiveShell<RunDriver> tester = new InteractiveShell<RunDriver>(
				RunDriver.class);
		if (args.length == 0) {
			tester.showDocumentation();
		} else {
			tester.runCommand(args);
		}
	}
}