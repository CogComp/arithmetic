package run;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;

public class RunInfSolver extends AbstractInferenceSolver implements
Serializable {

	private static final long serialVersionUID = 5253748728743334706L;
	private AbstractFeatureGenerator featGen;
	
	public RunInfSolver(AbstractFeatureGenerator featGen) throws Exception {
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
		List<String> labels = Arrays.asList("SAME_UNIT", "1_RATE_1", "1_RATE_2", 
				"2_RATE_1", "2_RATE_2", "NO_REL");
		double bestScore = -Double.MAX_VALUE;
		RunY best = null;
		for(String label : labels) {
			double score = weight.dotProduct(featGen.getFeatureVector(ins, new RunY(label)));
			if(bestScore < score) {
				best = new RunY(label);
				bestScore = score;
			}
		}
		return best;
	}

	@Override
	public float getLoss(IInstance ins, IStructure gold, IStructure pred) {
		return RunY.getLoss((RunY)gold, (RunY)pred);
	}	
}