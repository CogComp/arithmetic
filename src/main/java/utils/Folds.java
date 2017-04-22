package utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import reader.Reader;
import structure.Problem;
import structure.StanfordProblem;

public class Folds {
	
	public static int getNumFolds(String dir) {
		File directory = null;
		int fold = 0;
		directory = new File(dir);
		for(File file : directory.listFiles()) {
			if(file.getName().contains("fold")) {
				fold++;
			}
		}
		return fold;
	}

	// Returns List of 3 elements : train, val, test
	public static List<List<Problem>> getDataSplit(String dir, int fold)
			throws Exception {
		List<Problem> allTrain = new ArrayList<Problem>();
		List<Problem> train = new ArrayList<Problem>();
		List<Problem> val = new ArrayList<Problem>();
		List<Problem> test = new ArrayList<Problem>();
		List<Problem> probs = Reader.readProblemsFromJson();
		Set<Integer> testIndices = new HashSet<>();
		Set<Integer> trainIndices = new HashSet<>();
		int numFolds = getNumFolds(dir);
		for(int i=0; i<numFolds; ++i) {
			String str = FileUtils.readFileToString(new File(dir + "/fold" + i + ".txt"));
			for (String index : str.split("\n")) {
				if(i==fold) {
					testIndices.add(Integer.parseInt(index));
				} else {
					trainIndices.add(Integer.parseInt(index));
				}
			}
		}
		for(Problem prob : probs) {
			if(testIndices.contains(prob.id)) {
				test.add(prob);
			}
			if(trainIndices.contains(prob.id)) {
				allTrain.add(prob);
			}
		}
		Collections.shuffle(allTrain, new Random(0));
		val.addAll(allTrain.subList(0, (int)(Params.validationFrac*allTrain.size())));
		train.addAll(allTrain.subList((int)(Params.validationFrac*allTrain.size()), allTrain.size()));
		List<List<Problem>> splits = new ArrayList<>();
		splits.add(train);
		splits.add(val);
		splits.add(test);
		return splits;
	}

	// Returns List of 3 elements : train, val, test
	public static List<List<StanfordProblem>> getDataSplitForStanford(String dir, int fold)
			throws Exception {
		List<StanfordProblem> allTrain = new ArrayList<StanfordProblem>();
		List<StanfordProblem> train = new ArrayList<StanfordProblem>();
		List<StanfordProblem> val = new ArrayList<StanfordProblem>();
		List<StanfordProblem> test = new ArrayList<StanfordProblem>();
		List<StanfordProblem> probs = Reader.readStanfordProblemsFromJson();
		Set<Integer> testIndices = new HashSet<>();
		Set<Integer> trainIndices = new HashSet<>();
		int numFolds = getNumFolds(dir);
		for(int i=0; i<numFolds; ++i) {
			String str = FileUtils.readFileToString(new File(dir + "/fold" + i + ".txt"));
			for (String index : str.split("\n")) {
				if(i==fold) {
					testIndices.add(Integer.parseInt(index));
				} else {
					trainIndices.add(Integer.parseInt(index));
				}
			}
		}
		for(StanfordProblem prob : probs) {
			if(testIndices.contains(prob.id)) {
				test.add(prob);
			}
			if(trainIndices.contains(prob.id)) {
				allTrain.add(prob);
			}
		}
		Collections.shuffle(allTrain, new Random(0));
		val.addAll(allTrain.subList(0, (int)(Params.validationFrac*allTrain.size())));
		train.addAll(allTrain.subList((int)(Params.validationFrac*allTrain.size()), allTrain.size()));
		List<List<StanfordProblem>> splits = new ArrayList<>();
		splits.add(train);
		splits.add(val);
		splits.add(test);
		return splits;
	}
	
}
