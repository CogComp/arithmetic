package logic;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.Triple;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		LogicX logicX = (LogicX) ins;
		for(Triple<LogicInput, LogicInput, LogicInput> logicInput : enumerateLogicInputs(logicX)) {
			Map<Pair<String, Integer>, Double> logicOutput = Logic.logicSolver(
					logicInput.getFirst(), logicInput.getSecond(), logicInput.getThird());
			for (String label : Logic.labels) {
				for (int infRule = 0; infRule < Logic.maxNumInferenceTypes; infRule++) {
					LogicY logicY = new LogicY(label, infRule, logicInput.getFirst(),
							logicInput.getSecond(), logicInput.getThird());
					double score = weight.dotProduct(featGen.getFeatureVector(ins, logicY)) +
							logicOutput.getOrDefault(new Pair<>(label, infRule), 0.0);
					if (bestScore < score) {
						best = logicY;
						bestScore = score;
					}
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
		for(Triple<LogicInput, LogicInput, LogicInput> logicInput : enumerateLogicInputs(ins)){
			for (int infRule = 0; infRule < Logic.maxNumInferenceTypes; infRule++) {
				LogicY logicY = new LogicY(gold.label, infRule, logicInput.getFirst(),
						logicInput.getSecond(), logicInput.getThird());
				double score = weight.dotProduct(featGen.getFeatureVector(ins, logicY));
				if (bestScore < score) {
					best = logicY;
					bestScore = score;
				}
			}
		}
		return best;
	}

	public static List<Triple<LogicInput, LogicInput, LogicInput>> enumerateLogicInputs(LogicX x) {
		List<Triple<LogicInput, LogicInput, LogicInput>> inputs = new ArrayList<>();
		inputs.add(new Triple<>(
				new LogicInput(x.schema.quantSchemas.get(x.quantIndex1)),
				new LogicInput(x.schema.quantSchemas.get(x.quantIndex1)),
				new LogicInput(x.schema.questionSchema)
		));
		return inputs;
	}
}