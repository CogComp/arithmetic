package structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.Tools;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;

public class Schema {
	
	public int problemId;
	public Map<Integer, IntPair> coref;
	public List<String> questionTokens;
	public List<QuantitySchema> quantSchemas;
	
	public Schema(Problem prob) {
		problemId = prob.id;
		questionTokens = new ArrayList<String>();
		quantSchemas = new ArrayList<QuantitySchema>();
		// Simple Coreference
		coref = simpleCoref(prob);
		// QuestionNPs
		questionTokens = getQuestionTokens(prob);
		// Quantity Schemas
		for(int i=0; i<prob.quantities.size(); ++i) {
			QuantSpan qs = prob.quantities.get(i);
			QuantitySchema schema = new QuantitySchema(qs);
			// Find related verb, using dependency parse
			schema.verbPhrase = schema.getDependentVerb(prob, qs);
			if(schema.verbPhrase != null) {
				schema.verbLemma = prob.lemmas.get(schema.verbPhrase.getEndSpan()-1);
				schema.verb = prob.ta.getToken(schema.verbPhrase.getEndSpan()-1);
				if(schema.verb == null) {
					System.out.println("ISSUE ! No Verb "+prob.question);
				}
			}
			// Find unit of the quantity, for robustness, the related NP chunk is chosen
			Pair<String, Constituent> pair = schema.getUnit(prob, i);
			schema.unit = pair.getFirst();
			schema.quantPhrase = pair.getSecond();
			// Find related NPs, subject of verb and NP PP NP connections
			schema.connectedNPs = schema.getConnectedNPs(prob);
			schema.rateUnit = schema.getRateUnit(prob);
			quantSchemas.add(schema);
		}
		// Fill up missing units 
		for(int i=0; i<quantSchemas.size(); ++i) {
			QuantitySchema qs = quantSchemas.get(i);
			if((qs.unit.trim().equals("")) && i>0) {
				qs.unit = quantSchemas.get(i-1).unit;
			}
			if((qs.unit.trim().equals("")) && 
					(i+1)<quantSchemas.size()) {
				qs.unit = quantSchemas.get(i+1).unit;
			}
		}
		for(int i=0; i<quantSchemas.size(); ++i) {
			QuantitySchema qs = quantSchemas.get(i);
			if((qs.unit.trim().equals("")) && i>0) {
				qs.unit = quantSchemas.get(i-1).unit;
			}
			if((qs.unit.trim().equals("")) && 
					(i+1)<quantSchemas.size()) {
				qs.unit = quantSchemas.get(i+1).unit;
			}
		}
	}
	
	public List<String> getQuestionTokens(Problem prob) {
		List<String> questionTokens = new ArrayList<>();
		// NPs from question
		int questionSentId = Tools.getQuestionSentenceId(prob.ta);
		int start = -1, end = -1;
		for(int i=prob.ta.getSentence(questionSentId).getStartSpan(); 
				i<prob.ta.getSentence(questionSentId).getEndSpan(); ++i) {
			if(prob.ta.getToken(i).equalsIgnoreCase("how")) {
				start = i;
				end = prob.ta.getSentence(questionSentId).getEndSpan();
				for(int j=start; j<prob.ta.getSentence(questionSentId).getEndSpan(); ++j) {
					if(prob.ta.getToken(j).contains(",") || 
							prob.ta.getToken(j).contains("if")) {
						end = j;
						break;
					}
				}
				for(int j=start; j<end; ++j) {
					questionTokens.add(prob.lemmas.get(j));
				}
			}
		}
		return questionTokens;
	}
		
	@Override
	public String toString() {
		String str = "\n";
		str += "Problem : " + problemId + "\n";
		str += "QuestionNPs : " + Arrays.asList(questionTokens) + "\n";
		str += "QuantSchema :\n";
		for(QuantitySchema qs : quantSchemas) { 
			str+=qs+"\n";
		}
		str += "Coref : ";
		for(Integer index : coref.keySet()) {
			 str += index + ":" + coref.get(index)+" ";
		}
		str+="\n";
		return str;
	}
	
	
	public Map<Integer, IntPair> simpleCoref(Problem prob) {
		Map<Integer, IntPair> coref = new HashMap<Integer, IntPair>();
		Set<String> corefMentions = new HashSet<String>();
		corefMentions.addAll(Arrays.asList("he", "she", "him", "her"));
		for(int i=0; i<prob.ta.size(); ++i) {
			if(corefMentions.contains(prob.ta.getToken(i).toLowerCase())) {
				for(Constituent cons : prob.ner) {
					if(cons.getLabel().equals("PER") && cons.getStartSpan() < i) {
						coref.put(i, cons.getSpan());
						break;
					}
				}
			}
		}
		return coref;
	}
	
	
	public static void main(String args[]) throws Exception {
	}
}
