package structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import logic.Logic;
import utils.Tools;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;

public class Schema {
	
	public int problemId;
	public Map<Integer, IntPair> coref;
	public List<String> questionTokens;
	public List<QuantitySchema> quantSchemas;
	public QuantitySchema questionSchema;
	
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
			schema.math = schema.getMath(prob);
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
		createQuestionSchema(prob);
	}

	public IntPair getQuestionSpan(Problem prob) {
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
				return new IntPair(start, end);
			}
		}
		return new IntPair(start, end);
	}

	public List<String> getQuestionTokens(Problem prob) {
		List<String> questionTokens = new ArrayList<>();
		IntPair quesSpan = getQuestionSpan(prob);
		for(int j=quesSpan.getFirst(); j<quesSpan.getSecond(); ++j) {
			questionTokens.add(prob.lemmas.get(j));
		}
		return questionTokens;
	}

	public void createQuestionSchema(Problem prob) {
		IntPair quesSpan = getQuestionSpan(prob);
		questionSchema = new QuantitySchema(null);
		Constituent result = QuantitySchema.getDependencyConstituentCoveringTokenId(
				prob, quesSpan.getFirst());
		while(result != null) {
			if(result.getIncomingRelations().size() == 0) break;
			result = result.getIncomingRelations().get(0).getSource();
			if(prob.posTags.get(result.getStartSpan()).getLabel().startsWith("VB")) {
				questionSchema.verbPhrase = result;
				questionSchema.verbLemma = prob.lemmas.get(questionSchema.verbPhrase.getEndSpan()-1);
				questionSchema.verb = prob.ta.getToken(questionSchema.verbPhrase.getEndSpan()-1);
			}
		}
		if(questionSchema.verbPhrase != null) {
			List<Relation> relations = questionSchema.verbPhrase.getOutgoingRelations();
			for (Relation relation : relations) {
				if (!relation.getRelationName().equals("nsubj")) continue;
				Constituent dst = relation.getTarget();
				for (Constituent cons : prob.chunks) {
					if (cons.getStartSpan() <= dst.getStartSpan() &&
							cons.getEndSpan() > dst.getStartSpan() &&
							cons.getLabel().equals("NP")) {
						questionSchema.subject = cons;
						break;
					}
				}
			}
			// Added for object detection
			for (Relation relation : relations) {
				if (relation.getRelationName().equals("iobj") ||
						relation.getRelationName().equals("nmod")) {
					Constituent dst = relation.getTarget();
					for (Constituent cons : prob.chunks) {
						if (cons.getStartSpan() <= dst.getStartSpan() &&
								cons.getEndSpan() > dst.getStartSpan() &&
								cons.getLabel().equals("NP")) {
							questionSchema.object = cons;
							break;
						}
					}
				}
			}
		}
		questionSchema.unit = "";
		int chunkId = QuantitySchema.getChunkIndex(prob, quesSpan.getFirst());
		Constituent unitCons = prob.chunks.get(chunkId);
		for(int i=unitCons.getStartSpan(); i<unitCons.getEndSpan(); ++i) {
			questionSchema.unit += prob.lemmas.get(i) + " ";
		}
		for(int i=quesSpan.getFirst(); i<quesSpan.getSecond(); ++i) {
			if(prob.ta.getToken(i).equalsIgnoreCase("each") ||
					prob.ta.getToken(i).equalsIgnoreCase("per") ||
					prob.ta.getToken(i).equalsIgnoreCase("every")) {
				chunkId = QuantitySchema.getChunkIndex(prob, i+1);
				questionSchema.rateUnit = prob.chunks.get(chunkId);
			}
		}

		for(int i=quesSpan.getFirst(); i<quesSpan.getSecond(); ++i) {
			if(Logic.addTokens.contains(prob.ta.getToken(i))) {
				questionSchema.math = "ADD";
			}
			if(Logic.subTokens.contains(prob.ta.getToken(i))) {
				questionSchema.math = "SUB";
			}
			if(Logic.mulTokens.contains(prob.ta.getToken(i))) {
				questionSchema.math = "MUL";
			}
		}
	}
		
	@Override
	public String toString() {
		String str = "\n";
		str += "Problem : " + problemId + "\n";
		str += "QuestionNPs : " + Arrays.asList(questionTokens) + "\n";
		str += "QuantSchema :\n";
		for (QuantitySchema qs : quantSchemas) {
			str += qs + "\n";
		}
		str += "\n";
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

}
