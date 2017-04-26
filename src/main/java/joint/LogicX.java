package joint;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.semgraph.SemanticGraph;
import structure.Node;
import structure.QuantSpan;
import structure.StanfordProblem;
import structure.StanfordSchema;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogicX implements IInstance {
	
	public int problemId;
	public String text;
	public List<QuantSpan> quantities;
	public List<List<CoreLabel>> tokens;
	public List<SemanticGraph> dependencies;
	public List<StanfordSchema> schema;
	public StanfordSchema questionSchema;
	public IntPair questionSpan;
	public Map<Pair<String, String>, String> wordnetRelations;
	public Set<Integer> relevantQuantIndices;

	public LogicX(StanfordProblem prob) {
		this.problemId = prob.id;
		this.text = prob.question;
		this.quantities = prob.quantities;
		this.tokens = prob.tokens;
		this.dependencies = prob.dependencies;
		this.schema = prob.schema;
		this.questionSchema = prob.questionSchema;
		this.questionSpan = StanfordProblem.getQuestionSpan(
				prob.tokens.get(questionSchema.sentId));
		this.wordnetRelations = prob.wordnetRelations;
		this.relevantQuantIndices = new HashSet<>();
		if(prob.expr != null) {
			for (Node leaf : prob.expr.getLeaves()) {
				relevantQuantIndices.add(leaf.quantIndex);
			}
		}
	}

	public LogicX(LogicX prob) {
		this.problemId = prob.problemId;
		this.text = prob.text;
		this.quantities = prob.quantities;
		this.tokens = prob.tokens;
		this.dependencies = prob.dependencies;
		this.schema = prob.schema;
		this.questionSchema = prob.questionSchema;
		this.questionSpan = StanfordProblem.getQuestionSpan(
				prob.tokens.get(questionSchema.sentId));
		this.wordnetRelations = prob.wordnetRelations;
		this.relevantQuantIndices = prob.relevantQuantIndices;
	}

}
