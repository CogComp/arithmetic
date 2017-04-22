package demo;

import java.util.List;

import constraints.ConsDriver;
import constraints.ConsInfSolver;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import pair.PairDriver;
import pair.PairX;
import rate.RateDriver;
import reader.Reader;
import relevance.RelDriver;
import relevance.RelX;
import run.Annotations;
import run.RunDriver;
import structure.Problem;
import utils.Folds;
import utils.Params;

public class Trainer {
	
	public static SLModel relModel, pairModel, runModel, rateModel;

	public static void loadModels() throws Exception {
		relModel = SLModel.loadModel("models/Rel.save");
		pairModel = SLModel.loadModel("models/Pair.save");
		runModel = SLModel.loadModel("models/Run.save");
		rateModel = SLModel.loadModel("models/Rate.save");
	}
	
	public static void tuneAndRetrain(String dataset) throws Exception {
		System.out.println("Tuning ...");
		Params.printMistakes = false;
		Params.validationFrac = 0.0;
		List<List<Problem>> split = Folds.getDataSplit(dataset, 0);
		System.out.println("Training Relevance model ... ");
		RelDriver.trainModel("models/Rel.save", RelDriver.getSP(split.get(0)));
		System.out.println("Training Pair model ... ");
		PairDriver.trainModel("models/Pair.save", PairDriver.getSP(split.get(0)));
		System.out.println("Training Run model ... ");
		RunDriver.trainModel("models/Run.save", Annotations.getSP(split.get(0), 
				Annotations.readRateAnnotations(Params.ratesFile)));
		System.out.println("Training Rate model ... ");
		RateDriver.trainModel("models/Rate.save", RateDriver.getSP(split.get(0),
				Annotations.readRateAnnotations(Params.ratesFile)));
		loadModels();
		ConsDriver.tune(split.get(2), relModel, pairModel, runModel, rateModel);
		System.out.println("Tuned parameters");
		System.out.println("wRate : "+ConsInfSolver.wRate);
		System.out.println("wRun : "+ConsInfSolver.wRun);
		System.out.println("wRel : "+ConsInfSolver.wRel);
		System.out.println("Retraining on all training data");
		List<Problem> allProbs = Reader.readProblemsFromJson();
		System.out.println("Training Relevance model ... ");
		RelDriver.trainModel("models/Rel.save", RelDriver.getSP(allProbs));
		System.out.println("Training Pair model ... ");
		PairDriver.trainModel("models/Pair.save", PairDriver.getSP(allProbs));
		System.out.println("Training Run model ... ");
		RunDriver.trainModel("models/Run.save", Annotations.getSP(
				allProbs, Annotations.readRateAnnotations(Params.ratesFile)));
		System.out.println("Training Rate model ... ");
		RateDriver.trainModel("models/Rate.save", RateDriver.getSP(
				allProbs, Annotations.readRateAnnotations(Params.ratesFile)));
	}
	
	public static String genTableHtml(Problem prob, String answer) throws Exception {
		// Table for relevance
		String str = "{\"relevance\" : { ";
		for(int i=0; i<prob.quantities.size(); ++i) {
			str += "\""+prob.quantities.get(i).val+"\" : \""+relModel.infSolver.getBestStructure(
					relModel.wv, new RelX(prob, i)) + "\",";
		}
		str = str.substring(0, str.length()-1)+"},";
		// Table for pair
		str += "\"lca\" : { ";
		for(int i=0; i<prob.quantities.size(); ++i) {
			str += "\""+prob.quantities.get(i).val+"\" : {";
			for(int j=0; j<prob.quantities.size(); ++j) {
				if(j>i) {
					str += "\"" + prob.quantities.get(j).val + "\" : \"" +
							pairModel.infSolver.getBestStructure(
							pairModel.wv, new PairX(prob, i, j)) + "\",";
				} else if(i>j) {
					str += "\"" + prob.quantities.get(j).val + "\" : \"" +
							pairModel.infSolver.getBestStructure(
							pairModel.wv, new PairX(prob, j, i)) + "\",";
				} else {
					str += "\"" + prob.quantities.get(j).val + "\" : \"N/A\",";

				}
			}
			str = str.substring(0, str.length()-1)+"},";
		}
		str = str.substring(0, str.length()-1)+"}}";
		if(!Params.noUDG) {
			// TODO : Implementation for rate and run tables
//			for(int i=-1; i<prob.quantities.size(); ++i) {
//				str += rateModel.infSolver.getBestStructure(rateModel.wv, new RateX(prob, i)) + "  ";
//			}
//			for(int i=0; i<prob.quantities.size(); ++i) {
//				for(int j=i+1; j<prob.quantities.size(); ++j) {
//					str += runModel.infSolver.getBestStructure(runModel.wv, new RunX(prob, i, j)) + "  ";
//				}
//				str += runModel.infSolver.getBestStructure(runModel.wv, new RunX(prob, i, -1)) + "  ";
//			}
		}
		return "{\"answer\":\""+answer+"\", \"explanation\": "+str+"}";
	}
	
	public static String genNumberQueryHtml(String answer) {
		return "{\"answer\":\""+answer+"\", \"explanation\":\"Number Query\"}";
	}
	
	public static String genErrorHtml(String answer) {
		return "{\"answer\":\""+answer+"\", \"explanation\":\"\"}";
	}

}
