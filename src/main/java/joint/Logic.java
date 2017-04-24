package joint;

import edu.stanford.nlp.ling.CoreLabel;
import reader.Reader;
import structure.StanfordProblem;
import structure.StanfordSchema;
import utils.Tools;
import java.util.*;

public class Logic {

    public static List<String> labels = Arrays.asList(
            "ADD", "SUB", "SUB_REV", "MUL", "DIV", "DIV_REV");
    public static List<String> addTokens = Arrays.asList(
            "taller", "more", "older", "higher", "faster", "heavier", "farther", "longer");
    public static List<String> subTokens = Arrays.asList(
            "shorter", "less", "younger", "slower", "lighter", "fewer");
    public static List<String> mulTokens = Arrays.asList(
            "times", "twice", "thrice", "double", "triple");

    public static List<String> inferenceTypes = Arrays.asList(
            "Math0_Add", "Math0_Sub", "Math0_Mul", "Math1_Add", "Math1_Sub",
            "Math1_Mul", "MathQues_Add", "MathQues_Sub", "MathQues_Mul",
            "Partition", "Verb", "Rate0", "Rate1", "RateQues");

    // Classification for partition: 0_0, 1_0, 0_0, QUES, QUES_REV
    public static String unitDependency(String infType, String key) {
        if(infType.endsWith("0") && key.equals("0_0")) return "DIV_REV";
        if(infType.endsWith("1") && key.equals("0_0")) return "DIV";
        if(key.equals("0_1") || key.equals("1_0")) return "MUL";
        if(key.equals("QUES")) return "DIV";
        if(key.equals("QUES_REV")) return "DIV_REV";
        return null;
    }

    // Classification for partition: SIBLING, HYPO, HYPER
    public static String partition(String key) {
        if(key.equals("SIBLING")) return "ADD";
        if(key.equals("HYPO")) return "SUB";
        if(key.equals("HYPER")) return "SUB";
        return null;
    }

    // Classification for math: 0_0, 0_1, 1_0, QUES, QUES_REV
    public static String math(String infType, String key) {
        if(infType.equals("Math0_Add") && key.equals("0_0")) return "SUB";
        if(infType.equals("Math1_Add") && key.equals("0_0")) return "SUB";
        if(infType.equals("Math0_Sub") && key.equals("0_0")) return "ADD";
        if(infType.equals("Math1_Sub") && key.equals("0_0")) return "ADD";

        if(infType.equals("Math0_Add") && key.equals("1_0")) return "ADD";
        if(infType.equals("Math1_Add") && key.equals("0_1")) return "ADD";
        if(infType.equals("Math0_Sub") && key.equals("1_0")) return "SUB";
        if(infType.equals("Math1_Sub") && key.equals("0_1")) return "SUB";

        if(infType.equals("Math0_Mul") && key.equals("0_0")) return "DIV_REV";
        if(infType.equals("Math1_Mul") && key.equals("0_0")) return "DIV";
        if(infType.equals("Math0_Mul") && key.equals("1_0")) return "MUL";
        if(infType.equals("Math1_Mul") && key.equals("0_1")) return "MUL";

        if(infType.equals("MathQues_Add") || infType.equals("MathQues_Sub")) {
            return "SUB";
        }

        if(infType.equals("MathQues_Mul")) {
            if(key.equals("QUES")) return "DIV";
            if(key.equals("QUES_REV")) return "DIV_REV";
        }
        return null;
    }

    // Classification for each verb: POSITIVE, NEGATIVE, STATE
    public static String verb(String verb1, String verb2, List<String> unit1,
                              List<String> unit2, String key, boolean flipVerbOrder) {
        String vc1 = Tools.getKeyForMaxValue(Verbs.verbClassify(verb1, unit1));
        String vc2 = Tools.getKeyForMaxValue(Verbs.verbClassify(verb2, unit2));
        String op = null;
        if(vc1.equals("STATE")) {
            if(vc2.equals("STATE")) op = "SUB";
            if(vc2.equals("POSITIVE")) op = "ADD";
            if(vc2.equals("NEGATIVE")) op = "SUB";
        }
        if(vc1.equals("POSITIVE")) {
            if(vc2.equals("STATE")) op = "SUB";
            if(vc2.equals("POSITIVE")) op = "ADD";
            if(vc2.equals("NEGATIVE")) op = "SUB";
        }
        if(vc1.equals("NEGATIVE")) {
            if(vc2.equals("STATE")) op = "ADD";
            if(vc2.equals("POSITIVE")) op = "SUB";
            if(vc2.equals("NEGATIVE")) op = "ADD";
        }
        if(key.equals("0_1") || key.equals("1_0")) {
            if(op.startsWith("SUB")) return "ADD";
            if(op.startsWith("ADD")) return "SUB";
        }
        if(flipVerbOrder) {
            if(op.equals("SUB")) {
                op = "ADD";
            } else {
                op = "SUB";
            }
        }
        return op;
    }


    public static String verb(List<List<CoreLabel>> tokens,
                              StanfordSchema num1,
                              StanfordSchema num2,
                              String key) {
        boolean flipVerbOrder = false;
        for(int i=Math.max(0, num2.verb-3);
            i<Math.min(tokens.get(num2.sentId).size(), num2.verb+4);
            ++i) {
            String word = tokens.get(num2.sentId).get(i).word().toLowerCase();
            if(word.equals("originally")) {
                flipVerbOrder = true;
            }
        }
        String before = tokens.get(num2.sentId).get(num2.verb-1).word().toLowerCase();
        if(num2.verb >= 1 && (before.equals("not") || before.equals("hadn't") ||
                before.equals("didn't"))) {
            flipVerbOrder = true;
        }
        return  Logic.verb(
                tokens.get(num1.sentId).get(num1.verb).lemma(),
                tokens.get(num2.sentId).get(num2.verb).lemma(),
                Tools.spanToLemmaList(tokens.get(num1.sentId), num1.unit),
                Tools.spanToLemmaList(tokens.get(num2.sentId), num2.unit),
                key,
                flipVerbOrder);

    }


    public static List<String> getRelevantKeys(String infType) {
        List<String> keys = new ArrayList<>();
        if(infType.equals("Verb")) {
            keys.addAll(Arrays.asList("0_0", "0_1", "1_0"));
        }
        if(infType.equals("Partition")) {
            keys.addAll(Arrays.asList("SIBLING", "HYPO", "HYPER"));
        }
        if(infType.startsWith("Math") || infType.startsWith("Rate")) {
            if(infType.contains("Ques")) {
                keys.addAll(Arrays.asList("QUES", "QUES_REV"));
            } else {
                keys.addAll(Arrays.asList("0_0"));
                if(infType.contains("1")) {
                    keys.add("0_1");
                } else {
                    keys.add("1_0");
                }
            }
        }
        return keys;
    }

    public static String getMathInfType(List<List<CoreLabel>> tokens,
                                        StanfordSchema num1,
                                        StanfordSchema num2,
                                        StanfordSchema ques,
                                        boolean isTopmost) {
        String math = null, order = null;
        if(num1.math != -1) {
            math = tokens.get(num1.sentId).get(num1.math).word();
            order = "0_";
        } else if(num2.math != -1) {
            math = tokens.get(num2.sentId).get(num2.math).word();
            order = "1_";
        } else if(ques.math != -1 && isTopmost) {
            math = tokens.get(ques.sentId).get(ques.math).word();
            order = "Ques_";
        }
        if(Logic.addTokens.contains(math)) {
            return "Math"+order+"Add";
        }
        if(Logic.subTokens.contains(math)) {
            return "Math"+order+"Sub";
        }
        if(Logic.mulTokens.contains(math)) {
            return "Math"+order+"Mul";
        }
        return null;
    }

    public static boolean irrelevance(List<StanfordSchema> schemas,
                                      StanfordSchema quesSchema,
                                      int quantIndex,
                                      List<List<CoreLabel>> tokens) {
        double maxSim = 0.0, minSim=1.0;
        int n = schemas.size();
        if(n == 2) return false;
        StanfordSchema schema = schemas.get(quantIndex);
        int tokenId = Tools.getTokenIdFromCharOffset(
                tokens.get(schema.sentId), schema.qs.start);

        Set<Integer> quantTokenIdsInSameSentence = new HashSet<>();
        boolean otherDependentNumber = false;
        for(StanfordSchema s : schemas) {
            if(Tools.getTokenIdFromCharOffset(
                    tokens.get(schema.sentId), s.qs.start) != -1) {
                quantTokenIdsInSameSentence.add(
                        Tools.getTokenIdFromCharOffset(
                                tokens.get(schema.sentId), s.qs.start));
            }
        }
        for(int i=tokenId+1; i<tokens.get(schema.sentId).size(); ++i) {
            if (tokens.get(schema.sentId).get(i).lemma().equals("if") ||
                    tokens.get(schema.sentId).get(i).lemma().equals(",") ||
                    tokens.get(schema.sentId).get(i).lemma().equals(";") ||
                    tokens.get(schema.sentId).get(i).lemma().equals("and")) {
                break;
            }
            if (quantTokenIdsInSameSentence.contains(i)) {
                otherDependentNumber = true;
                break;
            }
        }
        for(int i=tokenId-1; i>=0; --i) {
            if (tokens.get(schema.sentId).get(i).lemma().equals("if") ||
                    tokens.get(schema.sentId).get(i).lemma().equals(",") ||
                    tokens.get(schema.sentId).get(i).lemma().equals(";") ||
                    tokens.get(schema.sentId).get(i).lemma().equals("and")) {
                break;
            }
            if (quantTokenIdsInSameSentence.contains(i)) {
                otherDependentNumber = true;
                break;
            }
        }
        if(Tools.safeEquals(schema.qs.val, 1.0)) {
            if(tokenId < 2) return true;
            if(tokenId >= 1 && (tokens.get(schema.sentId).get(tokenId-1).lemma().equals("each") ||
                    tokens.get(schema.sentId).get(tokenId-1).lemma().equals("every"))) {
                return true;
            }
            if(schema.sentId == quesSchema.sentId) {
                return true;
            }
            if(otherDependentNumber) {
                return true;
            }
            for(int i=tokenId-1; i>=0; --i) {
                if (tokens.get(schema.sentId).get(i).lemma().equals("if") ||
                        tokens.get(schema.sentId).get(i).lemma().equals(",") ||
                        tokens.get(schema.sentId).get(i).lemma().equals(";") ||
                        tokens.get(schema.sentId).get(i).lemma().equals("and")) {
                    break;
                }
                if (quantTokenIdsInSameSentence.contains(i)) {
                    return true;
                }
            }
        }
        if(schema.unit == null || schema.unit.getFirst() == -1) {
            return false;
        }
        List<StanfordSchema> allSchemas = new ArrayList<>();
        allSchemas.addAll(schemas);
        allSchemas.add(quesSchema);
        double simIndex = 0.0;
        for(int i=0; i<n; ++i) {
            StanfordSchema schema1 = allSchemas.get(i);

            double sim = Collections.max(Arrays.asList(
                    Tools.jaccardSim(
                            Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.unit),
                            Tools.spanToLemmaList(tokens.get(quesSchema.sentId), quesSchema.unit)),
                    Tools.jaccardSim(
                            Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.rate),
                            Tools.spanToLemmaList(tokens.get(quesSchema.sentId), quesSchema.unit)),
                    Tools.jaccardSim(
                            Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.unit),
                            Tools.spanToLemmaList(tokens.get(quesSchema.sentId), quesSchema.rate)),
                    Tools.jaccardSim(
                            Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.rate),
                            Tools.spanToLemmaList(tokens.get(quesSchema.sentId), quesSchema.rate))));

            if (tokens.get(quesSchema.sentId).get(quesSchema.verb).lemma().equals("cost") &&
                    (Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.unit).contains("dollar")
                            ||Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.unit).contains("cent")
                            ||Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.unit).contains("buck")
                            ||Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.unit).contains("$"))) {
                sim = 1.0;
            }
            if(i==quantIndex) {
                simIndex = sim;
            } else {
                if (sim > maxSim) {
                    maxSim = sim;
                }
                if (sim < minSim) {
                    minSim = sim;
                }
            }
        }
        if(minSim - simIndex > 0.4 && !otherDependentNumber) {
            boolean foundInRate = false;
            for(int i=0; i<n; ++i) {
                if(i==quantIndex) continue;
                StanfordSchema schema1 = schemas.get(i);
                if(Tools.jaccardSim(
                        Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.rate),
                        Tools.spanToLemmaList(tokens.get(schema.sentId), schema.unit))>0.5) {
                    foundInRate = true;
                }
            }
            if(!foundInRate) return true;
        }
        return false;
    }

    public static void testIrrelevanceWithDefaultExtraction() throws Exception {
        Set<Integer> incorrect = new HashSet<>();
        Set<Integer> total = new HashSet<>();
        int t = 0;
        List<StanfordProblem> probs = Reader.readStanfordProblemsFromJson();
        double acc = 0.0;
        for (StanfordProblem prob : probs) {
            total.add(prob.id);
            for(int i=0; i<prob.quantities.size(); ++i) {
                t++;
                String gold = prob.expr.findRelevanceLabel(i);
                boolean pred = irrelevance(prob.schema, prob.questionSchema, i, prob.tokens);
                if((gold.equals("REL") && !pred) || (gold.equals("IRR") && pred)) {
                    acc += 1.0;
                } else {
                    incorrect.add(prob.id);
                    System.out.println(prob.id+" : "+prob.question);
                    System.out.println();
                    for(StanfordSchema schema : prob.schema) {
                        System.out.println(schema);
                    }
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
        System.out.println("Accuracy : = " + acc + " / " + t + " = " + (acc/t));
        System.out.println("Strict Accuracy : = 1 - " + incorrect.size() + " / " +
                total.size() + " = " + (1-1.0*incorrect.size()/total.size()));
    }

    public static void main(String[] args) throws Exception {
        testIrrelevanceWithDefaultExtraction();
    }

}
