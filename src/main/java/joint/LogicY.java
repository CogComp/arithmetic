package joint;

import edu.illinois.cs.cogcomp.sl.core.IStructure;
import structure.Node;


public class LogicY implements IStructure {
	
	public Node expr;
	
	public LogicY(Node expr) {
		this.expr = expr;
	}
	
	public LogicY(LogicY other) {
		this.expr = other.expr;
	}
	
	@Override
	public String toString() {
		return expr.toString();
	}
	
	public static float getLoss(LogicY gold, LogicY pred) {
		if(gold == null || pred == null) {
			return 1.0f;
		}
		return Node.getLoss(gold.expr, pred.expr);
	}

}
