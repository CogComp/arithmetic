package coref;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLParameters;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import edu.stanford.nlp.ling.CoreLabel;
import logic.Verbs;
import structure.Node;
import structure.StanfordProblem;
import structure.StanfordSchema;
import utils.Folds;
import utils.Params;
import utils.Tools;

import java.util.*;

public class CorefDriver {

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
		Params.trainingNow = true;
		trainModel(Params.modelDir+Params.corefPrefix+id+Params.modelSuffix, train);
		Params.trainingNow = false;
		return testModel(Params.modelDir+Params.corefPrefix+id+Params.modelSuffix, test);
	}

	public static SLProblem getSP(List<StanfordProblem> problemList, boolean train)
			throws Exception{
		SLProblem problem = new SLProblem();
		for(StanfordProblem prob : problemList){
            if(prob.id == 793 || prob.id == 838 || prob.id == 777 ||
                    prob.id == 778 || prob.id == 837 || prob.id == 1600 ||
					prob.id == 1610 || prob.id == 1232857115 || prob.id == 1232856836 ||
					prob.id == 1232856820 || prob.id == 1232856830 || prob.id == 1232857096 ||
					prob.id == 1232857165 || prob.id == 1232857252) continue;
			logic.LogicX x = new logic.LogicX(prob);
			logic.LogicY y = new logic.LogicY(x, prob.expr, prob.rates);
			List<Node> nodes = y.expr.getAllSubNodes();
			List<CorefX> xList = new ArrayList<>();
			List<CorefY> yList = new ArrayList<>();
			boolean reasonsFound = true;
            for(Node node : nodes) {
                if(node.children.size() == 0) continue;
                int quantLeft = node.children.get(0).quantIndex < node.children.get(1).quantIndex ?
                        node.children.get(0).quantIndex : node.children.get(1).quantIndex;
                int quantRight = node.children.get(0).quantIndex < node.children.get(1).quantIndex ?
                        node.children.get(1).quantIndex : node.children.get(0).quantIndex;
                CorefX logicX = new CorefX(prob, quantLeft, quantRight, node.infRuleType);
                String label = node.label;
                if(node.children.get(0).quantIndex >= node.children.get(1).quantIndex &&
                        node.label.equals("DIV")) {
                    label += "_REV";
                }
                CorefY logicY = new CorefY(label, null);
				xList.add(logicX);
				yList.add(logicY);
				reasonsFound = true;
                if(node.infRuleType == null) {
					reasonsFound = false;
					System.out.println("==========================================");
                    System.out.println(prob.id+" : "+prob.question);
                    System.out.println();
                    for(StanfordSchema schema : prob.schema) {
                        System.out.println(schema);
                        System.out.println("VerbCat:"+ Tools.getKeyForMaxValue(Verbs.verbClassify(
                                prob.tokens.get(schema.sentId).get(schema.verb).lemma(),
                                Tools.spanToLemmaList(prob.tokens.get(schema.sentId), schema.unit))));
                    }
                    System.out.println(prob.questionSchema);
                    System.out.println();
                    System.out.println("Quantities : "+prob.quantities);
                    System.out.println("Quant of Interest: "+quantLeft+" "+quantRight);
                    System.out.println();
					System.out.println("==========================================");
					break;
                }
            }
            if(reasonsFound) {
				for(int i=0; i<xList.size(); ++i) {
					problem.addExample(xList.get(i), yList.get(i));
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
			System.out.println("==========================================");
			CorefX prob = (CorefX) sp.instanceList.get(i);
			CorefY gold = (CorefY) sp.goldStructureList.get(i);
			CorefY pred = (CorefY) model.infSolver.getBestStructure(model.wv, prob);
			total.add(prob.problemId);
			boolean correct = false;
			if(CorefY.getLoss(gold, pred) < 0.0001) {
				acc += 1;
				correct = true;
			} else {
				incorrect.add(prob.problemId);
			}
			if((correct && Params.printCorrect) ||
					(!correct && Params.printMistakes)){
				System.out.println(prob.problemId+" : "+prob.text);
				for(List<CoreLabel> tokens : prob.tokens) {
					for(CoreLabel token : tokens) {
						System.out.print(token.lemma()+"/"+token.tag()+" ");
					}
				}
				System.out.println();
				for(StanfordSchema schema : prob.schema) {
					System.out.println(schema);
					System.out.println("VerbCat:"+ Tools.getKeyForMaxValue(Verbs.verbClassify(
							prob.tokens.get(schema.sentId).get(schema.verb).lemma(),
							Tools.spanToLemmaList(prob.tokens.get(schema.sentId), schema.unit))));
				}
				System.out.println(prob.questionSchema);
				System.out.println("Wordnet: "+Arrays.asList(prob.wordnetRelations));
				System.out.println();
                System.out.println("InferenceType: "+prob.infType);
				System.out.println("Quantities : "+prob.quantities);
				System.out.println("Quant of Interest: "+prob.quantIndex1+" "+prob.quantIndex2);
                System.out.println();
				System.out.println("Gold : "+gold);
				System.out.println("Pred : "+pred);
				System.out.println("Loss : "+ CorefY.getLoss(gold, pred));
				System.out.println();
			}
			System.out.println("==========================================");
		}
		System.out.println("Accuracy : = " + acc + " / " + sp.instanceList.size()
				+ " = " + (acc/sp.instanceList.size()));
		System.out.println("Strict Accuracy : ="+ (1-1.0*incorrect.size()/total.size()));
		System.out.println("Incorrect Ids: "+ Arrays.asList(incorrect));
		return new Pair<>(acc/sp.instanceList.size(), 1-1.0*incorrect.size()/total.size());
	}

	public static void trainModel(String modelPath, SLProblem train)
			throws Exception {
		SLModel model = new SLModel();
		Lexiconer lm = new Lexiconer();
		lm.setAllowNewFeatures(true);
		model.lm = lm;
		CorefFeatGen fg = new CorefFeatGen(lm);
		model.featureGenerator = fg;
		model.infSolver = new CorefInfSolver(fg);
		SLParameters para = new SLParameters();
		para.loadConfigFile(Params.spConfigFile);
		para.MAX_NUM_ITER = 5;
		Learner learner = LearnerFactory.getLearner(model.infSolver, fg, para);
		model.wv = latentSVMLearner(learner, train, (CorefInfSolver) model.infSolver, 5);
		lm.setAllowNewFeatures(false);
		model.saveModel(modelPath);
	}

	public static WeightVector latentSVMLearner(
			Learner learner, SLProblem sp, CorefInfSolver infSolver,
			int maxIter) throws Exception {
		WeightVector wv = new WeightVector(7000);
		wv.setExtendable(true);
		for(int i=0; i<maxIter; ++i) {
			System.err.println("Latent SSVM : Iteration "+i);
			SLProblem newProb = new SLProblem();
			for(int j=0; j<sp.goldStructureList.size(); ++j) {
				CorefX prob = (CorefX) sp.instanceList.get(j);
				CorefY gold = (CorefY) sp.goldStructureList.get(j);
				CorefY bestLatent = infSolver.getBestStructure(prob, gold, wv, true);
				newProb.addExample(prob, bestLatent);
			}
			System.err.println("Learning SSVM");
			wv = learner.train(newProb, wv);
			System.err.println("Done");
		}
		return wv;
	}
}