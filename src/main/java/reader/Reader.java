package reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import structure.*;
import utils.Params;
import utils.Tools;


public class Reader {
	
	public static List<Problem> readProblemsFromJson() throws Exception {
		String json = FileUtils.readFileToString(new File(Params.questionsFile));
		List<DataFormat> kushmanProbs = new Gson().fromJson(json,
				new TypeToken<List<DataFormat>>(){}.getType());
		List<Problem> problemList = new ArrayList<>();
		for(DataFormat kushmanProb : kushmanProbs) {
			Problem prob = new Problem(kushmanProb.iIndex, kushmanProb.sQuestion, 
					kushmanProb.lSolutions.get(0));
//			prob.quants = kushmanProb.quants;
			prob.extractQuantities();
			prob.expr = Node.parseNode(kushmanProb.lEquations.get(0));
			assert prob.expr.getLeaves().size() == kushmanProb.lAlignments.size();
			for(int j=0; j<prob.expr.getLeaves().size(); ++j) {
				Node node = prob.expr.getLeaves().get(j);
				node.quantIndex = kushmanProb.lAlignments.get(j);
				node.qs = prob.quantities.get(node.quantIndex);
			}
			prob.extractAnnotations();
			problemList.add(prob);
		}
		return problemList;
	}

	public static List<StanfordProblem> readStanfordProblemsFromJson()
			throws Exception {
		String json = FileUtils.readFileToString(new File(Params.questionsFile));
		List<DataFormat> kushmanProbs = new Gson().fromJson(json,
				new TypeToken<List<DataFormat>>(){}.getType());
		List<StanfordProblem> problemList = new ArrayList<>();
		for(DataFormat kushmanProb : kushmanProbs) {
			StanfordProblem prob = new StanfordProblem(
					kushmanProb.iIndex,
					kushmanProb.sQuestion,
					kushmanProb.lSolutions.get(0));
//			prob.quants = kushmanProb.quants;
			prob.extractQuantities();
			prob.expr = Node.parseNode(kushmanProb.lEquations.get(0));
			assert prob.expr.getLeaves().size() == kushmanProb.lAlignments.size();
			for(int j=0; j<prob.expr.getLeaves().size(); ++j) {
				Node node = prob.expr.getLeaves().get(j);
				node.quantIndex = kushmanProb.lAlignments.get(j);
				node.qs = prob.quantities.get(node.quantIndex);
			}
			prob.extractAnnotations();
			problemList.add(prob);
		}
		return problemList;
	}

	public static void createTabSeparatedAnswerPerturbFile(
			List<StanfordProblem> problems, String tabFile) throws IOException {
		String str = "";
		List<String> labels = Arrays.asList("ADD", "SUB", "MUL", "DIV", "SUB_REV", "DIV_REV");
		for(StanfordProblem prob : problems) {
			Node copy = new Node(prob.expr);
			for(Node node : copy.getAllSubNodes()) {
				if(!labels.contains(node.label)) continue;
				String origLabel = node.label;
				for(String label : labels) {
					if((origLabel.equals("ADD") || origLabel.equals("SUB")) &&
							(label.startsWith("MUL") || label.startsWith("DIV"))) continue;
					if((origLabel.equals("MUL") || origLabel.equals("DIV")) &&
							(label.startsWith("ADD") || label.startsWith("SUB"))) continue;
					if(label.equals(origLabel)) continue;
					if(label.endsWith("REV")) {
						node.label = label.substring(0, 3);
						node.children.add(node.children.get(0));
						node.children.remove(0);
					} else {
						node.label = label;
					}
					double val = copy.getValue();
					boolean answerSame = false;
					if(Tools.safeEquals(val, prob.answer)) {
						System.out.println("Problem here between "+
								prob.expr.toString()+" and "+copy.toString());
						answerSame = true;
					}
					if(val > 1.0 && !answerSame) {
						str += prob.question + "\t" + copy.toString() + "=" +
								copy.getValue() + "\n";
					}
					if(label.endsWith("REV")) {
						node.children.add(node.children.get(0));
						node.children.remove(0);
					}
				}
				node.label = origLabel;
			}
		}
		FileUtils.writeStringToFile(new File(tabFile), str);
	}

	public static void main(String args[]) throws Exception {
		createTabSeparatedAnswerPerturbFile(
				Reader.readStanfordProblemsFromJson(), "perturbFile");
	}


}
