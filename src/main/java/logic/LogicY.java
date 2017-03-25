package logic;


import edu.illinois.cs.cogcomp.sl.core.IStructure;

public class LogicY implements IStructure {
	
	public String label;
	public int inferenceRule;
	
	public LogicY(String label, int inferenceRule) {
		this.label = label;
		this.inferenceRule = inferenceRule;
	}
	
	public LogicY(LogicY other) {
		this.label = other.label;
		this.inferenceRule = other.inferenceRule;
	}
	
	@Override
	public String toString() {
		return label+" "+inferenceRule;
	}
	
	public static float getLoss(LogicY gold, LogicY pred) {
		if(gold.label.equals(pred.label)) {
			return 0.0f;
		}
		return 1.0f;
	}

}
