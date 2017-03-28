package structure;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import logic.Logic;
import utils.Tools;

import java.util.List;

public class StanfordSchema {

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
		subject = new IntPair(-1, -1);
		object = new IntPair(-1, -1);
		unit = new IntPair(-1, -1);
		rate = new IntPair(-1, -1);
	}

	public StanfordSchema(StanfordProblem prob, QuantSpan qs) {
		this.qs = qs;
		sentId = Tools.getSentenceIdFromCharOffset(prob.tokens, qs.start);
		verb = getDependentVerb(prob.dependencies.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
		subject = getSubject(prob.tokens.get(sentId), prob.dependencies.get(sentId), verb);
		object = getObject(prob.tokens.get(sentId), prob.dependencies.get(sentId), verb);
		unit = getUnit(prob.tokens.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
		rate = getRate(prob.tokens.get(sentId));
		math = getMath(prob.tokens.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
	}

	@Override
	public String toString() {
		return "(Num: " + (qs != null ? "" + qs.val : "Null") + ") (Subj: " +
				subject + ") (Verb: " + verb + ") (Unit: " + unit +
				") (Obj: " + object + ") (Rate : " + rate + ")" +
				") (Math: " + math + ")";
	}

	public static int getDependentVerb(SemanticGraph dependency, int tokenId) {
		IndexedWord word = dependency.getNodeByIndex(tokenId+1);
		IndexedWord prev;
		while(true) {
			if (word.tag().startsWith("V")) {
				return word.index()-1;
			}
			prev = word;
			word = dependency.getParent(word);
			if (word == null || word.index() == 0) {
				return prev.index() - 1;
			}
		}
	}

	public static IntPair getUnit(List<CoreLabel> tokens, int tokenId) {
		if (tokenId >= 1 && tokens.get(tokenId-1).equals("$")) {
			return new IntPair(tokenId-1, tokenId);
		}
		for(int i=tokenId + 1; i<tokens.size(); ++i) {
			if (tokens.get(i).tag().startsWith("N")) {
				if (tokens.get(i-1).tag().startsWith("J")) {
					return new IntPair(i-1, i+1);
				} else{
					return new IntPair(i, i+1);
				}
			}
		}
		return new IntPair(-1, -1);
	}

	public static IntPair getRate(List<CoreLabel> tokens) {
		for(int i=0; i<tokens.size(); ++i) {
			if (tokens.get(i).lemma().equals("per") ||
					tokens.get(i).lemma().equals("every") ||
					tokens.get(i).lemma().equals("each")) {
				for(int j=i+1; j<tokens.size(); ++j) {
					if (tokens.get(j).tag().startsWith("N")) {
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

	public static int getMath(List<CoreLabel> tokens, int tokenId) {
		int window = 5;
		for(int i=Math.max(0, tokenId-window);
			i<Math.min(tokens.size(), tokenId+window+1);
			++i) {
			if (Logic.addTokens.contains(tokens.get(i).lemma()) ||
					Logic.subTokens.contains(tokens.get(i).lemma()) ||
					Logic.mulTokens.contains(tokens.get(i).lemma())) {
				return i;
			}
		}
		return -1;
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
}
