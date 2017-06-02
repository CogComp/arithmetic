package logic;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.stanford.nlp.ling.CoreLabel;
import reader.Reader;
import structure.StanfordProblem;
import structure.StanfordSchema;
import utils.Tools;

import java.util.*;

public class Relevance {

    public static Map<IntPair, List<String>> extractionsForRelevance(LogicX x) {
        // Generate table with units and rates
        Map<IntPair, List<String>> unitRates = new HashMap<>();
        for(int i=0; i<x.schema.size(); ++i) {
            StanfordSchema s = x.schema.get(i);
            List<String> unit = Tools.spanToLemmaList(
                    x.tokens.get(x.schema.get(i).sentId), x.schema.get(i).unit);
            List<CoreLabel> tokens = x.tokens.get(s.sentId);
            if(unit.size() == -1 && i >= 1 &&
                    unitRates.get(new IntPair(i-1, 0)).size() > 0) {
                unit.addAll(unitRates.get(new IntPair(i-1, 0)));
            }
            if(s.unit.getFirst() != -1 &&
                    s.unit.getSecond() < (tokens.size()-1) &&
                    tokens.get(s.unit.getSecond()).lemma().equals("of")) {
                unit.addAll(Tools.spanToLemmaList(tokens,
                        Tools.getMaximalNounPhraseSpan(tokens, s.unit.getSecond()+1)));
            }
            if((s.unit.getSecond() - s.unit.getFirst()) == 1 &&
                    s.unit.getSecond() < tokens.size()-3 &&
                    tokens.get(s.unit.getFirst()).tag().startsWith("J") &&
                    tokens.get(s.unit.getSecond()).lemma().equals("and") &&
                    tokens.get(s.unit.getSecond()+1).tag().equals("CD") &&
                    tokens.get(s.unit.getSecond()+2).tag().startsWith("J") &&
                    tokens.get(s.unit.getSecond()+3).tag().startsWith("N")) {
                unit.add(tokens.get(s.unit.getSecond()+3).lemma());

            }
            if(s.unit.getFirst() != -1 && s.unit.getSecond() < tokens.size()-3 &&
                    tokens.get(s.unit.getSecond()-1).tag().startsWith("N") &&
                    tokens.get(s.unit.getSecond()).lemma().equals("'s") &&
                    (tokens.get(s.unit.getSecond()+1).tag().startsWith("J") ||
                            tokens.get(s.unit.getSecond()+1).tag().startsWith("N"))) {
                unit.clear();
                unit.addAll(Tools.spanToLemmaList(tokens,
                        Tools.getMaximalNounPhraseSpan(tokens, s.unit.getSecond()+1)));

            }
            unitRates.put(new IntPair(i, 0), unit);
            unitRates.put(new IntPair(i, 1), Tools.spanToLemmaList(
                    x.tokens.get(x.schema.get(i).sentId),
                    x.schema.get(i).rate));
        }
        return unitRates;
    }

    public static boolean irrelevance(LogicX x, int quantIndex) {
        int n = x.schema.size();
        if(n == 2) return false;
        StanfordSchema schema = x.schema.get(quantIndex);
        int tokenId = Tools.getTokenIdFromCharOffset(
                x.tokens.get(schema.sentId), schema.qs.start);
        Set<Integer> quantTokenIdsInSameSentence = new HashSet<>();
        boolean otherDependentNumber = false;
        for(StanfordSchema s : x.schema) {
            if(Tools.getTokenIdFromCharOffset(
                    x.tokens.get(schema.sentId), s.qs.start) != -1) {
                quantTokenIdsInSameSentence.add(
                        Tools.getTokenIdFromCharOffset(
                                x.tokens.get(schema.sentId), s.qs.start));
            }
        }
        for(int i=tokenId+1; i<x.tokens.get(schema.sentId).size(); ++i) {
            if (x.tokens.get(schema.sentId).get(i).lemma().equals("if") ||
                    (x.tokens.get(schema.sentId).get(i).lemma().equals(",") &&
                            !x.tokens.get(schema.sentId).get(i+1).lemma().equals("each")) ||
                    x.tokens.get(schema.sentId).get(i).lemma().equals(";") ||
                    x.tokens.get(schema.sentId).get(i).lemma().equals("and")) {
                break;
            }
            if (quantTokenIdsInSameSentence.contains(i)) {
                otherDependentNumber = true;
                break;
            }
        }
        for(int i=tokenId-1; i>=0; --i) {
            if (x.tokens.get(schema.sentId).get(i).lemma().equals("if") ||
                    x.tokens.get(schema.sentId).get(i).lemma().equals(",") ||
                    x.tokens.get(schema.sentId).get(i).lemma().equals(";") ||
                    x.tokens.get(schema.sentId).get(i).lemma().equals("and")) {
                break;
            }
            if (quantTokenIdsInSameSentence.contains(i)) {
                otherDependentNumber = true;
                break;
            }
        }
        boolean numberInQuesSpan = false;
        if(schema.sentId == x.questionSchema.sentId) {
            if(tokenId >= x.questionSpan.getFirst() &&
                    tokenId < x.questionSpan.getSecond()) {
                numberInQuesSpan = true;
            }
        }
        Map<IntPair, List<String>> unitRates = extractionsForRelevance(x);
        List<String> quesUnit = Tools.spanToLemmaList(
                x.tokens.get(x.questionSchema.sentId),
                x.questionSchema.unit);
        List<String> quesRate = Tools.spanToLemmaList(
                x.tokens.get(x.questionSchema.sentId),
                x.questionSchema.rate);
        List<String> quesTokens = Tools.spanToLemmaList(
                x.tokens.get(x.questionSchema.sentId), x.questionSpan);
        if(unitRates.get(new IntPair(quantIndex, 0)).contains("dollar") ||
                unitRates.get(new IntPair(quantIndex, 0)).contains("cent") ||
                unitRates.get(new IntPair(quantIndex, 0)).contains("$")) {
            if(quesTokens.contains("much")) return false;
        }
        if(Tools.safeEquals(schema.qs.val, 1.0)) {
            if(tokenId < 2) return true;
            if(tokenId >= 1 && (x.tokens.get(schema.sentId).get(tokenId-1).lemma().equals("each") ||
                    x.tokens.get(schema.sentId).get(tokenId-1).lemma().equals("every"))) {
                return true;
            }
            if(otherDependentNumber) {
                return true;
            }
            if(numberInQuesSpan) {
                return true;
            }
        }
        if(Tools.safeEquals(schema.qs.val, 2.0)) {
            if(schema.sentId == 0 && quantIndex == 0 && quantTokenIdsInSameSentence.size()==1) {
                return true;
            }
        }
        if(Tools.safeEquals(schema.qs.val, 2.0) || Tools.safeEquals(schema.qs.val, 1.0)) {
            for(List<CoreLabel> tokens : x.tokens) {
                for(CoreLabel token : tokens) {
                    if(token.lemma().equals("another") || token.lemma().equals("1.0") ||
                            token.lemma().equals("other") || token.lemma().equals("first") ||
                            token.lemma().equals("second")) {
                        return true;
                    }
                }
            }
        }
        if(schema.unit == null || schema.unit.getFirst() == -1) {
            return false;
        }
        if(numberInQuesSpan) {
            for(int i=0; i<quantIndex; ++i) {
                if(Tools.safeEquals(x.quantities.get(i).val,
                        x.quantities.get(quantIndex).val)) {
                    if((""+Arrays.asList(unitRates.get(new IntPair(i, 0)))).equals(
                            ""+Arrays.asList(unitRates.get(new IntPair(quantIndex, 0))))) {
                        return true;
                    }
                }
            }
        }
        double maxSim = 0.0, minSim = 1.0, maxSimForIndex = 0.0;
        for(int i=0; i<n; ++i) {
            for(int j=i+1; j<n; ++j) {
                double sim = Math.max(
                        Tools.jaccardSim(unitRates.get(new IntPair(i, 0)), unitRates.get(new IntPair(j, 0))),
                        Tools.jaccardSim(unitRates.get(new IntPair(i, 0)), unitRates.get(new IntPair(j, 1))));
                if (i == quantIndex || j == quantIndex) {
                    if(sim > maxSimForIndex) maxSimForIndex = sim;
                } else {
                    if (sim > maxSim) {
                        maxSim = sim;
                    }
                    if (sim < minSim) {
                        minSim = sim;
                    }
                }
            }
        }
//        System.out.println("ProblemId: "+x.problemId+"|| QuantIndex: "+quantIndex+
//                "|| MaxSimForIndex: "+maxSimForIndex+"|| MaxSim: "+maxSim+
//                "|| MinSim: "+minSim);
//        System.out.println("Extractions: "+Arrays.asList(unitRates));
//        System.out.println();
        if(minSim - maxSimForIndex > 0.51 && !otherDependentNumber) {
            if(Tools.jaccardSim(quesUnit, unitRates.get(
                        new IntPair(quantIndex, 0))) < minSim-0.01) {
                return true;
            }
        }
//        List<StanfordSchema> schemas = new ArrayList<>();
//        schemas.addAll(x.schema);
//        schemas.add(x.questionSchema);
//        boolean allSubjectSame = true, allVerbSame = true, indexSubjSame = true, indexVerbSame = true;
//        for(int i=0; i<schemas.size(); ++i) {
//            StanfordSchema s1 = schemas.get(i);
//            List<String> subj1 = Tools.spanToLemmaList(x.tokens.get(s1.sentId), s1.subject);
//            String verb1 = x.tokens.get(s1.sentId).get(s1.verb).lemma();
//            for(int j=i+1; j<schemas.size(); ++j) {
//                StanfordSchema s2 = schemas.get(j);
//                List<String> subj2 = Tools.spanToLemmaList(x.tokens.get(s2.sentId), s2.subject);
//                String verb2 = x.tokens.get(s2.sentId).get(s2.verb).lemma();
//                if(i==quantIndex || j==quantIndex) {
//                    if(!verb1.equals(verb2)) indexVerbSame = false;
//                    if(!(""+Arrays.asList(subj1)).equals(""+Arrays.asList(subj2))) {
//                        indexSubjSame = false;
//                    }
//                } else {
//                    if(!verb1.equals(verb2)) allVerbSame = false;
//                    if(!(""+Arrays.asList(subj1)).equals(""+Arrays.asList(subj2))) {
//                        allSubjectSame = false;
//                    }
//                }
//            }
//        }
//        if(allSubjectSame && !indexSubjSame) return true;
//        if(allVerbSame && !indexVerbSame) return true;

        return false;
    }

    public static void testIrrelevanceWithDefaultExtraction() throws Exception {
        Set<Integer> incorrect = new HashSet<>();
        Set<Integer> total = new HashSet<>();
        int t = 0;
        List<StanfordProblem> probs = Reader.readStanfordProblemsFromJson();
        double acc = 0.0, relAcc = 0.0, irrAcc = 0.0, relTot = 0.0, irrTot = 0.0;
        for (StanfordProblem prob : probs) {
            total.add(prob.id);
            for(int i=0; i<prob.quantities.size(); ++i) {
                t++;
                String gold = prob.expr.findRelevanceLabel(i);
                if(gold.equals("REL")) {
                    relTot += 1.0;
                } else {
                    irrTot += 1.0;
                }
                LogicX x = new LogicX(prob);
                boolean pred = irrelevance(x, i);
                if((gold.equals("REL") && !pred) || (gold.equals("IRR") && pred)) {
                    acc += 1.0;
                    if(gold.equals("REL") && !pred) {
                        relAcc += 1.0;
                    }
                    if(gold.equals("IRR") && pred) {
                        irrAcc += 1.0;
                    }
                } else {
                    incorrect.add(prob.id);
                    System.out.println(prob.id+" : "+prob.question);
                    System.out.println();
                    for(StanfordSchema schema : prob.schema) {
                        System.out.println(schema);
                    }
                    System.out.println();
                    System.out.println(Tools.spanToLemmaList(
                            x.tokens.get(x.questionSchema.sentId),
                            x.questionSpan));
                    System.out.println(prob.questionSchema);
                    System.out.println();
                    System.out.println("Quantities : "+prob.quantities);
                    System.out.println("Quant of Interest: "+i);
                    System.out.println();
                    System.out.println("Gold : "+gold);
                    System.out.println("Pred : "+pred);
                    System.out.println();
                }
            }
        }
        System.out.println("Rel Accuracy : = " + relAcc + " / " + relTot + " = " + (relAcc/relTot));
        System.out.println("Irr Accuracy : = " + irrAcc + " / " + irrTot + " = " + (irrAcc/irrTot));
        System.out.println("Accuracy : = " + acc + " / " + t + " = " + (acc/t));
        System.out.println("Strict Accuracy : = 1 - " + incorrect.size() + " / " +
                total.size() + " = " + (1-1.0*incorrect.size()/total.size()));
    }

    public static void main(String[] args) throws Exception {
        testIrrelevanceWithDefaultExtraction();
    }

}
