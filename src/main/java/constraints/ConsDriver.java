package constraints;

import java.util.ArrayList;
import java.util.List;

import pair.PairDriver;
import rate.RateDriver;
import relevance.RelDriver;
import run.Annotations;
import run.RunDriver;
import structure.Problem;
import utils.Folds;
import utils.Params;
import edu.illinois.cs.cogcomp.sl.core.SLModel;

public class ConsDriver {

	public static double doTest(List<Problem> testProblems, int id) throws Exception {
		SLModel relModel = SLModel.loadModel(Params.modelDir+Params.relPrefix+id+Params.modelSuffix);
		SLModel pairModel = SLModel.loadModel(Params.modelDir+Params.pairPrefix+id+Params.modelSuffix);
		SLModel runModel = null, rateModel = null;
		if(!Params.noUDG) {
			runModel = SLModel.loadModel(Params.modelDir + Params.runPrefix + id + Params.modelSuffix);
			rateModel = SLModel.loadModel(Params.modelDir + Params.ratePrefix + id + Params.modelSuffix);
		}
		return ConsInfSolver.constrainedInf(testProblems, relModel, pairModel, runModel, rateModel);
	}
	
	public static void tune(List<Problem> validation, SLModel relModel, SLModel pairModel,
			SLModel runModel, SLModel rateModel) throws Exception {
		double vals[] = {0.01, 0.1, 1.0, 10.0, 100.0};
		double bestRate = 0.0;
		double bestRun = 0.0;
		double bestRel = 0.0;
		double bestAcc = 0.0;
		if(Params.noUDG) {
			for(int c=0; c<vals.length; ++c) {
				ConsInfSolver.wRel = vals[c];
				double acc = ConsInfSolver.constrainedInf(validation, relModel, 
						pairModel, runModel, rateModel);
				if(acc > bestAcc) {
					bestAcc = acc;
					bestRel = ConsInfSolver.wRel;
				}
			}
		} else {
			for(int a=0; a<vals.length; ++a) {
				for(int b=0; b<vals.length; ++b) {
					for(int c=0; c<vals.length; ++c) {
						ConsInfSolver.wRun = vals[a];
						ConsInfSolver.wRate = vals[b];
						ConsInfSolver.wRel = vals[c];
						double acc = ConsInfSolver.constrainedInf(
								validation, relModel, pairModel, runModel, rateModel);
						if(acc > bestAcc) {
							bestAcc = acc;
							bestRun = ConsInfSolver.wRun;
							bestRate = ConsInfSolver.wRate;
							bestRel = ConsInfSolver.wRel;
						}
					}
				}
			}
		}
		ConsInfSolver.wRate = bestRate;
		ConsInfSolver.wRun = bestRun;
		ConsInfSolver.wRel = bestRel;
	}
	
	public static void tune(List<Problem> devProbs, int id) throws Exception {
		SLModel relModel = SLModel.loadModel(Params.modelDir+Params.relPrefix+id+Params.modelSuffix);
		SLModel pairModel = SLModel.loadModel(Params.modelDir+Params.pairPrefix+id+Params.modelSuffix);
		SLModel runModel = SLModel.loadModel(Params.modelDir+Params.runPrefix+id+Params.modelSuffix);
		SLModel rateModel = SLModel.loadModel(Params.modelDir+Params.ratePrefix+id+Params.modelSuffix);
		tune(devProbs, relModel, pairModel, runModel, rateModel);
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

	public static Double doTrainTest(List<Problem> probs, List<Integer> trainIndices,
									 List<Integer> testIndices, int id) throws Exception {
		double validationFrac;
		System.out.println("Tuning ...");
		validationFrac = 0.2;
		List<List<Problem>> split = Folds.getDataSplit(probs, trainIndices, testIndices, validationFrac);
		System.out.println("Training Relevance model ... ");
		RelDriver.trainModel(Params.modelDir+Params.relPrefix+id+Params.modelSuffix,
				RelDriver.getSP(split.get(0)));
		System.out.println("Training Pair model ... ");
		PairDriver.trainModel(Params.modelDir+Params.pairPrefix+id+Params.modelSuffix,
				PairDriver.getSP(split.get(0)));
		System.out.println("Training Run model ... ");
		RunDriver.trainModel(Params.modelDir+Params.runPrefix+id+Params.modelSuffix,
				Annotations.getSP(split.get(0)));
		System.out.println("Training Rate model ... ");
		RateDriver.trainModel(Params.modelDir+Params.ratePrefix+id+Params.modelSuffix,
				RateDriver.getSP(split.get(0)));
		tune(split.get(1), id);
		System.out.println("Tuned parameters");
		System.out.println("wRate : "+ConsInfSolver.wRate);
		System.out.println("wRun : "+ConsInfSolver.wRun);
		System.out.println("wRel : "+ConsInfSolver.wRel);
		System.out.println("Retraining on all training data");
		validationFrac = 0.0;
		split = Folds.getDataSplit(probs, trainIndices, testIndices, validationFrac);
		System.out.println("Training Relevance model ... ");
		RelDriver.trainModel(Params.modelDir+Params.relPrefix+id+Params.modelSuffix,
				RelDriver.getSP(split.get(0)));
		System.out.println("Training Pair model ... ");
		PairDriver.trainModel(Params.modelDir+Params.pairPrefix+id+Params.modelSuffix,
				PairDriver.getSP(split.get(0)));
		System.out.println("Training Run model ... ");
		RunDriver.trainModel(Params.modelDir+Params.runPrefix+id+Params.modelSuffix,
				Annotations.getSP(split.get(0)));
		System.out.println("Training Rate model ... ");
		RateDriver.trainModel(Params.modelDir+Params.ratePrefix+id+Params.modelSuffix,
				RateDriver.getSP(split.get(0)));
		return doTest(split.get(2), id);
	}
 
}
