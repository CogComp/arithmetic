package logic;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.semgraph.SemanticGraph;
import structure.QuantSpan;
import structure.StanfordProblem;
import structure.StanfordSchema;

import java.util.List;
import java.util.Map;

public class LogicX implements IInstance {
	
	public int problemId;
	public int quantIndex1;
	public int quantIndex2;
	public String text;
	public List<QuantSpan> quantities;
	public List<List<CoreLabel>> tokens;
	public List<SemanticGraph> dependencies;
	public List<StanfordSchema> schema;
	public StanfordSchema questionSchema;
	public Map<Pair<String, String>, String> wordnetRelations;
	public StanfordSchema num1, num2;
	public String infType;
	public boolean isTopmost;
	public String mathOp;

	// Span of question within the sentence. You also need to know sentenceId
	// to get the text of questionSpan
	public IntPair questionSpan;

	public LogicX(StanfordProblem prob, int quantIndex1, int quantIndex2,
				  String mathOp, String infType, boolean isTopmost) {
		this.problemId = prob.id;
		this.quantIndex1 = quantIndex1;
		this.quantIndex2 = quantIndex2;
		this.text = prob.question;
		this.quantities = prob.quantities;
		this.tokens = prob.tokens;
		this.dependencies = prob.dependencies;
		this.schema = prob.schema;
		this.questionSchema = prob.questionSchema;
		this.questionSpan = StanfordProblem.getQuestionSpan(
				tokens.get(questionSchema.sentId));
		this.wordnetRelations = prob.wordnetRelations;
		this.infType = infType;
		this.mathOp = mathOp;
		this.isTopmost = isTopmost;
		this.num1 = schema.get(quantIndex1);
		this.num2 = schema.get(quantIndex2);
	}
}
