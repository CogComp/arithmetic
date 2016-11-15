package rate;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;

public class RateInfSolver extends AbstractInferenceSolver implements
Serializable {

	private static final long serialVersionUID = 5253748728743334706L;
	private AbstractFeatureGenerator featGen;
	
	public RateInfSolver(AbstractFeatureGenerator featGen) throws Exception {
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
		List<String> labels = Arrays.asList("RATE", "NOT_RATE");
		double bestScore = -Double.MAX_VALUE;
		RateY best = null;
		for(String label : labels) {
			double score = weight.dotProduct(featGen.getFeatureVector(ins, new RateY(label)));
			if(bestScore < score) {
				best = new RateY(label);
				bestScore = score;
			}
		}
		return best;
	}

	@Override
	public float getLoss(IInstance ins, IStructure gold, IStructure pred) {
		return RateY.getLoss((RateY)gold, (RateY)pred);
	}	
}