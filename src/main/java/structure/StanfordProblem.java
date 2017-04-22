package structure;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import reader.Reader;
import utils.Params;
import utils.Tools;

import java.util.*;

public class StanfordProblem {

	public int id;
	public String question;
	public Double answer;
	public List<QuantSpan> quantities;
	public Node expr;
	public List<List<CoreLabel>> tokens;
	public List<SemanticGraph> dependencies;
	public List<StanfordSchema> schema;
	public StanfordSchema questionSchema;
	public Map<Pair<String, String>, String> wordnetRelations;


	@Override
	public String toString() {
		String str = "";
		str += "\nQuestion : "+question+"\nAnswer : "+answer +
				"\nQuantities : "+Arrays.asList(quantities) +
				"\nExpression : "+expr;
		return str;
	}

	public StanfordProblem(int id, String q, double a) throws AnnotatorException {
		this.id = id;
		question = q;
		answer = a;
		quantities = new ArrayList<QuantSpan>();
	}
	
	public void extractQuantities() throws Exception {
		List<QuantSpan> spanArray = Tools.quantifier.getSpans(question);
		quantities = new ArrayList<QuantSpan>();
		for(QuantSpan span:spanArray){
			boolean containsDigit = false;
			for(int i=span.start; i<span.end; ++i) {
				if(Character.isDigit(question.charAt(i))) {
					containsDigit = true;
					break;
				}
			}
			if(containsDigit){
				if(Character.isLowerCase(question.charAt(span.end-1)) ||
						Character.isUpperCase(question.charAt(span.end-1))) continue;
				if(span.start>0 && (Character.isLowerCase(question.charAt(span.start-1))
						|| Character.isUpperCase(question.charAt(span.start-1)))) continue;
				quantities.add(span);
			} else {
				quantities.add(span);
			}
		}
	}
	
	public void extractAnnotations() throws Exception {
		List<CoreMap> sentences = Tools.annotateWithStanfordCoreNLP(question);
		tokens = new ArrayList<>();
		dependencies = new ArrayList<>();
		for(CoreMap sentence: sentences) {
			tokens.add(sentence.get(CoreAnnotations.TokensAnnotation.class));
			dependencies.add(sentence.get(SemanticGraphCoreAnnotations.
					CollapsedCCProcessedDependenciesAnnotation.class));
		}
		schema = new ArrayList<>();
		for(int i=0; i<quantities.size(); ++i) {
			StanfordSchema ss = new StanfordSchema(this, i);
			if(ss.unit.equals(ss.rate)) {
				ss.rate = new IntPair(-1, -1);
			}
			if(ss.unit.equals(ss.object)) {
				ss.object = new IntPair(-1, -1);
			}
			if(ss.unit.equals(ss.subject)) {
				ss.subject = new IntPair(-1, -1);
			}
			schema.add(ss);
		}
		questionSchema = getQuestionSchema(this);
		if(questionSchema.unit.equals(questionSchema.rate)) {
			questionSchema.unit = new IntPair(-1, -1);
		}
//		if(questionSchema.unit.equals(questionSchema.object)) {
//			questionSchema.object = new IntPair(-1, -1);
//		}
//		if(questionSchema.unit.equals(questionSchema.subject)) {
//			questionSchema.subject = new IntPair(-1, -1);
//		}
		wordnetRelations = getWordnetRelations();
	}

	public StanfordSchema getQuestionSchema(StanfordProblem prob) {
		StanfordSchema schema = new StanfordSchema();
		List<String> whWords = Arrays.asList("what", "how", "when");
		schema.tokens = prob.tokens;
		List<CoreLabel> tokens = null;
		for(int i=0; i<prob.tokens.size(); ++i) {
			List<CoreLabel> sentence = prob.tokens.get(i);
			for(CoreLabel token : sentence) {
				if(token.word().equals("?") || whWords.contains(token.lemma())) {
					tokens = sentence;
					schema.sentId = i;
					break;
				}
			}
		}
		if (tokens == null) {
			return schema;
		}
		IntPair quesSpan = getQuestionSpan(tokens);
		SemanticGraph dependency = prob.dependencies.get(schema.sentId);
		schema.verb = schema.getDependentVerb(tokens, dependency, quesSpan.getFirst());
		schema.rate = getRateForQuestion(tokens, quesSpan.getFirst());
		schema.subject = schema.getSubject(tokens, dependency, schema.verb);
		schema.object = schema.getObject(tokens, dependency, schema.verb);
		Pair<IntPair, IntPair> unitPair = schema.getUnit(tokens, quesSpan.getFirst());
		schema.unit = unitPair.getFirst();
		if(unitPair.getSecond().getFirst() >= 0) {
			schema.object = unitPair.getSecond();
		}
		Pair<Integer, IntPair> mathPair = schema.getMath(tokens, quesSpan.getFirst(), true);
		if(mathPair.getFirst() >= 0) {
			schema.math = mathPair.getFirst();
			schema.object = mathPair.getSecond();
		}
		return schema;
	}

	public static IntPair getQuestionSpan(List<CoreLabel> tokens) {
		List<String> whWords = Arrays.asList("what", "how", "when");
		int start = 0, end = tokens.size();
		for(int i=0; i<tokens.size(); ++i) {
			if(whWords.contains(tokens.get(i).lemma())) {
				start = i;
			}
		}
		for(int i=start+1; i<tokens.size(); ++i) {
			if(tokens.get(i).word().equals("?") || tokens.get(i).word().equals("if") ||
					tokens.get(i).word().equals(",")) {
				end = i;
			}
		}
		return new IntPair(start, end);
	}

	public Map<Pair<String, String>, String> getWordnetRelations() {
		Map<Pair<String, String>, String> map = new HashMap<>();
		for(List<CoreLabel> sent1 : tokens) {
			for(CoreLabel word1 : sent1) {
				for (List<CoreLabel> sent2 : tokens) {
					for (CoreLabel word2 : sent2) {
						if(word1.lemma().equals(word2.lemma())) {
							continue;
						}
						String wn = Tools.wordNetIndicator(
								word1.lemma(), word2.lemma(), word1.tag(), word2.tag());
						if(wn != null) {
							map.put(new Pair<>(word1.lemma(), word2.lemma()), wn);
						}
					}
				}
			}
		}
		return map;
	}

	public static void main(String args[]) throws Exception {
		List<StanfordProblem> probs =
				Reader.readStanfordProblemsFromJson();
		for(StanfordProblem prob : probs) {
			if(prob.id != 750) continue;
			System.out.println(prob);
			for(List<CoreLabel> tokens : prob.tokens) {
				for(CoreLabel token : tokens) {
					System.out.print("["+token.tag()+"_"+token.lemma()+"]");
				}
			}
			System.out.println();
			for(StanfordSchema schema : prob.schema) {
				System.out.println(schema);
			}
			System.out.println(prob.questionSchema);
			System.out.println();
		}
	}


	public static IntPair getRateForQuestion(List<CoreLabel> tokens, int tokenId) {
		for(int i=tokenId+1; i<tokens.size(); ++i) {
			if(tokens.get(i).word().equals("if") || tokens.get(i).word().equals("and") ||
					tokens.get(i).word().equals(",") || tokens.get(i).word().equals(";")) {
				break;
			}
			if (tokens.get(i).lemma().equals("per") ||
					tokens.get(i).lemma().equals("every") ||
					tokens.get(i).lemma().equals("each") ||
					tokens.get(i).lemma().equals("1.0")) {
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
		return new IntPair(-1, -1);
	}
	
}
