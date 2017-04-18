package logic;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.stanford.nlp.ling.CoreLabel;
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
		String key = y.key;
		if(key.equals("0_NUM") || key.equals("1_NUM")) {
			features.addAll(getPairSchemaFeatures(x, x.num1, x.num2, "UNIT", "UNIT"));
		}
		if(key.equals("0_DEN")) {
			features.addAll(getPairSchemaFeatures(x, x.num1, x.num2, "RATE", "UNIT"));
		}
		if(key.equals("1_DEN")) {
			features.addAll(getPairSchemaFeatures(x, x.num1, x.num2, "UNIT", "RATE"));
		}
		if(key.equals("QUES")) {
			features.addAll(getPairSchemaFeatures(x, x.num1, x.questionSchema, "UNIT", "UNIT"));
		}
		if(key.equals("QUES_REV")) {
			features.addAll(getPairSchemaFeatures(x, x.num2, x.questionSchema, "UNIT", "UNIT"));
		}
		if(key.equals("0_0")) {
			features.addAll(getPairSchemaFeatures(x, x.num1, x.num2, "SUBJ", "SUBJ"));
		}
		if(key.equals("0_1")) {
			features.addAll(getPairSchemaFeatures(x, x.num1, x.num2, "SUBJ", "OBJ"));
		}
		if(key.equals("1_0")) {
			features.addAll(getPairSchemaFeatures(x, x.num1, x.num2, "OBJ", "SUBJ"));
		}
		if(key.equals("0")) {
			features.addAll(getPairSchemaFeatures(x, x.num1, x.questionSchema, "SUBJ", "SUBJ"));
		}
		if(key.equals("1")) {
			features.addAll(getPairSchemaFeatures(x, x.num2, x.questionSchema, "OBJ", "SUBJ"));
		}
		String wn;
		if(key.equals("HYPO") || key.equals("HYPER") || key.equals("SIBLING")) {
			wn = Tools.wordnetIndicator(
					Tools.spanToLemmaList(x.tokens.get(x.num1.sentId), x.num1.unit),
					Tools.spanToLemmaList(x.tokens.get(x.num2.sentId), x.num2.unit),
					Tools.populatePos(x.tokens.get(x.num1.sentId), x.num1.unit),
					Tools.populatePos(x.tokens.get(x.num2.sentId), x.num2.unit),
					x.wordnetRelations);
			if(wn != null) features.add(wn);
		}
		if(key.equals("QUES_1_SIBLING")) {
			wn = Tools.wordnetIndicator(
					Tools.spanToLemmaList(x.tokens.get(x.num1.sentId), x.num1.unit),
					Tools.spanToLemmaList(x.tokens.get(x.questionSchema.sentId), x.questionSchema.unit),
					Tools.populatePos(x.tokens.get(x.num1.sentId), x.num1.unit),
					Tools.populatePos(x.tokens.get(x.questionSchema.sentId), x.questionSchema.unit),
					x.wordnetRelations);
			if(wn != null) features.add(wn);
		}
		if(key.equals("QUES_1_SIBLING")) {
			wn = Tools.wordnetIndicator(
					Tools.spanToLemmaList(x.tokens.get(x.num2.sentId), x.num2.unit),
					Tools.spanToLemmaList(x.tokens.get(x.questionSchema.sentId), x.questionSchema.unit),
					Tools.populatePos(x.tokens.get(x.num2.sentId), x.num2.unit),
					Tools.populatePos(x.tokens.get(x.questionSchema.sentId), x.questionSchema.unit),
					x.wordnetRelations);
			if(wn != null) features.add(wn);
		}
		features.addAll(FeatGen.getConjunctions(features));
		features.addAll(FeatGen.getFeaturesConjWithLabels(
				getSingleSchemaFeatures(x, x.num1, x.isTopmost), "1"+y.key));
		features.addAll(FeatGen.getFeaturesConjWithLabels(
				getSingleSchemaFeatures(x, x.num2, x.isTopmost), "2"+y.key));
		features.addAll(FeatGen.getFeaturesConjWithLabels(
				getSingleSchemaFeatures(x, x.questionSchema, x.isTopmost), "Q"+y.key));
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
//		if(sim > 0.5) features.add("SimMoreThanHalfPhraseMatch");
		if(sim > 0.9) features.add("SimExactPhraseMatch");

		double entail = Tools.jaccardEntail(phrase1, phrase2);
//		if(entail > 0.5) features.add("EntailMoreThanHalfPhraseMatch");
//		if(entail > 0.9) features.add("EntailExactPhraseMatch");
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
//			features.add("OneOfThemEmpty");
			if(emptyUnitSchema.qs != null && emptyUnitSchema.quantId >= 1) {
				StanfordSchema prevSchema = x.schema.get(emptyUnitSchema.quantId-1);
				sim = Tools.jaccardSim(Tools.spanToLemmaList(
						x.tokens.get(prevSchema.sentId),
						prevSchema.unit), phraseOther);
//				if(sim > 0.5) features.add("SimMoreThanHalfPhraseMatch");
				if(sim > 0.9) features.add("SimExactPhraseMatch");
			}
		}
		if(sim < 0.2) features.add("SimAbsolutelyNoMatch");
		return features;
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

	public static List<String> getSingleSchemaFeatures(
			LogicX x, StanfordSchema schema, boolean isTopmost) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens = x.tokens.get(schema.sentId);
		if(schema.rate != null && schema.rate.getFirst() >= 0) {
			features.add("RateDetected");
		}
		if(schema.math != -1) {
			features.add("MathDetected");
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

}

