package logic;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;

import java.io.Serializable;

public class LogicInfSolver extends AbstractInferenceSolver implements Serializable {

	private static final long serialVersionUID = 5253748728743334706L;
	private AbstractFeatureGenerator featGen;
	
	public LogicInfSolver(AbstractFeatureGenerator featGen) throws Exception {
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
		double bestScore = -Double.MAX_VALUE;
		LogicY best = null;
		for(String label : Logic.labels) {
			for(int infRule=0; infRule<4; infRule++) {
				double score = weight.dotProduct(featGen.getFeatureVector(
						ins, new LogicY(label, infRule)));
				if (bestScore < score) {
					best = new LogicY(label, infRule);
					bestScore = score;
				}
			}
		}
		return best;
	}

	@Override
	public float getLoss(IInstance ins, IStructure gold, IStructure pred) {
		return LogicY.getLoss((LogicY)gold, (LogicY)pred);
	}

	public LogicY getLatentBestStructure(LogicX ins, LogicY gold, WeightVector weight) {
		double bestScore = -Double.MAX_VALUE;
		LogicY best = null;
		for(int infRule=0; infRule<4; infRule++) {
			double score = weight.dotProduct(featGen.getFeatureVector(
					ins, new LogicY(gold.label, infRule)));
			if (bestScore < score) {
				best = new LogicY(gold.label, infRule);
				bestScore = score;
			}
		}
		return best;
	}
}