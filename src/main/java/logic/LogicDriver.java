package logic;

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
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import structure.Problem;
import utils.Folds;
import utils.Params;

import java.util.*;

public class LogicDriver {
	
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
		SLProblem train = getSP(trainProbs);
		SLProblem test = getSP(testProbs);
		System.out.println("Train : "+train.instanceList.size()+" Test : "+test.instanceList.size());
		if(isTrain.equalsIgnoreCase("true")) {
			trainModel("models/Logic"+testFold+".save", train);
		}
		return testModel("models/Logic"+testFold+".save", test);
	}
	
	public static SLProblem getSP(List<Problem> problemList) throws Exception{
		SLProblem problem = new SLProblem();
		for(Problem prob : problemList){
			for(int i=0; i<prob.quantities.size(); ++i) {
				for(int j=i+1; j<prob.quantities.size(); ++j) {
					String label = prob.expr.findLabelofLCA(i, j);
					LogicX x = new LogicX(prob, i, j);
					LogicY y = new LogicY(label, -1);
					problem.addExample(x, y);
				}
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
			LogicX prob = (LogicX) sp.instanceList.get(i);
			LogicY gold = (LogicY) sp.goldStructureList.get(i);
			LogicY pred = (LogicY) model.infSolver.getBestStructure(model.wv, prob);
			total.add(prob.problemId);
			if(LogicY.getLoss(gold, pred) < 0.0001) {
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
				System.out.println("Loss : "+ LogicY.getLoss(gold, pred));
//				System.out.println("Labels : "+Arrays.asList(getLabelsWithScores(prob, model)));
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
		AbstractFeatureGenerator fg = new LogicFeatGen(lm);
		model.featureGenerator = fg;
		model.infSolver = new LogicInfSolver(fg);
		SLParameters para = new SLParameters();
		para.loadConfigFile(Params.spConfigFile);
		para.MAX_NUM_ITER = 5;
		Learner learner = LearnerFactory.getLearner(model.infSolver, fg, para);
		model.wv = latentSVMLearner(learner, train, (LogicInfSolver) model.infSolver, 5);
		lm.setAllowNewFeatures(false);
		model.saveModel(modelPath);
	}

	public static WeightVector latentSVMLearner(
			Learner learner, SLProblem sp, LogicInfSolver infSolver,
			int maxIter) throws Exception {
		WeightVector wv = new WeightVector(7000);
		wv.setExtendable(true);
		for(int i=0; i<maxIter; ++i) {
			System.err.println("Latent SSVM : Iteration "+i);
			SLProblem newProb = new SLProblem();
			for(int j=0; j<sp.goldStructureList.size(); ++j) {
				LogicX prob = (LogicX) sp.instanceList.get(j);
				LogicY gold = (LogicY) sp.goldStructureList.get(j);
				LogicY bestLatent = infSolver.getLatentBestStructure(prob, gold, wv);
				newProb.addExample(prob, bestLatent);
			}
			System.err.println("Learning SSVM");
			wv = learner.train(newProb, wv);
			System.err.println("Done");
		}
		return wv;
	}

	public static void main(String[] args) throws Exception {
		InteractiveShell<LogicDriver> tester = new InteractiveShell<LogicDriver>(LogicDriver.class);
		if (args.length == 0) {
			tester.showDocumentation();
		} else {
			tester.runCommand(args);
		}
	}
}