package joint;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.semgraph.SemanticGraph;
import structure.QuantSpan;
import structure.StanfordProblem;
import structure.StanfordSchema;
import java.util.List;

public class LogicX implements IInstance {
	
	public int problemId;
	public String text;
	public List<QuantSpan> quantities;
	public List<List<CoreLabel>> tokens;
	public List<SemanticGraph> dependencies;
	public List<StanfordSchema> schema;
	public StanfordSchema questionSchema;

	// Span of question within the sentence. You also need to know sentenceId
	// to get the text of questionSpan
	public IntPair questionSpan;

	// The following has one item for each of the 3 modes
	public List<Integer> sentIds;
	public List<Integer> tokenIds;
	public List<StanfordSchema> relevantSchemas;

	public LogicX(StanfordProblem prob) {
		this.problemId = prob.id;
		this.text = prob.question;
		this.quantities = prob.quantities;
		this.tokens = prob.tokens;
		this.dependencies = prob.dependencies;
		this.schema = prob.schema;
		this.questionSchema = prob.questionSchema;
	}

}
