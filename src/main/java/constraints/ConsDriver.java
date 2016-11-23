package constraints;

import java.util.List;

import pair.PairDriver;
import rate.RateDriver;
import relevance.RelDriver;
import run.Annotations;
import run.RunDriver;
import structure.Problem;
import utils.Folds;
import utils.Params;
import edu.illinois.cs.cogcomp.core.utilities.commands.InteractiveShell;
import edu.illinois.cs.cogcomp.sl.core.SLModel;

public class ConsDriver {

	public static double doTest(int testFold, String dataset) throws Exception {
		List<List<Problem>> split = Folds.getDataSplit(dataset, testFold);
		SLModel relModel = SLModel.loadModel("models/Rel"+testFold+".save");
		SLModel pairModel = SLModel.loadModel("models/Pair"+testFold+".save");
		SLModel runModel = SLModel.loadModel("models/Run"+testFold+".save");
		SLModel rateModel = SLModel.loadModel("models/Rate"+testFold+".save");
		return ConsInfSolver.constrainedInf(split.get(2), relModel, pairModel, 
				runModel, rateModel);
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
	
	public static void tune(int testFold, String dataset) throws Exception {
		List<List<Problem>> split = Folds.getDataSplit(dataset, testFold);
		SLModel relModel = SLModel.loadModel("models/Rel"+testFold+".save");
		SLModel pairModel = SLModel.loadModel("models/Pair"+testFold+".save");
		SLModel runModel = SLModel.loadModel("models/Run"+testFold+".save");
		SLModel rateModel = SLModel.loadModel("models/Rate"+testFold+".save");
		tune(split.get(1), relModel, pairModel, runModel, rateModel);
	}
	
	public static void tunedCrossValAndRetrain(String dataset, String noUDG) throws Exception {
		Params.noUDG = Boolean.parseBoolean(noUDG);
		double acc = 0.0;
		int numFolds = Folds.getNumFolds(dataset);
		for(int i=0;i<numFolds;i++) {
			System.out.println("Tuning ...");
			Params.printMistakes = false;
			Params.validationFrac = 0.2;
			List<List<Problem>> split = Folds.getDataSplit(dataset, i);
			System.out.println("Training Relevance model ... ");
			RelDriver.trainModel("models/Rel"+i+".save", RelDriver.getSP(split.get(0)));
			System.out.println("Training Pair model ... ");
			PairDriver.trainModel("models/Pair"+i+".save", PairDriver.getSP(split.get(0)));
			System.out.println("Training Run model ... ");
			RunDriver.trainModel("models/Run"+i+".save", Annotations.getSP(
					split.get(0),
//					ExtractRate.computeRateAnnotations(dataset)));
					Annotations.readRateAnnotations(dataset+"rateAnnotations.txt")));
			System.out.println("Training Rate model ... ");
			RateDriver.trainModel("models/Rate"+i+".save", RateDriver.getSP(
					split.get(0),
//					ExtractRate.computeRateAnnotations(dataset)));
					Annotations.readRateAnnotations(dataset+"rateAnnotations.txt")));
			tune(i, dataset);
			System.out.println("Tuned parameters");
			System.out.println("wRate : "+ConsInfSolver.wRate);
			System.out.println("wRun : "+ConsInfSolver.wRun);
			System.out.println("wRel : "+ConsInfSolver.wRel);
			System.out.println("Retraining on all training data");
			Params.validationFrac = 0.0;
			split = Folds.getDataSplit(dataset, i);
			System.out.println("Training Relevance model ... ");
			RelDriver.trainModel("models/Rel"+i+".save", RelDriver.getSP(split.get(0)));
			System.out.println("Training Pair model ... ");
			PairDriver.trainModel("models/Pair"+i+".save", PairDriver.getSP(split.get(0)));
			System.out.println("Training Run model ... ");
			RunDriver.trainModel("models/Run"+i+".save", Annotations.getSP(
					split.get(0),
//					ExtractRate.computeRateAnnotations(dataset)));
					Annotations.readRateAnnotations(dataset+"rateAnnotations.txt")));
			System.out.println("Training Rate model ... ");
			RateDriver.trainModel("models/Rate"+i+".save", RateDriver.getSP(
					split.get(0),
//					ExtractRate.computeRateAnnotations(dataset)));
					Annotations.readRateAnnotations(dataset+"rateAnnotations.txt")));
			Params.printMistakes = true;
			acc += doTest(i, dataset);
		}
		System.out.println("CV : " + (acc/numFolds));
	}
	
	public static void main(String[] args) throws Exception {
		InteractiveShell<ConsDriver> tester = new InteractiveShell<ConsDriver>(
				ConsDriver.class);
		if (args.length == 0) {
			tester.showDocumentation();
		} else {
			tester.runCommand(args);
		}
	}
 
}
