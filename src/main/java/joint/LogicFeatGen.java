package joint;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.stanford.nlp.ling.CoreLabel;
import structure.Node;
import structure.StanfordSchema;
import utils.FeatGen;
import utils.Tools;

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
		features.addAll(getFeatures(x, y.expr, true));
		return features;
	}

	public static List<String> getFeatures(LogicX x, Node node, boolean isTopmost) {
		List<String> features = new ArrayList<>();
		if(node.children.size() == 0) return features;
		if(node.children.get(0).quantIndex < node.children.get(1).quantIndex) {
			features.addAll(getCombinationFeatures(
					x,
					x.schema.get(node.children.get(0).quantIndex),
					x.schema.get(node.children.get(1).quantIndex),
					x.questionSchema,
					node.infRuleType,
					node.key,
					isTopmost));
		} else {
			features.addAll(getCombinationFeatures(
					x,
					x.schema.get(node.children.get(1).quantIndex),
					x.schema.get(node.children.get(0).quantIndex),
					x.questionSchema,
					node.infRuleType,
					node.key,
					isTopmost));
		}
		features.addAll(getFeatures(x, node.children.get(0), false));
		features.addAll(getFeatures(x, node.children.get(1), false));
		return features;
	}

	public IFeatureVector getCombinationFeatureVector(LogicX x,
													  StanfordSchema num1,
													  StanfordSchema num2,
													  StanfordSchema ques,
													  int infRuleType,
													  String key,
													  boolean isTopmost) {
		List<String> features = getCombinationFeatures(
				x, num1, num2, ques, infRuleType, key, isTopmost);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public static List<String> getCombinationFeatures(LogicX x,
													  StanfordSchema num1,
													  StanfordSchema num2,
													  StanfordSchema ques,
													  int infRuleType,
													  String key,
													  boolean isTopmost) {
		List<String> infFeatures = new ArrayList<>();
		infFeatures.addAll(getInfTypeFeatures(x, num1, num2, ques, infRuleType, isTopmost));

		List<String> features = new ArrayList<>();
		features.addAll(infFeatures);
		return features;
	}

	public static List<String> getInfTypeFeatures(LogicX x,
												  StanfordSchema num1,
												  StanfordSchema num2,
												  StanfordSchema ques,
												  int infRuleType,
												  boolean isTopmost) {
		List<String> features = new ArrayList<>();
		features.addAll(getSingleSchemaFeatures(x, num1, isTopmost));
		features.addAll(getSingleSchemaFeatures(x, num2, isTopmost));
		features.addAll(getSingleSchemaFeatures(x, ques, isTopmost));

		if(!features.contains("RateDetected")) {
			features.add("RateNotDetected");
		}
		if(!features.contains("MathDetected")) {
			features.add("MathNotDetected");
		}
		if(x.tokens.get(num1.sentId).get(num1.verb).lemma().equals(
				x.tokens.get(num2.sentId).get(num2.verb).lemma())) {
			features.add("Verb12Same");
		} else {
			features.add("Verb12Diff");
		}
		boolean midVerb = LogicY.midVerb(x.tokens, num1, num2);
		features.add("MidVerb:"+midVerb);
		return FeatGen.getFeaturesConjWithLabels(
				features,
//				FeatGen.getConjunctions(features),
				"InfRule:"+infRuleType);
	}



	public static List<String> getSingleSchemaFeatures(
			LogicX x, StanfordSchema schema, boolean isTopmost) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens = x.tokens.get(schema.sentId);
		if(isTopmost && schema.qs == null) {
			for (int i = x.questionSpan.getFirst();
				 i < x.questionSpan.getSecond();
				 ++i) {
				if (!tokens.get(i).tag().startsWith("N")) {
					features.add("QuesUnigram_" + tokens.get(i).lemma());
				}
			}
		}
		if(schema.rate != null && schema.rate.getFirst() >= 0) {
			features.add("RateDetected");
		}
		if(schema.math != -1) {
			features.add("MathDetected");
		}
		return features;
	}





}

