package run;


import edu.illinois.cs.cogcomp.sl.core.IStructure;

public class RunY implements IStructure {
	
	public String label;
	
	public RunY(String label) {
		this.label = label;
	}
	
	public RunY(RunY other) {
		this.label = other.label;
	}
	
	@Override
	public String toString() {
		return label;
	}
	
	public static float getLoss(RunY gold, RunY pred) {
		if(gold.label.equals(pred.label)) {
			return 0.0f;
		}
		return 1.0f;
	}

}
