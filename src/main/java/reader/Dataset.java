package reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import run.Annotations;
import structure.DataFormat;
import structure.Node;
import structure.QuantSpan;
import utils.Params;
import utils.Tools;

public class Dataset {
	
	public static List<DataFormat> createDatasetFromOldFormat()
			throws Exception {
		String json = FileUtils.readFileToString(new File("data/questions.json"));
		List<DataFormat> oldProbs = new Gson().fromJson(json,
				new TypeToken<List<DataFormat>>(){}.getType());
		Map<Integer, List<Integer>> rateAnns =
				Annotations.readRateAnnotations(Params.ratesFile);
		List<DataFormat> newProbs = new ArrayList<>();
		for(DataFormat kushmanProb : oldProbs) {
			DataFormat ks = new DataFormat();
			ks.iIndex = kushmanProb.iIndex;
			ks.sQuestion = kushmanProb.sQuestion;
			ks.lEquations = kushmanProb.lEquations;
			ks.lSolutions = kushmanProb.lSolutions;
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
	
	public static void main(String args[]) throws Exception {
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
 	
}
