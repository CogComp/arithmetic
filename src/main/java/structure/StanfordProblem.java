package structure;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import joint.Logic;
import reader.Reader;
import utils.Params;
import utils.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		for(QuantSpan qs : quantities) {
			schema.add(new StanfordSchema(this, qs));
		}
		questionSchema = getQuestionSchema(this);
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
		schema.verb = schema.getDependentVerb(dependency, quesSpan.getFirst());
		schema.unit = schema.getUnit(tokens, quesSpan.getFirst());
		schema.rate = schema.getRate(tokens, quesSpan.getFirst());
		schema.subject = schema.getSubject(tokens, dependency, schema.verb);
		schema.object = schema.getObject(tokens, dependency, schema.verb);
		for(int i=quesSpan.getFirst(); i<quesSpan.getSecond(); ++i) {
			if (Logic.addTokens.contains(tokens.get(i).lemma()) ||
					Logic.subTokens.contains(tokens.get(i).lemma()) ||
					Logic.mulTokens.contains(tokens.get(i).lemma())) {
				schema.math = i;
				break;
			}
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

	public static void main(String args[]) throws Exception {
		List<StanfordProblem> probs =
				Reader.readStanfordProblemsFromJson(Params.allArithDir);
		for(StanfordProblem prob : probs) {
			for(List<CoreLabel> cl1 : prob.tokens) {
				for(CoreLabel cl2 : cl1) {
					System.out.println(cl2.word()+" "+cl2.beginPosition()+" "+cl2.endPosition());
				}
			}
//			System.out.println(prob);
//			for(StanfordSchema schema : prob.schema) {
//				System.out.println(schema);
//			}
//			System.out.println(prob.questionSchema);
			System.out.println();
		}
	}
	
}
