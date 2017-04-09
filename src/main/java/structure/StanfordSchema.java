package structure;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import joint.Logic;
import utils.Tools;

import java.util.Arrays;
import java.util.List;

public class StanfordSchema {

	List<List<CoreLabel>> tokens;
	public int sentId;
	public QuantSpan qs;
	public IntPair unit;
	public int verb;
	public IntPair subject;
	public IntPair object;
	public IntPair rate;
	public int math;

	public StanfordSchema() {
		this.qs = null;
		sentId = -1;
		verb = -1;
		math = -1;
		subject = new IntPair(-1, -1);
		object = new IntPair(-1, -1);
		unit = new IntPair(-1, -1);
		rate = new IntPair(-1, -1);
	}

	public StanfordSchema(StanfordProblem prob, QuantSpan qs) {
		this();
		this.qs = qs;
		this.tokens = prob.tokens;
		sentId = Tools.getSentenceIdFromCharOffset(prob.tokens, qs.start);
		verb = getDependentVerb(
				prob.tokens.get(sentId),
				prob.dependencies.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
		subject = getSubject(prob.tokens.get(sentId), prob.dependencies.get(sentId), verb);
		Pair<Integer, IntPair> mathPair = getMath(prob.tokens.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
		unit = getUnit(prob.tokens.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
		object = getObject(prob.tokens.get(sentId), prob.dependencies.get(sentId), verb);
		rate = getRate(prob.tokens.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
		if(mathPair.getFirst() >= 0) {
			math = mathPair.getFirst();
			object = mathPair.getSecond();
		}
	}

	@Override
	public String toString() {
		if (sentId == -1) return "No schema found";
		return "(Num: " + (qs != null ? "" + qs.val : "Null") + ") (Subj: " +
				Arrays.asList(Tools.spanToLemmaList(tokens.get(sentId), subject)) +
				") (Verb: " +
				Arrays.asList(Tools.spanToLemmaList(tokens.get(sentId), new IntPair(verb, verb+1))) +
				") (Unit: " + Arrays.asList(Tools.spanToLemmaList(tokens.get(sentId), unit)) +
				") (Obj: " + Arrays.asList(Tools.spanToLemmaList(tokens.get(sentId), object)) +
				") (Rate : " + Arrays.asList(Tools.spanToLemmaList(tokens.get(sentId), rate)) +
				") (Math: " +
				Arrays.asList(Tools.spanToLemmaList(tokens.get(sentId), new IntPair(math, math+1))) +
				")";
	}

	public static int getDependentVerb(List<CoreLabel> tokens,
									   SemanticGraph dependency,
									   int tokenId) {
		IndexedWord word = dependency.getNodeByIndexSafe(tokenId+1);
		IndexedWord prev;
		int verbIndex;
		while(true) {
			if (word.tag().startsWith("V")) {
				verbIndex = word.index()-1;
				break;
			}
			prev = word;
			word = dependency.getParent(word);
			if (word == null || word.index() == 0) {
				verbIndex = prev.index() - 1;
				break;
			}
		}
		// HACK for wrong verb mapping
//		if(verbIndex > tokenId) {
//			for(int i=tokenId+1; i<verbIndex; ++i) {
//				if(tokens.get(i).word().equals("and")) {
//					for(int j=tokenId-1; j>=Math.max(0, tokenId-4); --j) {
//						if(tokens.get(j).tag().startsWith("V")) {
//							verbIndex = j;
//							break;
//						}
//					}
//				}
//			}
//		}
		return verbIndex;

	}

	public static IntPair getUnit(List<CoreLabel> tokens, int tokenId) {
		if (tokenId >= 1 && tokens.get(tokenId-1).word().equals("$")) {
			return new IntPair(tokenId-1, tokenId);
		}
		for(int i=tokenId + 1; i<tokens.size(); ++i) {
			if(tokens.get(i).word().equals("many") ||
					tokens.get(i).word().equals("much")) continue;
			if (tokens.get(i).tag().startsWith("N") ||
					tokens.get(i).tag().startsWith("PRP") ||
					tokens.get(i).tag().startsWith("J")) {
				if (tokens.get(i-1).tag().startsWith("J")) {
					return new IntPair(i-1, i+1);
				} else{
					return new IntPair(i, i+1);
				}
			}
		}
		return new IntPair(-1, -1);
	}

	// Math concepts require different subject and object extraction, should be called
	// before subject, object extraction
	public static Pair<Integer, IntPair> getMath(List<CoreLabel> tokens, int tokenId) {
		int math = -1;
		int start = tokenId + 1, end = tokens.size();
		for(int i=tokenId+1; i<tokens.size(); ++i) {
			if(tokens.get(i).word().equals("if") || tokens.get(i).word().equals("and")
					|| tokens.get(i).word().equals(",") || tokens.get(i).word().equals(";")) {
				break;
			}
			if (Logic.addTokens.contains(tokens.get(i).word()) ||
					Logic.subTokens.contains(tokens.get(i).word()) ||
					Logic.mulTokens.contains(tokens.get(i).word())) {
				math = i;
				break;
			}
		}
		if(math == -1) return new Pair<>(math, null);
		for(int i=start; i<tokens.size(); ++i) {
			if (tokens.get(i).word().equals("if") || tokens.get(i).word().equals(",") ||
					tokens.get(i).word().equals(";")) {
				end = i;
				break;
			}
		}
		boolean realMath = false;
		for(int i=start; i<end; ++i) {
			if(tokens.get(i).word().equals("as") || tokens.get(i).word().equals("than")) {
				realMath = true;
				break;
			}
		}
		if(!realMath) return new Pair<>(-1, null);
		IntPair obj = new IntPair(-1, -1);
		for(int i=start; i<end; ++i) {
			if((tokens.get(i).word().equals("as") &&
					!(tokens.get(i+1).word().equals("many") ||
							tokens.get(i+1).word().equals("much"))) ||
					(tokens.get(i).word().equals("than"))) {
				for(int j=i+1; j<end; ++j) {
					if (tokens.get(j).tag().startsWith("N") ||
							tokens.get(j).tag().startsWith("PRP")) {
						if (tokens.get(j-1).tag().startsWith("J")) {
							obj = new IntPair(j-1, j+1);
						} else{
							obj = new IntPair(j, j+1);
						}
					}
				}
			}
		}
		return new Pair<>(math, obj);
	}

	public static IntPair getSubject(List<CoreLabel> tokens,
									 SemanticGraph dependency,
									 int verbIndex) {
		IndexedWord word = dependency.getNodeByIndexSafe(verbIndex+1);
		if (word == null) return new IntPair(-1, -1);
		for(SemanticGraphEdge edge : dependency.getOutEdgesSorted(word)) {
			if(edge.getRelation().getShortName().equals("nsubj")) {
				int i = edge.getTarget().index()-1;
				if (i >= 1 && tokens.get(i-1).tag().startsWith("J")) {
					return new IntPair(i-1, i+1);
				} else{
					return new IntPair(i, i+1);
				}
			}
		}
		return new IntPair(-1, -1);
	}

	// Assumes math to have been called before this
	public static IntPair getObject(List<CoreLabel> tokens,
									SemanticGraph dependency,
									int verbIndex) {
		IndexedWord word = dependency.getNodeByIndexSafe(verbIndex+1);
		if (word == null) return new IntPair(-1, -1);
		for(SemanticGraphEdge edge : dependency.getOutEdgesSorted(word)) {
			if(edge.getRelation().getShortName().equals("iobj") ||
					edge.getRelation().getShortName().equals("nmod")) {
				int i = edge.getTarget().index()-1;
				if (i >= 1 && tokens.get(i-1).tag().startsWith("J")) {
					return new IntPair(i-1, i+1);
				} else{
					return new IntPair(i, i+1);
				}
			}
		}
		return new IntPair(-1, -1);
	}

	// Assumes subject to have been called before this
	public static IntPair getRate(List<CoreLabel> tokens, int tokenId) {
		for(int i=tokenId; i<tokens.size(); ++i) {
			if(tokens.get(i).word().equals("if") || tokens.get(i).word().equals("and")
					|| tokens.get(i).word().equals(",") || tokens.get(i).word().equals(";")) {
				break;
			}
			if (tokens.get(i).lemma().equals("per") ||
					tokens.get(i).lemma().equals("every") ||
					tokens.get(i).lemma().equals("each")) {
				for(int j=i+1; j<tokens.size(); ++j) {
					if (tokens.get(j).tag().startsWith("N") ||
							tokens.get(j).tag().startsWith("PRP")) {
						if (tokens.get(j-1).tag().startsWith("J")) {
							return new IntPair(j-1, j+1);
						} else{
							return new IntPair(j, j+1);
						}
					}
				}
			}
		}
		for(int i=tokenId-1; i>=0; --i) {
			if(tokens.get(i).word().equals("if") || tokens.get(i).word().equals("and")
					|| tokens.get(i).word().equals(",") || tokens.get(i).word().equals(";")) {
				break;
			}
			if (tokens.get(i).lemma().equals("per") ||
					tokens.get(i).lemma().equals("every") ||
					tokens.get(i).lemma().equals("each")) {
				for(int j=i+1; j<tokens.size(); ++j) {
					if (tokens.get(j).tag().startsWith("N") ||
							tokens.get(j).tag().startsWith("PRP")) {
						if (tokens.get(j-1).tag().startsWith("J")) {
							return new IntPair(j-1, j+1);
						} else{
							return new IntPair(j, j+1);
						}
					}
				}
			}
		}

		return new IntPair(-1, -1);
	}
}
