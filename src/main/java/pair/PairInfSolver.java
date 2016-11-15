package pair;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;

public class PairInfSolver extends AbstractInferenceSolver implements
Serializable {

	private static final long serialVersionUID = 5253748728743334706L;
	private AbstractFeatureGenerator featGen;
	
	public PairInfSolver(AbstractFeatureGenerator featGen) throws Exception {
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
		List<String> labels = Arrays.asList("ADD", "SUB", "SUB_REV", "MUL", "DIV", "DIV_REV");
		double bestScore = -Double.MAX_VALUE;
		PairY best = null;
		for(String label : labels) {
			double score = weight.dotProduct(featGen.getFeatureVector(ins, new PairY(label)));
			if(bestScore < score) {
				best = new PairY(label);
				bestScore = score;
			}
		}
		return best;
	}

	@Override
	public float getLoss(IInstance ins, IStructure gold, IStructure pred) {
		return PairY.getLoss((PairY)gold, (PairY)pred);
	}	
}