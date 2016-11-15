package relevance;

import java.io.Serializable;
import java.util.*;

import structure.QuantSpan;
import structure.QuantitySchema;
import utils.FeatGen;
import utils.Tools;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

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
		List<String> featuresWithPrefix = new ArrayList<String>();
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
		List<String> features = new ArrayList<String>();
		features.addAll(unitFeatures(x));
		features.addAll(miscFeatures(x));
		features.addAll(connectedNPFeatures(x));
		return features;
	}

	public static List<String> unitFeatures(RelX x) {
		List<String> features = new ArrayList<String>();
		QuantitySchema qSchema = x.schema.quantSchemas.get(x.quantIndex);
		int bestMatchQuestion = 0, bestMatchQuestionIndex = 0, numBestMatches = 0;
		bestMatchQuestionIndex = Tools.getNumTokenMatches(
				x.schema.questionTokens, 
				Arrays.asList(qSchema.unit.split(" ")));
		for(QuantitySchema qs : x.schema.quantSchemas) {
			int numMatches = Tools.getNumTokenMatches(
					x.schema.questionTokens,Arrays.asList(qs.unit.split(" ")));
			if(bestMatchQuestion < numMatches) {
				bestMatchQuestion = numMatches;
				numBestMatches = 1;
			} else if(bestMatchQuestion == numMatches) {
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
		for(int i=0; i<x.schema.quantSchemas.size(); ++i) {
			if(i!=x.quantIndex && qSchema.unit.trim().equals(
					x.schema.quantSchemas.get(i).unit.trim())) {
				features.add("ExactMatchUnit");
				break;
			}
		}
		for(int i=0; i<x.schema.quantSchemas.size(); ++i) {
			for(int j=i+1; j<x.schema.quantSchemas.size(); ++j) {
				if(i!=x.quantIndex && j!=x.quantIndex && 
						x.schema.quantSchemas.get(i).unit.trim().equals(
								x.schema.quantSchemas.get(j).unit.trim())) {
					features.add("OtherPairExactMatchUnit");
					break;
				}
			}
		}
		int bestMatch = 0;
		for(int i=0; i<x.schema.quantSchemas.size(); ++i) {
			for(int j=i+1; j<x.schema.quantSchemas.size(); ++j) {
				String unit1 = x.schema.quantSchemas.get(i).unit.trim();
				String unit2 = x.schema.quantSchemas.get(j).unit.trim();
				int match = Tools.getNumTokenMatches(Arrays.asList(unit1.split(" ")), 
						Arrays.asList(unit2.split(" ")));
				if(match > bestMatch) {
					bestMatch = match;
				}
			}
		}
		int bestMatchForIndex = 0;
		for(int i=0; i<x.schema.quantSchemas.size(); ++i) {
			if(i==x.quantIndex) continue;
			String unit1 = x.schema.quantSchemas.get(i).unit.trim();
			String unit2 = qSchema.unit.trim();
			int match = Tools.getNumTokenMatches(Arrays.asList(unit1.split(" ")), 
					Arrays.asList(unit2.split(" ")));
			if(match > bestMatchForIndex) {
				bestMatchForIndex = match;
			}
		}
		if(bestMatchForIndex == 0) {
			features.add("NoMatchWithOtherQuantUnits");
		}
		if(bestMatch == bestMatchForIndex) {
			features.add("BestMatchAmongQuantUnit");
		}
		return features;
	}
	
	public static List<String> connectedNPFeatures(RelX x) {
		List<String> features = new ArrayList<String>();
		QuantitySchema qSchema = x.schema.quantSchemas.get(x.quantIndex);
		int bestMatchQuestion = 0, bestMatchQuestionIndex = 0;
		List<String> allQuantTokens = new ArrayList<String>(
				Arrays.asList(qSchema.unit.split(" ")));
		for(Constituent c : qSchema.connectedNPs) {
			allQuantTokens.addAll(Tools.getTokensList(c));
		}
		bestMatchQuestionIndex = Tools.getNumTokenMatches(
				x.schema.questionTokens, allQuantTokens);
		for(QuantitySchema qs : x.schema.quantSchemas) {
			allQuantTokens = new ArrayList<String>(Arrays.asList(qs.unit.split(" ")));
			for(Constituent c : qs.connectedNPs) {
				allQuantTokens.addAll(Tools.getTokensList(c));
			}
			bestMatchQuestion = Tools.getNumTokenMatches(x.schema.questionTokens, allQuantTokens);
		}
		if(bestMatchQuestionIndex == 0) {
			features.add("UnitNPFoundInQuestion");
		}
		if(bestMatchQuestion == bestMatchQuestionIndex) {
			features.add("BestQuantUnitNPMatchInQuestion");
		}
		int bestMatch = 0;
		for(int i=0; i<x.schema.quantSchemas.size(); ++i) {
			for(int j=i+1; j<x.schema.quantSchemas.size(); ++j) {
				QuantitySchema qs1 = x.schema.quantSchemas.get(i);
				QuantitySchema qs2 = x.schema.quantSchemas.get(j);
				List<String> allQuantTokens1 = new ArrayList<String>(
						Arrays.asList(qs1.unit.split(" ")));
				for(Constituent c : qs1.connectedNPs) {
					allQuantTokens1.addAll(Tools.getTokensList(c));
				}
				List<String> allQuantTokens2 = new ArrayList<String>(
						Arrays.asList(qs2.unit.split(" ")));
				for(Constituent c : qs2.connectedNPs) {
					allQuantTokens2.addAll(Tools.getTokensList(c));
				}
				int match = Tools.getNumTokenMatches(allQuantTokens1, allQuantTokens2);
				if(match > bestMatch) {
					bestMatch = match;
				}
			}
		}
		int bestMatchForIndex = 0;
		for(int i=0; i<x.schema.quantSchemas.size(); ++i) {
			if(i==x.quantIndex) continue;
			QuantitySchema qs1 = x.schema.quantSchemas.get(i);
			QuantitySchema qs2 = qSchema;
			List<String> allQuantTokens1 = new ArrayList<String>(
					Arrays.asList(qs1.unit.split(" ")));
			for(Constituent c : qs1.connectedNPs) {
				allQuantTokens1.addAll(Tools.getTokensList(c));
			}
			List<String> allQuantTokens2 = new ArrayList<String>(
					Arrays.asList(qs2.unit.split(" ")));
			for(Constituent c : qs2.connectedNPs) {
				allQuantTokens2.addAll(Tools.getTokensList(c));
			}
			int match = Tools.getNumTokenMatches(allQuantTokens1, allQuantTokens2);
			if(match > bestMatchForIndex) {
				bestMatchForIndex = match;
			}
		}
		if(bestMatchForIndex == 0) {
			features.add("NoMatchNPWithOtherQuantUnits");
		}
		if(bestMatch == bestMatchForIndex) {
			features.add("BestMatchNPAmongQuantUnit");
		}
		return features;
	}
	
	public static List<String> miscFeatures(RelX x) {
		QuantSpan qs = x.quantities.get(x.quantIndex);
		List<String> features = new ArrayList<String>();
		if(x.quantities.size() == 2) {
			features.add("Two_quantities");
		}
		if(Tools.safeEquals(qs.val, 1.0) || Tools.safeEquals(qs.val, 2.0)) {
			features.add("One_or_Two");
		}
		features.addAll(FeatGen.getNeighborhoodFeatures(x.ta, x.posTags,
				x.ta.getTokenIdFromCharacterOffset(qs.start), 2));
		return features;
	}		
}

