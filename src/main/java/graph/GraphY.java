package graph;

import edu.illinois.cs.cogcomp.sl.core.IStructure;

import java.util.Arrays;
import java.util.List;

public class GraphY implements IStructure {

    // Labels only for relevant quantities, and for question
    // Order : Vertex label for each quantity, followed by vertex label of question,
    // next, edge labels for all edges (following order of for loop on the above
    // vertex order)
    public List<String> labels;

    public GraphY(List<String> labels) {
        this.labels = labels;
    }

    public static float getLoss(GraphY y1, GraphY y2) {
        if((""+ Arrays.asList(y1.labels)).equals((""+ Arrays.asList(y2.labels)))) {
            return 0.0f;
        }
        return 1.0f;
    }

    @Override
    public String toString() {
        return ""+Arrays.asList(labels);
    }
}
