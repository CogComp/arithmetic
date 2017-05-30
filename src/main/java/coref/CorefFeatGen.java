package coref;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import utils.FeatGen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CorefFeatGen extends AbstractFeatureGenerator implements Serializable {

	private static final long serialVersionUID = -5902462551801564955L;
	public Lexiconer lm = null;
	
	public CorefFeatGen(Lexiconer lm) {
		this.lm = lm;
	}

	@Override
	public IFeatureVector getFeatureVector(IInstance prob, IStructure struct) {
		CorefX x = (CorefX) prob;
		CorefY y = (CorefY) struct;
		List<String> features = getFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public static List<String> getFeatures(CorefX x, CorefY y) {
		List<String> features = new ArrayList<>();
		try {
			features.addAll(logic.LogicFeatGen.getKeyFeatures(
					x,
					x.schema.get(x.quantIndex1),
					x.schema.get(x.quantIndex2),
					x.questionSchema,
					x.infType,
					y.key));
		} catch (Exception e) {
//			System.out.println("Exception in "+x.text);
		}
		features.addAll(FeatGen.getConjunctions(features));
		return features;
	}



}

