package logic;

import edu.illinois.cs.cogcomp.sl.core.IStructure;

public class LogicY implements IStructure {
	
	public String label;
	public String key;

	public LogicY(String label, String key) {
		this.label = label;
		this.key = key;
	}
	
	@Override
	public String toString() {
		return label+"_"+key;
	}
	
	public static float getLoss(LogicY gold, LogicY pred) {
		if(gold == null || pred == null) {
			return 1.0f;
		}
		if(gold.label.equals(pred.label)) {
			return 0.0f;
		}
		return 1.0f;
	}

}
