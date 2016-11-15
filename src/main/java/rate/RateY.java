package rate;


import edu.illinois.cs.cogcomp.sl.core.IStructure;

public class RateY implements IStructure {
	
	public String label;
	
	public RateY(String label) {
		this.label = label;
	}
	
	public RateY(RateY other) {
		this.label = other.label;
	}
	
	@Override
	public String toString() {
		return label;
	}
	
	public static float getLoss(RateY gold, RateY pred) {
		if(gold.label.equals(pred.label)) {
			return 0.0f;
		}
		return 1.0f;
	}

}
