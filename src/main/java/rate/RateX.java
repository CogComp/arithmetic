package rate;

import java.util.List;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import graph.GraphX;
import structure.Problem;
import structure.QuantSpan;
import structure.Schema;

public class RateX implements IInstance {
	
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
	
	public RateX(Problem prob, int quantIndex) {
		this.problemId = prob.id;
		this.quantIndex = quantIndex;
		this.ta = prob.ta;
		this.quantities = prob.quantities;
		this.posTags = prob.posTags;
		this.chunks = prob.chunks;
		this.schema = prob.schema;
		this.lemmas = prob.lemmas;
	}

	public RateX(GraphX prob, int quantIndex) {
		this.problemId = prob.problemId;
		this.quantIndex = quantIndex;
		this.ta = prob.ta;
		this.quantities = prob.quantities;
		this.posTags = prob.posTags;
		this.chunks = prob.chunks;
		this.schema = prob.schema;
		this.lemmas = prob.lemmas;
	}

}
