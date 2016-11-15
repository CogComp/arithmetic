package relevance;

import java.util.List;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import structure.Problem;
import structure.QuantSpan;
import structure.Schema;

public class RelX implements IInstance {
	
	public int problemId;
	public int quantIndex;
	public TextAnnotation ta;
	public List<QuantSpan> quantities;
	public List<Constituent> posTags;
	public List<Constituent> chunks;
	public List<Constituent> parse;
	public List<Constituent> dependency;
	public List<String> lemmas;
	public Schema schema;
	
	public RelX(Problem prob, int quantIndex) {
		this.problemId = prob.id;
		this.quantIndex = quantIndex;
		this.ta = prob.ta;
		this.quantities = prob.quantities;
		this.posTags = prob.posTags;
		this.chunks = prob.chunks;
		this.lemmas = prob.lemmas; 
		this.schema = prob.schema;
	}
	
}
