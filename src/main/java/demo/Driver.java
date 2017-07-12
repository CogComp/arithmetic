package demo;

import coref.CorefDriver;
import pair.PairDriver;
import rate.RateDriver;
import reader.Reader;
import relevance.RelDriver;
import run.RunDriver;
import structure.Problem;
import structure.StanfordProblem;
import utils.Folds;
import utils.Params;
import utils.Tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class Driver {

    public static String mode;

    public static void main(String args[]) throws Exception {

        String dataFile = null, modelDir = null;
        List<String> trainFolds = null, testFolds = null, cvFolds = null;
        List<String> commands = Arrays.asList("--data", "--mode", "--train",
                "--test", "--cv", "--model_dir", "--print_mistakes", "--print_correct",
                "--demo", "--demo_server");
        boolean doCV = false;
        Params.printMistakes = false;
        Params.printCorrect = false;

        String str = "Usage: \n";
        str += "--mode\t\t\tMode of operation (one of Rel, Pair, " +
                "Vertex, Edge, LCA, UnitDep, Coref, Logic, E2ELogic)\n";
        str += "--data\t\t\tData file with questions and answers\n";
        str += "--train\t\t\tTrain folds (Required if cv not provided; follow it by " +
                "any number of fold files separated by space)\n";
        str += "--test\t\t\tTest folds (Required if cv not provided; follow it by " +
                "any number of fold files separated by space)\n";
        str += "--cv\t\t\tCV folds (Required if train and test not provided; follow it by " +
                "any number of fold files separated by space)\n";
        str += "--model_dir\t\tModel Directory\n";
        str += "--print_mistakes\tPrints examples which the model got wrong\n";
        str += "--print_correct\t\tPrints examples which the model got correct\n";
        str += "\nAll arguments except mode are optional\n";

        if(args.length == 0) {
            System.err.println(str);
        }

        for(int i=0; i<args.length; ++i) {
            if(args[i].equals("--data") && args.length > (i+1) &&
                    !commands.contains(args[i+1])) {
                dataFile = args[i+1];
                continue;
            }
            if(args[i].equals("--mode") && args.length > (i+1) &&
                    !commands.contains(args[i+1])) {
                mode = args[i+1];
                continue;
            }
            if(args[i].equals("--model_dir") && args.length > (i+1) &&
                    !commands.contains(args[i+1])) {
                modelDir = args[i+1];
                continue;
            }
            if(args[i].equals("--train") && args.length > (i+1) &&
                    !commands.contains(args[i+1])) {
                trainFolds = new ArrayList<>();
                for(int j=i+1; j<args.length; ++j) {
                    if(commands.contains(args[j])) break;
                    trainFolds.add(args[j]);
                }
                continue;
            }
            if(args[i].equals("--test") && args.length > (i+1) &&
                    !commands.contains(args[i+1])) {
                testFolds = new ArrayList<>();
                for(int j=i+1; j<args.length; ++j) {
                    if(commands.contains(args[j])) break;
                    testFolds.add(args[j]);
                }
                continue;
            }
            if(args[i].equals("--cv") && args.length > (i+1) &&
                    !commands.contains(args[i+1])) {
                cvFolds = new ArrayList<>();
                for(int j=i+1; j<args.length; ++j) {
                    if(commands.contains(args[j])) break;
                    cvFolds.add(args[j]);
                }
                continue;
            }
            if(args[i].equals("--print_mistakes")) {
                Params.printMistakes = true;
                continue;
            }
            if(args[i].equals("--print_correct")) {
                Params.printCorrect = true;
                continue;
            }
            if(args[i].equals("--demo")) {
                Params.runDemo = true;
                continue;
            }
            if(args[i].equals("--demo_server")) {
                Params.startDemoServer = true;
                Params.runDemo = true;
                continue;
            }
        }
        if(mode == null) {
            System.err.println("Mode not provided");
            System.exit(0);
        }
        if((trainFolds == null || testFolds == null) && cvFolds == null) {
            System.err.println("Either both train and test needs to be provided, or " +
                    "cv needs to be provided");
            System.exit(0);
        } else if(cvFolds != null) {
            doCV = true;
        }
        if(Params.runDemo && trainFolds == null) {
            System.err.println("Demo requires train folds");
            System.exit(0);
        }
        if(dataFile == null) {
            Params.questionsFile = "data/questions.json";
            System.err.println("Data File not provided. Using data/questions.json");
        } else {
            Params.questionsFile = dataFile;
        }
        if(modelDir == null) {
            Params.modelDir = "models/";
            System.err.println("Model directory not provided. Using ./models/ instead. " +
                    "Consecutive runs with the same location will overwrite older models");
        } else {
            Params.modelDir = modelDir+"/";
        }
        System.out.println("\n\n*********** Command Details *************");
        System.out.println("DataFile: " + Params.questionsFile);
        System.out.println("Mode: " + mode);
        if(trainFolds != null) {
            System.out.println("Train: " + Arrays.asList(trainFolds));
        }
        if(testFolds != null) {
            System.out.println("Test: " + Arrays.asList(testFolds));
        }
        if(cvFolds != null) {
            System.out.println("CV: " + cvFolds);
        }
        System.out.println("Model Dir: " + Params.modelDir);
        System.out.println("Print Mistakes: " + Params.printMistakes);
        System.out.println("Print Correct: " + Params.printCorrect);
        System.out.println("Run Demo: " + Params.runDemo);
        System.out.println("Start Demo Server: " + Params.startDemoServer);
        System.out.println();
        System.out.println("*****************************************\n\n");

        List<List<Integer>> cvIndices = new ArrayList<>();
        if(doCV) {
            for (String cvFold : cvFolds) {
                cvIndices.add(Folds.readFoldIndices(cvFold));
            }
        }
        List<Integer> trainIndices = new ArrayList<>();
        List<Integer> testIndices = new ArrayList<>();
        if(!doCV) {
            for (String trainFold : trainFolds) {
                trainIndices.addAll(Folds.readFoldIndices(trainFold));
            }
            for (String testFold : testFolds) {
                testIndices.addAll(Folds.readFoldIndices(testFold));
            }
        }
        if (mode.equals("Rel") || mode.equals("Pair") || mode.equals("Vertex") ||
                mode.equals("Edge") || mode.equals("GraphDecompose") ||
                mode.equals("GraphJoint") || mode.equals("LCA") || mode.equals("UnitDep")) {
            Params.useIllinoisTools = true;
            Tools.initIllinoisTools();
            List<Problem> probs = Reader.readProblemsFromJson();
            if(mode.equals("Rel")) {
                if(doCV) RelDriver.crossVal(probs, cvIndices);
                else RelDriver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
            if(mode.equals("Pair")) {
                if(doCV) PairDriver.crossVal(probs, cvIndices);
                else PairDriver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
            if(mode.equals("Vertex")) {
                if(doCV) RateDriver.crossVal(probs, cvIndices);
                else RateDriver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
            if(mode.equals("Edge")) {
                if(doCV) RunDriver.crossVal(probs, cvIndices);
                else RunDriver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
            if(mode.equals("GraphDecompose")) {
                if(doCV) constraints.GraphDriver.crossVal(probs, cvIndices);
                else constraints.GraphDriver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
            if(mode.equals("GraphJoint")) {
                if(doCV) graph.GraphDriver.crossVal(probs, cvIndices);
                else graph.GraphDriver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
            if(mode.equals("LCA") || mode.equals("UnitDep")) {
                if(mode.equals("LCA")) Params.noUDG = true;
                else Params.noUDG = false;
                if(doCV) constraints.ConsDriver.crossVal(probs, cvIndices);
                else constraints.ConsDriver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
        }
        if (mode.equals("Coref") || mode.equals("E2ELogic") || mode.equals("Logic")) {
            Params.useStanfordTools = true;
            Tools.initStanfordTools();
            List<StanfordProblem> probs = Reader.readStanfordProblemsFromJson();
            if(mode.equals("Coref")) {
                if(doCV) CorefDriver.crossVal(probs, cvIndices);
                else CorefDriver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
            if(mode.equals("Logic")) {
                if(doCV) logic.LogicDriver.crossVal(probs, cvIndices);
                else logic.LogicDriver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
            if(mode.equals("E2ELogic")) {
                if(doCV) logic.Driver.crossVal(probs, cvIndices);
                else logic.Driver.doTrainTest(probs, trainIndices, testIndices, 100);
            }
        }
        if(Params.runDemo) {
            Demo.loadModels();
            if (Params.startDemoServer) {
                Server.startServer(8182);
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    System.out.println("Enter question for " + mode + ": (Type END to exit)");
                    String question = br.readLine();
                    if (question.trim().equals("END")) break;
                    System.out.println("Answer: " + Demo.answerQuestion(question.trim()));
                }
            }
        }
    }
}
