package pair;


import edu.illinois.cs.cogcomp.sl.core.IStructure;

public class PairY implements IStructure {
	
	public String label;
	
	public PairY(String label) {
		this.label = label;
	}
	
	public PairY(PairY other) {
		this.label = other.label;
	}
	
	@Override
	public String toString() {
		return label;
	}
	
	public static float getLoss(PairY gold, PairY pred) {
		if(gold.label.equals(pred.label)) {
			return 0.0f;
		}
		return 1.0f;
	}

}
