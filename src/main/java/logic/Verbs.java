package logic;

import utils.Params;
import utils.Tools;

import java.io.*;
import java.util.*;

public class Verbs {

    public static Map<String, double[]> vectors;

    static {
        try {
            System.out.println("Reading vectors for verbs ...");
            vectors = readVectors(Params.vectorsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String verbCategory(String verb) {
        List<String> state = Arrays.asList("be", "have", "own", "start", "worth", "remain");
        List<String> positive = Arrays.asList("get", "gain", "borrow", "receive",
                "take", "collect");
        List<String> negative = Arrays.asList("give", "lose", "lend", "spend", "pay",
                "share", "leave");
        List<String> construct = Arrays.asList( "add", "increase", "build",
                "score", "save", "win", "arrive", "fill");
        List<String> destroy = Arrays.asList("defeat", "destroy", "need", "eat",
                "decrease", "reduce", "remove", "use", "throw");
        Map<String, Double> map = new HashMap<>();
        map.put("STATE", -Double.MAX_VALUE);
        map.put("POSITIVE", -Double.MAX_VALUE);
        map.put("NEGATIVE", -Double.MAX_VALUE);
        map.put("CONSTRUCT", -Double.MAX_VALUE);
        map.put("DESTROY", -Double.MAX_VALUE);
        for(String s : state) {
            if(getVectorSim(verb, s) > map.get("STATE")) {
                map.put("STATE", getVectorSim(verb, s));
            }
        }
        for(String s : positive) {
            if(getVectorSim(verb, s) > map.get("POSITIVE")) {
                map.put("POSITIVE", getVectorSim(verb, s));
            }
        }
        for(String s : negative) {
            if(getVectorSim(verb, s) > map.get("NEGATIVE")) {
                map.put("NEGATIVE", getVectorSim(verb, s));
            }
        }
        for(String s : construct) {
            if(getVectorSim(verb, s) > map.get("CONSTRUCT")) {
                map.put("CONSTRUCT", getVectorSim(verb, s));
            }
        }
        for(String s : destroy) {
            if(getVectorSim(verb, s) > map.get("DESTROY")) {
                map.put("DESTROY", getVectorSim(verb, s));
            }
        }
        return Tools.getKeyForMaxValue(map);
    }

    public static Map<String, Double> verbClassify(String verbLemma, List<String> unit) {
        Map<String, Double> map = new HashMap<>();
        map.put("STATE", 0.0);
        map.put("POSITIVE", 0.0);
        map.put("NEGATIVE", 0.0);
        map.put("CONSTRUCT", 0.0);
        map.put("DESTROY", 0.0);
        // Hard decision for now
        if (verbLemma.equals("buy") || verbLemma.equals("purchase")) {
            if (unit != null && (unit.contains("$") ||
                    unit.contains("dollar") ||
                    unit.contains("cent") ||
                    unit.contains("dime") ||
                    unit.contains("nickel"))) {
                map.put("NEGATIVE", 1.0);
            } else {
                map.put("POSITIVE", 1.0);
            }
            return map;
        }
        if (verbLemma.equals("sell") || verbLemma.equals("return")) {
            if (unit != null && (unit.contains("$") ||
                    unit.contains("dollar") ||
                    unit.contains("cent") ||
                    unit.contains("dime") ||
                    unit.contains("nickel"))) {
                map.put("POSITIVE", 1.0);
            } else {
                map.put("NEGATIVE", 1.0);
            }
            return map;
        }
        String vcc = verbCategory(verbLemma);
        map.put(vcc, 1.0);
        return map;
    }

    public static double getVectorSim(String word1, String word2) {
//        System.out.println("Running vector similarity on "+word1+" and "+word2);
        if (word1 == null || word2 == null) {
            return 0.0;
        }
        if(vectors.containsKey(word1) && vectors.containsKey(word2)) {
            double[] v1 = vectors.get(word1);
            double[] v2 = vectors.get(word2);
            double dot = 0.0, norm1 = 0.0, norm2 = 0.0;
            for (int i=0; i<v1.length; ++i) {
                dot += (v1[i]*v2[i]);
                norm1 += (v1[i]*v1[i]);
                norm2 += (v2[i]*v2[i]);
            }
            double sim = dot / (Math.sqrt(norm1 * norm2));
//            System.out.println("Returned "+sim);
            return sim;
        }
        return 0.0;
    }

    public static Map<String, double[]> readVectors(String vectorFile) throws IOException {
        Map<String, double[]> vectors = new HashMap<>();
        BufferedReader br;
        String line;
        br = new BufferedReader(new FileReader(new File(vectorFile)));
        while((line = br.readLine()) != null) {
            String strArr[] = line.split(" ");
            String word = strArr[0].trim();
            double d[] = new double[strArr.length-1];
            for(int i=1; i<strArr.length; ++i) {
                d[i-1] = Double.parseDouble(strArr[i]);
            }
            vectors.put(word, d);
        }
        br.close();
        return vectors;
    }
}
