package reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import structure.*;
import utils.Params;
import utils.Tools;

class QuantSchemaPython {
	List<String> unit;
	List<String> rate;
	String verb;

	public QuantSchemaPython(QuantitySchema qs) {
		if(qs.unit != null) {
			unit = Arrays.asList(qs.unit.trim().split(" "));
		} else {
			unit = new ArrayList<>();
		}
		if(qs.rateUnit != null) {
			rate = Arrays.asList(qs.rateUnit.getTokenizedSurfaceForm().trim().split(" "));
		} else {
			rate = new ArrayList<>();
		}
		if(qs.verb != null) {
			verb = qs.verb;
		} else {
			verb = "";
		}
	}
}

class ProblemPython {
	int id;
	List<String> tokens, lemmas, posTags, tokenIdToNum, questionTokens;
	List<QuantSchemaPython> quantSchemas;
	String expression;
	Double solution;

	public ProblemPython(Problem prob) {
		id = prob.id;
		tokens = new ArrayList<>();
		lemmas = new ArrayList<>();
		posTags = new ArrayList<>();
		tokenIdToNum = new ArrayList<>();
		quantSchemas = new ArrayList<>();
		for(int i=0; i<prob.ta.size(); ++i) {
			tokens.add(prob.ta.getToken(i));
			posTags.add(prob.posTags.get(i).getLabel());
			lemmas.add(prob.lemmas.get(i));
		}
		for(QuantSpan qs : prob.quantities) {
			tokenIdToNum.add(prob.ta.getTokenIdFromCharacterOffset(qs.start)+"||"+qs.val);
		}
		for(QuantitySchema qs : prob.schema.quantSchemas) {
			quantSchemas.add(new QuantSchemaPython(qs));
		}
		questionTokens = prob.schema.questionTokens;
		expression = prob.expr.toStringForPython();
		solution = prob.answer;
	}
}

public class Reader {
	
	public static List<Problem> readProblemsFromJson() throws Exception {
		String json = FileUtils.readFileToString(new File(Params.questionsFile));
		List<KushmanFormat> kushmanProbs = new Gson().fromJson(json, 
				new TypeToken<List<KushmanFormat>>(){}.getType());
		List<Problem> problemList = new ArrayList<>();
		for(KushmanFormat kushmanProb : kushmanProbs) {
//			System.out.println(kushmanProb.iIndex);
//			System.out.println(kushmanProb.sQuestion);
//			System.out.println(Arrays.asList(kushmanProb.lSolutions));
			Problem prob = new Problem(kushmanProb.iIndex, kushmanProb.sQuestion, 
					kushmanProb.lSolutions.get(0));
//			prob.quantities = kushmanProb.quantities;
			prob.extractQuantities();
			prob.expr = Node.parseNode(kushmanProb.lEquations.get(0));
			assert prob.expr.getLeaves().size() == kushmanProb.lAlignments.size();
			for(int j=0; j<prob.expr.getLeaves().size(); ++j) {
				Node node = prob.expr.getLeaves().get(j);
				node.quantIndex = kushmanProb.lAlignments.get(j);
				node.qs = prob.quantities.get(node.quantIndex);
			}
			if(kushmanProb.key != null) {
				prob.expr.key = kushmanProb.key;
				prob.expr.infRuleType = kushmanProb.infType;
			}
			prob.extractAnnotations();
			problemList.add(prob);
		}
		return problemList;
	}

	public static List<StanfordProblem> readStanfordProblemsFromJson()
			throws Exception {
		String json = FileUtils.readFileToString(new File(Params.questionsFile));
		List<KushmanFormat> kushmanProbs = new Gson().fromJson(json,
				new TypeToken<List<KushmanFormat>>(){}.getType());
		List<StanfordProblem> problemList = new ArrayList<>();
		for(KushmanFormat kushmanProb : kushmanProbs) {
			StanfordProblem prob = new StanfordProblem(
					kushmanProb.iIndex,
					kushmanProb.sQuestion,
					kushmanProb.lSolutions.get(0));
//			prob.quantities = kushmanProb.quantities;
			prob.extractQuantities();
			prob.expr = Node.parseNode(kushmanProb.lEquations.get(0));
			assert prob.expr.getLeaves().size() == kushmanProb.lAlignments.size();
			for(int j=0; j<prob.expr.getLeaves().size(); ++j) {
				Node node = prob.expr.getLeaves().get(j);
				node.quantIndex = kushmanProb.lAlignments.get(j);
				node.qs = prob.quantities.get(node.quantIndex);
			}
//			if(kushmanProb.key != null) {
//				prob.expr.key = kushmanProb.key;
//				prob.expr.infRuleType = kushmanProb.infType;
//			}
			prob.extractAnnotations();
			problemList.add(prob);
		}
		return problemList;
	}

	public static void createTabSeparatedFile(List<StanfordProblem> problems, String tabFile)
			throws IOException {
		String str = "";
		for(StanfordProblem prob : problems) {
			str += prob.question + "\t" + prob.expr.toString() + "="+prob.answer + "\n";
		}
		FileUtils.writeStringToFile(new File(tabFile), str);
	}

//
//	public static void performConsistencyChecks(String dir) throws Exception {
//		List<Problem> probs = Reader.readProblemsFromJson(dir);
//		System.out.println("Problems read : "+probs.size());
//		Set<String> ids = new HashSet<>();
//		for(Problem prob : probs) {
////			ids.add(Dataset.getNormalizedText(prob.ta.getText()));
//			add(ids, Dataset.getNormalizedText(prob.ta.getText()));
//			for(Node leaf : prob.expr.getLeaves()) {
//				if(!Tools.safeEquals(leaf.val, prob.quantities.get(leaf.quantIndex).val)) {
//					System.out.println("Number do not match");
//					System.out.println(prob.id+" "+leaf.val+" "+prob.quantities.get(leaf.quantIndex).val);
//				}
//			}
//			if(!Tools.safeEquals(prob.expr.getValue(), prob.answer)) {
//				System.out.println("Solution do not match");
//				System.out.println(prob.id+" "+prob.expr.getValue()+" "+prob.answer);
//			}
//		}
//		System.out.println("Unique ids found : "+ids.size());
//	}
	
	public static boolean add(Set<String> ids, String normalizedText) {
		boolean allow = true;
		String a1[] = normalizedText.split("_");
		Set<String> set1 = new HashSet<>();
		set1.addAll(Arrays.asList(a1));
		for(int i=0; i<a1.length-1; ++i) {
			set1.add(a1[i]+"_"+a1[i+1]);
		}
		for(String id : ids) {
			String a2[] = id.split("_");
			Set<String> set2 = new HashSet<>();
			set2.addAll(Arrays.asList(a2));
			for(int i=0; i<a2.length-1; ++i) {
				set2.add(a2[i]+"_"+a2[i+1]);
			}
			if(getSim(set1, set2) > 0.8) {
				System.out.println("==>"+normalizedText+"\n==>"+id+"\n");
				allow = false;
				break;
			}
		}
		if(allow) {
			ids.add(normalizedText);
			return true;
		}
		return false;
	}

	public static void createSubsetFromMawpsOutput(String mawpsFile, 
			String allFile, String subsetFile) throws IOException, AnnotatorException {
		String json = FileUtils.readFileToString(new File(mawpsFile));
		List<KushmanFormat> mawpsProbs = new Gson().fromJson(json, 
				new TypeToken<List<KushmanFormat>>(){}.getType());
		json = FileUtils.readFileToString(new File(allFile));
		List<KushmanFormat> allProbs = new Gson().fromJson(json, 
				new TypeToken<List<KushmanFormat>>(){}.getType());
		List<KushmanFormat> subsetProbs = new ArrayList<>();
		Map<String, KushmanFormat> probIdMap = new HashMap<>();
		System.out.println("All problems size : "+allProbs.size());
		for(KushmanFormat prob : allProbs) {
			probIdMap.put(Dataset.getNormalizedText(prob.sQuestion), prob);
		}
		System.out.println("Unique ids : "+probIdMap.keySet().size());
		for(KushmanFormat prob : mawpsProbs) {
			String id = Dataset.getNormalizedText(prob.sQuestion);
			if(!probIdMap.containsKey(id)) {
				System.out.println("Not found "+id);
				System.out.println(prob.sQuestion);
			}
			subsetProbs.add(probIdMap.get(id));
		}
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		json = gson.toJson(subsetProbs);
		FileUtils.writeStringToFile(new File(subsetFile), json);
	}
	
	public static double getSim(Set<String> set1, Set<String> set2) {
		double intersect = 0, union = set2.size();
		for(String str : set1) {
			if(set2.contains(str)) intersect++;
			if(!set2.contains(str)) union++;
		}
		return intersect / (union+0.001);
	}

	public static void analyzeResults(String resultsFile1, String resultsFile2)
			throws IOException {
		List<String> lines1 = FileUtils.readLines(new File(resultsFile1));
		List<String> lines2 = FileUtils.readLines(new File(resultsFile2));
		Set<Integer> errors1 = new HashSet<>();
		Set<Integer> errors2 = new HashSet<>();
		for(int i=0; i<lines1.size(); ++i) {
			if(lines1.get(i).contains("Correct Below")) {
				errors1.add(Integer.parseInt(lines1.get(i+1).split(" ")[0].trim()));
			}
		}
		for(int i=0; i<lines2.size(); ++i) {
			if(lines2.get(i).contains("Correct Below")) {
				errors2.add(Integer.parseInt(lines2.get(i+1).split(" ")[0].trim()));
			}
		}
		System.out.println("Correct 1 : "+errors1.size());
		System.out.println("Correct 2 : "+errors2.size());
		Set<Integer> In1NotIn2 = new HashSet<>();
		for(Integer i : errors1) {
			if(!errors2.contains(i)) {
				In1NotIn2.add(i);
			}
		}
		System.out.println("In 1 Not In 2 : "+Arrays.asList(In1NotIn2));
		System.out.println("In 1 Not In 2 : "+In1NotIn2.size());
		Set<Integer> In2NotIn1 = new HashSet<>();
		for(Integer i : errors2) {
			if(!errors1.contains(i)) {
				In2NotIn1.add(i);
			}
		}
		System.out.println("In 2 Not In 1 : "+Arrays.asList(In2NotIn1));
		System.out.println("In 2 Not In 1 : "+In2NotIn1.size());


	}

	public static void printQuestionsForPython(String outputFile) throws Exception {
		List<Problem> probs = readProblemsFromJson();
		List<ProblemPython> probsPython = new ArrayList<>();
		for(Problem prob : probs) {
			probsPython.add(new ProblemPython(prob));
		}
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		String json = gson.toJson(probsPython);
		FileUtils.writeStringToFile(new File(outputFile), json);
	}

	public static void main(String args[]) throws Exception {
//		performConsistencyChecks(Params.allArithDir);
//		performConsistencyChecks(Params.allArithDirLex);
//		performConsistencyChecks(Params.allArithDirTmpl);
//		createSubsetFromMawpsOutput("data/allArithLex/lex.json", "data/allArith/questions.json", 
//				"data/allArithLex/questions.json");
//		createSubsetFromMawpsOutput("data/allArithTmpl/tmpl.json", "data/allArith/questions.json", 
//				"data/allArithTmpl/questions.json");
//		for(Problem prob : Reader.readProblemsFromJson(Params.allArithDir)) {
//			System.out.println(prob.id+" : "+prob.ta.getText());
//			System.out.println();
//			System.out.println("Schema : "+prob.schema);
//			System.out.println();
//			System.out.println("Quantities : "+prob.quantities);
//			System.out.println();
//			System.out.println("Equantion : "+prob.expr);
//			System.out.println("Answer : "+prob.answer);
//		}

//		analyzeResults("log/InfLCA_v2.out", "log/InfAll_v2.out");

//		printQuestionsForPython("allArithPython.txt");

		createTabSeparatedFile(Reader.readStanfordProblemsFromJson(), "tabFile");
	}


}
