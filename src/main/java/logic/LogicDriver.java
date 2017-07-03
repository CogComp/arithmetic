package logic;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLParameters;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import edu.stanford.nlp.ling.CoreLabel;
import structure.StanfordProblem;
import structure.StanfordSchema;
import utils.Folds;
import utils.Params;
import utils.Tools;

import java.util.*;

public class LogicDriver {

	public static SLModel corefModel;
	public static boolean useInfModel = true;
	public static boolean useGoldRelevance = true;

	public static void crossVal(List<StanfordProblem> probs, List<List<Integer>> foldIndices)
			throws Exception {
		double acc1 = 0.0, acc2 = 0.0;
		for(int i=0;i<foldIndices.size(); i++) {
			List<Integer> train = new ArrayList<>();
			List<Integer> test = new ArrayList<>();
			for(int j=0; j<foldIndices.size(); ++j) {
				if(i==j) test.addAll(foldIndices.get(j));
				else train.addAll(foldIndices.get(j));
			}
			Pair<Double, Double> pair = doTrainTest(probs, train, test, i);
			acc1 += pair.getFirst();
			acc2 += pair.getSecond();
		}
		System.out.println("CV : " + (acc1/foldIndices.size()) + " " + (acc2/foldIndices.size()));
	}

	public static Pair<Double, Double> doTrainTest(
			List<StanfordProblem> probs, List<Integer> trainIndices,
			List<Integer> testIndices, int id) throws Exception {
		List<List<StanfordProblem>> split = Folds.getDataSplitForStanford(
				probs, trainIndices, testIndices, 0.0);
		List<StanfordProblem> trainProbs = split.get(0);
		List<StanfordProblem> testProbs = split.get(2);
		SLProblem train = getSP(trainProbs, true);
		SLProblem test = getSP(testProbs, false);
		System.out.println("Train : "+train.instanceList.size()+" Test : "+test.instanceList.size());
		trainModel(Params.modelDir+Params.logicPrefix+id+Params.modelSuffix, train,
				Params.modelDir+Params.corefPrefix+id+Params.modelSuffix);
		return testModel(Params.modelDir+Params.logicPrefix+id+Params.modelSuffix, test);
	}
	
	public static SLProblem getSP(List<StanfordProblem> problemList, boolean train)
			throws Exception{
		SLProblem problem = new SLProblem();
		for(StanfordProblem prob : problemList){
			if(train && (prob.id == 793 || prob.id == 838 || prob.id == 777 ||
					prob.id == 778 || prob.id == 837 || prob.id == 1600 ||
					prob.id == 1610 || prob.id == 1232857115 || prob.id == 1232856836 ||
					prob.id == 1232856820 || prob.id == 1232856830 || prob.id == 1232857096 ||
					prob.id == 1232857165 || prob.id == 1232857252)) continue;
			LogicX x = new LogicX(prob);
			LogicY y = new LogicY(x, prob.expr, prob.rates);
			problem.addExample(x, y);
		}
		return problem;
	}

	public static Pair<Double, Double> testModel(String modelPath, SLProblem sp)
			throws Exception {
		SLModel model = SLModel.loadModel(modelPath);
		Set<Integer> incorrectKeys = new HashSet<>();
		incorrectKeys.addAll(Arrays.asList(164, 1108, 870, 136, 347, 1243, 1171,
				4, 916, 69, 1112, 1434, 1195, 1435, 1181, 1168, 1107, 1188, 151,
				907, 1227, 142, 158, 1231, 161, 3, 644, 1189, 902, 968, 1450, 747,
				1164, 1613, 1167, 1174, 951, 794, 117, 758, 1098, 1594, 1163, 140,
				1229, 1102, 831));
		double answerAcc = 0.0, relAcc = 0.0, infTypeAcc = 0.0, oldAcc = 0.0, newAcc = 0.0,
		oldTot = 0.0, newTot = 0.0;
		for (int i = 0; i < sp.instanceList.size(); i++) {
			LogicX prob = (LogicX) sp.instanceList.get(i);
			LogicY gold = (LogicY) sp.goldStructureList.get(i);
			LogicY pred = (LogicY) model.infSolver.getBestStructure(model.wv, prob);
			if(prob.problemId < 10000) {
				oldTot += 1;
			} else {
				newTot += 1;
			}
			if(LogicY.getLoss(pred, gold) < 0.01) {
				infTypeAcc += 1;
			}
			if(pred.expr.getAllSubNodes().size() == gold.expr.getAllSubNodes().size()) {
				relAcc += 1;
			}
			boolean correct = false;
			if(Tools.safeEquals(gold.expr.getValue(), pred.expr.getValue()) ||
					Tools.safeEquals(-gold.expr.getValue(), pred.expr.getValue())) {
				answerAcc += 1;
				correct = true;
				if(prob.problemId < 10000) {
					oldAcc += 1;
				} else {
					newAcc += 1;
				}
			}
			if((!correct && Params.printMistakes) ||
					(correct && Params.printCorrect)) {
				System.out.println(prob.problemId+" : "+prob.text);
				for(List<CoreLabel> tokens : prob.tokens) {
					for(CoreLabel token : tokens) {
						System.out.print(token.lemma()+" ");
					}
				}
				System.out.println();
				for(StanfordSchema schema : prob.schema) {
					System.out.println(schema);
					System.out.println("VerbCat:"+ Tools.getKeyForMaxValue(Verbs.verbClassify(
							prob.tokens.get(schema.sentId).get(schema.verb).lemma(),
							Tools.spanToLemmaList(prob.tokens.get(schema.sentId), schema.unit))));
				}
				System.out.println();
				System.out.println(Tools.spanToLemmaList(
						prob.tokens.get(prob.questionSchema.sentId), prob.questionSpan));
				System.out.println(prob.questionSchema);
				System.out.println();
				System.out.println("Quantities : "+prob.quantities);
				System.out.println("Gold : "+gold);
				System.out.println("Pred : "+pred);
				System.out.println();
			}
		}
//		System.out.println("Relevance Accuracy : = " + relAcc + " / " +
//				sp.instanceList.size() + " = " + (relAcc/sp.instanceList.size()));
//		System.out.println("Inference Type Accuracy : = " + infTypeAcc + " / " +
//				sp.instanceList.size() + " = " + (infTypeAcc/sp.instanceList.size()));
//		System.out.println("Old Accuracy : = " + oldAcc + " / " +
//				oldTot + " = " + (oldAcc/oldTot));
//		System.out.println("New Accuracy : = " + newAcc + " / " +
//				newTot + " = " + (newAcc/newTot));
		System.out.println("Answer Accuracy : = " + answerAcc + " / " +
				sp.instanceList.size() + " = " + (answerAcc/sp.instanceList.size()));
		return new Pair<>(answerAcc/sp.instanceList.size(), answerAcc/sp.instanceList.size());
	}

	public static void trainModel(String modelPath, SLProblem train, String corefModelPath)
			throws Exception {
		SLModel model = new SLModel();
		Lexiconer lm = new Lexiconer();
		lm.setAllowNewFeatures(true);
		model.lm = lm;
		LogicFeatGen fg = new LogicFeatGen(lm);
		model.featureGenerator = fg;
		model.infSolver = new LogicInfSolver(fg);
		SLParameters para = new SLParameters();
		para.loadConfigFile(Params.spConfigFile);
		para.MAX_NUM_ITER = 10;
		Learner learner = LearnerFactory.getLearner(model.infSolver, fg, para);
		if(useInfModel) {
			corefModel = SLModel.loadModel(corefModelPath);
			model.wv = learner.train(train);
		} else {
			model.wv = latentSVMLearner(learner, train,
					(LogicInfSolver) model.infSolver, model.wv, 5);
		}
		lm.setAllowNewFeatures(false);
		model.saveModel(modelPath);
	}

	public static WeightVector latentSVMLearner(
			Learner learner,
			SLProblem sp,
			LogicInfSolver infSolver,
			WeightVector initWeight,
			int maxIter) throws Exception {
		WeightVector wv;
		if(initWeight == null) {
			wv = new WeightVector(7000);
		} else {
			wv = initWeight;
		}
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

}