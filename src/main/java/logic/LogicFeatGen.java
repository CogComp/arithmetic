package logic;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
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
		features.addAll(getFeatures(x, y.num1));
		features.addAll(getFeatures(x, y.num2));
		features.addAll(getFeatures(x, y.ques));
		return features;
	}

	public static List<String> getFeatures(LogicX x, LogicInput y) {
		List<String> features = new ArrayList<>();
		features.addAll(getSubjectFeatures(x, y.subjSpan));
		features.addAll(getObjectFeatures(x, y.objSpan));
		features.addAll(getUnitFeatures(x, y.unitSpan));
		features.addAll(getRateFeatures(x, y.rateSpan));
		features.addAll(getVerbFeatures(x, y.verbIndex));
		return features;
	}

	public static List<String> getSubjectFeatures(LogicX x, IntPair y) {
		List<String> features = new ArrayList<>();
		return features;
	}

	public static List<String> getObjectFeatures(LogicX x, IntPair y) {
		List<String> features = new ArrayList<>();
		return features;
	}

	public static List<String> getUnitFeatures(LogicX x, IntPair y) {
		List<String> features = new ArrayList<>();
		return features;
	}

	public static List<String> getRateFeatures(LogicX x, IntPair y) {
		List<String> features = new ArrayList<>();
		return features;
	}

	public static List<String> getVerbFeatures(LogicX x, int y) {
		List<String> features = new ArrayList<>();
		return features;
	}

	public static List<String> getLogicFeatures(LogicX x, IntPair y) {
		List<String> features = new ArrayList<>();
		return features;
	}

	public IFeatureVector getSubjectFeatureVector(LogicX x, IntPair y) {
		List<String> features = getSubjectFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getObjectFeatureVector(LogicX x, IntPair y) {
		List<String> features = getObjectFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getUnitFeatureVector(LogicX x, IntPair y) {
		List<String> features = getUnitFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getRateFeatureVector(LogicX x, IntPair y) {
		List<String> features = getRateFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getVerbFeatureVector(LogicX x, int y) {
		List<String> features = getVerbFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getLogicFeatureVector(LogicX x, IntPair y) {
		List<String> features = getLogicFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

}

