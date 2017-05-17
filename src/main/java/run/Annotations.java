package run;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import structure.Node;
import structure.Problem;

public class Annotations {
	
	public static String getLabel(List<String> path, boolean rate1, boolean rate2) {
		int count = 0;
		for(String label : path) {
			if(label.startsWith("MUL") || label.startsWith("DIV")) {
				count++;
			}
		}
		if(rate1 == rate2 && count == 0) {
			return "SAME_UNIT";
		}
		if(rate1 != rate2 && count == 1) {
			if(path.contains("MUL")) {
				if(rate1) {
					return "1_RATE_2";
				} else {
					return "2_RATE_2";
				}
			}
			if(path.contains("DIV")) {
				if(rate1) {
					return "NO_REL";
				} else {
					return "2_RATE_1";
				}
			}
			if(path.contains("DIV_REV")) {
				if(rate1) {
					return "1_RATE_1";
				} else {
					return "NO_REL";
				}
			}
		}
		if(count == 2) {
			// Currently no good heuristic, so just returning no relation
		}
		return "NO_REL";
 	}
	
	public static SLProblem getSP(List<Problem> problemList)
			throws Exception{
		SLProblem problem = new SLProblem();
		for(Problem prob : problemList) {
			for(int i=0; i<prob.quantities.size(); ++i) {
				if(!prob.expr.hasLeaf(i)) continue;
				for(int j=i+1; j<prob.quantities.size(); ++j) {
					if(!prob.expr.hasLeaf(j)) continue;
					// Relation with j
					List<String> path = prob.expr.getPath(i, j);
					RunX x = new RunX(prob, i, j);
					RunY y = new RunY(getLabel(path, prob.rates.contains(i),
							prob.rates.contains(j)));
					problem.addExample(x, y);
				}
				// Relation with question
				List<String> path = prob.expr.getPathToRoot(i);
				RunX x = new RunX(prob, i, -1);
				RunY y = new RunY(getLabel(path, prob.rates.contains(i),
						prob.rates.contains(-1)));
				problem.addExample(x, y);
			}
			
		}
		return problem;
	}
	
	public static Map<Integer, List<Integer>> readRateAnnotations(String fileName) 
			throws Exception{
		Map<Integer, List<Integer>> rates = new HashMap<Integer, List<Integer>>();
		for(String line : FileUtils.readLines(new File(fileName))) {
			if(line.trim().equals("")) continue;
			String strArr[] = line.split("\t");
			List<Integer> indices = new ArrayList<>();
			if(strArr.length >= 2) {
				for(String str : strArr[1].split(" ")) {
					indices.add(Integer.parseInt(str));
				}
			}
			rates.put(Integer.parseInt(strArr[0]), indices);
		}
		return rates;
	}
	
	public static void getRateAnnotations(List<Problem> problemList, String dir) throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		int count = 0;
		String str = "";
		for(Problem prob : problemList) {
			boolean onlyAddSub = true;
			for(Node node : prob.expr.getAllSubNodes()) {
				if(node.label.startsWith("MUL") || node.label.startsWith("DIV")) {
					onlyAddSub = false;
					break;
				}
			}
			if(onlyAddSub) continue;
			count++;
			if(count%10 == 0) {
				System.out.println("Annotated "+count+" problems");
				FileUtils.writeStringToFile(new File(dir+"/rateAnnotations.txt"), str);
			}
			System.out.println(prob.id+" "+prob.question);
			System.out.println("-1 : "+Arrays.asList(prob.schema.questionTokens));
			for(int i=0; i<prob.quantities.size(); ++i) {
				System.out.println(i+" : "+prob.quantities.get(i));
			}
			System.out.println();
			str += prob.id+"\t"+br.readLine().trim()+"\n";
			System.out.println();
		}
		System.out.println("Ann Reqd : "+count+" Total : "+problemList.size());
	}

	public static void getRateAnnotationsFromMrinmaya(
			List<Problem> problemList, Map<Integer, List<Integer>> rateAnns)
			throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		int matched = 0, total = 0;
		String str;

		for(int j=problemList.size()-1; j>=0; j-=20) {
			Problem prob = problemList.get(j);
			System.out.println(prob.id+" :  "+prob.question);
			System.out.println("\nQuantities detected : "+Arrays.asList(prob.quantities));
			System.out.println("\nIs the question asking for a rate ? (y/n) : ");
			str = br.readLine().trim();
			total++;
			if(str.equalsIgnoreCase("y") && rateAnns.containsKey(prob.id) &&
					rateAnns.get(prob.id).contains(-1)) {
				matched++;
			}
			if(str.equalsIgnoreCase("n") && (!rateAnns.containsKey(prob.id) ||
					!rateAnns.get(prob.id).contains(-1))) {
				matched++;
			}
			for(int i=0; i<prob.quantities.size(); ++i) {
				System.out.println("Is "+prob.quantities.get(i)+" a rate ? (y/n) : ");
				str = br.readLine().trim();
				total++;
				if(str.equalsIgnoreCase("y") && rateAnns.containsKey(prob.id) &&
						rateAnns.get(prob.id).contains(i)) {
					matched++;
				}
				if(str.equalsIgnoreCase("n") && (!rateAnns.containsKey(prob.id) ||
						!rateAnns.get(prob.id).contains(i))) {
					matched++;
				}
			}
			System.out.println();
			System.out.println("Matched : "+matched+" Total : "+total);
			System.out.println();
			System.out.println();
		}
	}

}