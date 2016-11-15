package structure;
import java.io.*;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;


public class QuantSpan implements Serializable{
	
	private static final long serialVersionUID = -5787092712439863265L;
	public int start, end;
	public double val;

	public QuantSpan(double val, int start, int end) {
		this.val = val;
		this.start = start;
		this.end = end;
	}
	
	public String toString() {
		IntPair ip = new IntPair(start, end);
		return val+":"+ip;
	}
}