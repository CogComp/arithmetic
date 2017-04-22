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
import java.util.ArrayList;
import java.util.List;

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
//		LogicY ruleBased = ruleBasedKey(x);
//		if(ruleBased != null) return ruleBased;
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

	public static LogicY ruleBasedKey(LogicX x) {
		List<String> possibleKeys = new ArrayList<>();
		if(x.infType.startsWith("Verb") || x.infType.startsWith("Math")) {
			for(String key : Logic.getRelevantKeys(x.infType)) {
				List<String> phrase1 = new ArrayList<>();
				List<String> phrase2 = new ArrayList<>();
				if(key.startsWith("0")) {
					phrase1 = joint.LogicFeatGen.getPhraseByMode(
							x.tokens, x.schema.get(x.quantIndex1), "SUBJ");
				} else if(key.startsWith("1")) {
					phrase1 = joint.LogicFeatGen.getPhraseByMode(
							x.tokens, x.schema.get(x.quantIndex1), "OBJ");
				}
//				else if(key.equals("QUES")) {
//					phrase1 = joint.LogicFeatGen.getPhraseByMode(
//							x.tokens, x.schema.get(x.quantIndex1), "SUBJ");
//				} else {
//					phrase1 = joint.LogicFeatGen.getPhraseByMode(
//							x.tokens, x.schema.get(x.quantIndex2), "SUBJ");
//				}
				if(key.endsWith("0")) {
					phrase2 = joint.LogicFeatGen.getPhraseByMode(
							x.tokens, x.schema.get(x.quantIndex2), "SUBJ");
				} else if(key.endsWith("1")) {
					phrase2 = joint.LogicFeatGen.getPhraseByMode(
							x.tokens, x.schema.get(x.quantIndex2), "OBJ");
				}
//				else {
//					phrase2 = joint.LogicFeatGen.getPhraseByMode(
//							x.tokens, x.questionSchema, "SUBJ");
//				}
				if(Tools.jaccardSim(phrase1, phrase2) > 0.2 ||
						phrase2.contains("he") || phrase2.contains("she")) {
					possibleKeys.add(key);
				}
			}
		}
		if(x.infType.startsWith("Rate")) {
			for(String key : Logic.getRelevantKeys(x.infType)) {
				List<String> phrase1 = new ArrayList<>();
				List<String> phrase2 = new ArrayList<>();
				if(key.startsWith("0")) {
					phrase1 = joint.LogicFeatGen.getPhraseByMode(
							x.tokens, x.schema.get(x.quantIndex1), "UNIT");
				} else if(key.startsWith("1")) {
					phrase1 = joint.LogicFeatGen.getPhraseByMode(
							x.tokens, x.schema.get(x.quantIndex1), "RATE");
				}
//				else if(key.equals("QUES")) {
//					phrase1 = joint.LogicFeatGen.getPhraseByMode(
//							x.tokens, x.schema.get(x.quantIndex1), "UNIT");
//				}
//				else {
//					phrase1 = joint.LogicFeatGen.getPhraseByMode(
//							x.tokens, x.schema.get(x.quantIndex2), "UNIT");
//				}
				if(key.endsWith("0")) {
					phrase2 = joint.LogicFeatGen.getPhraseByMode(
							x.tokens, x.schema.get(x.quantIndex2), "UNIT");
				} else if(key.endsWith("1")) {
					phrase2 = joint.LogicFeatGen.getPhraseByMode(
							x.tokens, x.schema.get(x.quantIndex2), "RATE");
				}
//				else {
//					phrase2 = joint.LogicFeatGen.getPhraseByMode(
//							x.tokens, x.questionSchema, "UNIT");
//				}
				if(Tools.jaccardSim(phrase1, phrase2) > 0.2 ||
						phrase2.contains("he") || phrase2.contains("she")) {
					possibleKeys.add(key);
				}
			}
		}
		if(possibleKeys.size() == 1) {
			String key = possibleKeys.get(0);
			String label = null;
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
			if(label != null) return new LogicY(label, key);
		}
		return null;
	}

}