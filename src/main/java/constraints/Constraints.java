package constraints;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import utils.Tools;

public class Constraints {
	
	public static boolean isPositive(Double val) {
		if(val < 0.0001) return false;
		return true;
	}
	
	public static boolean isInteger(TextAnnotation ta, Double val) {
		if(ta.getText().contains("How many") || ta.getText().contains("how many")) {
			if(!Tools.safeEquals(val, val.intValue()*1.0) &&
					!Tools.safeEquals(val, val.intValue()*1.0+1.0)) {
				return false;
			}
		}
		return true;
	}
	
	

}
