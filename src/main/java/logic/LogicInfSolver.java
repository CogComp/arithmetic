package logic;

import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import joint.Logic;
import utils.Tools;

import java.io.Serializable;

public class LogicInfSolver extends AbstractInferenceSolver implements Serializable {

	private static final long serialVersionUID = 5253748728743334706L;
	private LogicFeatGen featGen;

	public LogicInfSolver(LogicFeatGen featGen) throws Exception {
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
		LogicX x = (LogicX) ins;
		return getBestStructure(x, null, weight, false);
	}

	@Override
	public float getLoss(IInstance ins, IStructure gold, IStructure pred) {
		return LogicY.getLoss((LogicY)gold, (LogicY)pred);
	}

	public LogicY getBestStructure(LogicX x,
								   LogicY gold,
								   WeightVector weight,
								   boolean labelCompletion) {
		double bestScore = -Double.MAX_VALUE;
		LogicY best = null;
		String label;
		for(String key : Logic.getRelevantKeys(x.infType, x.isTopmost, x.mathOp)) {
//			System.out.println(x.tokens+"\n"+x.infType+" "+x.isTopmost+" "+x.mathOp);
			label = null;
			if(x.infType == 0) {
				label = Logic.verb(
						x.tokens.get(x.num1.sentId).get(x.num1.verb).lemma(),
						x.tokens.get(x.num2.sentId).get(x.num2.verb).lemma(),
						Tools.spanToLemmaList(x.tokens.get(x.num1.sentId), x.num1.unit),
						Tools.spanToLemmaList(x.tokens.get(x.num2.sentId), x.num2.unit),
						key);
			}
			if(x.infType == 1) {
				label = Logic.partition(key);
			}
			if(x.infType == 2) {
				label = Logic.math(x.mathOp, key);
			}
			if(x.infType == 3) {
				label = Logic.unitDependency(key);
			}
			if(label == null) continue;
//			System.out.println(label + " :: " + gold);
			if(labelCompletion && !label.equals(gold.label)) continue;
			LogicY y = new LogicY(label, key);
			double score = weight.dotProduct(featGen.getFeatureVector(x, y));
			if (bestScore < score) {
				best = y;
				bestScore = score;
			}
		}
		return best;
	}

}