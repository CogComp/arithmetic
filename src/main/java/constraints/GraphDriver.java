package constraints;

import edu.illinois.cs.cogcomp.core.utilities.commands.InteractiveShell;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import rate.RateDriver;
import run.Annotations;
import run.RunDriver;
import structure.Problem;
import utils.Folds;
import utils.Params;

import java.util.List;

public class GraphDriver {

	public static double doTest(int testFold, String dataset) throws Exception {
		List<List<Problem>> split = Folds.getDataSplit(dataset, testFold);
		SLModel runModel = SLModel.loadModel("models/Run"+testFold+".save");
		SLModel rateModel = SLModel.loadModel("models/Rate"+testFold+".save");
		return GraphInfSolver.constrainedInf(split.get(2), runModel, rateModel);
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

	public static void tune(int testFold, String dataset) throws Exception {
		List<List<Problem>> split = Folds.getDataSplit(dataset, testFold);
		SLModel runModel = SLModel.loadModel("models/Run"+testFold+".save");
		SLModel rateModel = SLModel.loadModel("models/Rate"+testFold+".save");
		tune(split.get(1), runModel, rateModel);
	}

	public static void tunedCrossValAndRetrain(String dataset) throws Exception {
		double acc = 0.0;
		int numFolds = Folds.getNumFolds(dataset);
		for(int i=0;i<numFolds;i++) {
			System.out.println("Tuning ...");
			Params.printMistakes = false;
			Params.validationFrac = 0.2;
			List<List<Problem>> split = Folds.getDataSplit(dataset, i);
			System.out.println("Training Run model ... ");
			RunDriver.trainModel("models/Run"+i+".save", Annotations.getSP(
					split.get(0),
					Annotations.readRateAnnotations(Params.ratesFile)));
			System.out.println("Training Rate model ... ");
			RateDriver.trainModel("models/Rate"+i+".save", RateDriver.getSP(
					split.get(0),
					Annotations.readRateAnnotations(Params.ratesFile)));
			tune(i, dataset);
			System.out.println("Tuned parameters");
			System.out.println("wRun : "+GraphInfSolver.wRun);
			System.out.println("Retraining on all training data");
			Params.validationFrac = 0.0;
			split = Folds.getDataSplit(dataset, i);
			System.out.println("Training Run model ... ");
			RunDriver.trainModel("models/Run"+i+".save", Annotations.getSP(
					split.get(0),
					Annotations.readRateAnnotations(Params.ratesFile)));
			System.out.println("Training Rate model ... ");
			RateDriver.trainModel("models/Rate"+i+".save", RateDriver.getSP(
					split.get(0),
					Annotations.readRateAnnotations(Params.ratesFile)));
			Params.printMistakes = true;
			acc += doTest(i, dataset);
		}
		System.out.println("CV : " + (acc/numFolds));
	}

	public static void main(String[] args) throws Exception {
		InteractiveShell<GraphDriver> tester = new InteractiveShell<GraphDriver>(
				GraphDriver.class);
		if (args.length == 0) {
			tester.showDocumentation();
		} else {
			tester.runCommand(args);
		}
	}

}
