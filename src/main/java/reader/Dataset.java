package reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import net.didion.jwnl.data.IndexWord;
import org.apache.commons.collections.collection.CompositeCollection;
import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import structure.DataFormat;
import structure.Node;
import structure.QuantSpan;
import structure.StanfordProblem;
import utils.FeatGen;
import utils.Tools;

public class Dataset {
	
	public static List<DataFormat> createDatasetFromOldFormat()
			throws Exception {
		String json = FileUtils.readFileToString(new File("data/questions.json"));
		List<DataFormat> oldProbs = new Gson().fromJson(json,
				new TypeToken<List<DataFormat>>(){}.getType());
		Map<Integer, List<Integer>> rateAnns = null;
//				Annotations.readRateAnnotations(Params.ratesFile);
		List<DataFormat> newProbs = new ArrayList<>();
		for(DataFormat kushmanProb : oldProbs) {
			DataFormat ks = new DataFormat();
			ks.iIndex = kushmanProb.iIndex;
			ks.sQuestion = kushmanProb.sQuestion;
			ks.lEquations = kushmanProb.lEquations;
			ks.lSolutions = kushmanProb.lSolutions;
			ks.lAlignments = kushmanProb.lAlignments;
			ks.quants = new ArrayList<>();
			for(QuantSpan qs : Tools.quantifier.getSpans(ks.sQuestion)) {
				ks.quants.add(qs.val);
			}
			ks.rates = new ArrayList<>();
			if(rateAnns.containsKey(ks.iIndex)) {
				ks.rates.addAll(rateAnns.get(ks.iIndex));
			}
			newProbs.add(ks);
		}
		return newProbs;
	}
	
	public static List<DataFormat> createDatasetFromCrowdFlower(String crowdFlowerFile)
			throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		List<CrowdFlower> cfProblems = CrowdFlower.readCrowdFlowerFile(crowdFlowerFile);
		List<DataFormat> newProbs = new ArrayList<>();
		for(CrowdFlower prob : cfProblems) {
			for(int judgment = 0; judgment < prob.results.judgments.size(); ++judgment) {
				if(prob.results.judgments.get(judgment).tainted) continue;
				DataFormat ks = new DataFormat();
				ks.iIndex = prob.id;
				ks.sQuestion = prob.results.judgments.get(judgment).data.question1;
				ks.quants = new ArrayList<>();
				for (QuantSpan qs : Tools.quantifier.getSpans(ks.sQuestion)) {
					ks.quants.add(qs.val);
				}
				ks.lEquations = new ArrayList<>();
				String exp = prob.data.answer.split("=")[0].trim();
				String monotonic = covertExpressionToMonotonic(exp);
				//			if(!exp.trim().equals(monotonic.trim())) {
				//				System.out.println(exp + " converted to "+monotonic);
				//			}
				ks.lEquations.add(monotonic);
				ks.lSolutions = new ArrayList<>();
				ks.lSolutions.add(Double.parseDouble(prob.data.answer.split("=")[1].trim()));
				ks.rates = new ArrayList<>();
				ks.lAlignments = new ArrayList<>();
				Node expr = Node.parseNode(ks.lEquations.get(0));
				List<Node> leaves = expr.getLeaves();
				for (int j = 0; j < leaves.size(); ++j) {
					Node leaf = leaves.get(j);
					List<Integer> matchedQuantIndices = new ArrayList<>();
					for (int i = 0; i < ks.quants.size(); ++i) {
						if (Tools.safeEquals(ks.quants.get(i), leaf.val)) {
							matchedQuantIndices.add(i);
						}
					}
					if (matchedQuantIndices.size() == 0) {
						System.out.println(ks.iIndex + ": Quantity not found in " + leaf.val);
						System.out.println("Initial Problem: " + prob.data.question);
						System.out.println("Modified Problem: " + ks.sQuestion);
						System.out.println("WorkerId: " + prob.results.judgments.get(judgment).worker_id);
						System.out.println("Quantities: " + Arrays.asList(ks.quants));
						System.out.println("Answer: " + ks.lEquations.get(0) + " = " + ks.lSolutions.get(0));
						System.out.println();
					} else if (matchedQuantIndices.size() > 1) {
						System.out.println(ks.iIndex + ": More than 1 match found with " + leaf.val);
						System.out.println("Initial Problem: " + prob.data.question);
						System.out.println("Modified Problem: " + ks.sQuestion);
						System.out.println("WorkerId: " + prob.results.judgments.get(judgment).worker_id);
						System.out.println("Quantities: " + Arrays.asList(ks.quants));
						System.out.println("Answer: " + ks.lEquations.get(0) + " = " + ks.lSolutions.get(0));
						System.out.println();
					} else {
						ks.lAlignments.add(matchedQuantIndices.get(0));
					}
				}
				newProbs.add(ks);
			}
		}
		return newProbs;
	}

	public static String covertExpressionToMonotonic(String expression) {
		Node expr = Node.parseNode(expression);
		List<Node> nodes = expr.getAllSubNodes();
		Collections.reverse(nodes);
		for(Node node : nodes) {
			if (node.children.size() == 0) continue;
			if (node.children.get(0).label.equals("SUB")) {
				if (node.label.equals("ADD")) {
					node.label = "SUB";
					Node ab = node.children.get(0);
					Node c = node.children.get(1);
					node.children.clear();
					node.children.add(new Node("ADD", Arrays.asList(ab.children.get(0), c)));
					node.children.add(ab.children.get(1));
				} else if (node.label.equals("SUB")) {
					Node ab = node.children.get(0);
					Node c = node.children.get(1);
					node.children.clear();
					node.children.add(ab.children.get(0));
					node.children.add(new Node("ADD", Arrays.asList(ab.children.get(1), c)));
				}
			}
			if (node.children.get(1).label.equals("SUB")) {
				if (node.label.equals("ADD")) {
					node.label = "SUB";
					Node a = node.children.get(0);
					Node bc = node.children.get(1);
					node.children.clear();
					node.children.add(new Node("ADD", Arrays.asList(a, bc.children.get(0))));
					node.children.add(bc.children.get(1));
				} else if (node.label.equals("SUB")) {
					node.label = "SUB";
					Node a = node.children.get(0);
					Node bc = node.children.get(1);
					node.children.clear();
					node.children.add(new Node("ADD", Arrays.asList(a, bc.children.get(1))));
					node.children.add(bc.children.get(0));
				}
			}
			// For Mul, Div
			if (node.children.get(0).label.equals("DIV")) {
				if (node.label.equals("MUL")) {
					node.label = "DIV";
					Node ab = node.children.get(0);
					Node c = node.children.get(1);
					node.children.clear();
					node.children.add(new Node("MUL", Arrays.asList(ab.children.get(0), c)));
					node.children.add(ab.children.get(1));
				} else if (node.label.equals("DIV")) {
					Node ab = node.children.get(0);
					Node c = node.children.get(1);
					node.children.clear();
					node.children.add(ab.children.get(0));
					node.children.add(new Node("MUL", Arrays.asList(ab.children.get(1), c)));
				}
			}
			if (node.children.get(1).label.equals("DIV")) {
				if (node.label.equals("MUL")) {
					node.label = "DIV";
					Node a = node.children.get(0);
					Node bc = node.children.get(1);
					node.children.clear();
					node.children.add(new Node("MUL", Arrays.asList(a, bc.children.get(0))));
					node.children.add(bc.children.get(1));
				} else if (node.label.equals("DIV")) {
					node.label = "DIV";
					Node a = node.children.get(0);
					Node bc = node.children.get(1);
					node.children.clear();
					node.children.add(new Node("MUL", Arrays.asList(a, bc.children.get(1))));
					node.children.add(bc.children.get(0));
				}
			}
		}
		return expr.toString();
	}

	public static void makeExpressionEquation() throws IOException {
		String json = FileUtils.readFileToString(new File("data/questionsNew.json"));
		List<DataFormat> probs = new Gson().fromJson(json,
				new TypeToken<List<DataFormat>>(){}.getType());
		for(DataFormat prob : probs) {
			if(prob.lEquations.get(0).contains("=")) continue;
			prob.lEquations.add("X="+prob.lEquations.get(0));
			prob.lEquations.remove(0);
		}
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		json = gson.toJson(probs);
		FileUtils.writeStringToFile(new File("data/questions.json"), json);
	}

	public static void addRateAnnotations() throws IOException {
		String json = FileUtils.readFileToString(new File("data/questionsOld.json"));
		List<DataFormat> oldProbs = new Gson().fromJson(json,
				new TypeToken<List<DataFormat>>(){}.getType());
		json = FileUtils.readFileToString(new File("data/questions.json"));
		List<DataFormat> probs = new Gson().fromJson(json,
				new TypeToken<List<DataFormat>>(){}.getType());
		int count = 0;
		for(DataFormat prob : probs) {
			if(prob.lAlignments == null || prob.lAlignments.size() == 0) {
				for(DataFormat oldProb : oldProbs) {
					if(prob.iIndex == oldProb.iIndex) {
						System.out.println("Alignment added from "+prob.iIndex);
						prob.lAlignments = oldProb.lAlignments;
						count++;
						break;
					}
				}
			}
		}
		System.out.println("Changes made: "+count);
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		json = gson.toJson(probs);
		FileUtils.writeStringToFile(new File("data/questions.json"), json);
	}

	public static void combineTwoSetsToOneDataset() throws Exception {
		List<DataFormat> probs1 = createDatasetFromOldFormat();
		System.out.println("Probs1: "+probs1.size());
		List<DataFormat> probs2 = createDatasetFromCrowdFlower("data/job_1012604.json");
		System.out.println("Probs2: "+probs2.size());
		List<DataFormat> probs = new ArrayList<>();
		probs.addAll(probs1);
		probs.addAll(probs2);
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		String json = gson.toJson(probs);
		FileUtils.writeStringToFile(new File("data/questionsNew.json"), json);
	}

	public static void consistencyChecks() throws Exception {
		String json = FileUtils.readFileToString(new File("data/questions.json"));
		List<DataFormat> kushmanProbs = new Gson().fromJson(json,
				new TypeToken<List<DataFormat>>(){}.getType());
//		for(DataFormat prob : probs) {
//			Node expr = Node.parseNode(prob.lEquations.get(0).split("=")[1].trim());
//			if(expr.getLeaves().size() != prob.lAlignments.size()) {
//				System.out.println("Id: "+prob.iIndex);
//			}
//		}
		List<StanfordProblem> probs = Reader.readStanfordProblemsFromJson();
		for(int i=0; i<probs.size(); ++i) {
			StanfordProblem prob = probs.get(i);
			DataFormat df = kushmanProbs.get(i);
			List<Node> leaves = prob.expr.getLeaves();
			if(leaves.size() != df.lAlignments.size()) {
				System.out.println("Alignment size mismatch "+prob.id);
			}
			if(df.quants.size() != prob.quantities.size()) {
				System.out.println("Number list size mismatch "+prob.id);
			}
			if(df.quants.size() == prob.quantities.size()) {
				for(int j=0; j<prob.quantities.size(); ++j) {
					if(!Tools.safeEquals(prob.quantities.get(j).val, df.quants.get(j))) {
						System.out.println("Number not matching "+prob.id);
					}
				}
			}
			if(leaves.size() == df.lAlignments.size()) {
				for (int j = 0; j < leaves.size(); ++j) {
					if(!Tools.safeEquals(leaves.get(j).val,
							prob.quantities.get(df.lAlignments.get(j)).val)) {
						System.out.println("Alignment entry does not match number "+prob.id);
					}
				}
			}
			if(!Tools.safeEquals(prob.expr.getValue(), prob.answer)) {
				System.out.println("Answer not matching with expression "+prob.id);
			}
			for(Node node : prob.expr.getAllSubNodes()) {
				if(node.label.equals("MUL") || node.label.equals("DIV")) {
					if(prob.rates.size() == 0) {
						System.out.println("Rates absent "+prob.id);
						break;
					}
				}
			}
		}
		System.out.println("Problems read: "+probs.size());
	}

	public static void createFoldFiles() throws Exception {
		List<StanfordProblem> probs = Reader.readStanfordProblemsFromJson();
		List<Integer> indices = new ArrayList<>();
		List<Integer> oldIndices = new ArrayList<>();
		List<Integer> newIndices = new ArrayList<>();
		String oldP = "", newP = "";
		for(StanfordProblem prob : probs) {
			indices.add(prob.id);
			if(prob.id < 100000) {
				oldIndices.add(prob.id);
				oldP += prob.id + "\n";
			} else {
				newIndices.add(prob.id);
				newP += prob.id + "\n";
			}
		}
		Collections.shuffle(indices);
		double n = indices.size()*1.0 / 5;
		for(int i=0; i<5; ++i) {
			String str = "";
			int min = (int)(i*n);
			int max = (int)((i+1)*n);
			if(i == 4) max = indices.size();
			for(int j=min; j<max; ++j) {
				str += indices.get(j) + "\n";
			}
			FileUtils.writeStringToFile(new File("fold"+i+".txt"), str);
		}
		FileUtils.writeStringToFile(new File("old.txt"), oldP);
		FileUtils.writeStringToFile(new File("new.txt"), newP);
	}

	public static void computePMI(int startIndex, int endIndex) throws Exception {
		List<StanfordProblem> probs = Reader.readStanfordProblemsFromJson();
		int count = 0;
		Map<String, Integer> countsOp = new HashMap<>();
		Map<String, Integer> countsFeats = new HashMap<>();
		Map<String, Integer> countsJoint = new HashMap<>();
		for(StanfordProblem prob : probs) {
			if(prob.id > endIndex && endIndex != -1) {
				continue;
			}
			if(prob.id < startIndex && startIndex != -1) {
				continue;
			}
			count++;
			for(Node node : prob.expr.getAllSubNodes()) {
				Set<String> feats = new HashSet<>();
				if(node.children.size() > 0 &&
						node.children.get(0).children.size() == 0) {
					int sentId = Tools.getSentenceIdFromCharOffset(
							prob.tokens, node.children.get(0).qs.start);
					int tokenId = Tools.getTokenIdFromCharOffset(
							prob.tokens.get(sentId), node.children.get(0).qs.start);
					feats.addAll(FeatGen.getUnigramBigramFeatures(
							prob.tokens.get(sentId), tokenId, 3));
				}
				if(node.children.size() > 0 &&
						node.children.get(1).children.size() == 0) {
					int sentId = Tools.getSentenceIdFromCharOffset(
							prob.tokens, node.children.get(1).qs.start);
					int tokenId = Tools.getTokenIdFromCharOffset(
							prob.tokens.get(sentId), node.children.get(1).qs.start);
					feats.addAll(FeatGen.getUnigramBigramFeatures(
							prob.tokens.get(sentId), tokenId, 3));
				}
				if(feats.size() > 0) {
					countsOp.put(node.label, countsOp.getOrDefault(node.label, 0) + 1);
					for(String feat : feats) {
						countsFeats.put(feat, countsFeats.getOrDefault(feat, 0) + 1);
						countsJoint.put(node.label+"_"+feat,
								countsJoint.getOrDefault(node.label+"_"+feat, 0) + 1);
					}
				}
			}
		}
		System.out.println("Count: "+count);
		double aggregate = 0.0, aggEntropy = 0.0;
		for(String feat : countsFeats.keySet()) {
			int max = 0;
			for(String op : countsOp.keySet()) {
				if(countsJoint.getOrDefault(op+"_"+feat, 0) > max) {
					max = countsJoint.getOrDefault(op+"_"+feat, 0);
				}
			}
			aggregate += (max * 1.0 / countsFeats.get(feat));
			double entropy = 0.0;
			for(String op : countsOp.keySet()) {
				double p = countsJoint.getOrDefault(op+"_"+feat, 0) *1.0 / countsFeats.get(feat);
				if(p > 0.00001) {
					entropy += -p * Math.log(p);
				}
			}
			aggEntropy += entropy;
		}
		System.out.println("Average Best Choice Prob: "+
				(aggregate / countsFeats.keySet().size()));
		System.out.println("Average Entropy: "+
				(aggEntropy / countsFeats.keySet().size()));
	}


	public static void main(String args[]) throws Exception {
		Tools.initStanfordTools();
//		consistencyChecks();
//		createFoldFiles();
		computePMI(0, 10000);
		computePMI(10000, -1);
		computePMI(-1, -1);
	}

 	
}
