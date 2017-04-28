package demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import constraints.ConsDriver;
import constraints.ConsInfSolver;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import joint.LogicDriver;
import joint.LogicY;
import org.apache.commons.io.FileUtils;
import pair.PairDriver;
import pair.PairX;
import rate.RateDriver;
import reader.Reader;
import relevance.RelDriver;
import relevance.RelX;
import run.Annotations;
import run.RunDriver;
import structure.*;
import utils.Folds;
import utils.Params;
import utils.Tools;


public class Trainer {
	
	public static SLModel relModel, pairModel, runModel, rateModel, corefModel, logicModel;
	public static String mode; // Has to be one of LCA, UnitDep, Logic

	public static void loadModels() throws Exception {
		if(mode.equals("LCA") || mode.equals("UnitDep")) {
			relModel = SLModel.loadModel("models/Rel.save");
			pairModel = SLModel.loadModel("models/Pair.save");
			runModel = SLModel.loadModel("models/Run.save");
			rateModel = SLModel.loadModel("models/Rate.save");
		} else {
			corefModel = SLModel.loadModel("models/Coref.save");
			logicModel = SLModel.loadModel("models/Logic.save");
		}
	}
	
	public static void tuneAndRetrain(String dataset) throws Exception {
		System.out.println("Tuning ...");
		Params.printMistakes = false;
		Params.validationFrac = 0.0;
//		List<List<Problem>> split = Folds.getDataSplit(dataset, 0);
//		System.out.println("Training Relevance model ... ");
//		RelDriver.trainModel("models/Rel.save", RelDriver.getSP(split.get(0)));
//		System.out.println("Training Pair model ... ");
//		PairDriver.trainModel("models/Pair.save", PairDriver.getSP(split.get(0)));
//		System.out.println("Training Run model ... ");
//		RunDriver.trainModel("models/Run.save", Annotations.getSP(split.get(0),
//				Annotations.readRateAnnotations(Params.ratesFile)));
//		System.out.println("Training Rate model ... ");
//		RateDriver.trainModel("models/Rate.save", RateDriver.getSP(split.get(0),
//				Annotations.readRateAnnotations(Params.ratesFile)));
//		loadModels();
//		ConsDriver.tune(split.get(2), relModel, pairModel, runModel, rateModel);
		/**
		 * UnitDep
		 * wRate : 1.0
		 * wRun : 0.01
		 * wRel : 100.0
		 *
		 * LCA
		 * wRate : 0.0
		 * wRun : 0.0
		 * wRel : 100.0
		 */
		if(mode.equals("UnitDep")) {
			ConsInfSolver.wRate = 1.0;
			ConsInfSolver.wRun = 0.01;
			ConsInfSolver.wRel = 100.0;
		} else {
			ConsInfSolver.wRate = 0.0;
			ConsInfSolver.wRun = 0.0;
			ConsInfSolver.wRel = 100.0;
		}
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

	public static void trainLogicModel() throws Exception {
		System.out.println("Tuning ...");
		Params.printMistakes = false;
		Params.validationFrac = 0.0;
		LogicDriver.useGoldRelevance = true;
		System.out.println("Training on all training data");
		List<StanfordProblem> allProbs = Reader.readStanfordProblemsFromJson();
		System.out.println("Training Coref model ... ");
		logic.LogicDriver.trainModel("models/Coref.save", logic.LogicDriver.getSP(allProbs,
				Annotations.readRateAnnotations(Params.ratesFile), true));
		System.out.println("Training Logic model ... ");
		joint.LogicDriver.trainModel(
				"models/Logic.save",
				joint.LogicDriver.getSP(
						allProbs, Annotations.readRateAnnotations(Params.ratesFile), true),
				"models/Coref.save");
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

	public static double answerMathProblemWithLCA(String input) throws Exception {
		Problem problem = new Problem(0, input.trim(), 0);
		problem.quantities = Tools.quantifier.getSpans(input.trim());
		String newQues = "";
		int index = 0;
		for(QuantSpan qs : problem.quantities) {
			newQues += problem.question.substring(index, qs.start);
			newQues += qs.val;
			index = qs.end;
		}
		newQues += problem.question.substring(index);
		problem = new Problem(0, newQues, 0);
		problem.quantities = Tools.quantifier.getSpans(problem.question);
		if(problem.quantities.size() < 2) return 0.0;
		problem.extractAnnotations();
		Node node = ConsInfSolver.constrainedInf(problem, Trainer.relModel,
				Trainer.pairModel, Trainer.runModel, Trainer.rateModel);
		return node.getValue();
	}

	public static double answerMathProblemWithLogic(String input) throws Exception {
		LogicDriver.useGoldRelevance = false;
		StanfordProblem problem = new StanfordProblem(0, input.trim(), 0);
		problem.quantities = Tools.quantifier.getSpans(input.trim());
		String newQues = "";
		int index = 0;
		for(QuantSpan qs : problem.quantities) {
			newQues += problem.question.substring(index, qs.start);
			newQues += qs.val;
			index = qs.end;
		}
		newQues += problem.question.substring(index);
		problem = new StanfordProblem(0, newQues, 0);
		problem.quantities = Tools.quantifier.getSpans(problem.question);
		if(problem.quantities.size() < 2) return 0.0;
		problem.extractAnnotations();
		joint.LogicY y = (LogicY) logicModel.infSolver.getBestStructure(
				logicModel.wv, new joint.LogicX(problem));
		return y.expr.getValue();
	}

	public static double answerQuestion(String input) throws Exception {
		if(mode.equals("LCA") || mode.equals("UnitDep")) {
			return answerMathProblemWithLCA(input);
		}
		if(mode.equals("Logic")) {
			return answerMathProblemWithLogic(input);
		}
		return 0.0;
	}

	public static void testModel(List<KushmanFormat> kfs) throws Exception {
		double acc = 0.0;
		for(KushmanFormat kf : kfs) {
			double pred = answerQuestion(kf.sQuestion.trim());
			double gold = kf.lSolutions.get(0);
			if(Tools.safeEquals(pred, gold) || Tools.safeEquals(pred, -gold)) {
				System.out.println("Right: "+kf.sQuestion);
				acc += 1.0;
			} else {
				System.out.println("Wrong: "+kf.sQuestion);
			}
		}
		System.out.println("Acc = "+acc+" / "+kfs.size()+" = "+(acc / kfs.size()));
	}

	public static void commandLineDemo() throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			System.out.println("Enter question for "+mode+": (Type END to exit)");
			String question = br.readLine();
			if(question.trim().equals("END")) break;
			System.out.println("Answer: "+answerQuestion(question.trim()));
		}
	}

	public static void main(String args[]) throws Exception {
		if(args.length < 1) {
			System.err.println("Usage: Command <mode>(LCA/UnitDep/Logic) <questions_file>(optional)");
		}
		mode = args[0];
		if(!Arrays.asList("LCA", "UnitDep", "Logic").contains(mode)) {
			System.err.println("Mode has to be one of LCA, UnitDep, Logic");
		}
		if(mode.equals("LCA")) Params.noUDG = true;
		if(mode.equals("UnitDep")) Params.noUDG = false;
		List<KushmanFormat> kushmanProbs = null;
		if(args.length >= 2) {
			String questionsFile = args[1];
			String json = FileUtils.readFileToString(new File(questionsFile));
			kushmanProbs = new Gson().fromJson(
					json, new TypeToken<List<KushmanFormat>>(){}.getType());
		}
		if(mode.equals("Logic")) {
			trainLogicModel();
			corefModel = SLModel.loadModel("models/Coref.save");
			logicModel = SLModel.loadModel("models/Logic.save");
		} else {
			tuneAndRetrain(Params.allArithDir);
			relModel = SLModel.loadModel("models/Rel.save");
			pairModel = SLModel.loadModel("models/Pair.save");
			runModel = SLModel.loadModel("models/Run.save");
			rateModel = SLModel.loadModel("models/Rate.save");
		}
		if(kushmanProbs != null) {
			testModel(kushmanProbs);
		}
//		commandLineDemo();
	}

}
