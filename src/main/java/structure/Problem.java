package structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javatools.parsers.PlingStemmer;
import utils.Tools;
import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;

public class Problem {

	public int id;
	public String question;
	public TextAnnotation ta;
	public Double answer;
	public List<QuantSpan> quantities;
	public Node expr;
	public List<String> stems;
	public List<String> lemmas;
	public List<Constituent> posTags;
	public List<Constituent> chunks;
	public List<Constituent> dependency;
	public List<Constituent> ner;
	public Schema schema;
	public List<Integer> rates;
	
	@Override
	public String toString() {
		String str = "";
		str += "\nQuestion : "+question+"\nAnswer : "+answer +
				"\nQuantities : "+Arrays.asList(quantities) + 
				"\nExpression : "+expr;
		return str;
	}
 	
	public Problem(int id, String q, double a) throws AnnotatorException {
		this.id = id;
		question = q;
		ta = Tools.pipeline.createAnnotatedTextAnnotation("", "", q);
		answer = a;
		quantities = new ArrayList<>();
	}

	public void extractQuantities() throws Exception {
		quantities = Tools.quantifier.getSpans(question);
	}
	
	public void extractAnnotations() throws Exception {
		posTags = ta.getView(ViewNames.POS).getConstituents();
		chunks = ta.getView(ViewNames.SHALLOW_PARSE).getConstituents();
		dependency = ta.getView(ViewNames.DEPENDENCY_STANFORD).getConstituents();
		ner = ta.getView(ViewNames.NER_CONLL).getConstituents();
		stems = new ArrayList<>();
		for(String token : ta.getTokens()) {
			stems.add(PlingStemmer.stem(token));
		}
		lemmas = new ArrayList<>();
		for(Constituent cons : ta.getView(ViewNames.LEMMA).getConstituents()) {
			lemmas.add(cons.getLabel());
		}
		// Fix some chunker issues
		chunks = fixChunkerIssues(chunks);
		schema = new Schema(this);
	}
	
	public List<Constituent> fixChunkerIssues(List<Constituent> chunks) {
		List<Constituent> newChunks = new ArrayList<>();
		for(int i=0; i<chunks.size(); ++i) {
			Constituent chunk = chunks.get(i);
			if(i+1<chunks.size() && chunks.get(i+1).getSurfaceForm().startsWith("'s")) {
				newChunks.add(new Constituent("NP", null, ta, chunk.getStartSpan(), 
						chunks.get(i+1).getEndSpan()));
				i++;
				continue;
			}
			boolean hasQuant = false, doneSplit = false; int quantToken = -1;
			for(QuantSpan qs : quantities) {
				if(ta.getTokenIdFromCharacterOffset(qs.start) >= chunk.getStartSpan() && 
						ta.getTokenIdFromCharacterOffset(qs.start) < chunk.getEndSpan()) {
					hasQuant = true;
					quantToken = ta.getTokenIdFromCharacterOffset(qs.start);
				}
			}
			if(hasQuant) {
				if(ta.getToken(chunk.getStartSpan()).equals("him") || ta.getToken(chunk.getStartSpan()).equals("her")
						|| ta.getToken(chunk.getStartSpan()).equals("his")) {
					newChunks.add(new Constituent("NP", null, ta, chunk.getStartSpan(), 
							quantToken));
					newChunks.add(new Constituent("NP", null, ta, quantToken, 
							chunk.getEndSpan()));
					doneSplit = true;
					continue;
				}
				for(Constituent cons : ner) {
					if(cons.getStartSpan() == chunk.getStartSpan() && quantToken > cons.getStartSpan()) {
						newChunks.add(new Constituent("NP", null, ta, chunk.getStartSpan(), 
								quantToken));
						newChunks.add(new Constituent("NP", null, ta, quantToken, 
								chunk.getEndSpan()));
						doneSplit = true;
						break;
					}
				}
			}
			if(doneSplit) continue;
			newChunks.add(new Constituent(chunk.getLabel(), null, ta, chunk.getStartSpan(), chunk.getEndSpan()));
		}
		return newChunks;
	}
	
}
