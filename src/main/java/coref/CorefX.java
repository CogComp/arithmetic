package coref;

import edu.illinois.cs.cogcomp.sl.core.IInstance;
import structure.StanfordProblem;

public class CorefX extends logic.LogicX implements IInstance {

	public int quantIndex1;
	public int quantIndex2;
	public String infType;

	public CorefX(StanfordProblem prob, int quantIndex1, int quantIndex2, String infType) {
		super(prob);
		this.quantIndex1 = quantIndex1;
		this.quantIndex2 = quantIndex2;
		this.infType = infType;
	}

	public CorefX(logic.LogicX prob, int quantIndex1, int quantIndex2, String infType) {
		super(prob);
		this.quantIndex1 = quantIndex1;
		this.quantIndex2 = quantIndex2;
		this.infType = infType;
	}
}
