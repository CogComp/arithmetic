package graph;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import structure.Problem;
import structure.QuantSpan;
import structure.Schema;

import java.util.List;

public class GraphX implements IInstance {

    public int problemId;
    public TextAnnotation ta;
    public List<QuantSpan> quantities;
    public List<Constituent> posTags;
    public List<Constituent> chunks;
    public List<String> lemmas;
    public Schema schema;
    public List<Integer> relevantQuantIndices;

    public GraphX(Problem prob, List<Integer> relevantQuantIndices) {
        this.problemId = prob.id;
        this.ta = prob.ta;
        this.quantities = prob.quantities;
        this.posTags = prob.posTags;
        this.chunks = prob.chunks;
        this.schema = prob.schema;
        this.lemmas = prob.lemmas;
        this.relevantQuantIndices = relevantQuantIndices;
    }
}
