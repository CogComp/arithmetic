package relevance;

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

public class RelDriver {
	
	@CommandDescription(description = "Params : train (true/false), dataset_folder")
	public static void crossVal(String train, String dataset) 
			throws Exception {
		double acc1 = 0.0, acc2 = 0.0;
		int numFolds = Folds.getNumFolds(dataset);
		for(int i=0;i<numFolds; i++) {
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
		SLProblem train = getSP(trainProbs);
		SLProblem test = getSP(testProbs);
		System.out.println("Train : "+train.instanceList.size()+" Test : "+
				test.instanceList.size());
		if(isTrain.equalsIgnoreCase("true")) {
			trainModel("models/Rel"+testFold+".save", train);
		}
		return testModel("models/Rel"+testFold+".save", test);
	}
	
	public static SLProblem getSP(List<Problem> problemList) throws Exception{
		SLProblem problem = new SLProblem();
		for(Problem prob : problemList){
			for(int i=0; i<prob.quantities.size(); ++i) {
				RelX x = new RelX(prob, i);
				RelY y = new RelY(prob.expr.findRelevanceLabel(i));
				problem.addExample(x, y);
			}
		}
		return problem;
	}
	
	public static Pair<Double, Double> testModel(String modelPath, SLProblem sp)
			throws Exception {
		SLModel model = SLModel.loadModel(modelPath);
		Set<Integer> incorrect = new HashSet<>();
		Set<Integer> total = new HashSet<>();
		double acc = 0.0;
		for (int i = 0; i < sp.instanceList.size(); i++) {
			RelX prob = (RelX) sp.instanceList.get(i);
			RelY gold = (RelY) sp.goldStructureList.get(i);
			RelY pred = (RelY) model.infSolver.getBestStructure(model.wv, prob);
			total.add(prob.problemId);
			if(RelY.getLoss(gold, pred) < 0.0001) {
				acc += 1;
			} else {
				incorrect.add(prob.problemId);
				System.out.println(prob.problemId+" : "+prob.ta.getText());
				System.out.println();
				System.out.println("Schema : "+prob.schema);
				System.out.println();
				System.out.println("Quantities : "+prob.quantities);
				System.out.println("Quant of Interest: "+prob.quantIndex);
				System.out.println("Gold : "+gold);
				System.out.println("Pred : "+pred);
				System.out.println();
			}
		}
		System.out.println("Accuracy : = " + acc + " / " + sp.instanceList.size() 
				+ " = " + (acc/sp.instanceList.size()));
		System.out.println("Strict Accuracy : ="+ (1-1.0*incorrect.size()/total.size()));
		return new Pair<Double, Double>(acc/sp.instanceList.size(),
				1-1.0*incorrect.size()/total.size());
	}
	
	public static void trainModel(String modelPath, SLProblem train) 
			throws Exception {
		SLModel model = new SLModel();
		Lexiconer lm = new Lexiconer();
		lm.setAllowNewFeatures(true);
		model.lm = lm;
		AbstractFeatureGenerator fg = new RelFeatGen(lm);
		model.featureGenerator = fg;
		model.infSolver = new RelInfSolver(fg);
		SLParameters para = new SLParameters();
		para.loadConfigFile(Params.spConfigFile);
		para.MAX_NUM_ITER = 5;
		Learner learner = LearnerFactory.getLearner(model.infSolver, fg, para);
		model.wv = learner.train(train);
		lm.setAllowNewFeatures(false);
		model.saveModel(modelPath);
	}
	
	public static Map<String, Double> getLabelsWithScores(RelX prob, SLModel model) {
		List<String> labels = Arrays.asList("REL", "IRR");
		Map<String, Double> labelsWithScores = new HashMap<String, Double>();
		for(String label : labels) {
			labelsWithScores.put(prob.quantIndex+"_"+label,
					1.0*model.wv.dotProduct(model.featureGenerator.getFeatureVector(
							prob,
							new RelY(label))));
		}
		return labelsWithScores;
	}
	
	public static void main(String[] args) throws Exception {
		InteractiveShell<RelDriver> tester = new InteractiveShell<RelDriver>(
				RelDriver.class);
		if (args.length == 0) {
			tester.showDocumentation();
		} else {
			tester.runCommand(args);
		}
	}
}