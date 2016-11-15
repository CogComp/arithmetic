package structure;

import java.util.Comparator;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;

public abstract class PairComparator<A> implements Comparator<Pair<A, Double>> {

	public int compare(Pair<A,Double> pair1, Pair<A,Double> pair2) {
		if(pair1.getSecond() == pair2.getSecond()) return 0;
		return (pair1.getSecond() < pair2.getSecond()) ? 1 : -1;
	}
}
