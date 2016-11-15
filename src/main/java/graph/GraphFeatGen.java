package graph;

import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import rate.RateFeatGen;
import rate.RateX;
import run.RunFeatGen;
import run.RunX;
import utils.FeatGen;

import java.util.ArrayList;
import java.util.List;

public class GraphFeatGen extends AbstractFeatureGenerator {

    public Lexiconer lm = null;

    public GraphFeatGen(Lexiconer lm) {
        this.lm = lm;
    }

    @Override
    public IFeatureVector getFeatureVector(IInstance iInstance, IStructure iStructure) {
        GraphX x = (GraphX) iInstance;
        GraphY y = (GraphY) iStructure;
        return FeatGen.getFeatureVectorFromListString(getFeatures(x, y), lm);
    }

    public IFeatureVector getRateFeatureVector(
            GraphX x, int relevantQuantIndex, String label) {
        return FeatGen.getFeatureVectorFromListString(
                getRateFeatures(x, relevantQuantIndex, label), lm);
    }

    public IFeatureVector getRunFeatureVector(
            GraphX x, int relevantQuantIndex1, int relevantQuantIndex2, String label) {
        return FeatGen.getFeatureVectorFromListString(
                getRunFeatures(x, relevantQuantIndex1, relevantQuantIndex2, label), lm);
    }

    public static List<String> getFeatures(GraphX x, GraphY y) {
        List<String> feats = new ArrayList<>();
        int n = x.relevantQuantIndices.size();
        assert y.labels.size() == (n + n*(n-1)/2);
        for(int i=0; i<n; ++i) {
            feats.addAll(getRateFeatures(x, i, y.labels.get(i)));
        }
        int next = n;
        for(int i=0; i<n; ++i) {
            for(int j=i+1; j<n; ++j) {
                feats.addAll(getRunFeatures(x, i, j, y.labels.get(next)));
                next++;
            }
        }
        return feats;
    }

    public static List<String> getRateFeatures(
            GraphX x, int relevantQuantIndex, String label) {
        List<String> feats = new ArrayList<>();
        feats.addAll(RateFeatGen.getFeatures(new RateX(
                x, x.relevantQuantIndices.get(relevantQuantIndex)), label));
        return feats;
    }

    public static List<String> getRunFeatures(
            GraphX x, int relevantQuantIndex1, int relevantQuantIndex2, String label) {
        List<String> feats = new ArrayList<>();
        feats.addAll(RunFeatGen.getFeatures(new RunX(
                x, x.relevantQuantIndices.get(relevantQuantIndex1),
                x.relevantQuantIndices.get(relevantQuantIndex2)), label));
        return feats;
    }



}
