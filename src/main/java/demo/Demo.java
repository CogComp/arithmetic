package demo;

import constraints.ConsInfSolver;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import logic.LogicY;
import structure.*;
import utils.Params;
import utils.Tools;


public class Demo {
	
	public static SLModel relModel, pairModel, runModel, rateModel, corefModel, logicModel;

	public static void loadModels() throws Exception {
		if(Driver.mode.equals("LCA") || Driver.mode.equals("UnitDep")) {
			relModel = SLModel.loadModel(Params.modelDir+Params.relPrefix+"100"+Params.modelSuffix);
			pairModel = SLModel.loadModel(Params.modelDir+Params.pairPrefix+"100"+Params.modelSuffix);
			if(Driver.mode.equals("UnitDep")) {
				runModel = SLModel.loadModel(Params.modelDir+Params.runPrefix+"100"+Params.modelSuffix);
				rateModel = SLModel.loadModel(Params.modelDir+Params.ratePrefix+"100"+Params.modelSuffix);
			}
		} else {
			corefModel = SLModel.loadModel(Params.modelDir+Params.corefPrefix+"100"+Params.modelSuffix);
			logicModel = SLModel.loadModel(Params.modelDir+Params.logicPrefix+"100"+Params.modelSuffix);
		}
	}

	public static String answerMathProblemWithLCA(String input) throws Exception {
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
		if(problem.quantities.size() < 2) return "";
		problem.extractAnnotations();
		Node node = ConsInfSolver.constrainedInf(problem, Demo.relModel,
				Demo.pairModel, Demo.runModel, Demo.rateModel);
		return node.toString()+" = "+node.getValue();
	}

	public static String answerMathProblemWithLogic(String input) throws Exception {
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
		if(problem.quantities.size() < 2) return "";
		problem.extractAnnotations();
		logic.LogicY y = (LogicY) logicModel.infSolver.getBestStructure(
				logicModel.wv, new logic.LogicX(problem));
		return y.expr.toString()+" = "+y.expr.getValue();
	}

	public static String answerQuestion(String input) throws Exception {
		if(Driver.mode.equals("LCA") || Driver.mode.equals("UnitDep")) {
			return answerMathProblemWithLCA(input);
		}
		if(Driver.mode.equals("E2ELogic")) {
			return answerMathProblemWithLogic(input);
		}
		return "";
	}
}
