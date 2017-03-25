package logic;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import structure.Problem;
import structure.QuantSpan;
import structure.QuantitySchema;
import structure.Schema;

import java.util.List;

public class LogicX implements IInstance {
	
	public int problemId;
	public int quantIndex1;
	public int quantIndex2;
	public TextAnnotation ta;
	public List<QuantSpan> quantities;
	public List<Constituent> posTags;
	public List<Constituent> chunks;
	public List<Constituent> dependency;
	public List<String> lemmas;
	public Schema schema;
	
	public LogicX(Problem prob, int quantIndex1, int quantIndex2) {
		this.problemId = prob.id;
		this.quantIndex1 = quantIndex1;
		this.quantIndex2 = quantIndex2;
		this.ta = prob.ta;
		this.quantities = prob.quantities;
		this.posTags = prob.posTags;
		this.chunks = prob.chunks;
		this.schema = prob.schema;
		this.lemmas = prob.lemmas;
	}
}
