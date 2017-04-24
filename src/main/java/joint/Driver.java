package joint;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.utilities.commands.InteractiveShell;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import run.Annotations;
import structure.StanfordProblem;
import utils.Folds;
import utils.Params;

import java.util.List;

public class Driver {

    public static void runLogicSolver(String dataset) throws Exception {
        double acc = 0.0;
        int numFolds = Folds.getNumFolds(dataset);
        Params.validationFrac = 0.0;
        LogicDriver.useInfModel = true;

        for(int i=0;i<numFolds;i++) {
            List<List<StanfordProblem>> split = Folds.getDataSplitForStanford(dataset, i);
            System.out.println("Training Coreference model ... ");
            logic.LogicDriver.trainModel(Params.modelDir+Params.corefPrefix+i+Params.modelSuffix,
                    logic.LogicDriver.getSP(
                            split.get(0),
                            Annotations.readRateAnnotations(Params.ratesFile),
                            true));
            System.out.println("Training Logic model ... ");
            LogicDriver.useGoldRelevance = true;
            LogicDriver.trainModel(Params.modelDir+Params.logicPrefix+i+Params.modelSuffix,
                    LogicDriver.getSP(
                            split.get(0),
                            Annotations.readRateAnnotations(Params.ratesFile),
                            true),
                    Params.modelDir+Params.corefPrefix+i+Params.modelSuffix);
            Params.printMistakes = true;
            LogicDriver.useGoldRelevance = true;
            SLProblem test = new SLProblem();
            for(StanfordProblem prob : split.get(2)) {
                LogicX x = new LogicX(prob);
                LogicY y = new LogicY(prob.expr);
                test.addExample(x, y);
            }
            Pair<Double, Double> scores = LogicDriver.testModel(
                    Params.modelDir+Params.logicPrefix+i+Params.modelSuffix,
                    test);
            acc += scores.getSecond();
        }
        System.out.println("CV : " + (acc/numFolds));
    }

    public static void main(String[] args) throws Exception {
        InteractiveShell<Driver> tester = new InteractiveShell<>(Driver.class);
        if (args.length == 0) {
            tester.showDocumentation();
        } else {
            tester.runCommand(args);
        }
    }
}
