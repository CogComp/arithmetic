package structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import utils.Tools;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;

public class QuantitySchema {
	
	public QuantSpan qs;
	public Constituent quantPhrase;
	public String unit;
	public Constituent verbPhrase;
	public String verb;
	public String verbLemma;
	public List<Constituent> connectedNPs;
	public Constituent subject;
	public Constituent object;
	public Constituent rateUnit;
	
	public QuantitySchema(QuantSpan qs) {
		this.qs= qs; 
		connectedNPs = new ArrayList<>();
	}
	
	@Override
	public String toString() {
		return "Num: "+qs.val+" Subject: "+subject+" Verb: "+verb+" Unit: "+unit +
				" Object: "+object+" Rate : "+rateUnit;
	}
	
	public List<Constituent> getConnectedNPs(Problem prob) {
		List<Constituent> npList = new ArrayList<>();
		List<Constituent> npListQuantRemoved = new ArrayList<>();
		boolean onlyQuantityInSentence = true;
		int sentId = prob.ta.getSentenceFromToken(quantPhrase.getStartSpan()).getSentenceId();
		for(QuantSpan qs : prob.quantities) {
			int tokenId = prob.ta.getTokenIdFromCharacterOffset(qs.start);
			if(prob.ta.getSentenceFromToken(tokenId).getSentenceId() == sentId &&
					!(quantPhrase.getStartSpan()<=tokenId && quantPhrase.getEndSpan()>tokenId)) {
				onlyQuantityInSentence = false;
				break;
			}
		}
		// Find NPs from children of verb
		if(verbPhrase != null) {
			List<Relation> relations = verbPhrase.getOutgoingRelations();
			for(Relation relation : relations) {
				if(!relation.getRelationName().equals("nsubj")) continue;
				Constituent dst = relation.getTarget();
				for(Constituent cons : prob.chunks) {
					if(cons.getStartSpan() <= dst.getStartSpan() &&
							cons.getEndSpan() > dst.getStartSpan() &&
							cons.getLabel().equals("NP") &&
							!npList.contains(cons)) {
						npList.add(cons);
						subject = cons;
						break;
					}
				}
			}
			// Added for object detection
			for(Relation relation : relations) {
				if(relation.getRelationName().equals("iobj") ||
						relation.getRelationName().equals("nmod")) {
					Constituent dst = relation.getTarget();
					for (Constituent cons : prob.chunks) {
						if (cons.getStartSpan() <= dst.getStartSpan() &&
								cons.getEndSpan() > dst.getStartSpan() &&
								cons.getLabel().equals("NP")) {
							object = cons;
							break;
						}
					}
				}
			}
		}
		// Find NPs from PP NP connection
		int quantPhraseId = getChunkIndex(prob, quantPhrase.getStartSpan());
		if(quantPhraseId+2 < prob.chunks.size() && 
				!prob.chunks.get(quantPhraseId+1).getSurfaceForm().trim().equals("of") &&
				prob.chunks.get(quantPhraseId+1).getLabel().equals("PP") &&
				prob.chunks.get(quantPhraseId+2).getLabel().equals("NP") &&
				!npList.contains(prob.chunks.get(quantPhraseId+2))) {
			npList.add(prob.chunks.get(quantPhraseId+2));
		}
		if(quantPhraseId-2 >= 0 && 
				prob.chunks.get(quantPhraseId-1).getLabel().equals("PP") &&
				prob.chunks.get(quantPhraseId-2).getLabel().equals("NP") &&
				!npList.contains(prob.chunks.get(quantPhraseId-2))) {
			npList.add(prob.chunks.get(quantPhraseId-2));
		}
		//Get preceding NP
		if(quantPhraseId-1 >= 0 && 
				prob.chunks.get(quantPhraseId-1).getLabel().equals("NP") &&
				!prob.posTags.get(prob.chunks.get(quantPhraseId-1).getEndSpan())
				.getLabel().equals("CC") &&
				!npList.contains(prob.chunks.get(quantPhraseId-1))) {
			npList.add(prob.chunks.get(quantPhraseId-1));
		}
		//Get succeeding NP
		if(quantPhraseId+1 <prob.chunks.size() && 
				prob.chunks.get(quantPhraseId+1).getLabel().equals("NP") &&
				!prob.posTags.get(prob.chunks.get(quantPhraseId).getEndSpan())
				.getLabel().equals("CC") &&
				!npList.contains(prob.chunks.get(quantPhraseId+1))) {
			npList.add(prob.chunks.get(quantPhraseId+1));
		}
//		 If only quantity in sentence, all NPs are connected
		if(onlyQuantityInSentence) {
			for(int i=0; i<prob.chunks.size(); ++i) {
				Constituent cons = prob.chunks.get(i);
				if(cons.getSentenceId() == sentId && 
						(i>quantPhraseId+2 || i<quantPhraseId-2) && 
						!npList.contains(cons) && 
						cons.getLabel().equals("NP")) {
					npList.add(cons);
				}
			}
		}
		// Remove quantity phrases, subject, and other sentence stuff from npList
		for(Constituent cons : npList) {
			boolean allow = true;
			for(QuantSpan qs : prob.quantities) {
				int index = prob.ta.getTokenIdFromCharacterOffset(qs.start);
				if(index >= cons.getStartSpan() && index < cons.getEndSpan()) {
					allow = false;
					break;
				}
			}
			if(prob.ta.getSentenceFromToken(cons.getStartSpan()).getSentenceId() != sentId) {
				continue;
			}
			if(allow) {
				npListQuantRemoved.add(cons);
			}
		}
		return npListQuantRemoved;
	}
	
	public Constituent getDependentVerb(Problem prob, QuantSpan qs) {
		Constituent result = getDependencyConstituentCoveringTokenId(
				prob, prob.ta.getTokenIdFromCharacterOffset(qs.start));
		if(result == null) {
			System.out.println("Text : "+prob.question+" Token : "+prob.ta.getTokenIdFromCharacterOffset(qs.start));
			Tools.printCons(prob.dependency);
		}
		while(result != null) {
			if(result.getIncomingRelations().size() == 0) break;
//			System.out.println(result.getIncomingRelations().get(0).getSource()+" --> "+result);
			result = result.getIncomingRelations().get(0).getSource();
			if(prob.posTags.get(result.getStartSpan()).getLabel().startsWith("VB")) {
				return result;
			}
		}
		return result;
	}

	public Pair<String, Constituent> getUnit(Problem prob, int quantIndex) {
		String unit = "";
		int tokenId = prob.ta.getTokenIdFromCharacterOffset(
				prob.quantities.get(quantIndex).start);
		int quantPhraseId = getChunkIndex(prob, tokenId);
		Constituent quantPhrase = prob.chunks.get(quantPhraseId); 
		// Detect cases like 4 red and 6 blue balls
		int numQuantInChunk = 0;
		for(QuantSpan qs : prob.quantities) {
			int index = prob.ta.getTokenIdFromCharacterOffset(qs.start);
			if(index >= quantPhrase.getStartSpan() && index < quantPhrase.getEndSpan()) {
				numQuantInChunk++;
			}
		}
		int start = quantPhrase.getStartSpan();
		int end = quantPhrase.getEndSpan();
		boolean addEndNoun = false;
		if(numQuantInChunk > 1) {
			for(int i=quantPhrase.getStartSpan(); i<quantPhrase.getEndSpan(); ++i) {
				if(prob.posTags.get(i).getLabel().equals("CC")) {
					if(tokenId < i) {
						end = i;
						addEndNoun = true;
					} else {
						start = i+1;
					}
					break;
				}
			}
		}
		for(int i=start; i<end; ++i) {
			if(i != tokenId) {
				if(prob.ta.getToken(i).contains("$")) {
					unit += "dollar ";
				} else {
					unit += prob.lemmas.get(i) + " ";
				}
			}
		}
		// Connecting disconnected units, as in, 5 red and 6 green apples 
		if(addEndNoun && quantPhrase.getEndSpan()<=prob.ta.size() && 
				prob.posTags.get(quantPhrase.getEndSpan()-1).getLabel().startsWith("N")) {
			unit += prob.lemmas.get(quantPhrase.getEndSpan()-1)+" ";
		}
		// Unit from neighboring phrases
		if(quantPhraseId+2 < prob.chunks.size() && 
				prob.chunks.get(quantPhraseId+1).getSurfaceForm().trim().equals("of") &&
				prob.chunks.get(quantPhraseId+2).getLabel().equals("NP")) {
			Constituent cons = prob.chunks.get(quantPhraseId+2);
			for(int j=cons.getStartSpan(); j<cons.getEndSpan(); ++j) {
				unit += prob.lemmas.get(j) + " ";
			}
			quantPhraseId += 2;
		}
		return new Pair<String, Constituent>(unit, quantPhrase);
	}
	
	public static Constituent getDependencyConstituentCoveringTokenId(
			Problem prob, int tokenId) {
		for(int i=0; i<=2; ++i) {
			for(Constituent cons : prob.dependency) {
				if(tokenId+i >= cons.getStartSpan() && 
						tokenId+i < cons.getEndSpan()) {
					return cons;
				}
			}
		}
		return null;
	}
	
	public static int getChunkIndex(Problem prob, int tokenId) {
		for(int i=0; i<=2; ++i) {
			for(int j=0; j<prob.chunks.size(); ++j) {
				Constituent cons = prob.chunks.get(j);
				if(tokenId+i >= cons.getStartSpan() && 
						tokenId+i < cons.getEndSpan()) {
					return j;
				}
			}
		}
		return -1;
	}

	public Constituent getRateUnit(Problem prob) {
		for(Constituent cons : connectedNPs) {
			for(String token : cons.getTokenizedSurfaceForm().split(" ")) {
				if(token.toLowerCase().equals("each")) {
					return cons;
				}
			}
		}
		for(Constituent cons : connectedNPs) {
			for(String token : cons.getTokenizedSurfaceForm().split(" ")) {
				if(token.toLowerCase().equals("every")) {
					return cons;
				}
			}
		}
		for(String token : quantPhrase.getTokenizedSurfaceForm().split(" ")) {
			if(token.toLowerCase().equals("every") || token.toLowerCase().equals("each")) {
				return quantPhrase;
			}
		}
		int chunkId = getChunkIndex(prob, quantPhrase.getStartSpan());
		if(chunkId+1<prob.chunks.size() && 
				prob.chunks.get(chunkId+1).getSurfaceForm().equals("each")) {
			return subject;
		}
		if(chunkId+2<prob.chunks.size() && 
				prob.chunks.get(chunkId+1).getSurfaceForm().equals("per")) {
			return prob.chunks.get(chunkId+2);
		}
		if(chunkId+2<prob.chunks.size() && 
				prob.chunks.get(chunkId+1).getSurfaceForm().equals("per")) {
			return prob.chunks.get(chunkId+2);
		}
		if(chunkId+1<prob.chunks.size() && 
				prob.chunks.get(chunkId+1).getSurfaceForm().startsWith("per ") &&
				prob.chunks.get(chunkId+1).getStartSpan() == prob.chunks.get(chunkId).getEndSpan()) {
			return prob.chunks.get(chunkId+1);
		}
		if(chunkId+2<prob.chunks.size() && 
				prob.chunks.get(chunkId+1).getSurfaceForm().equals("a")) {
			return prob.chunks.get(chunkId+2);
		}
		if(chunkId+1<prob.chunks.size() && 
				prob.chunks.get(chunkId+1).getSurfaceForm().startsWith("a ") &&
				prob.chunks.get(chunkId+1).getStartSpan() == prob.chunks.get(chunkId).getEndSpan()) {
			return prob.chunks.get(chunkId+1);
		}
		if(chunkId+2<prob.chunks.size() && 
				prob.chunks.get(chunkId+1).getSurfaceForm().equals("an")) {
			return prob.chunks.get(chunkId+2);
		}
		if(chunkId+1<prob.chunks.size() && 
				prob.chunks.get(chunkId+1).getSurfaceForm().startsWith("an ") &&
				prob.chunks.get(chunkId+1).getStartSpan() == prob.chunks.get(chunkId).getEndSpan()) {
			return prob.chunks.get(chunkId+1);
		}
		if(chunkId-2 >= 0 && 
				prob.chunks.get(chunkId-1).getSurfaceForm().equals("of") && 
				prob.posTags.get(prob.chunks.get(chunkId-2).getEndSpan()-1)
				.getLabel().equals("NNS")) {
			return prob.chunks.get(chunkId-2);
		}
		Sentence sent = prob.ta.getSentenceFromToken(quantPhrase.getStartSpan());
		for(int i=1;i<5; ++i) {
			if(chunkId+i<prob.chunks.size() && 
					prob.chunks.get(chunkId+i).getSentenceId() == sent.getSentenceId()) {
				for(String token : prob.chunks.get(chunkId+i).getTokenizedSurfaceForm().split(" ")) {
					if(token.toLowerCase().equals("every") || token.toLowerCase().equals("each")) {
						return prob.chunks.get(chunkId+i);
					}
				}
			}
		}
		Set<String> candidateUnits = new HashSet<>();
		for(QuantSpan qs : prob.quantities) {
			int tokenId = prob.ta.getTokenIdFromCharacterOffset(qs.start);
			if(tokenId+1<prob.lemmas.size()) {
				candidateUnits.add(prob.lemmas.get(tokenId+1));
			}
		}
		for(int i=0; i<prob.ta.size()-3; ++i) {
			if(prob.ta.getToken(i).equalsIgnoreCase("how") || 
					prob.ta.getToken(i+1).equalsIgnoreCase("many")) {
				if(prob.ta.getToken(i+2).equalsIgnoreCase("many")) {
					candidateUnits.add(prob.lemmas.get(i+3));
				} else {
					candidateUnits.add(prob.lemmas.get(i+2));
				}
			}
		}
		for(int i=1;i<5; ++i) {
			if(chunkId+i<prob.chunks.size() && 
					prob.chunks.get(chunkId+i).getSentenceId() == sent.getSentenceId()) {
				String strArr[] = prob.chunks.get(chunkId+i).getTokenizedSurfaceForm().split(" ");
				for(int j=0; j<strArr.length-1; ++j) {
					if((strArr[j].equalsIgnoreCase("a") || strArr[j].equalsIgnoreCase("an") || 
							strArr[j].equalsIgnoreCase("each")) 
							&& candidateUnits.contains(strArr[j+1])) {
						return prob.chunks.get(chunkId+i);
					}
				}
			}
			if(chunkId-i>=0 && 
					prob.chunks.get(chunkId-i).getSentenceId() == sent.getSentenceId()) {
				String strArr[] = prob.chunks.get(chunkId-i).getTokenizedSurfaceForm().split(" ");
				for(int j=0; j<strArr.length-1; ++j) {
					if((strArr[j].equalsIgnoreCase("a") || strArr[j].equalsIgnoreCase("an") || 
							strArr[j].equalsIgnoreCase("each")) 
							&& candidateUnits.contains(strArr[j+1])) {
						return prob.chunks.get(chunkId-i);
					}
				}
			}
		}
		return null;
	}
}
