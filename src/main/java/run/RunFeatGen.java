package run;

import java.io.Serializable;
import java.util.*;

import utils.FeatGen;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class RunFeatGen extends AbstractFeatureGenerator 
implements Serializable{

	private static final long serialVersionUID = -5902462551801564955L;
	public Lexiconer lm = null;
	
	public RunFeatGen(Lexiconer lm) {
		this.lm = lm;
	}

	@Override
	public IFeatureVector getFeatureVector(IInstance prob, IStructure struct) {
		RunX x = (RunX) prob;
		RunY y = (RunY) struct;
		return FeatGen.getFeatureVectorFromListString(getFeatures(x, y.label), lm);
	}

	public static List<String> getFeatures(RunX x, String label) {
		List<String> featuresWithPrefix = new ArrayList<String>();
		String prefix = label;
		featuresWithPrefix.add(prefix);
		List<String> features = getFeatures(x);
		features.addAll(FeatGen.getConjunctions(features));
		for(String feature : features) {
			featuresWithPrefix.add(prefix + "_" + feature);
		}
		return featuresWithPrefix;
	}

	public static List<String> getFeatures(RunX x) {
		List<String> features = new ArrayList<String>();
		features.addAll(perQuantityFeatures(x));
		features.addAll(pairQuantityFeatures(x));
		return features;
	}
	
	public static List<String> perQuantityFeatures(RunX x) {
		List<String> features = new ArrayList<String>();
		for(String feature : perQuantityFeatures(x, x.quantIndex1)) {
			features.add("1_"+feature);
		}
		for(String feature : perQuantityFeatures(x, x.quantIndex2)) {
			features.add("2_"+feature);
		}
		return features;
	}
	

	public static List<String> pairQuantityFeatures(RunX x) {
		List<String> features = new ArrayList<String>();
		int tokenId1 = x.ta.getTokenIdFromCharacterOffset(x.quantities.get(x.quantIndex1).start);
		String unit1 = x.schema.quantSchemas.get(x.quantIndex1).unit;
		Constituent rate1 = x.schema.quantSchemas.get(x.quantIndex1).rateUnit;
		if(x.quantIndex2 == -1) {
			for(String str1 : unit1.split(" ")) {
				for(int i=0; i<x.schema.questionTokens.size(); ++i) {
					String str2 = x.schema.questionTokens.get(i);
					if(str1.equalsIgnoreCase(str2)) {
						for(int j=Math.max(i-2, 0); j<Math.min(i+2, x.schema.questionTokens.size()-1); ++j) {
							features.add("QuestionTokenUnit_"+x.schema.questionTokens.get(j));
						}
						break;
					}
				}
			}
			if(rate1 == null) {
				features.add("Rate1Null");
			} else {
				features.add("Rate1Found");
				for(int k=rate1.getStartSpan(); k<rate1.getEndSpan(); ++k) {
					String str1 = x.lemmas.get(k);
					for(int i=0; i<x.schema.questionTokens.size(); ++i) {
						String str2 = x.schema.questionTokens.get(i);
						if(str1.equalsIgnoreCase(str2)) {
							for(int j=Math.max(i-2, 0); j<Math.min(i+2, x.schema.questionTokens.size()-1); ++j) {
								features.add("QuestionTokenUnit_"+x.schema.questionTokens.get(j));
							}
							break;
						}
					}
				}
			}
		} else {	
			int tokenId2 = x.ta.getTokenIdFromCharacterOffset(x.quantities.get(x.quantIndex2).start);
			if(x.ta.getSentenceFromToken(tokenId1).getSentenceId() ==
					x.ta.getSentenceFromToken(tokenId2).getSentenceId()) {
				features.add("BothInSameSentence");
			}
			String unit2 = x.schema.quantSchemas.get(x.quantIndex2).unit;
			Constituent rate2 = x.schema.quantSchemas.get(x.quantIndex2).rateUnit;
			boolean found = false;
			for(String str1 : unit1.split(" ")) {
				for(String str2 : unit2.split(" ")) {
					if(str1.equalsIgnoreCase(str2)) {
						features.add("CommonTokenInUnit");
						found = true;
						break;
					}
				}
				if(found) break;
			}
			if(rate1 == null) features.add("Rate1Null");
			else {
				features.add("Rate1Found");
				found = false;
				for(int i=rate1.getStartSpan(); i<rate1.getEndSpan(); i++) {
					String str1 = x.lemmas.get(i);
					for(String str2 : unit2.split(" ")) {
						if(str1.equalsIgnoreCase(str2)) {
							features.add("Unit2InRate1");
							found = true;
							break;
						}
					}
					if(found) break;
				}
			}
			if(rate2 == null) features.add("Rate2Null");
			else {
				features.add("Rate2Found");
				found = false;
				for(int i=rate2.getStartSpan(); i<rate2.getEndSpan(); i++) {
					String str1 = x.lemmas.get(i);
					for(String str2 : unit1.split(" ")) {
						if(str1.equalsIgnoreCase(str2)) {
							features.add("Unit1InRate2");
							found = true;
							break;
						}
					}
					if(found) break;
				}
			}
		} 
		return features;
	}
	
	public static List<String> perQuantityFeatures(RunX x, int quantIndex) {
		List<String> features = new ArrayList<String>();
		if(quantIndex == -1) {
			for(int i=0; i<x.schema.questionTokens.size()-1; ++i) {
				features.add("QuestionToken_"+x.schema.questionTokens.get(i));
			}
		} else {
			features.addAll(FeatGen.getNeighborhoodFeatures(x.ta, x.posTags,
					x.ta.getTokenIdFromCharacterOffset(x.quantities.get(quantIndex).start), 2));
		}
		return features;
	}

}

