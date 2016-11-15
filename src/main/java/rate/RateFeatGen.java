package rate;

import java.io.Serializable;
import java.util.*;

import utils.FeatGen;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class RateFeatGen extends AbstractFeatureGenerator 
implements Serializable{

	private static final long serialVersionUID = -5902462551801564955L;
	public Lexiconer lm = null;
	
	public RateFeatGen(Lexiconer lm) {
		this.lm = lm;
	}

	@Override
	public IFeatureVector getFeatureVector(IInstance prob, IStructure struct) {
		RateX x = (RateX) prob;
		RateY y = (RateY) struct;
		return FeatGen.getFeatureVectorFromListString(getFeatures(x, y.label), lm);
	}

	public static List<String> getFeatures(RateX x, String label) {
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
	
	public static List<String> getFeatures(RateX x) {
		List<String> features = new ArrayList<String>();
		features.addAll(perQuantityFeatures(x));
		return features;
	}
	
	public static List<String> perQuantityFeatures(RateX x) {
		List<String> features = new ArrayList<String>();
		for(String feature : perQuantityFeatures(x, x.quantIndex)) {
			features.add("1_"+feature);
		}
		return features;
	}
		
	public static List<String> perQuantityFeatures(RateX x, int quantIndex) {
		List<String> features = new ArrayList<String>();
		if(quantIndex == -1) {
			for(int i=0; i<x.schema.questionTokens.size()-1; ++i) {
				features.add("QuestionToken_"+x.schema.questionTokens.get(i));
			}
		} else {
			features.addAll(FeatGen.getNeighborhoodFeatures(x.ta, x.posTags,
					x.ta.getTokenIdFromCharacterOffset(x.quantities.get(quantIndex).start), 2));
			Constituent rate = x.schema.quantSchemas.get(x.quantIndex).rateUnit;
			if(rate == null) {
				features.add("Rate:Null");
			} else {
				features.add("Rate:NotNull");
			}
		}
		return features;
	}

}

