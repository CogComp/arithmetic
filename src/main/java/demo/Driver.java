package demo;


import utils.Params;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Driver {

    public static void main(String args[]) {

        String dataFile = null, mode = null, modelDir = null;
        List<String> trainFolds = null, testFolds = null, cvFolds = null;
        List<String> commands = Arrays.asList("--data", "--mode", "--train",
                "--test", "--cv", "--model_dir");

        String str = "Usage: \n--data\t\tRequired\tData file with questions and answers\n";
        str += "--mode\t\tRequired\tMode of operation\n";
        str += "--train\t\tOptional\tTrain folds (Required if cv not provided)\n";
        str += "--test\t\tOptional\tTest folds (Required if cv not provided)\n";
        str += "--cv\t\tOptional\tCV folds (Required if train and test not provided)\n";
        str += "--model_dir\tOptional\tModel Directory\n";

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
        }
        if(mode == null) {
            System.err.println("Mode not provided");
            System.exit(0);
        }
        if(dataFile == null) {
            Params.questionsFile = "data/questions.json";
            System.err.println("Data File not provided. Using data/questions.json");
        }
        if(modelDir == null) {
            Params.modelDir = "models/";
            System.err.println("Model directory not provided. Using ./models/ instead. Consecutive runs" +
                    "with the same location will overwrite older models");
        } else {
            Params.modelDir = modelDir+"/";
        }
        System.out.println("\n\nRun Details:");
        System.out.println("DataFile: " + dataFile);
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
        System.out.println("Model Dir: " + modelDir);
    }

}
