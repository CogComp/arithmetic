package joint;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import structure.Node;
import structure.StanfordSchema;
import utils.FeatGen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LogicFeatGen extends AbstractFeatureGenerator implements Serializable {

	private static final long serialVersionUID = -5902462551801564955L;
	public Lexiconer lm = null;

	public LogicFeatGen(Lexiconer lm) {
		this.lm = lm;
	}

	@Override
	public IFeatureVector getFeatureVector(IInstance prob, IStructure struct) {
		LogicX x = (LogicX) prob;
		LogicY y = (LogicY) struct;
		List<String> features = getFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public static List<String> getFeatures(LogicX x, LogicY y) {
		List<String> features = new ArrayList<>();
		for(Node node : y.expr.getAllSubNodes()) {
			if(node.children.size() == 0) continue;
			features.addAll(getInfTypeFeatures(
					x,
					x.schema.get(node.children.get(0).quantIndex),
					x.schema.get(node.children.get(1).quantIndex),
					x.questionSchema,
					node.infRuleType,
					node.key));
		}
		return features;
	}

	public static List<String> getInfTypeFeatures(LogicX x,
												  StanfordSchema num1,
												  StanfordSchema num2,
												  StanfordSchema ques,
												  int infRuleType,
												  String key) {
		List<String> features = new ArrayList<>();
		if(num1.rate != null && num1.rate.getFirst() != -1) {
			features.add("RateDetected");
		}
		if(num2.rate != null && num2.rate.getFirst() != -1) {
			features.add("RateDetected");
		}
		if(ques.rate != null && ques.rate.getFirst() != -1) {
			features.add("RateDetected");
		}
		if(num1.math != -1) {
			features.add("MathDetected");
		}
		if(num2.math != -1) {
			features.add("MathDetected");
		}
		if(ques.math != -1) {
			features.add("MathDetected");
		}
		if(x.tokens.get(num1.sentId).get(num1.verb).lemma().equals(
				x.tokens.get(num2.sentId).get(num2.verb).lemma())) {
			features.add("Verb12Same");
		}
		if(x.tokens.get(num1.sentId).get(num1.verb).lemma().equals(
				x.tokens.get(num2.sentId).get(num2.verb).lemma()) &&
				x.tokens.get(num1.sentId).get(num1.verb).lemma().equals(
						x.tokens.get(ques.sentId).get(ques.verb).lemma())) {
			features.add("AllVerbsSame");
		}
		return FeatGen.getFeaturesConjWithLabels(features, ""+infRuleType);
	}

	public IFeatureVector getInfTypeFeatureVector(LogicX x,
												  StanfordSchema num1,
												  StanfordSchema num2,
												  StanfordSchema ques,
												  int infRuleType,
												  String key) {
		List<String> features = getInfTypeFeatures(x, num1, num2, ques, infRuleType, key);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

}

