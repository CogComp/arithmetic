package demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import constraints.ConsInfSolver;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import logic.LogicDriver;
import logic.LogicY;
import pair.PairX;
import relevance.RelX;
import structure.*;
import utils.Params;
import utils.Tools;


public class Trainer {
	
	public static SLModel relModel, pairModel, runModel, rateModel, corefModel, logicModel;
	public static String mode; // Has to be one of LCA, UnitDep, Logic

	public static void loadModels() throws Exception {
		if(mode.equals("LCA") || mode.equals("UnitDep")) {
			relModel = SLModel.loadModel(Params.modelDir+Params.relPrefix+Params.modelSuffix);
			pairModel = SLModel.loadModel(Params.modelDir+Params.pairPrefix+Params.modelSuffix);
			runModel = SLModel.loadModel(Params.modelDir+Params.runPrefix+Params.modelSuffix);
			rateModel = SLModel.loadModel(Params.modelDir+Params.ratePrefix+Params.modelSuffix);
		} else {
			corefModel = SLModel.loadModel(Params.modelDir+Params.corefPrefix+Params.modelSuffix);
			logicModel = SLModel.loadModel(Params.modelDir+Params.logicPrefix+Params.modelSuffix);
		}
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
//			for(int i=-1; i<prob.quants.size(); ++i) {
//				str += rateModel.infSolver.getBestStructure(rateModel.wv, new RateX(prob, i)) + "  ";
//			}
//			for(int i=0; i<prob.quants.size(); ++i) {
//				for(int j=i+1; j<prob.quants.size(); ++j) {
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
		logic.LogicY y = (LogicY) logicModel.infSolver.getBestStructure(
				logicModel.wv, new logic.LogicX(problem));
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

	public static void testModel(List<DataFormat> kfs) throws Exception {
		double acc = 0.0;
		for(DataFormat kf : kfs) {
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

}
