package graph;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;
import structure.PairComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GraphInfSolver extends AbstractInferenceSolver {

    private GraphFeatGen featGen;

    public GraphInfSolver(GraphFeatGen featGen) {
        this.featGen = featGen;
    }

    @Override
    public IStructure getBestStructure(WeightVector weightVector, IInstance iInstance)
            throws Exception {
        return getLossAugmentedBestStructure(weightVector, iInstance, null);
    }

    @Override
    public IStructure getLossAugmentedBestStructure(
            WeightVector weightVector, IInstance iInstance, IStructure iStructure)
            throws Exception {
        boolean useConstraints = true;
        GraphX x = (GraphX) iInstance;
        PairComparator<List<String>> nodePairComparator =
                new PairComparator<List<String>>() {};
        MinMaxPriorityQueue<Pair<List<String>, Double>> beam1 =
                MinMaxPriorityQueue.orderedBy(nodePairComparator)
                        .maximumSize(200).create();
        MinMaxPriorityQueue<Pair<List<String>, Double>> beam2 =
                MinMaxPriorityQueue.orderedBy(nodePairComparator)
                        .maximumSize(200).create();
        int n = x.relevantQuantIndices.size();
        List<String> init = new ArrayList<>();
        for(int i=0; i<n; ++i) {
            init.add("NOT_RATE");
        }
        List<String> labels = new ArrayList<>();
        labels.addAll(init);
        beam1.add(new Pair<>(labels, 0.0));
        for(int i=0; i<n; ++i) {
            labels = new ArrayList<>();
            labels.addAll(init);
            labels.set(i, "RATE");
            beam1.add(new Pair<>(labels, 1.0*weightVector.dotProduct(
                    featGen.getRateFeatureVector(x, i, "RATE"))));
            for(int j=i+1; j<n; ++j) {
                labels = new ArrayList<>();
                labels.addAll(init);
                labels.set(i, "RATE");
                labels.set(j, "RATE");
                beam1.add(new Pair<>(labels, 1.0*weightVector.dotProduct(
                        featGen.getRunFeatureVector(x, i, j, "RATE"))));
            }
        }
        for(int i=0; i<n-1; ++i) {
            for(int j=i+1; j<n; ++j) {
                for(String label : Arrays.asList(
                        "SAME_UNIT", "1_RATE_1", "1_RATE_2",
                        "2_RATE_1", "2_RATE_2", "NO_REL")) {
                    for(Pair<List<String>, Double> pair : beam1) {
                        labels = new ArrayList<>();
                        labels.addAll(pair.getFirst());
                        labels.add(label);
                        if(useConstraints && !constraints.GraphInfSolver.satisfyConstraints(
                                labels.get(i),
                                labels.get(j),
                                label
                        )) continue;
                        beam2.add(new Pair<>(labels,
                                pair.getSecond() + 1.0*weightVector.dotProduct(
                                        featGen.getRunFeatureVector(x, i, j, label))));
                    }
                }
                beam1.clear();
                beam1.addAll(beam2);
                beam2.clear();
            }
        }
        assert beam1.element().getFirst().size() == n+n*(n-1)/2;
        return new GraphY(beam1.element().getFirst());
    }

    @Override
    public float getLoss(IInstance iInstance, IStructure iStructure, IStructure iStructure1) {
        return GraphY.getLoss((GraphY) iStructure, (GraphY) iStructure1);
    }
}
