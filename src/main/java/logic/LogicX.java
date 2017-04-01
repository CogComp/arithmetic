package logic;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.semgraph.SemanticGraph;
import structure.QuantSpan;
import structure.StanfordProblem;
import structure.StanfordSchema;
import utils.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	// Span of question within the sentence. You also need to know sentenceId
	// to get the text of questionSpan
	public IntPair questionSpan;

	// The following has one item for each of the 3 modes
	public List<Integer> sentIds;
	public List<Integer> tokenIds;
	public List<StanfordSchema> relevantSchemas;

	public LogicX(StanfordProblem prob, int quantIndex1, int quantIndex2) {
		this.problemId = prob.id;
		this.quantIndex1 = quantIndex1;
		this.quantIndex2 = quantIndex2;
		this.text = prob.question;
		this.quantities = prob.quantities;
		this.tokens = prob.tokens;
		this.dependencies = prob.dependencies;
		this.schema = prob.schema;
		this.questionSchema = prob.questionSchema;
		sentIds = new ArrayList<>();
		sentIds.add(questionSchema.sentId);
		sentIds.add(Tools.getSentenceIdFromCharOffset(
				tokens, quantities.get(quantIndex1).start));
		sentIds.add(Tools.getSentenceIdFromCharOffset(
				tokens, quantities.get(quantIndex2).start));
		tokenIds = new ArrayList<>();
		tokenIds.add(-1);
		tokenIds.add(Tools.getTokenIdFromCharOffset(tokens.get(sentIds.get(1)),
				quantities.get(quantIndex1).start));
		tokenIds.add(Tools.getTokenIdFromCharOffset(tokens.get(sentIds.get(2)),
				quantities.get(quantIndex2).start));
		relevantSchemas = new ArrayList<>();
		relevantSchemas.add(questionSchema);
		relevantSchemas.add(schema.get(quantIndex1));
		relevantSchemas.add(schema.get(quantIndex2));
		if (sentIds.get(0) >= 0) {
			questionSpan = StanfordProblem.getQuestionSpan(tokens.get(sentIds.get(0)));
		} else {
			questionSpan = new IntPair(-1, -1);
		}
	}

	@Override
	public String toString() {
		String str = "";
		str += quantIndex1 + " " + quantIndex2 + "\n";
		str += Arrays.asList(sentIds) + "\n";
		str += Arrays.asList(relevantSchemas) + "\n";
		return str;
	}
}
