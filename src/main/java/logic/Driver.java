package logic;

import coref.CorefDriver;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import structure.StanfordProblem;
import utils.Folds;
import utils.Params;

import java.util.ArrayList;
import java.util.List;

public class Driver {

    public static double doTrainTest(List<StanfordProblem> probs,
                                     List<Integer> trainIndices,
                                     List<Integer> testIndices,
                                     int id) throws Exception {
        List<List<StanfordProblem>> split = Folds.getDataSplitForStanford(
                probs, trainIndices, testIndices, 0.0);
        LogicDriver.useGoldRelevance = true;
        System.out.println("Training Coreference model ... ");
        CorefDriver.trainModel(Params.modelDir+Params.corefPrefix+id+Params.modelSuffix,
                CorefDriver.getSP(split.get(0), true));
        System.out.println("Training Logic model ... ");
        LogicDriver.trainModel(Params.modelDir+Params.logicPrefix+id+Params.modelSuffix,
                LogicDriver.getSP(split.get(0), true),
                Params.modelDir+Params.corefPrefix+id+Params.modelSuffix);
        LogicDriver.useGoldRelevance = false;
        SLProblem test = new SLProblem();
        for(StanfordProblem prob : split.get(2)) {
            LogicX x = new LogicX(prob);
            LogicY y = new LogicY(prob.expr);
            test.addExample(x, y);
        }
        Pair<Double, Double> scores = LogicDriver.testModel(
                Params.modelDir+Params.logicPrefix+id+Params.modelSuffix,
                test);
        return scores.getSecond();
    }

    public static void crossVal(List<StanfordProblem> probs,
                                List<List<Integer>> foldIndices) throws Exception {
        double acc = 0.0;
        LogicDriver.useInfModel = true;
        for(int i=0; i<foldIndices.size(); i++) {
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
}
