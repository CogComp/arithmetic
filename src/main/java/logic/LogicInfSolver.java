package logic;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.Triple;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.semgraph.SemanticGraph;
import structure.PairComparator;
import structure.StanfordSchema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		double bestScore = -Double.MAX_VALUE;
		LogicY best = null;
		LogicX logicX = (LogicX) ins;
		for(Pair<Triple<LogicInput, LogicInput, LogicInput>, Double> pair :
				enumerateLogicInputTriples(logicX, weight)) {
			Triple<LogicInput, LogicInput, LogicInput> logicInput = pair.getFirst();
			Double extractionScore = pair.getSecond();
			Map<Pair<String, Integer>, Double> logicOutput = Logic.logicSolver(
					logicInput.getFirst(), logicInput.getSecond(), logicInput.getThird());
			for (String label : Logic.labels) {
				for (int infRule = 0; infRule < Logic.maxNumInferenceTypes; infRule++) {
					LogicY logicY = new LogicY(label, infRule, logicInput.getFirst(),
							logicInput.getSecond(), logicInput.getThird());
					double score = weight.dotProduct(featGen.getFeatureVector(ins, logicY)) *
							logicOutput.getOrDefault(new Pair<>(label, infRule), 0.0) + extractionScore;
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
		for(Pair<Triple<LogicInput, LogicInput, LogicInput>, Double> pair :
				enumerateLogicInputTriples(ins, weight)) {
			Triple<LogicInput, LogicInput, LogicInput> logicInput = pair.getFirst();
			Double extractionScore = pair.getSecond();
			Map<Pair<String, Integer>, Double> logicOutput = Logic.logicSolver(
					logicInput.getFirst(), logicInput.getSecond(), logicInput.getThird());
			for (int infRule = 0; infRule < Logic.maxNumInferenceTypes; infRule++) {
				LogicY logicY = new LogicY(gold.label, infRule, logicInput.getFirst(),
						logicInput.getSecond(), logicInput.getThird());
				double score = weight.dotProduct(featGen.getFeatureVector(ins, logicY)) *
						logicOutput.getOrDefault(new Pair<>(gold.label, infRule), 0.0) +
						extractionScore;
				if (bestScore < score) {
					best = logicY;
					bestScore = score;
				}
			}
		}
		return best;
	}

	public MinMaxPriorityQueue<Pair<Triple<LogicInput, LogicInput, LogicInput>, Double>>
	enumerateLogicInputTriples(LogicX x, WeightVector wv) {
		PairComparator<Triple<LogicInput, LogicInput, LogicInput>> tripleComparator =
				new PairComparator<Triple<LogicInput, LogicInput, LogicInput>>() {};
		MinMaxPriorityQueue<Pair<Triple<LogicInput, LogicInput, LogicInput>, Double>> inputs =
				MinMaxPriorityQueue.orderedBy(tripleComparator).maximumSize(200).create();
		for(Pair<LogicInput, Double> pair1 : enumerateLogicInputs(x, 0, wv)) {
			for(Pair<LogicInput, Double> pair2 : enumerateLogicInputs(x, 1, wv)) {
				for(Pair<LogicInput, Double> pair3 : enumerateLogicInputs(x, 2, wv)) {
					inputs.add(new Pair<>(new Triple<>(pair1.getFirst(),
							pair2.getFirst(), pair3.getFirst()),
							pair1.getSecond() + pair2.getSecond() + pair3.getSecond()));
				}
			}
		}
		return inputs;
	}

	public MinMaxPriorityQueue<Pair<LogicInput, Double>> enumerateLogicInputs(
			LogicX x, int mode, WeightVector wv) {
		PairComparator<LogicInput> pairComparator = new PairComparator<LogicInput>() {};
		MinMaxPriorityQueue<Pair<LogicInput, Double>> inputs =
				MinMaxPriorityQueue.orderedBy(pairComparator).maximumSize(200).create();
		for(Integer verbIndex : enumerateVerbs(x, mode)) {
			for(IntPair subj : enumerateSubjects(x, mode, verbIndex)) {
				for(IntPair obj : enumerateObjects(x, mode, verbIndex)) {
					for(IntPair unit : enumerateUnits(x, mode)) {
						for(IntPair rate : enumerateRates(x, mode, subj)) {
							for(Integer mathIndex : enumerateMath(x, mode)) {
								LogicInput logicInput = new LogicInput(
										mode, subj, obj, unit, rate, verbIndex, mathIndex,
										x.sentIds.get(mode) >= 0 ?
												x.tokens.get(x.sentIds.get(mode)) : null);
								double score = wv.dotProduct(featGen.getVerbFeatureVector(x, verbIndex)) +
										wv.dotProduct(featGen.getSubjectFeatureVector(x, subj)) +
										wv.dotProduct(featGen.getObjectFeatureVector(x, obj)) +
										wv.dotProduct(featGen.getUnitFeatureVector(x, unit)) +
										wv.dotProduct(featGen.getRateFeatureVector(x, rate));
								inputs.add(new Pair<>(logicInput, score));
							}
						}
					}
				}
			}
		}
		return inputs;
	}

	public static List<IntPair> enumerateSubjects(LogicX x, int mode, int verbIndex) {
		List<IntPair> candidates = new ArrayList<>();
		if (x.sentIds.get(mode) < 0) {
			candidates.add(new IntPair(-1, -1));
			return candidates;
		}
		List<CoreLabel> tokens = x.tokens.get(x.sentIds.get(mode));
		IntPair span = (mode == 0) ? x.questionSpan : new IntPair(0, tokens.size());
		SemanticGraph dependency = x.dependencies.get(x.sentIds.get(mode));
		if (verbIndex != -1) {
			IntPair subj = StanfordSchema.getSubject(tokens, dependency, verbIndex);
			if (subj.getSecond() != -1) {
				candidates.add(subj);
			}
		}
		if(candidates.size() == 0 && span.getFirst() != -1) {
			for(int i=verbIndex - 1; i>=span.getFirst(); --i) {
				if(tokens.get(i).tag().startsWith("N") ||
						tokens.get(i).tag().equals("PRP")) {
					if(i >= 1 && tokens.get(i-1).tag().startsWith("J")) {
						candidates.add(new IntPair(i-1, i+1));
					} else {
						candidates.add(new IntPair(i, i+1));
					}
					break;
				}
			}
		}
		// No subject is possible
		candidates.add(new IntPair(-1, -1));
		return candidates;
	}

	public static List<IntPair> enumerateObjects(LogicX x, int mode, int verbIndex) {
		List<IntPair> candidates = new ArrayList<>();
		if (x.sentIds.get(mode) < 0) {
			candidates.add(new IntPair(-1, -1));
			return candidates;
		}
		List<CoreLabel> tokens = x.tokens.get(x.sentIds.get(mode));
		IntPair span = (mode == 0) ? x.questionSpan : new IntPair(0, tokens.size());
		int tokenId = x.tokenIds.get(mode);
		SemanticGraph dependency = x.dependencies.get(x.sentIds.get(mode));
		if (verbIndex != -1) {
			IntPair subj = StanfordSchema.getSubject(tokens, dependency, verbIndex);
			if (subj.getSecond() != -1) {
				candidates.add(subj);
			}
		}
		if(candidates.size() == 0) {
			for(int i=verbIndex + 1; i<span.getSecond(); ++i) {
				if(tokens.get(i).tag().startsWith("N") ||
						tokens.get(i).tag().equals("PRP")) {
					if(tokenId == i || tokenId == (i-1)) continue;
					if(i >= 1 && tokens.get(i-1).tag().startsWith("J")) {
						candidates.add(new IntPair(i-1, i+1));
					} else {
						candidates.add(new IntPair(i, i+1));
					}
					break;
				}
			}
		}
		// No object is possible
		candidates.add(new IntPair(-1, -1));
		return candidates;
	}

	public static List<IntPair> enumerateUnits(LogicX x, int mode) {
		List<IntPair> candidates = new ArrayList<>();
		if (x.sentIds.get(mode) < 0) {
			candidates.add(new IntPair(-1, -1));
			return candidates;
		}
		List<CoreLabel> tokens = x.tokens.get(x.sentIds.get(mode));
		IntPair span = (mode == 0) ? x.questionSpan : new IntPair(0, tokens.size());
		int tokenId = x.tokenIds.get(mode);
		if (tokenId >= 0 && tokens.get(tokenId).word().contains("$")) {
			candidates.add(new IntPair(tokenId, tokenId+1));
			return candidates;
		}
		if (tokenId >= 1 && tokens.get(tokenId-1).word().contains("$")) {
			candidates.add(new IntPair(tokenId-1, tokenId));
			return candidates;
		}
		for(int i=tokenId + 1; i<span.getSecond(); ++i) {
			if(tokens.get(i).tag().startsWith("N") ||
					tokens.get(i).tag().equals("PRP")) {
				if(i >= 1 && tokens.get(i-1).tag().startsWith("J")) {
					candidates.add(new IntPair(i-1, i+1));
				} else {
					candidates.add(new IntPair(i, i+1));
				}
				break;
			}
		}
		// No unit is possible, in this case, we need to use last number's unit
		candidates.add(new IntPair(-1, -1));
		return candidates;
	}

	public static List<IntPair> enumerateRates(LogicX x, int mode, IntPair subject) {
		List<IntPair> candidates = new ArrayList<>();
		if (x.sentIds.get(mode) < 0) {
			candidates.add(new IntPair(-1, -1));
			return candidates;
		}
		List<CoreLabel> tokens = x.tokens.get(x.sentIds.get(mode));
		IntPair span = (mode == 0) ? x.questionSpan : new IntPair(0, tokens.size());
		for(int i=span.getFirst(); i<span.getSecond(); ++i) {
			if(tokens.get(i).word().equalsIgnoreCase("per") ||
					tokens.get(i).word().equalsIgnoreCase("each") ||
					tokens.get(i).word().equalsIgnoreCase("every")) {
				for(int j=i+1; j<span.getSecond(); ++j) {
					if(tokens.get(j).tag().startsWith("N") ||
							tokens.get(j).tag().equals("PRP")) {
						if(tokens.get(j-1).tag().startsWith("J")) {
							candidates.add(new IntPair(j-1, j+1));
						} else {
							candidates.add(new IntPair(j, j+1));
						}
					}
				}
				if (candidates.size() == 0) {
					candidates.add(subject);
				}
				break;
			}
		}
		if (candidates.size() == 0) candidates.add(new IntPair(-1, -1));
		return candidates;
	}

	public static List<Integer> enumerateVerbs(LogicX x, int mode) {
		List<Integer> candidates = new ArrayList<>();
		if (x.sentIds.get(mode) < 0) {
			candidates.add(-1);
			return candidates;
		}
		int tokenId = x.tokenIds.get(mode);
		List<CoreLabel> tokens = x.tokens.get(x.sentIds.get(mode));
//		for(CoreLabel token : tokens) {
//			System.out.print(token+" ");
//		}
//		System.out.println();
//		System.out.println("Tokens size : "+tokens.size());
		StanfordSchema qSchema = x.relevantSchemas.get(mode);
		int verbTokenIndex;
		if (qSchema.verb != -1) {
			verbTokenIndex = qSchema.verb;
//			System.out.println("Verb from schema : "+verbTokenIndex);
//			System.out.println("Mode: "+mode);
//			System.out.println("LogicX: "+x);
			candidates.add(verbTokenIndex);
			return candidates;
		}
		if (mode == 0) {
			for(int i=x.questionSpan.getFirst(); i<x.questionSpan.getSecond(); ++i) {
				if(tokens.get(i).tag().startsWith("V")) {
					if(i+1<x.questionSpan.getSecond() && tokens.get(i+1).tag().startsWith("V")) {
						continue;
					}
//					System.out.println("Verb from Ques: "+i);
					candidates.add(i);
				}
			}
		} else {
			for(int i=tokenId+1; i<tokens.size(); ++i) {
				if(tokens.get(i).tag().startsWith("V")) {
					if(i+1<tokens.size() && tokens.get(i+1).tag().startsWith("V")) {
						continue;
					}
//					System.out.println("Verb from right: "+i);
					candidates.add(i);
					break;
				}
			}
			for(int i=tokenId - 1; i>=0; --i) {
				if(tokens.get(i).tag().startsWith("V")) {
//					System.out.println("Verb from Ques: "+i);
					candidates.add(i);
					break;
				}
			}
		}
		if (candidates.size() == 0) {
			candidates.add(-1);
			System.out.println("Verb candidates : " + candidates.size());
			for(int i=0; i<tokens.size(); ++i) {
				System.out.print("["+tokens.get(i).word()+" "+tokens.get(i).tag()+"] ");
			}
			System.out.println();
		}
		return candidates;
	}

	public static List<Integer> enumerateMath(LogicX x, int mode) {
		List<Integer> candidates = new ArrayList<>();
		if (x.sentIds.get(mode) < 0) {
			candidates.add(-1);
			return candidates;
		}
		int window = 5;
		List<CoreLabel> tokens = x.tokens.get(x.sentIds.get(mode));
		int tokenId = x.tokenIds.get(mode);
		if(tokenId >= 0) {
			for (int i = Math.max(0, tokenId - window);
				 i < Math.min(tokens.size(), tokenId + window + 1);
				 ++i) {
				if (Logic.addTokens.contains(tokens.get(i)) ||
						Logic.subTokens.contains(tokens.get(i)) ||
						Logic.mulTokens.contains(tokens.get(i))) {
					candidates.add(i);
				}
			}
		} else {
			for (int i = x.questionSpan.getFirst(); i < x.questionSpan.getSecond(); ++i) {
				if (Logic.addTokens.contains(tokens.get(i)) ||
						Logic.subTokens.contains(tokens.get(i)) ||
						Logic.mulTokens.contains(tokens.get(i))) {
					candidates.add(i);
				}
			}
		}
		if (candidates.size() == 0) candidates.add(-1);
		return candidates;
	}
}