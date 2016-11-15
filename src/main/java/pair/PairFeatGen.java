package pair;

import java.io.Serializable;
import java.util.*;

import structure.QuantitySchema;
import utils.FeatGen;
import utils.Tools;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class PairFeatGen extends AbstractFeatureGenerator
implements Serializable{

	private static final long serialVersionUID = -5902462551801564955L;
	public Lexiconer lm = null;
	
	public PairFeatGen(Lexiconer lm) {
		this.lm = lm;
	}

	@Override
	public IFeatureVector getFeatureVector(IInstance prob, IStructure struct) {
		PairX x = (PairX) prob;
		PairY y = (PairY) struct;
		List<String> featuresWithPrefix = new ArrayList<String>();
		String prefix = y.label;
		featuresWithPrefix.add(prefix);
		List<String> features = getFeatures(x);
		features.addAll(FeatGen.getConjunctions(features));
		for(String feature : features) {
			featuresWithPrefix.add(prefix + "_" + feature);
		}
		return FeatGen.getFeatureVectorFromListString(featuresWithPrefix, lm);
	}
	
	public static List<String> getFeatures(PairX x) {
		List<String> features = new ArrayList<String>();
		features.addAll(globalFeatures(x));
		features.addAll(perQuantityFeatures(x));
		features.addAll(pairQuantityFeatures(x));
		return features;
	}
	
	// Whether question objects are found elsewhere, more or less is used
	public static List<String> globalFeatures(PairX x) {
		List<String> features = new ArrayList<String>();
		// Just take the first NP
		for(String token : x.schema.questionTokens) {
			if(token.equalsIgnoreCase("more") || token.equalsIgnoreCase("less") ||
					token.equalsIgnoreCase("than")) {
				features.add("MoreOrLessOrThanPresentInQuestion");
				break;
			}
		}
		for(String token : x.schema.questionTokens) {
			if(token.equalsIgnoreCase("each")) {
				features.add("EachPresentInQuestion");
				break;
			}
		}
		for(String token : x.schema.questionTokens) {
			if(token.equalsIgnoreCase("left")) {
				features.add("LeftPresentInQuestion");
				break;
			}
		}
		return features;
	}
	
	public static List<String> perQuantityFeatures(PairX x) {
		List<String> features = new ArrayList<String>();
		for(String feature : perQuantityFeatures(x, x.quantIndex1)) {
			features.add("1_"+feature);
		}
		for(String feature : perQuantityFeatures(x, x.quantIndex2)) {
			features.add("2_"+feature);
		}
		return features;
	}
	

	public static List<String> pairQuantityFeatures(PairX x) {
		List<String> features = new ArrayList<String>();
		Constituent verbPhrase1 = x.schema.quantSchemas.get(x.quantIndex1).verbPhrase;
		Constituent verbPhrase2 = x.schema.quantSchemas.get(x.quantIndex2).verbPhrase;
		String verb1 = x.schema.quantSchemas.get(x.quantIndex1).verb;
		String verb2 = x.schema.quantSchemas.get(x.quantIndex2).verb;
		String unit1 = x.schema.quantSchemas.get(x.quantIndex1).unit;
		String unit2 = x.schema.quantSchemas.get(x.quantIndex2).unit;
		Constituent rate1 = x.schema.quantSchemas.get(x.quantIndex1).rateUnit;
		Constituent rate2 = x.schema.quantSchemas.get(x.quantIndex2).rateUnit;
		if(verbPhrase1 != null && verbPhrase2 != null) {
			if(verbPhrase1.getSpan().equals(verbPhrase2.getSpan())) {
				features.add("SameVerbInstance");
			}
		}
		if(verb1.equals(verb2)) {
			features.add("SameVerbForm");
		}
		if(Tools.getNumTokenMatches(Arrays.asList(unit1.split(" ")), 
				Arrays.asList(unit2.split(" ")))>0) {
			features.add("CommonTokenInUnit");
		}
		if(rate2 != null && Tools.getNumTokenMatches(Arrays.asList(unit1.split(" ")), 
				Tools.getTokensList(rate2))>0) {
			features.add("CommonTokenInRate2");
		}
		if(rate1 != null && Tools.getNumTokenMatches(Arrays.asList(unit2.split(" ")), 
				Tools.getTokensList(rate1))>0) {
			features.add("CommonTokenInRate1");
		}
		if(x.quantities.get(x.quantIndex1).val > x.quantities.get(x.quantIndex2).val) {
			features.add("Ascending");
		}
		return features;
	}
	
	public static List<String> perQuantityFeatures(PairX x, int quantIndex) {
		List<String> features = new ArrayList<String>();
		QuantitySchema qSchema = x.schema.quantSchemas.get(quantIndex);
		int tokenId = x.ta.getTokenIdFromCharacterOffset(x.quantities.get(quantIndex).start);
		features.add("Context_Verb_"+qSchema.verb);
//		if(qSchema.verbPhrase != null) {
//			if(qSchema.quantPhrase.getStartSpan() < qSchema.verbPhrase.getStartSpan()) {
//				features.add("QuantityToTheLeftOfVerb");
//			} else {
//				features.add("QuantityToTheRightOfVerb");
//			}
//		}
		// Neighboring adverbs or comparative adjectives
		for(int i=-3; i<=3; ++i) {
			if(tokenId+i<x.ta.size() && tokenId+i>=0 && (
					x.posTags.get(tokenId+i).getLabel().startsWith("RB") || 
					x.posTags.get(tokenId+i).getLabel().startsWith("JJR"))) {
				features.add("Neighbor_"+x.lemmas.get(tokenId+i));
			}
		}
		// Rate present
		if(qSchema.rateUnit != null) {
			features.add("RateUnitPresent");
		}
		if(qSchema.rateUnit != null && Tools.getNumTokenMatches(x.schema.questionTokens, 
				Tools.getTokensList(qSchema.rateUnit))>0) {
			features.add("RateFoundInQuestion");
		}
		features.addAll(FeatGen.getNeighborhoodFeatures(x.ta, x.posTags,
				x.ta.getTokenIdFromCharacterOffset(x.quantities.get(quantIndex).start), 2));
		return features;
	}

}

