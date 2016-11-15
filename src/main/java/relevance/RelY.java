package relevance;

import edu.illinois.cs.cogcomp.sl.core.IStructure;

public class RelY implements IStructure {
	
	public String relevance;
	
	public RelY(String relevance) {
		this.relevance = relevance;
	}
	
	public RelY(RelY other) {
		this.relevance = other.relevance;
	}
	
	@Override
	public String toString() {
		return relevance;
	}
	
	public static float getLoss(RelY gold, RelY pred) {
		if(gold.relevance.equals(pred.relevance)) {
			return 0.0f;
		}
		return 1.0f;
	}

}
