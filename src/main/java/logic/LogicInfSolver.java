package logic;

import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import joint.Logic;
import joint.Verbs;
import structure.StanfordSchema;
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
			LogicY y = new LogicY(label, key);
			double score = weight.dotProduct(featGen.getFeatureVector(x, y));
			if (bestScore < score) {
				best = y;
				bestScore = score;
			}
		}
		if(best == null) {
			System.out.println("==================================");
			System.out.println(x.tokens);
			System.out.println(x.quantIndex1+" "+x.quantIndex2);
			if(gold != null) System.out.println("Gold:"+gold.label);
			System.out.println();
			for(StanfordSchema schema : x.schema) {
				System.out.println(schema);
				System.out.println("VerbCat:"+ Verbs.verbClassify(
						x.tokens.get(schema.sentId).get(schema.verb).lemma(),
						Tools.spanToLemmaList(x.tokens.get(schema.sentId), schema.unit)));
			}
			System.out.println(x.questionSchema);
			System.out.println();
			System.out.println("Quantities : "+x.quantities);
			System.out.println("==================================");

		}
		return best;
	}

}