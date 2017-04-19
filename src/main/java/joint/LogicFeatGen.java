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
													  String infRuleType,
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
													  String infRuleType,
													  String key,
													  boolean isTopmost) {
		List<String> infFeatures = new ArrayList<>();
		infFeatures.addAll(getInfTypeFeatures(x, num1, num2, ques, infRuleType, isTopmost));

		List<String> keyFeatures = new ArrayList<>();
		keyFeatures.addAll(FeatGen.getFeaturesConjWithLabels(
				getKeyFeatures(x, num1, num2, ques, infRuleType, key, isTopmost), infRuleType+""));

		List<String> features = new ArrayList<>();
		features.addAll(infFeatures);
		features.addAll(keyFeatures);
		return features;
	}

	public static List<String> getInfTypeFeatures(LogicX x,
												  StanfordSchema num1,
												  StanfordSchema num2,
												  StanfordSchema ques,
												  String infRuleType,
												  boolean isTopmost) {
		List<String> features = new ArrayList<>();
		if(infRuleType.startsWith("Rate")) {
			if(infRuleType.startsWith("Rate0")) {
				features.addAll(getRateFeatures(x, num1));
				features.addAll(getNeighborhoodFeatures(x, num1));
			}
			if(infRuleType.startsWith("Rate1")) {
				features.addAll(getRateFeatures(x, num2));
				features.addAll(getNeighborhoodFeatures(x, num2));
			}
			if(infRuleType.startsWith("RateQues")) {
				features.addAll(getRateFeatures(x, ques));
				features.addAll(getNeighborhoodFeatures(x, ques));
			}
		}
		if(x.tokens.get(num1.sentId).get(num1.verb).lemma().equals(
				x.tokens.get(num2.sentId).get(num2.verb).lemma())) {
			features.add("Verb12Same");
		} else {
			features.add("Verb12Diff");
		}
		boolean midVerb = LogicY.midVerb(x.tokens, num1, num2);
		features.add("MidVerb:"+midVerb);
		features.addAll(getPartitonFeatures(x, num1, num2));
		return FeatGen.getFeaturesConjWithLabels(
				features,
				"InfRule:"+infRuleType);
	}


	public static List<String> getPhraseByMode(List<List<CoreLabel>> tokens,
											   StanfordSchema schema,
											   String mode) {
		if(mode.equals("SUBJ")) {
			return Tools.spanToLemmaList(tokens.get(schema.sentId), schema.subject);
		}
		if(mode.equals("OBJ")) {
			return Tools.spanToLemmaList(tokens.get(schema.sentId), schema.object);
		}
		if(mode.equals("UNIT")) {
			return Tools.spanToLemmaList(tokens.get(schema.sentId), schema.unit);
		}
		if(mode.equals("RATE")) {
			return Tools.spanToLemmaList(tokens.get(schema.sentId), schema.rate);
		}
		return new ArrayList<>();
	}

	public static List<String> getSingleKeyFeatures(
			LogicX x, StanfordSchema schema, boolean isTopmost) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens = x.tokens.get(schema.sentId);
		if(schema.rate != null && schema.rate.getFirst() >= 0) {
			features.add("RateDetected");
		}
		if(isTopmost && schema.qs == null) {
			for (int i = x.questionSpan.getFirst();
				 i < x.questionSpan.getSecond();
				 ++i) {
				if (!tokens.get(i).tag().startsWith("N")) {
					features.add("Unigram_" + tokens.get(i).lemma());
				}
			}
		}
		if(schema.qs != null) {
			int tokenId = Tools.getTokenIdFromCharOffset(tokens, schema.qs.start);
			for(int i=Math.max(0, tokenId-3); i<Math.min(tokenId+4, tokens.size()); ++i) {
				if (!tokens.get(i).tag().startsWith("N")) {
					features.add("Unigram_" + tokens.get(i).lemma());
				}
			}
		}
		return features;
	}

	public static List<String> getKeyFeatures(LogicX x,
											  StanfordSchema num1,
											  StanfordSchema num2,
											  StanfordSchema ques,
											  String infRuleType,
											  String key,
											  boolean isTopmost) {
		List<String> features = new ArrayList<>();
		features.add(infRuleType+"_"+key);

		if(infRuleType.startsWith("Verb") || infRuleType.startsWith("Math")) {
			if(key.equals("0_0")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "SUBJ", "SUBJ"));
			}
			if(key.equals("0_1")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "SUBJ", "OBJ"));
			}
			if(key.equals("1_0")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "OBJ", "SUBJ"));
			}
			if(key.equals("QUES")) {
				features.addAll(getPairSchemaFeatures(x, num1, ques, "SUBJ", "SUBJ"));
			}
			if(key.equals("QUES_REV")) {
				features.addAll(getPairSchemaFeatures(x, num2, ques, "OBJ", "SUBJ"));
			}
		}
		if(infRuleType.startsWith("Rate")) {
			if(key.equals("0_0")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "UNIT", "UNIT"));
			}
			if(key.equals("0_1")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "UNIT", "RATE"));
			}
			if(key.equals("1_0")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "RATE", "UNIT"));
			}
			if(key.equals("QUES")) {
				features.addAll(getPairSchemaFeatures(x, num1, ques, "UNIT", "UNIT"));
				features.addAll(getPairSchemaFeatures(x, num2, ques, "UNIT", "RATE"));
			}
			if(key.equals("QUES_REV")) {
				features.addAll(getPairSchemaFeatures(x, num2, ques, "UNIT", "UNIT"));
				features.addAll(getPairSchemaFeatures(x, num1, ques, "UNIT", "RATE"));
			}
		}
		if(infRuleType.equals("Partition")) {
			features.addAll(FeatGen.getFeaturesConjWithLabels(
					getPartitonFeatures(x, num1, num2), key));
			for(int i=x.questionSpan.getFirst(); i<x.questionSpan.getSecond(); ++i) {
				if(x.tokens.get(ques.sentId).get(i).word().equals("all") ||
						x.tokens.get(ques.sentId).get(i).word().equals("altogether") ||
						x.tokens.get(ques.sentId).get(i).word().equals("overall")) {
					features.add("QuestionAll_"+key);
				}
			}
		}
		features.addAll(FeatGen.getConjunctions(features));
		return features;
	}

	// Mode has to be one of "SUBJ", "OBJ", "UNIT", "RATE"
	// Note that this is a symmetric similarity function, lets keep it that way
	public static List<String> getPairSchemaFeatures(
			LogicX x, StanfordSchema schema1, StanfordSchema schema2, String mode1, String mode2) {
		List<String> features = new ArrayList<>();
		List<String> phrase1 = getPhraseByMode(x.tokens, schema1, mode1);
		List<String> phrase2 = getPhraseByMode(x.tokens, schema2, mode2);
		double sim = Tools.jaccardSim(phrase1, phrase2);
		if(sim > 0.5) features.add("SimMoreThanHalfPhraseMatch");
		if(sim > 0.9) features.add("SimExactPhraseMatch");

		double entail = Tools.jaccardEntail(phrase1, phrase2);
		if(entail > 0.5) features.add("EntailMoreThanHalfPhraseMatch");
		if(entail > 0.9) features.add("EntailExactPhraseMatch");
//		if(entail < 0.2) features.add("EntailAbsolutelyNoMatch");

		StanfordSchema emptyUnitSchema = null;
		List<String> phraseOther = null;
		if(phrase1.size() == 0 && mode1.equals("UNIT")) {
			emptyUnitSchema = schema1;
			phraseOther = phrase2;
		}
		if(phrase2.size() == 0 && mode2.equals("UNIT")) {
			emptyUnitSchema = schema2;
			phraseOther = phrase1;
		}
		// If unit was not extracted, copy last unit over
		if(emptyUnitSchema != null) {
			if(emptyUnitSchema.qs != null && emptyUnitSchema.quantId >= 1) {
				StanfordSchema prevSchema = x.schema.get(emptyUnitSchema.quantId-1);
				sim = Tools.jaccardSim(Tools.spanToLemmaList(
						x.tokens.get(prevSchema.sentId),
						prevSchema.unit), phraseOther);
				if(sim > 0.5) features.add("SimMoreThanHalfPhraseMatch");
				if(sim > 0.9) features.add("SimExactPhraseMatch");
			}
		}
		if(sim < 0.2) features.add("SimAbsolutelyNoMatch");
		if(phrase1.size() == 0) features.add("Empty"+mode1);
		if(phrase2.size() == 0) features.add("Empty"+mode2);
		return features;
	}

	public static List<String> getPartitonFeatures(
			LogicX x, StanfordSchema num1, StanfordSchema num2) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens = x.tokens.get(num1.sentId);
		int tokenId = Tools.getTokenIdFromCharOffset(tokens, num1.qs.start);
		for (int i = tokenId + 1; i < tokens.size(); ++i) {
			if (tokens.get(i).word().equals("remaining") ||
					tokens.get(i).word().equals("rest")) {
				features.add("RemainingRest");
			}
			if (tokens.get(i).word().toLowerCase().equals("either")) {
				features.add("Either");
			}
		}
		tokens = x.tokens.get(num2.sentId);
		tokenId = Tools.getTokenIdFromCharOffset(tokens, num2.qs.start);
		for (int i = tokenId + 1; i < tokens.size(); ++i) {
			if (tokens.get(i).word().equals("remaining") ||
					tokens.get(i).word().equals("rest")) {
				features.add("RemainingRest");
			}
			if (tokens.get(i).word().toLowerCase().equals("either")) {
				features.add("Either");
			}
		}
		return features;
	}

	public static List<String> getRateFeatures(LogicX x, StanfordSchema schema) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens = x.tokens.get(schema.sentId);
		if(schema.qs == null) {
			for(int i=x.questionSpan.getFirst(); i<x.questionSpan.getSecond(); ++i) {
				if (tokens.get(i).lemma().equals("each") ||
						tokens.get(i).lemma().equals("every") ||
						tokens.get(i).lemma().equals("per")) {
					features.add("RateStuffDetected");
					break;
				}
			}
		} else {
			int tokenId = Tools.getTokenIdFromCharOffset(tokens, schema.qs.start);
			for (int i = Math.max(0, tokenId - 3); i < Math.min(tokenId + 4, tokens.size()); ++i) {
				if (tokens.get(i).lemma().equals("each") ||
						tokens.get(i).lemma().equals("every") ||
						tokens.get(i).lemma().equals("per")) {
					features.add("RateStuffDetected");
					break;
				}
			}
		}
		if(schema.rate != null && schema.rate.getFirst() >= 0) {
			features.add("RateDetected");
		}
		return features;
	}

	public static List<String> getNeighborhoodFeatures(LogicX x, StanfordSchema schema) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens = x.tokens.get(schema.sentId);
		if(schema.qs == null) {
			for (int i = x.questionSpan.getFirst();
				 i < x.questionSpan.getSecond();
				 ++i) {
				if (!tokens.get(i).tag().startsWith("N")) {
					features.add("QuesUnigram_" + tokens.get(i).lemma());
				}
			}
		} else {
			int tokenId = Tools.getTokenIdFromCharOffset(tokens, schema.qs.start);
			for (int i = Math.max(0, tokenId - 3); i < Math.min(tokenId + 4, tokens.size()); ++i) {
				if (!tokens.get(i).tag().startsWith("N")) {
					features.add("QuesUnigram_" + tokens.get(i).lemma());
				}
			}
		}
		return features;
	}





}

