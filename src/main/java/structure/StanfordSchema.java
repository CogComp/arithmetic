package structure;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import logic.Logic;
import utils.Tools;

import java.util.Arrays;
import java.util.List;

public class StanfordSchema {

	List<List<CoreLabel>> tokens;
	public int sentId;
	public int quantId;
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
		quantId = -1;
		verb = -1;
		math = -1;
		subject = new IntPair(-1, -1);
		object = new IntPair(-1, -1);
		unit = new IntPair(-1, -1);
		rate = new IntPair(-1, -1);
	}

	public StanfordSchema(StanfordProblem prob, int quantId) {
		this();
		this.quantId = quantId;
		this.qs = prob.quantities.get(quantId);
		this.tokens = prob.tokens;
		sentId = Tools.getSentenceIdFromCharOffset(prob.tokens, qs.start);
		verb = getDependentVerb(
				prob.tokens.get(sentId),
				prob.dependencies.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
		subject = getSubject(prob.tokens.get(sentId), prob.dependencies.get(sentId), verb);
		object = getObject(prob.tokens.get(sentId), prob.dependencies.get(sentId), verb);
		Pair<Integer, IntPair> mathPair = getMath(prob.tokens.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start), false);
		math = mathPair.getFirst();
		if(mathPair.getSecond() != null && mathPair.getSecond().getFirst() >= 0) {
			object = mathPair.getSecond();
		}
		Pair<IntPair, IntPair> unitPair = getUnit(prob.tokens.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
		unit = unitPair.getFirst();
		if(unitPair.getSecond() != null && unitPair.getSecond().getFirst() >= 0) {
			object = unitPair.getSecond();
		}
		rate = getRate(prob.tokens.get(sentId),
				Tools.getTokenIdFromCharOffset(prob.tokens.get(sentId), qs.start));
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
			if (word.tag().startsWith("V") || word.lemma().equals("cost")) {
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
		return verbIndex;
	}

	public static Pair<IntPair, IntPair> getUnit(List<CoreLabel> tokens, int tokenId) {
		if (tokenId >= 1 && tokens.get(tokenId-1).word().equals("$")) {
			return new Pair<>(new IntPair(tokenId-1, tokenId), new IntPair(-1, -1));
		}
		for(int i=tokenId + 1; i<tokens.size(); ++i) {
//			if(tokens.get(i).word().equals("at") ||
//					tokens.get(i).word().equals("during") ||
//					tokens.get(i).tag().startsWith("V")) break;
			if(tokens.get(i).word().equals("many") ||
					tokens.get(i).word().equals("much")) continue;
			if(tokens.get(i).word().equals("to") || tokens.get(i).word().equals("from")) {
				return new Pair<>(new IntPair(-1, -1), Tools.getMaximalNounPhraseSpan(tokens, i+1));
			}
			if (tokens.get(i).tag().startsWith("N") ||
					tokens.get(i).tag().startsWith("PRP") ||
					tokens.get(i).tag().startsWith("J")) {
				return new Pair<>(Tools.getMaximalNounPhraseSpan(tokens, i), new IntPair(-1, -1));
			}
		}
		return new Pair<>(new IntPair(-1, -1), new IntPair(-1, -1));
	}

	// Math concepts require different subject and object extraction, should be called
	// before subject, object extraction
	public static Pair<Integer, IntPair> getMath(List<CoreLabel> tokens,
												 int tokenId,
												 boolean isQuestion) {
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
			if(tokens.get(i).word().equals("as") || tokens.get(i).word().equals("than") ||
					Logic.mulTokens.contains(tokens.get(math).word())) {
				realMath = true;
				break;
			}
		}
		if(!isQuestion && !realMath) return new Pair<>(-1, null);
		IntPair obj = new IntPair(-1, -1);
		for(int i=start; i<end; ++i) {
			if((tokens.get(i).word().equals("as") &&
					!(tokens.get(i+1).word().equals("many") ||
							tokens.get(i+1).word().equals("much"))) ||
					(tokens.get(i).word().equals("than"))) {
				for(int j=i+1; j<end; ++j) {
					if (tokens.get(j).tag().startsWith("N") ||
							tokens.get(j).tag().startsWith("PRP")) {
						obj = Tools.getMaximalNounPhraseSpan(tokens, j);
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
				return Tools.getMaximalNounPhraseSpan(tokens, i);
			}
		}
		return new IntPair(-1, -1);
	}

	// Assumes math to have been called before this
	public static IntPair getObject(List<CoreLabel> tokens,
									SemanticGraph dependency,
									int verbIndex) {
		if(verbIndex < tokens.size() - 1 &&
				tokens.get(verbIndex + 1).lemma().equals("he")) {
			return new IntPair(verbIndex + 1, verbIndex + 2);
		}
		if(verbIndex < tokens.size() - 2 &&
				tokens.get(verbIndex + 1).lemma().equals("she") &&
				(tokens.get(verbIndex + 2).tag().equals("CD") ||
						tokens.get(verbIndex + 2).tag().equals("$"))) {
			return new IntPair(verbIndex + 1, verbIndex + 2);
		}
		IndexedWord word = dependency.getNodeByIndexSafe(verbIndex+1);
		if (word == null) return new IntPair(-1, -1);
//		for(SemanticGraphEdge edge : dependency.edgeListSorted()) {
//			System.out.println(edge.getRelation().getShortName()+":"+
//					edge.getSource()+"-->"+edge.getTarget());
//		}
		for(SemanticGraphEdge edge : dependency.getOutEdgesSorted(word)) {
//			System.out.println(edge.getRelation().getShortName()+":"+
//					edge.getSource()+"-->"+edge.getTarget());
			if(edge.getRelation().getShortName().equals("iobj") ||
					edge.getRelation().getShortName().equals("nmod") ||
					edge.getRelation().getShortName().equals("prep")) {
				int i = edge.getTarget().index()-1;
//				System.out.println("Returned:"+
//						Arrays.asList(Tools.getMaximalNounPhraseSpan(tokens, i)));
				return Tools.getMaximalNounPhraseSpan(tokens, i);
			}
		}
		return new IntPair(-1, -1);
	}

	// Assumes subject to have been called before this
	public static IntPair getRate(List<CoreLabel> tokens, int tokenId) {
		for(int i=tokenId+1; i<tokens.size(); ++i) {
			if(tokens.get(i).word().equals("if") || tokens.get(i).word().equals("and") ||
					tokens.get(i).word().equals(",") || tokens.get(i).word().equals(";") ||
					tokens.get(i).tag().equals("CD")) {
				break;
			}
			if (tokens.get(i).lemma().equals("per") ||
					tokens.get(i).lemma().equals("every") ||
					tokens.get(i).lemma().equals("each")) {
				for(int j=i+1; j<tokens.size(); ++j) {
					if(tokens.get(j).word().equals("if") || tokens.get(j).word().equals("and")
							|| tokens.get(j).word().equals(",") || tokens.get(j).word().equals(";")
							|| tokens.get(j).tag().equals("CD")) {
						break;
					}
					if (tokens.get(j).tag().startsWith("N") ||
							tokens.get(j).tag().startsWith("PRP")) {
						return Tools.getMaximalNounPhraseSpan(tokens, j);
					}
				}
				for(int j=tokenId-1; j>=0; --j) {
					if (tokens.get(j).word().equals("if") || tokens.get(j).word().equals("and")
							|| tokens.get(j).word().equals(",") || tokens.get(j).word().equals(";")) {
						break;
					}
					if (tokens.get(j).tag().startsWith("N")) {
						return Tools.getMaximalNounPhraseSpan(tokens, j);
					}
				}
			}
		}
		boolean foundVerb = false;
		for(int i=tokenId-1; i>=0; --i) {
			if(tokens.get(i).word().equals("if") || tokens.get(i).word().equals("and")
					|| tokens.get(i).word().equals(",") || tokens.get(i).word().equals(";")) {
				break;
			}
			if(tokens.get(i).tag().startsWith("V") || tokens.get(i).lemma().equals("cost")) foundVerb = true;
			if (tokens.get(i).lemma().equals("per") ||
					tokens.get(i).lemma().equals("every") ||
					tokens.get(i).lemma().equals("each") ||
					tokens.get(i).lemma().equals("1.0")) {
				if(!foundVerb) break;
				for(int j=i+1; j<tokens.size(); ++j) {
					if(tokens.get(j).word().equals("if") || tokens.get(j).word().equals("and")
							|| tokens.get(j).word().equals(",") || tokens.get(j).word().equals(";")) {
						break;
					}
					if (tokens.get(j).tag().startsWith("N") ||
							tokens.get(j).tag().startsWith("PRP")) {
						return Tools.getMaximalNounPhraseSpan(tokens, j);
					}
				}
			}
		}
		if(tokenId+2 < tokens.size() && (tokens.get(tokenId+1).word().equals("a") ||
				tokens.get(tokenId+1).word().equals("an")) &&
				tokens.get(tokenId+2).tag().startsWith("N")) {
			return Tools.getMaximalNounPhraseSpan(tokens, tokenId+2);

		}
		if(tokenId+3 < tokens.size() && tokens.get(tokenId+1).tag().startsWith("N") &&
				(tokens.get(tokenId+2).word().equals("a") ||
						tokens.get(tokenId+2).word().equals("an")) &&
				tokens.get(tokenId+3).tag().startsWith("N")) {
			return Tools.getMaximalNounPhraseSpan(tokens, tokenId+3);
		}
		return new IntPair(-1, -1);
	}
}
