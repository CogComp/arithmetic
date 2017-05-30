package coref;

import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import logic.Logic;

import java.io.Serializable;

public class CorefInfSolver extends AbstractInferenceSolver implements Serializable {

	private static final long serialVersionUID = 5253748728743334706L;
	private CorefFeatGen featGen;

	public CorefInfSolver(CorefFeatGen featGen) throws Exception {
		this.featGen = featGen;
	}

	@Override
	public IStructure getBestStructure(WeightVector weight, IInstance ins)
			throws Exception {
		return getLossAugmentedBestStructure(weight, ins, null);
	}

	@Override
	public IStructure getLossAugmentedBestStructure(WeightVector weight,
			IInstance ins, IStructure goldStructure) throws Exception {
		CorefX x = (CorefX) ins;
		return getBestStructure(x, null, weight, false);
	}

	@Override
	public float getLoss(IInstance ins, IStructure gold, IStructure pred) {
		return CorefY.getLoss((CorefY)gold, (CorefY)pred);
	}

	public CorefY getBestStructure(CorefX x,
								   CorefY gold,
								   WeightVector weight,
								   boolean labelCompletion) {
		double bestScore = -Double.MAX_VALUE;
		CorefY best = null;
		String label;
		for(String key : Logic.getRelevantKeys(x.infType)) {
			label = null;
			if(x.infType.startsWith("Verb")) {
				label = Logic.verb(x.tokens, x.schema.get(x.quantIndex1),
						x.schema.get(x.quantIndex2), key);
			}
			if(x.infType.startsWith("Partition")) {
				label = Logic.partition(key);
			}
			if(x.infType.startsWith("Math")) {
				label = Logic.math(x.infType, key);
			}
			if(x.infType.startsWith("Rate")) {
				label = Logic.unitDependency(x.infType, key);
			}
			if(label == null) continue;
			if(labelCompletion) {
				if(!label.equals(gold.label)) {
					continue;
				}
			}
			CorefY y = new CorefY(label, key);
//			List<String> feats = CorefFeatGen.getFeatures(x, y);
//			if(feats.contains("BestOption")) return y;
			double score = weight.dotProduct(featGen.getFeatureVector(x, y));
			if (bestScore < score) {
				best = y;
				bestScore = score;
			}
		}
		return best;
	}

}