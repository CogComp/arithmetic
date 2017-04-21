package logic;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.utilities.commands.CommandDescription;
import edu.illinois.cs.cogcomp.core.utilities.commands.InteractiveShell;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLParameters;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import joint.Verbs;
import run.Annotations;
import structure.Node;
import structure.StanfordProblem;
import structure.StanfordSchema;
import utils.Folds;
import utils.Params;
import utils.Tools;

import java.util.*;

public class LogicDriver {

	@CommandDescription(description = "Params : train (true/false), dataset_folder")
	public static void crossVal(String train, String dataset)
			throws Exception {
		double acc1 = 0.0, acc2 = 0.0;
		int numFolds = Folds.getNumFolds(dataset);
		for(int i=0; i<numFolds; i++) {
			Pair<Double, Double> pair = doTrainTest(i, train, dataset);
			acc1 += pair.getFirst();
			acc2 += pair.getSecond();
		}
		System.out.println("CV : " + (acc1/numFolds) + " " + (acc2/numFolds));
	}

	@CommandDescription(description = "Params : testFold, train (true/false), dataset_folder")
	public static Pair<Double, Double> doTrainTest(int testFold, String isTrain, String dataset)
			throws Exception {
		List<List<StanfordProblem>> split = Folds.getDataSplitForStanford(dataset, testFold);
		List<StanfordProblem> trainProbs = split.get(0);
		List<StanfordProblem> testProbs = split.get(2);
		Map<Integer, List<Integer>> rateAnnotations =
				Annotations.readRateAnnotations(dataset+"rateAnnotations.txt");
		SLProblem train = getSP(trainProbs, rateAnnotations);
		SLProblem test = getSP(testProbs, rateAnnotations);
		System.out.println("Train : "+train.instanceList.size()+" Test : "+test.instanceList.size());
		if(isTrain.equalsIgnoreCase("true")) {
			trainModel("models/InfType"+testFold+".save", train);
		}
		return testModel("models/InfType"+testFold+".save", test);
	}

	public static SLProblem getSP(List<StanfordProblem> problemList,
								  Map<Integer, List<Integer>> rateAnnotations)
			throws Exception{
		SLProblem problem = new SLProblem();
		for(StanfordProblem prob : problemList){
            if(prob.id == 793 || prob.id == 838 || prob.id == 777 ||
                    prob.id == 778 || prob.id == 837 || prob.id == 1600 ||
					prob.id == 1610) continue;
			joint.LogicX x = new joint.LogicX(prob);
			joint.LogicY y = new joint.LogicY(x, prob.expr,
					rateAnnotations.containsKey(prob.id)?
							rateAnnotations.get(prob.id):new ArrayList<Integer>());
			List<Node> nodes = y.expr.getAllSubNodes();
            for(Node node : nodes) {
                if(node.children.size() == 0) continue;
                int quantLeft = node.children.get(0).quantIndex < node.children.get(1).quantIndex ?
                        node.children.get(0).quantIndex : node.children.get(1).quantIndex;
                int quantRight = node.children.get(0).quantIndex < node.children.get(1).quantIndex ?
                        node.children.get(1).quantIndex : node.children.get(0).quantIndex;
                LogicX logicX = new LogicX(prob, quantLeft, quantRight, node.infRuleType);
                String label = node.label;
                if(node.children.get(0).quantIndex >= node.children.get(1).quantIndex &&
                        node.label.equals("DIV")) {
                    label += "_REV";
                }
                LogicY logicY = new LogicY(label, null);
                problem.addExample(logicX, logicY);
                if(node.infRuleType == null) {
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
			LogicX prob = (LogicX) sp.instanceList.get(i);
			LogicY gold = (LogicY) sp.goldStructureList.get(i);
			LogicY pred = (LogicY) model.infSolver.getBestStructure(model.wv, prob);
			total.add(prob.problemId);
			if(LogicY.getLoss(gold, pred) < 0.0001) {
				acc += 1;
			} else {
				incorrect.add(prob.problemId);
				System.out.println(prob.problemId+" : "+prob.text);
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
				System.out.println("Loss : "+ LogicY.getLoss(gold, pred));
				System.out.println();
			}
		}
		System.out.println("Accuracy : = " + acc + " / " + sp.instanceList.size()
				+ " = " + (acc/sp.instanceList.size()));
		System.out.println("Strict Accuracy : ="+ (1-1.0*incorrect.size()/total.size()));
		return new Pair<>(acc/sp.instanceList.size(), 1-1.0*incorrect.size()/total.size());
	}

	public static void trainModel(String modelPath, SLProblem train)
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
		para.MAX_NUM_ITER = 5;
		Learner learner = LearnerFactory.getLearner(model.infSolver, fg, para);
		model.wv = latentSVMLearner(learner, train, (LogicInfSolver) model.infSolver, 5);
		lm.setAllowNewFeatures(false);
		model.saveModel(modelPath);
	}

	public static WeightVector latentSVMLearner(
			Learner learner, SLProblem sp, LogicInfSolver infSolver,
			int maxIter) throws Exception {
		WeightVector wv = new WeightVector(7000);
		wv.setExtendable(true);
		for(int i=0; i<maxIter; ++i) {
			System.err.println("Latent SSVM : Iteration "+i);
			SLProblem newProb = new SLProblem();
			for(int j=0; j<sp.goldStructureList.size(); ++j) {
				LogicX prob = (LogicX) sp.instanceList.get(j);
				LogicY gold = (LogicY) sp.goldStructureList.get(j);
				LogicY bestLatent = infSolver.getBestStructure(prob, gold, wv, true);
				newProb.addExample(prob, bestLatent);
			}
			System.err.println("Learning SSVM");
			wv = learner.train(newProb, wv);
			System.err.println("Done");
		}
		return wv;
	}

	public static void main(String[] args) throws Exception {
		InteractiveShell<LogicDriver> tester = new InteractiveShell<>(LogicDriver.class);
		if (args.length == 0) {
			tester.showDocumentation();
		} else {
			tester.runCommand(args);
		}
	}
}