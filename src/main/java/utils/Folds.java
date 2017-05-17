package utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FileUtils;
import structure.Problem;
import structure.StanfordProblem;

public class Folds {

	public static List<Integer> readFoldIndices(String foldFile) throws IOException {
		String str = FileUtils.readFileToString(new File(foldFile));
		List<Integer> foldIndices = new ArrayList<>();
		for(String index : str.split("\n")) {
			foldIndices.add(Integer.parseInt(index));
		}
		return foldIndices;
	}

	// Returns List of 3 elements : train, val, test
	public static List<List<Problem>> getDataSplit(
			List<Problem> probs, List<Integer> trainIndices,
			List<Integer> testIndices, double validationFrac) throws Exception {
		List<Problem> allTrain = new ArrayList<>();
		List<Problem> train = new ArrayList<>();
		List<Problem> val = new ArrayList<>();
		List<Problem> test = new ArrayList<>();
		for(Problem prob : probs) {
			if(testIndices.contains(prob.id)) {
				test.add(prob);
			}
			if(trainIndices.contains(prob.id)) {
				allTrain.add(prob);
			}
		}
		Collections.shuffle(allTrain, new Random(0));
		val.addAll(allTrain.subList(0, (int)(validationFrac*allTrain.size())));
		train.addAll(allTrain.subList((int)(validationFrac*allTrain.size()), allTrain.size()));
		List<List<Problem>> splits = new ArrayList<>();
		splits.add(train);
		splits.add(val);
		splits.add(test);
		return splits;
	}

	// Returns List of 3 elements : train, val, test
	public static List<List<StanfordProblem>> getDataSplitForStanford(
			List<StanfordProblem> probs, List<Integer> trainIndices,
			List<Integer> testIndices, double validationFrac) throws Exception {
		List<StanfordProblem> allTrain = new ArrayList<>();
		List<StanfordProblem> train = new ArrayList<>();
		List<StanfordProblem> val = new ArrayList<>();
		List<StanfordProblem> test = new ArrayList<>();
		for(StanfordProblem prob : probs) {
			if(testIndices.contains(prob.id)) {
				test.add(prob);
			}
			if(trainIndices.contains(prob.id)) {
				allTrain.add(prob);
			}
		}
		Collections.shuffle(allTrain, new Random(0));
		val.addAll(allTrain.subList(0, (int)(validationFrac*allTrain.size())));
		train.addAll(allTrain.subList((int)(validationFrac*allTrain.size()), allTrain.size()));
		List<List<StanfordProblem>> splits = new ArrayList<>();
		splits.add(train);
		splits.add(val);
		splits.add(test);
		return splits;
	}
	
}
