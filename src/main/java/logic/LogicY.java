package logic;


import edu.illinois.cs.cogcomp.sl.core.IStructure;

public class LogicY implements IStructure {
	
	public String label;
	public int inferenceRule;
	public LogicInput num1, num2, ques;
	
	public LogicY(String label, int inferenceRule, LogicInput num1,
				  LogicInput num2, LogicInput ques) {
		this.label = label;
		this.inferenceRule = inferenceRule;
		this.num1 = num1;
		this.num2 = num2;
		this.ques = ques;
	}
	
	public LogicY(LogicY other) {
		this.label = other.label;
		this.inferenceRule = other.inferenceRule;
		this.num1 = other.num1;
		this.num2 = other.num2;
		this.ques = other.ques;
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
