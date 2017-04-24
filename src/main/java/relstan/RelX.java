package relstan;

import edu.illinois.cs.cogcomp.sl.core.IInstance;
import structure.StanfordProblem;

public class RelX extends joint.LogicX implements IInstance {

	public int quantIndex;

	public RelX(StanfordProblem prob, int quantIndex) {
		super(prob);
		this.quantIndex = quantIndex;
	}
	
}
