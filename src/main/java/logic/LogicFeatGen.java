package logic;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.stanford.nlp.ling.CoreLabel;
import joint.LogicInput;
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
		features.addAll(getFeatures(x, y.num1));
		features.addAll(getFeatures(x, y.num2));
		features.addAll(getFeatures(x, y.ques));
		features.addAll(getLogicFeatures(x, y));
		return features;
	}

	public static List<String> getFeatures(LogicX x, LogicInput y) {
		List<String> features = new ArrayList<>();
		features.addAll(getSubjectFeatures(x, y.subjSpan, y.mode));
		features.addAll(getObjectFeatures(x, y.objSpan, y.mode));
		features.addAll(getUnitFeatures(x, y.unitSpan, y.mode));
		features.addAll(getRateFeatures(x, y.rateSpan, y.mode));
		features.addAll(getVerbFeatures(x, y.verbIndex, y.mode));
		return features;
	}

	public static List<String> getSubjectFeatures(LogicX x, IntPair y, int mode) {
		List<String> features = new ArrayList<>();
		int tokenId = x.tokenIds.get(mode);
		int sentId = x.sentIds.get(mode);
		List<CoreLabel> tokens = x.tokens.get(sentId);
		StanfordSchema schema = x.relevantSchemas.get(mode);
//		if(tokenId >= 0 && y != null) {
//			if(tokenId > y.getFirst()) {
//				features.add("Number_Right");
//				for(int i=y.getSecond()+1; i<tokenId; ++i) {
//					features.add("Number_Right_"+tokens.get(i).word());
//					features.add("Number_Right_"+tokens.get(i).tag());
//				}
//			} else {
//				features.add("Number_Left");
//				for(int i=tokenId+1; i<y.getFirst(); ++i) {
//					features.add("Number_Left_"+tokens.get(i).word());
//					features.add("Number_Left_"+tokens.get(i).tag());
//				}
//			}
//		}
//		if (y == null) {
//			features.add("y is null");
//		}
		return FeatGen.getFeaturesConjWithLabels(features, "Subject");
	}

	public static List<String> getObjectFeatures(LogicX x, IntPair y, int mode) {
		List<String> features = new ArrayList<>();
		int tokenId = x.tokenIds.get(mode);
		int sentId = x.sentIds.get(mode);
		List<CoreLabel> tokens = x.tokens.get(sentId);
		StanfordSchema schema = x.relevantSchemas.get(mode);
//		if(tokenId >= 0 && y != null) {
//			if(tokenId > y.getFirst()) {
//				features.add("Number_Right");
//				for(int i=y.getSecond()+1; i<tokenId; ++i) {
//					features.add("Number_Right_"+tokens.get(i).word());
//					features.add("Number_Right_"+tokens.get(i).tag());
//				}
//			} else {
//				features.add("Number_Left");
//				for(int i=tokenId+1; i<y.getFirst(); ++i) {
//					features.add("Number_Left_"+tokens.get(i).word());
//					features.add("Number_Left_"+tokens.get(i).tag());
//				}
//			}
//		}
//		if (y == null) {
//			features.add("y is null");
//		}
		return FeatGen.getFeaturesConjWithLabels(features, "Object");
	}

	public static List<String> getUnitFeatures(LogicX x, IntPair y, int mode) {
		List<String> features = new ArrayList<>();
		int tokenId = x.tokenIds.get(mode);
		int sentId = x.sentIds.get(mode);
		List<CoreLabel> tokens = x.tokens.get(sentId);
		StanfordSchema schema = x.relevantSchemas.get(mode);
//		if(tokenId >= 0 && y != null) {
//			if(tokenId > y.getFirst()) {
//				features.add("Number_Right");
//				for(int i=y.getSecond()+1; i<tokenId; ++i) {
//					features.add("Number_Right_"+tokens.get(i).word());
//					features.add("Number_Right_"+tokens.get(i).tag());
//				}
//			} else {
//				features.add("Number_Left");
//				for(int i=tokenId+1; i<y.getFirst(); ++i) {
//					features.add("Number_Left_"+tokens.get(i).word());
//					features.add("Number_Left_"+tokens.get(i).tag());
//				}
//			}
//		}
//		if (y == null) {
//			features.add("y is null");
//		}
		return FeatGen.getFeaturesConjWithLabels(features, "Unit");
	}

	public static List<String> getRateFeatures(LogicX x, IntPair y, int mode) {
		List<String> features = new ArrayList<>();
		int tokenId = x.tokenIds.get(mode);
		int sentId = x.sentIds.get(mode);
		List<CoreLabel> tokens = x.tokens.get(sentId);
		StanfordSchema schema = x.relevantSchemas.get(mode);
//		if(tokenId >= 0 && y != null) {
//			if(tokenId > y.getFirst()) {
//				features.add("Number_Right");
//				for(int i=y.getSecond()+1; i<tokenId; ++i) {
//					features.add("Number_Right_"+tokens.get(i).word());
//					features.add("Number_Right_"+tokens.get(i).tag());
//				}
//			} else {
//				features.add("Number_Left");
//				for(int i=tokenId+1; i<y.getFirst(); ++i) {
//					features.add("Number_Left_"+tokens.get(i).word());
//					features.add("Number_Left_"+tokens.get(i).tag());
//				}
//			}
//		}
//		if (y == null) {
//			features.add("y is null");
//		}
		return FeatGen.getFeaturesConjWithLabels(features, "Rate");
	}

	public static List<String> getVerbFeatures(LogicX x, int y, int mode) {
		List<String> features = new ArrayList<>();
		int tokenId = x.tokenIds.get(mode);
		int sentId = x.sentIds.get(mode);
		List<CoreLabel> tokens = x.tokens.get(sentId);
		StanfordSchema schema = x.relevantSchemas.get(mode);
		return features;
	}

	public static List<String> getLogicFeatures(LogicX x, LogicY y) {
		List<String> features = new ArrayList<>();
		features.add("InferenceType_"+y.inferenceRule);
		StanfordSchema schema = x.relevantSchemas.get(0);
		if(y.num1.verbLemma.equals(y.num2.verbLemma)) {
			features.add("Num1Num2VerbSame");
		} else {
			features.add("Num1Num2VerbDiff");
		}
		if(y.num1.verbLemma.equals(y.ques.verbLemma)) {
			features.add("Num1QuesVerbSame");
		} else {
			features.add("Num1QuesVerbDiff");
		}
		if(y.num2.verbLemma.equals(y.ques.verbLemma)) {
			features.add("Num2QuesVerbSame");
		} else {
			features.add("Num2QuesVerbDiff");
		}
		if(y.num1.rate != null && y.num1.rate.size() > 0) {
			features.add("Rate1");
		}
		if(y.num2.rate != null && y.num2.rate.size() > 0) {
			features.add("Rate2");
		}
		if(y.ques.rate != null && y.ques.rate.size() > 0) {
			features.add("RateQuestion");
		}
		if(y.num1.math != null) {
			features.add("Math1");
		}
		if(y.num2.math != null) {
			features.add("Math2");
		}
		if(y.ques.math != null) {
			features.add("MathQuestion");
		}
		return FeatGen.getFeaturesConjWithLabels(features, ""+y.inferenceRule);
	}

	public IFeatureVector getSubjectFeatureVector(LogicX x, IntPair y, int mode) {
		List<String> features = getSubjectFeatures(x, y, mode);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getObjectFeatureVector(LogicX x, IntPair y, int mode) {
		List<String> features = getObjectFeatures(x, y, mode);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getUnitFeatureVector(LogicX x, IntPair y, int mode) {
		List<String> features = getUnitFeatures(x, y, mode);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getRateFeatureVector(LogicX x, IntPair y, int mode) {
		List<String> features = getRateFeatures(x, y, mode);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getVerbFeatureVector(LogicX x, int y, int mode) {
		List<String> features = getVerbFeatures(x, y, mode);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public IFeatureVector getLogicFeatureVector(LogicX x, LogicY y) {
		List<String> features = getLogicFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

}

