package structure;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
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
		Annotation document = new Annotation(question);
		Tools.stanfordPipeline.annotate(document);
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
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
		List<CoreLabel> tokens = null;
		for(int i=0; i<prob.tokens.size(); ++i) {
			List<CoreLabel> sentence = prob.tokens.get(i);
			if (sentence.get(sentence.size()-1).equals("?")) {
				tokens = sentence;
				schema.sentId = i;
				break;
			}
		}
		if (tokens == null) {
			return schema;
		}
		IntPair quesSpan = getQuestionSpan(prob.tokens.get(schema.sentId));
		SemanticGraph dependency = prob.dependencies.get(schema.sentId);
		schema.verb = schema.getDependentVerb(dependency, quesSpan.getFirst()+1);
		schema.unit = schema.getUnit(tokens, quesSpan.getFirst());
		schema.rate = schema.getRate(tokens);
		schema.subject = schema.getSubject(tokens, dependency, schema.verb);
		schema.object = schema.getObject(tokens, dependency, schema.verb);
		return schema;
	}

	public static IntPair getQuestionSpan(List<CoreLabel> tokens) {
		int start = -1, end = -1;
		for(int i=0; i<tokens.size()-1; ++i) {
			if (tokens.get(i).lemma().equals("how") &&
					(tokens.get(i+1).lemma().equals("many") ||
							tokens.get(i+1).lemma().equals("much"))) {
				start = i+1;
				end = tokens.size();
				for(int j=start+1; j<tokens.size(); ++j) {
					if(tokens.get(j).equals(",") || tokens.get(j).equals("if")) {
						end = j;
						break;
					}
				}
				break;
			}
		}
		return new IntPair(start, end);
	}

	public static void main(String args[]) throws Exception {
		List<StanfordProblem> probs =
				Reader.readStanfordProblemsFromJson(Params.allArithDir);
		for(StanfordProblem prob : probs) {
			System.out.println(prob);
			System.out.print(prob.schema);
			System.out.println();
		}
	}
	
}
