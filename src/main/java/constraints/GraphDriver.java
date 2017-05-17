package constraints;

import edu.illinois.cs.cogcomp.sl.core.SLModel;
import rate.RateDriver;
import run.Annotations;
import run.RunDriver;
import structure.Problem;
import utils.Folds;
import utils.Params;

import java.util.ArrayList;
import java.util.List;

public class GraphDriver {

	public static double doTest(List<Problem> testProblems, int id) throws Exception {
		SLModel runModel = SLModel.loadModel(Params.modelDir+Params.runPrefix+id+Params.modelSuffix);
		SLModel rateModel = SLModel.loadModel(Params.modelDir+Params.ratePrefix+id+Params.modelSuffix);
		return GraphInfSolver.constrainedInf(testProblems, runModel, rateModel);
	}

	public static void tune(List<Problem> validation, SLModel runModel, SLModel rateModel)
			throws Exception {
		double vals[] = {0.01, 0.1, 1.0, 10.0, 100.0};
		double bestRun = 0.0;
		double bestAcc = 0.0;
		for(int a=0; a<vals.length; ++a) {
			GraphInfSolver.wRun = vals[a];
			double acc = GraphInfSolver.constrainedInf(validation, runModel, rateModel);
			if(acc > bestAcc) {
				bestAcc = acc;
				bestRun = GraphInfSolver.wRun;
			}
		}
		ConsInfSolver.wRun = bestRun;
	}

	public static void tune(List<Problem> devProbs, int id) throws Exception {
		SLModel runModel = SLModel.loadModel(Params.modelDir+Params.runPrefix+id+Params.modelSuffix);
		SLModel rateModel = SLModel.loadModel(Params.modelDir+Params.ratePrefix+id+Params.modelSuffix);
		tune(devProbs, runModel, rateModel);
	}

	public static Double doTrainTest(List<Problem> probs, List<Integer> trainIndices,
									 List<Integer> testIndices, int id) throws Exception {
		System.out.println("Tuning ...");
		Params.printMistakes = false;
		double validationFrac = 0.2;
		List<List<Problem>> split = Folds.getDataSplit(probs, trainIndices, testIndices, validationFrac);
		System.out.println("Training Run model ... ");
		RunDriver.trainModel(Params.modelDir+Params.runPrefix+id+Params.modelSuffix,
				Annotations.getSP(split.get(0)));
		System.out.println("Training Rate model ... ");
		RateDriver.trainModel(Params.modelDir+Params.ratePrefix+id+Params.modelSuffix,
				RateDriver.getSP(split.get(0)));
		tune(split.get(1), id);
		System.out.println("Tuned parameters");
		System.out.println("wRun : "+GraphInfSolver.wRun);
		System.out.println("Retraining on all training data");
		validationFrac = 0.0;
		split = Folds.getDataSplit(probs, trainIndices, testIndices, validationFrac);
		System.out.println("Training Run model ... ");
		RunDriver.trainModel(Params.modelDir+Params.runPrefix+id+Params.modelSuffix,
				Annotations.getSP(split.get(0)));
		System.out.println("Training Rate model ... ");
		RateDriver.trainModel(Params.modelDir+Params.ratePrefix+id+Params.modelSuffix,
				RateDriver.getSP(split.get(0)));
		return doTest(split.get(2), id);
	}

	public static void crossVal(
			List<Problem> probs, List<List<Integer>> foldIndices) throws Exception {
		double acc = 0.0;
		for(int i=0;i<foldIndices.size();i++) {
			List<Integer> train = new ArrayList<>();
			List<Integer> test = new ArrayList<>();
			for(int j=0; j<foldIndices.size(); ++j) {
				if(i==j) test.addAll(foldIndices.get(j));
				else train.addAll(foldIndices.get(j));
			}
			acc += doTrainTest(probs, train, test, i);
		}
		System.out.println("CV : " + (acc/foldIndices.size()));
	}

}
