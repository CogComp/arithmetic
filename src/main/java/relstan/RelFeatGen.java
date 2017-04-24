package relstan;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import structure.QuantSpan;
import structure.StanfordSchema;
import utils.FeatGen;
import utils.Tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RelFeatGen extends AbstractFeatureGenerator implements Serializable{

	private static final long serialVersionUID = -5902462551801564955L;
	public Lexiconer lm = null;
	
	public RelFeatGen(Lexiconer lm) {
		this.lm = lm;
	}

	@Override
	public IFeatureVector getFeatureVector(IInstance prob, IStructure struct) {
		RelX x = (RelX) prob;
		RelY y = (RelY) struct;
		List<String> featuresWithPrefix = new ArrayList<>();
		String prefix = y.relevance;
		featuresWithPrefix.add(prefix);
		List<String> features = getFeatures(x);
		features.addAll(FeatGen.getConjunctions(features));
		for(String feature : features) {
			featuresWithPrefix.add(prefix + "_" + feature);
		}
		return FeatGen.getFeatureVectorFromListString(featuresWithPrefix, lm);
	}

	public static List<String> getFeatures(RelX x) {
		List<String> features = new ArrayList<>();
		if(x.quantities.size() == 2) {
			features.add("TwoQuantities");
			return features;
		}
		features.addAll(unitFeatures(x));
		features.addAll(miscFeatures(x));
		return features;
	}

	public static List<String> unitFeatures(RelX x) {
		List<String> features = new ArrayList<>();
		StanfordSchema schema = x.schema.get(x.quantIndex);
		double bestMatchQuestion = 0, bestMatchQuestionIndex, numBestMatches = 0;
		List<String> questionTokens = Tools.spanToLemmaList(
				x.tokens.get(x.questionSchema.sentId),
				x.questionSpan);
		List<String> unit = Tools.spanToLemmaList(x.tokens.get(schema.sentId), schema.unit);
		if(questionTokens.contains("cost") && (unit.contains("dollar") ||
				unit.contains("$") || unit.contains("cent") || unit.contains("nickel") ||
				unit.contains("dime") || unit.contains("buck"))) {
			features.add("CostStuff");
		}
		bestMatchQuestionIndex = Tools.jaccardSim(
				Tools.spanToLemmaList(x.tokens.get(x.questionSchema.sentId), x.questionSchema.unit),
				Tools.spanToLemmaList(x.tokens.get(schema.sentId), schema.unit));
		for(StanfordSchema qs : x.schema) {
			double  numMatches = Tools.jaccardSim(
					Tools.spanToLemmaList(x.tokens.get(x.questionSchema.sentId), x.questionSchema.unit),
					Tools.spanToLemmaList(x.tokens.get(qs.sentId), qs.unit));
			if(bestMatchQuestion < numMatches) {
				bestMatchQuestion = numMatches;
				numBestMatches = 1;
			} else if(Tools.safeEquals(bestMatchQuestion, numMatches)) {
				numBestMatches++;
			}
		}
		if(bestMatchQuestionIndex == 0) {
			features.add("UnitFoundInQuestion");
		}
		if(bestMatchQuestion == bestMatchQuestionIndex) {
			features.add("BestQuantUnitMatchInQuestion");
		}
		if(numBestMatches > 1) {
			features.add("MultipleQuantUnitBestMatchInQuestion");
		}
		for(int i=0; i<x.schema.size(); ++i) {
			if(i!=x.quantIndex) {
				StanfordSchema qs = x.schema.get(i);
				List<String> p2 = Tools.spanToLemmaList(x.tokens.get(qs.sentId), qs.unit);
				if(unit.size() == p2.size() && (Arrays.asList(unit)+"").equals(Arrays.asList(p2)+"")) {
					features.add("ExactMatchUnit");
					break;
				}
			}
		}
		for(int i=0; i<x.schema.size(); ++i) {
			StanfordSchema qs1 = x.schema.get(i);
			List<String> p1 = Tools.spanToLemmaList(x.tokens.get(qs1.sentId), qs1.unit);
			for(int j=i+1; j<x.schema.size(); ++j) {
				StanfordSchema qs2 = x.schema.get(j);
				List<String> p2 = Tools.spanToLemmaList(x.tokens.get(qs2.sentId), qs2.unit);
				if(i!=x.quantIndex && j!=x.quantIndex &&
						(Arrays.asList(p1)+"").equals(Arrays.asList(p2)+"")) {
					features.add("OtherPairExactMatchUnit");
					break;
				}
			}
		}
		double bestMatch = 0;
		for(int i=0; i<x.schema.size(); ++i) {
			StanfordSchema qs1 = x.schema.get(i);
			List<String> unit1 = Tools.spanToLemmaList(x.tokens.get(qs1.sentId), qs1.unit);
			for(int j=i+1; j<x.schema.size(); ++j) {
				StanfordSchema qs2 = x.schema.get(j);
				List<String> unit2 = Tools.spanToLemmaList(x.tokens.get(qs2.sentId), qs2.unit);
				double match = Tools.getNumTokenMatches(unit1, unit2);
				if(match > bestMatch) {
					bestMatch = match;
				}
			}
		}
		double bestMatchForIndex = 0;
		for(int i=0; i<x.schema.size(); ++i) {
			if(i==x.quantIndex) continue;
			StanfordSchema qs1 = x.schema.get(i);
			List<String> unit1 = Tools.spanToLemmaList(x.tokens.get(qs1.sentId), qs1.unit);
			List<String> unit2 = Tools.spanToLemmaList(x.tokens.get(schema.sentId), schema.unit);
			double match = Tools.jaccardSim(unit1, unit2);
			if(match > bestMatchForIndex) {
				bestMatchForIndex = match;
			}
		}
		if(bestMatchForIndex < 0.1) {
			features.add("NoMatchWithOtherQuantUnits");
		}
		if(Tools.safeEquals(bestMatch, bestMatchForIndex)) {
			features.add("BestMatchAmongQuantUnit");
		}
		if(schema.sentId == x.questionSchema.sentId) {
			List<String> unit2 = Tools.spanToLemmaList(x.tokens.get(schema.sentId), schema.unit);
			for(int i=0; i<x.quantIndex; ++i) {
				StanfordSchema s = x.schema.get(i);
				List<String> unit1 = Tools.spanToLemmaList(x.tokens.get(s.sentId), s.unit);
				if(Tools.safeEquals(s.qs.val, schema.qs.val) &&
						Tools.jaccardSim(unit1, unit2) > 0.2) {
					features.add("SeemsLikeARepeatedEntry");
				}
			}
		}
		return features;
	}

	public static List<String> miscFeatures(RelX x) {
		QuantSpan qs = x.quantities.get(x.quantIndex);
		List<String> features = new ArrayList<>();
		if(Tools.safeEquals(qs.val, 1.0)) {
			features.add("One");
		}
		if(Tools.safeEquals(qs.val, 2.0)) {
			features.add("Two");
		}
		features.addAll(joint.LogicFeatGen.getNeighborhoodFeatures(
				x, x.schema.get(x.quantIndex)));
		return features;
	}		
}

