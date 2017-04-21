package logic;

import edu.illinois.cs.cogcomp.sl.core.IInstance;
import structure.StanfordProblem;

public class LogicX extends joint.LogicX implements IInstance {

	public int quantIndex1;
	public int quantIndex2;
	public String infType;

	public LogicX(StanfordProblem prob, int quantIndex1, int quantIndex2, String infType) {
		super(prob);
		this.quantIndex1 = quantIndex1;
		this.quantIndex2 = quantIndex2;
		this.infType = infType;
	}

	public LogicX(joint.LogicX prob, int quantIndex1, int quantIndex2, String infType) {
		super(prob);
		this.quantIndex1 = quantIndex1;
		this.quantIndex2 = quantIndex2;
		this.infType = infType;
	}
}
