package structure;

import reader.Reader;
import run.Annotations;
import utils.Tools;

import java.util.*;

public class ExtractRate {

    // Hack to compute rate annotations for old datasets
    public static Map<Integer, List<Integer>> computeRateAnnotations(String dir) throws Exception {
        List<Problem> oldProblems = Reader.readProblemsFromJson(dir);
        List<Problem> newProblems = Reader.readProblemsFromJson("data/allArith/");
        Map<Integer, List<Integer>> newRateAnnotations =
                Annotations.readRateAnnotations("data/allArith/rateAnnotations.txt");
        Map<Integer, List<Integer>> oldRateAnnotations = new HashMap<>();
        int count = 0;
        for(Problem oldProb : oldProblems) {
            double minDist = 10000;
            Problem bestMatch = null;
            for(Problem newProb : newProblems) {
                double dist = 0.0;
                // = Tools.editDist(oldProb.ta.getTokens(), newProb.ta.getTokens(),
                // oldProb.ta.size(), newProb.ta.size());
                if(dist < minDist) {
                    minDist = dist;
                    bestMatch = newProb;
                }
            }
            List<Integer> newRateIndices = new ArrayList<>();
            if(newRateAnnotations.containsKey(bestMatch.id)) {
                newRateIndices = newRateAnnotations.get(bestMatch.id);
            }
            if(Math.abs(oldProb.quantities.size() - bestMatch.quantities.size())>0.1) {
//                System.out.println("OldProb : "+oldProb.ta.getText());
//                System.out.println("OldProb : "+Arrays.asList(oldProb.quantities));
//                System.out.println("NewProb : "+bestMatch.ta.getText());
//                System.out.println("NewProb : "+Arrays.asList(bestMatch.quantities));
//                System.out.println();
                count++;
                int j=0;
                List<Integer> oldRateIndices = new ArrayList<>();
                for(int i=0; i<bestMatch.quantities.size(); ++i) {
//                    System.out.println(i+"_"+j);
                    if(Tools.safeEquals(oldProb.quantities.get(j).val,
                            bestMatch.quantities.get(i).val)) {
                        if(newRateIndices.contains(i)) {
                            oldRateIndices.add(j);
                        }
                        j++;
                        if(j>=oldProb.quantities.size()) break;
                    }
                }
                oldRateAnnotations.put(oldProb.id, oldRateIndices);
            } else {
                oldRateAnnotations.put(oldProb.id, newRateIndices);
            }
        }
        System.out.println(count);
        return oldRateAnnotations;
    }

    public static void main(String args[]) throws Exception {
        computeRateAnnotations("data/illinois/");
        computeRateAnnotations("data/commoncore/");
    }


}
