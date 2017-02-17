package reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import structure.KushmanFormat;
import structure.Node;
import structure.Problem;
import structure.QuantSpan;
import utils.Params;
import utils.Tools;

public class Dataset {
	
	public static String getNormalizedText(String text) throws AnnotatorException {
		TextAnnotation ta = Tools.pipeline.createAnnotatedTextAnnotation("", "", text);
		String str = "";
		for(int i=0; i<ta.size(); ++i) {
			if(ta.getToken(i).equals(";")) continue;
			if(ta.getView(ViewNames.POS).getConstituents().get(i).getLabel().equals("CD") ||
					ta.getView(ViewNames.POS).getConstituents().get(i).getLabel().startsWith("N")) {
				str += ta.getView(ViewNames.POS).getConstituents().get(i).getLabel()+"_";
			} else {
				str += ta.getToken(i).toLowerCase()+"_";
			}
		}
		return str;
	}
	
	public static String askForEquationAnnotation(
			String sQuestion, List<String> lEquations, List<QuantSpan> quantities) 
					throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Q : "+sQuestion);
		System.out.println("E : "+Arrays.asList(lEquations));
		System.out.println("Quant : "+Arrays.asList(quantities)+"\n");
		System.out.print("Enter normalized equation : ");
		String eq = br.readLine();
		System.out.println();
		return eq.trim();
	}
	
	public static int askForAlignmentAnnotation(
			String sQuestion, List<String> lEquations, List<QuantSpan> quantities, int j) 
					throws NumberFormatException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Q : "+sQuestion);
		System.out.println("E : "+Arrays.asList(lEquations));
		System.out.println("Quant : "+Arrays.asList(quantities));
		System.out.println("Position in leaves list: "+j+"\n");
		System.out.print("Enter index of quant list: ");
		int loc = Integer.parseInt(br.readLine().trim());
		System.out.println();
		return loc;
	}
	
	public static List<KushmanFormat> readNumNormProblemsFromJsonWithoutAnn(String dir) 
			throws Exception {
		String json = FileUtils.readFileToString(new File(dir+"questions.json"));
		List<KushmanFormat> oldProbs = new Gson().fromJson(json, 
				new TypeToken<List<KushmanFormat>>(){}.getType());
		List<KushmanFormat> newProbs = new ArrayList<>();
		for(KushmanFormat kushmanProb : oldProbs) {
			kushmanProb.quantities = Tools.quantifier.getSpans(kushmanProb.sQuestion);
			String newQues = "";
			int index = 0;
			for(QuantSpan qs : kushmanProb.quantities) {
				newQues += kushmanProb.sQuestion.substring(index, qs.start);
				newQues += qs.val;
				index = qs.end;
			}
			newQues += kushmanProb.sQuestion.substring(index);
			KushmanFormat ks = new KushmanFormat();
			ks.iIndex = kushmanProb.iIndex;
			ks.sQuestion = newQues;
			ks.lEquations = kushmanProb.lEquations;
			ks.lSolutions = kushmanProb.lSolutions;
			ks.quantities = Tools.quantifier.getSpans(ks.sQuestion);
			newProbs.add(ks);
		}
		return newProbs;
	}
	
//	public static void computeProbOverlapStat() throws Exception {
//		String[] dirs = {Params.ai2Dir, Params.ilDir, Params.ccDir, Params.singleEqDir};
//		Set<String> uniqueQuestions = new HashSet<String>();
//		for(String dir1 : dirs) {
//			uniqueQuestions.clear();
//			List<KushmanFormat> probs1 = readNumNormProblemsFromJsonWithoutAnn(dir1);
//			for(KushmanFormat prob : probs1) {
//				String str = getNormalizedText(prob.sQuestion);
//				uniqueQuestions.add(str);
//			}
//			for(String dir2 : dirs) {
//				List<KushmanFormat> probs2 = readNumNormProblemsFromJsonWithoutAnn(dir2); 
//				int repitition = 0;
//				for(KushmanFormat prob : probs2) {
//					String str = getNormalizedText(prob.sQuestion);
//					if(uniqueQuestions.contains(str)) {
//						repitition++;
//					}
//				}
//				System.out.println(dir1 +" "+dir2+" "+repitition);
//			}
//		}
//		for(String dir1 : dirs) {
//			uniqueQuestions.clear();
//			List<KushmanFormat> probs1 = readNumNormProblemsFromJsonWithoutAnn(dir1); 
//			int repitition = 0;
//			for(KushmanFormat prob : probs1) {
//				String str = getNormalizedText(prob.sQuestion);
//				if(uniqueQuestions.contains(str)) {
//					repitition++;
//				}
//				uniqueQuestions.add(str);
//			}
//			System.out.println(dir1+" "+repitition);
//		}
//		System.out.println(uniqueQuestions.size());
//	}
	
//	public static void createPooledDataset() throws Exception {
//		List<KushmanFormat> pooled = new ArrayList<>();
//		List<KushmanFormat> probs = readNumNormProblemsFromJsonWithoutAnn(Params.ai2Dir);
//		probs.addAll(readNumNormProblemsFromJsonWithoutAnn(Params.ilDir));
//		probs.addAll(readNumNormProblemsFromJsonWithoutAnn(Params.ccDir));
//		Set<String> uniqueProbs = new HashSet<>();
//		int index = 0;
//		for(KushmanFormat prob : probs) {
//			if(uniqueProbs.contains(getNormalizedText(prob.sQuestion))) continue;
//			uniqueProbs.add(getNormalizedText(prob.sQuestion));
//			KushmanFormat ks = new KushmanFormat();
//			index ++;
//			if(index <= 1575) continue;
//			ks.iIndex = index;
//			ks.sQuestion = prob.sQuestion;
//			ks.quantities = prob.quantities;
//			ks.lEquations = prob.lEquations;
//			ks.lSolutions = prob.lSolutions;
//			ks.lAlignments = new ArrayList<>();
//			Problem p = new Problem(ks.iIndex, ks.sQuestion, ks.lSolutions.get(0));
//			p.extractQuantities();
//			p.expr = Node.parseNode(prob.lEquations.get(0));
//			List<Node> leaves = p.expr.getLeaves();
//			Set<Integer> matchedQuantIndices = new HashSet<>();
//			for(int j=0; j<leaves.size(); ++j) {
//				Node leaf = leaves.get(j);
//				int quantIndex = -1;
//				int numMatches = 0;
//				for(int i=0; i<ks.quantities.size(); ++i) {
//					QuantSpan qs = ks.quantities.get(i);
//					if(Tools.safeEquals(qs.val, leaf.val) && !matchedQuantIndices.contains(i)) {
//						quantIndex = i;
//						numMatches++;
//					}
//				}
//				if(numMatches > 1) {
//					// Ask for annotation
//					quantIndex = askForAlignmentAnnotation(ks.sQuestion, ks.lEquations, ks.quantities, j);
//				}
//				matchedQuantIndices.add(quantIndex);
//				leaf.quantIndex = quantIndex;
//				leaf.qs = ks.quantities.get(leaf.quantIndex);
//				ks.lAlignments.add(quantIndex);
//			}
//			pooled.add(ks);
//			if(index % 5 == 0) {
//				Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
//				String json = gson.toJson(pooled);
//				FileUtils.writeStringToFile(new File(Params.allArithDir+"questions"+index+".json"), json);
//			}
//		}
//		System.out.println("Now SingleEq");
//		probs.clear();
//		probs.addAll(readNumNormProblemsFromJsonWithoutAnn(Params.singleEqDir));
//		for(KushmanFormat prob : probs) {
//			if(uniqueProbs.contains(getNormalizedText(prob.sQuestion))) continue;
//			uniqueProbs.add(getNormalizedText(prob.sQuestion));
//			KushmanFormat ks = new KushmanFormat();
//			index ++;
//			if(index <= 1575) continue;
//			ks.iIndex = index;
//			ks.sQuestion = prob.sQuestion;
//			ks.quantities = prob.quantities;
//			ks.lSolutions = prob.lSolutions;
//			ks.lEquations = new ArrayList<>();
//			ks.lAlignments = new ArrayList<>();
//			Problem p = new Problem(ks.iIndex, ks.sQuestion, ks.lSolutions.get(0));
//			// Ask for equation annotation
//			String eq = askForEquationAnnotation(ks.sQuestion, prob.lEquations, ks.quantities);
//			if(eq.trim().equals("")) continue;
//			ks.lEquations.add(eq);
//			p.expr = Node.parseNode(ks.lEquations.get(0));
//			List<Node> leaves = p.expr.getLeaves();
//			Set<Integer> matchedQuantIndices = new HashSet<>();
//			for(int j=0; j<leaves.size(); ++j) {
//				Node leaf = leaves.get(j);
//				int quantIndex = -1;
//				int numMatches = 0;
//				for(int i=0; i<ks.quantities.size(); ++i) {
//					QuantSpan qs = ks.quantities.get(i);
//					if(Tools.safeEquals(qs.val, leaf.val) && !matchedQuantIndices.contains(i)) {
//						quantIndex = i;
//						numMatches++;
//					}
//				}
//				if(numMatches > 1) {
//					// Ask for annotation
//					quantIndex = askForAlignmentAnnotation(ks.sQuestion, ks.lEquations, ks.quantities, j);
//				}
//				matchedQuantIndices.add(quantIndex);
//				leaf.quantIndex = quantIndex;
//				leaf.qs = ks.quantities.get(leaf.quantIndex);
//				ks.lAlignments.add(quantIndex);
//			}
//			pooled.add(ks);
//			if(index % 5 == 0) {
//				Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
//				String json = gson.toJson(pooled);
//				FileUtils.writeStringToFile(new File(Params.allArithDir+"questions"+index+".json"), json);
//			}
//		}
//		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
//		String json = gson.toJson(pooled);
//		FileUtils.writeStringToFile(new File(Params.allArithDir+"questions"+index+".json"), json);
//
//	}
	
	
	public static void createDatasetFromOldFormat(String dir) throws Exception {
		List<KushmanFormat> pooled = new ArrayList<>();
		String json = FileUtils.readFileToString(new File(dir+"questions.json"));
		List<KushmanFormat> probs = new Gson().fromJson(json, 
				new TypeToken<List<KushmanFormat>>(){}.getType());
		for(KushmanFormat prob : probs) {
			KushmanFormat ks = new KushmanFormat();
			ks.iIndex = prob.iIndex;
			ks.sQuestion = prob.sQuestion;
			ks.quantities = prob.quantities;
			ks.lEquations = prob.lEquations;
			ks.lSolutions = prob.lSolutions;
			ks.quantities = Tools.quantifier.getSpans(ks.sQuestion);
			ks.lAlignments = new ArrayList<>();
			Problem p = new Problem(ks.iIndex, ks.sQuestion, ks.lSolutions.get(0));
			p.extractQuantities();
			p.expr = Node.parseNode(prob.lEquations.get(0));
			List<Node> leaves = p.expr.getLeaves();
			Set<Integer> matchedQuantIndices = new HashSet<>();
			for(int j=0; j<leaves.size(); ++j) {
				Node leaf = leaves.get(j);
				int quantIndex = -1;
				for(int i=0; i<ks.quantities.size(); ++i) {
					QuantSpan qs = ks.quantities.get(i);
					if(Tools.safeEquals(qs.val, leaf.val) 
							&& qs.start <= prob.lAlignments.get(j) 
							&& prob.lAlignments.get(j) < qs.end
							) {
						quantIndex = i;
					}
				}
				matchedQuantIndices.add(quantIndex);
				if(quantIndex == -1) {
					System.out.println("Quantity not found");
				}
				leaf.quantIndex = quantIndex;
				leaf.qs = ks.quantities.get(leaf.quantIndex);
				ks.lAlignments.add(quantIndex);
			}
			pooled.add(ks);
		}
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		json = gson.toJson(pooled);
		FileUtils.writeStringToFile(new File(dir+"/questionsNew.json"), json);
		
	}
	
	public static void reduceDataset(String oldQuestionsFile, String newQuestionsFile) throws Exception {
		String json = FileUtils.readFileToString(new File(oldQuestionsFile));
		List<KushmanFormat> oldProbs = new Gson().fromJson(json, 
				new TypeToken<List<KushmanFormat>>(){}.getType());
		List<KushmanFormat> newProbs = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for(KushmanFormat kushmanProb : oldProbs) {
			String id = getNormalizedText(kushmanProb.sQuestion);
			if(Reader.add(ids, id)) {
				newProbs.add(kushmanProb);
			}
		}
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		json = gson.toJson(newProbs);
		FileUtils.writeStringToFile(new File(newQuestionsFile), json);
	}
	
	public static void main(String args[]) throws Exception {
//		computeProbOverlapStat();
//		createPooledDataset();
//		reduceDataset("data/allArith/questionsLong.json", "data/allArith/short.json");
		createDatasetFromOldFormat("data/ai2/");
		createDatasetFromOldFormat("data/illinois/");
		createDatasetFromOldFormat("data/commoncore/");
	}
 	
}
