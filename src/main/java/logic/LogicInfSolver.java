package logic;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.Triple;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import structure.PairComparator;
import structure.QuantitySchema;
import utils.Tools;

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
						logicOutput.getOrDefault(new Pair<>(gold.label, infRule), 0.0) + extractionScore;
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
					inputs.add(new Pair<>(new Triple<>(pair1.getFirst(), pair2.getFirst(), pair3.getFirst()),
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
						for(IntPair rate : enumerateRates(x, mode, verbIndex, subj)) {
							for(Integer mathIndex : enumerateMath(x, mode)) {
								LogicInput logicInput = new LogicInput(mode, Tools.spanToList(x.lemmas, subj),
										Tools.spanToList(x.lemmas, obj), Tools.spanToList(x.lemmas, unit),
										Tools.spanToList(x.lemmas, rate), x.lemmas.get(verbIndex),
										mathIndex>=0?x.lemmas.get(mathIndex):null, x);
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
		Triple<Integer, IntPair, QuantitySchema> triple = getSpanAndSchema(x, mode);
		int tokenId = triple.getFirst();
		IntPair span = triple.getSecond();
		QuantitySchema qSchema = triple.getThird();
		Constituent verbPhrase = null;
		for(Constituent dep : x.dependency) {
			if (verbIndex >= dep.getStartSpan() && verbIndex < dep.getEndSpan()) {
				verbPhrase = dep;
				break;
			}
		}
		if (verbPhrase != null) {
			List<Relation> relations = verbPhrase.getOutgoingRelations();
			for (Relation relation : relations) {
				if (!relation.getRelationName().equals("nsubj")) continue;
				Constituent dst = relation.getTarget();
				int index = dst.getStartSpan();
				if (index >= 1 && x.posTags.get(index - 1).getLabel().startsWith("J")) {
					candidates.add(new IntPair(index - 1, index + 1));
				} else {
					candidates.add(new IntPair(index, index + 1));
				}
			}
		}
		if(candidates.size() == 0) {
			for(int i=verbIndex - 1; i>=span.getFirst(); --i) {
				if(x.posTags.get(i).getLabel().startsWith("N") ||
						x.posTags.get(i).getLabel().equals("PRP")) {
					if(i >= 1 && x.posTags.get(i-1).getLabel().startsWith("J")) {
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
		Triple<Integer, IntPair, QuantitySchema> triple = getSpanAndSchema(x, mode);
		int tokenId = triple.getFirst();
		IntPair span = triple.getSecond();
		QuantitySchema qSchema = triple.getThird();
		Constituent verbPhrase = null;
		for(Constituent dep : x.dependency) {
			if (verbIndex >= dep.getStartSpan() && verbIndex < dep.getEndSpan()) {
				verbPhrase = dep;
				break;
			}
		}
		if (verbPhrase != null) {
			List<Relation> relations = verbPhrase.getOutgoingRelations();
			for (Relation relation : relations) {
				if (!relation.getRelationName().equals("iobj") &&
						!relation.getRelationName().equals("nmod")) continue;
				Constituent dst = relation.getTarget();
				int index = dst.getStartSpan();
				if (index >= 1 && x.posTags.get(index - 1).getLabel().startsWith("J")) {
					candidates.add(new IntPair(index - 1, index + 1));
				} else {
					candidates.add(new IntPair(index, index + 1));
				}
			}
		}
		if(candidates.size() == 0) {
			for(int i=verbIndex + 1; i<span.getSecond(); ++i) {
				if(x.posTags.get(i).getLabel().startsWith("N") ||
						x.posTags.get(i).getLabel().equals("PRP")) {
					if(tokenId == i || tokenId == (i-1)) continue;
					if(i >= 1 && x.posTags.get(i-1).getLabel().startsWith("J")) {
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
		Triple<Integer, IntPair, QuantitySchema> triple = getSpanAndSchema(x, mode);
		int tokenId = triple.getFirst();
		IntPair span = triple.getSecond();
		QuantitySchema qSchema = triple.getThird();
		if (tokenId >= 0 && x.ta.getToken(tokenId).contains("$")) {
			candidates.add(new IntPair(tokenId, tokenId+1));
			return candidates;
		}
		if (tokenId >= 1 && x.ta.getToken(tokenId-1).contains("$")) {
			candidates.add(new IntPair(tokenId-1, tokenId));
			return candidates;
		}
		for(int i=tokenId + 1; i<span.getSecond(); ++i) {
			if(x.posTags.get(i).getLabel().startsWith("N") ||
					x.posTags.get(i).getLabel().equals("PRP")) {
				if(i >= 1 && x.posTags.get(i-1).getLabel().startsWith("J")) {
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

	public static List<IntPair> enumerateRates(LogicX x, int mode, int verbIndex, IntPair subject) {
		List<IntPair> candidates = new ArrayList<>();
		Triple<Integer, IntPair, QuantitySchema> triple = getSpanAndSchema(x, mode);
		int tokenId = triple.getFirst();
		IntPair span = triple.getSecond();
		QuantitySchema qSchema = triple.getThird();
		for(int i=span.getFirst(); i<span.getSecond(); ++i) {
			if(x.ta.getToken(i).equalsIgnoreCase("per") ||
					x.ta.getToken(i).equalsIgnoreCase("each") ||
					x.ta.getToken(i).equalsIgnoreCase("every")) {
				for(int j=i+1; j<span.getSecond(); ++j) {
					if(x.posTags.get(j).getLabel().startsWith("N") ||
							x.posTags.get(j).getLabel().equals("PRP")) {
						if(x.posTags.get(j-1).getLabel().startsWith("J")) {
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
		Triple<Integer, IntPair, QuantitySchema> triple = getSpanAndSchema(x, mode);
		int tokenId = triple.getFirst();
		IntPair span = triple.getSecond();
		QuantitySchema qSchema = triple.getThird();
		int verbTokenIndex = -1;
		if (qSchema.verbPhrase != null) {
			verbTokenIndex = qSchema.verbPhrase.getEndSpan() - 1;
		}
		if (verbTokenIndex >= 0 && x.posTags.get(verbTokenIndex).getLabel().startsWith("V")) {
			candidates.add(verbTokenIndex);
			return candidates;
		}
		if (tokenId == -1) {
			for(int i=span.getFirst(); i<span.getSecond(); ++i) {
				if(x.posTags.get(i).getLabel().startsWith("V")) {
					if(i+1<span.getSecond() && x.posTags.get(i+1).getLabel().startsWith("V")) {
						continue;
					}
					candidates.add(i);
				}
			}
		} else {
			for(int i=tokenId+1; i<span.getSecond(); ++i) {
				if(x.posTags.get(i).getLabel().startsWith("V")) {
					if(i+1<span.getSecond() && x.posTags.get(i+1).getLabel().startsWith("V")) {
						continue;
					}
					candidates.add(i);
					break;
				}
			}
			for(int i=tokenId - 1; i>=span.getFirst(); --i) {
				if(x.posTags.get(i).getLabel().startsWith("V")) {
					candidates.add(i);
					break;
				}
			}
		}
		if (candidates.size() == 0) {
			System.out.println("Verb candidates : " + candidates.size());
			for(int i=span.getFirst(); i<span.getSecond(); ++i) {
				System.out.print("["+x.ta.getToken(i)+" "+x.posTags.get(i).getLabel()+"] ");
			}
			System.out.println();
		}

		return candidates;
	}

	public static List<Integer> enumerateMath(LogicX x, int mode) {
		List<Integer> candidates = new ArrayList<>();
		int window = 5;
		Triple<Integer, IntPair, QuantitySchema> triple = getSpanAndSchema(x, mode);
		int tokenId = triple.getFirst();
		IntPair span = triple.getSecond();
		QuantitySchema qSchema = triple.getThird();
		if(tokenId >= 0) {
			for (int i = Math.max(span.getFirst(), tokenId - window);
				 i < Math.min(span.getSecond(), tokenId + window + 1);
				 ++i) {
				if (Logic.addTokens.contains(x.ta.getToken(i)) ||
						Logic.subTokens.contains(x.ta.getToken(i)) ||
						Logic.mulTokens.contains(x.ta.getToken(i))) {
					candidates.add(i);
				}
			}
		} else {
			for (int i = span.getFirst(); i < span.getSecond(); ++i) {
				if (Logic.addTokens.contains(x.ta.getToken(i)) ||
						Logic.subTokens.contains(x.ta.getToken(i)) ||
						Logic.mulTokens.contains(x.ta.getToken(i))) {
					candidates.add(i);
				}
			}
		}
		if (candidates.size() == 0) candidates.add(-1);
		return candidates;
	}

	public static Triple<Integer, IntPair, QuantitySchema> getSpanAndSchema(LogicX x, int mode) {
		int tokenId = -1;
		IntPair span = new IntPair(-1, -1);
		QuantitySchema qSchema = null;
		if(mode == 0) {
			span = x.schema.questionSpan;
			qSchema = x.schema.questionSchema;
		} else if(mode == 1) {
			tokenId = x.ta.getTokenIdFromCharacterOffset(x.quantities.get(x.quantIndex1).start);
			span = new IntPair(x.ta.getSentenceFromToken(tokenId).getStartSpan(),
					x.ta.getSentenceFromToken(tokenId).getEndSpan());
			qSchema = x.schema.quantSchemas.get(x.quantIndex1);
		} else if(mode == 2) {
			tokenId = x.ta.getTokenIdFromCharacterOffset(x.quantities.get(x.quantIndex2).start);
			span = new IntPair(x.ta.getSentenceFromToken(tokenId).getStartSpan(),
					x.ta.getSentenceFromToken(tokenId).getEndSpan());
			qSchema = x.schema.quantSchemas.get(x.quantIndex2);
		}
		return new Triple<>(tokenId, span, qSchema);
	}

}