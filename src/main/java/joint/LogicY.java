package joint;

import edu.illinois.cs.cogcomp.sl.core.IStructure;
import structure.Node;
import structure.StanfordSchema;
import utils.Tools;

import java.util.List;

public class LogicY implements IStructure {
	
	public Node expr;
	public List<StanfordSchema> extractions;
	
	public LogicY(Node expr, List<StanfordSchema> extractions) {
		this.expr = expr;
		this.extractions = extractions;
	}
	
	public LogicY(LogicY other) {
		this.expr = other.expr;
		this.extractions = other.extractions;
	}
	
	@Override
	public String toString() {
		String str = "\n";
		if(extractions != null) {
			for (StanfordSchema inp : extractions) {
				str += inp.toString() + "\n";
			}
		}
		return str + "Expression: "+expr.toString()+"\n";
	}
	
	public static float getLoss(LogicY gold, LogicY pred) {
		if(gold == null || pred == null) {
			return 1.0f;
		}
		if(Tools.safeEquals(gold.expr.getValue(), pred.expr.getValue())) {
			return 0.0f;
		}
		if(Tools.safeEquals(-gold.expr.getValue(), pred.expr.getValue())) {
			return 0.0f;
		}
		return 1.0f;
	}

}
