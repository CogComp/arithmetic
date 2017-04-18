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
            "shorter", "less", "younger", "slower", "lighter");
    public static List<String> mulTokens = Arrays.asList(
            "times", "twice", "thrice", "double", "triple");

    public static int maxNumInferenceTypes = 4;

    // Classification for partition: 0_NUM, 1_NUM, 0_DEN, 1_DEN, QUES, QUES_REV
    public static String unitDependency(String key) {
        if(key.equals("0_NUM")) return "DIV_REV";
        if(key.equals("1_NUM")) return "DIV";
        if(key.equals("0_DEN") || key.equals("1_DEN")) return "MUL";
        if(key.equals("QUES")) return "DIV";
        if(key.equals("QUES_REV")) return "DIV_REV";
        return null;
    }

    // Classification for partition: SIBLING, HYPO, HYPER, QUES_1_SIBLING, QUES_2_SIBLING
    public static String partition(String key) {
        if(key.equals("SIBLING")) return "ADD";
        if(key.equals("HYPO")) return "SUB_REV";
        if(key.equals("HYPER")) return "SUB";
        return null;
    }

    // Classification for math: 0_0, 0_1, 1_0, 0, 1,
    public static String math(String mathOp, String key) {
        if(mathOp.equals("FIRST_ADD") && key.equals("0_0")) return "SUB_REV";
        if(mathOp.equals("SECOND_ADD") && key.equals("0_0")) return "SUB";
        if(mathOp.equals("FIRST_SUB") && key.equals("0_0")) return "ADD";
        if(mathOp.equals("SECOND_SUB") && key.equals("0_0")) return "ADD";

        if(mathOp.equals("FIRST_ADD") && key.equals("1_0")) return "ADD";
        if(mathOp.equals("SECOND_ADD") && key.equals("0_1")) return "ADD";
        if(mathOp.equals("FIRST_SUB") && key.equals("1_0")) return "SUB_REV";
        if(mathOp.equals("SECOND_SUB") && key.equals("0_1")) return "SUB";

        if(mathOp.equals("FIRST_MUL") && key.equals("0_0")) return "DIV_REV";
        if(mathOp.equals("SECOND_MUL") && key.equals("0_0")) return "DIV";
        if(mathOp.equals("FIRST_MUL") && key.equals("1_0")) return "MUL";
        if(mathOp.equals("SECOND_MUL") && key.equals("0_1")) return "MUL";

        if(mathOp.equals("QUES_ADD") || mathOp.equals("QUES_SUB")) {
            if(key.equals("0")) return "SUB";
            if(key.equals("1")) return "SUB_REV";
        }

        if(mathOp.equals("QUES_MUL")) {
            if(key.equals("0")) return "DIV";
            if(key.equals("1")) return "DIV_REV";
        }
        return null;
    }

    // Classification for each verb: POSITIVE, NEGATIVE, STATE
    public static String verb(String verb1, String verb2, List<String> unit1,
                              List<String> unit2, String key) {
        String vc1 = Tools.getKeyForMaxValue(Verbs.verbClassify(verb1, unit1));
        String vc2 = Tools.getKeyForMaxValue(Verbs.verbClassify(verb2, unit2));
        String op = null;
        if(vc1.equals("STATE")) {
            if(vc2.equals("STATE")) op = "SUB_REV";
            if(vc2.equals("POSITIVE")) op = "ADD";
            if(vc2.equals("NEGATIVE")) op = "SUB";
        }
        if(vc1.equals("POSITIVE")) {
            if(vc2.equals("STATE")) op = "SUB_REV";
            if(vc2.equals("POSITIVE")) op = "ADD";
            if(vc2.equals("NEGATIVE")) op = "SUB";
        }
        if(vc1.equals("NEGATIVE")) {
            if(vc2.equals("STATE")) op = "ADD";
            if(vc2.equals("POSITIVE")) op = "SUB_REV";
            if(vc2.equals("NEGATIVE")) op = "ADD";
        }
        if(key.equals("0_1") || key.equals("1_0")) {
            if(op.startsWith("SUB")) return "ADD";
            if(op.equals("ADD") && key.equals("0_1")) {
                return "SUB";
            }
            if(op.equals("ADD") && key.equals("1_0")) {
                return "SUB_REV";
            }
        }
        return op;
    }

    public static List<String> getRelevantKeys(int infType, boolean isTopmost, String mathOp) {
        List<String> keys = new ArrayList<>();
        if(infType == 0) {
            keys.addAll(Arrays.asList("0_0", "0_1", "1_0"));
        }
        if(infType == 1) {
            keys.addAll(Arrays.asList("SIBLING", "HYPO", "HYPER"));
        }
        if(mathOp != null && infType == 2) {
            if(mathOp.startsWith("QUES")) {
                keys.addAll(Arrays.asList("0", "1"));
            } else {
                keys.addAll(Arrays.asList("0_0", "0_1", "1_0"));
            }
        }
        if(infType == 3) {
            if(isTopmost) {
                keys.addAll(Arrays.asList("0_NUM", "1_NUM", "0_DEN",
                        "1_DEN", "QUES", "QUES_REV"));
            } else {
                keys.addAll(Arrays.asList("0_NUM", "1_NUM", "0_DEN", "1_DEN"));
            }
        }
        return keys;
    }

    public static String getMathOp(List<List<CoreLabel>> tokens,
                                   StanfordSchema num1,
                                   StanfordSchema num2,
                                   StanfordSchema ques) {
        String math = null, order = null;
        if(num1.math != -1) {
            math = tokens.get(num1.sentId).get(num1.math).word();
            order = "FIRST_";
        } else if(num2.math != -1) {
            math = tokens.get(num2.sentId).get(num2.math).word();
            order = "SECOND_";
        } else if(ques.math != -1) {
            math = tokens.get(ques.sentId).get(ques.math).word();
            order = "QUES_";
        }
        if(Logic.addTokens.contains(math)) {
            return order+"ADD";
        }
        if(Logic.subTokens.contains(math)) {
            return order+"SUB";
        }
        if(Logic.mulTokens.contains(math)) {
            return order+"MUL";
        }
        return null;
    }

    public static boolean irrelevance(List<StanfordSchema> schemas,
                                      int quantIndex,
                                      List<List<CoreLabel>> tokens) {
        double maxSim = 0.0, minSim=1.0;
        int n = schemas.size() - 1;
        if(n == 2) return false;
        StanfordSchema schema = schemas.get(quantIndex);
        StanfordSchema quesSchema = schemas.get(n);
        if(Tools.safeEquals(schema.qs.val, 1.0)) {
            int tokenId = Tools.getTokenIdFromCharOffset(
                    tokens.get(schema.sentId), schema.qs.start);
            if(tokenId >= 1 && (tokens.get(schema.sentId).get(tokenId-1).lemma().equals("each") ||
                    tokens.get(schema.sentId).get(tokenId-1).lemma().equals("every"))) {
                return true;
            }
            if(schema.sentId == quesSchema.sentId) {
                return true;
            }
            Set<Integer> quantTokenIdsInSameSentence = new HashSet<>();
            for(StanfordSchema s : schemas) {
                if(s.qs != null && Tools.getTokenIdFromCharOffset(
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
                    return true;
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
                    return true;
                }
            }
        }
        if(schema.unit == null || schema.unit.getFirst() == schema.unit.getSecond()) {
            return false;
        }
        double simIndex = 0.0;
        for(int i=0; i<n; ++i) {
            StanfordSchema schema1 = schemas.get(i);
            double sim = Tools.jaccardSim(
                    Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.unit),
                    Tools.spanToLemmaList(tokens.get(quesSchema.sentId), quesSchema.unit));
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
        if(minSim - simIndex > 0.5) {
            boolean foundInRate = false;
            for(int i=0; i<n; ++i) {
                if(i==quantIndex) continue;
                StanfordSchema schema1 = schemas.get(i);
                if(Tools.jaccardSim(Tools.spanToLemmaList(tokens.get(schema1.sentId), schema1.rate),
                        Tools.spanToLemmaList(tokens.get(schema.sentId), schema.unit))>0.5) {
                    foundInRate = true;
                }
            }
            if(!foundInRate) return true;
        }
        return false;
    }

    public static void testIrrelevanceWithDefaultExtraction(String dataset) throws Exception {
        Set<Integer> incorrect = new HashSet<>();
        Set<Integer> total = new HashSet<>();
        int t = 0;
        List<StanfordProblem> probs = Reader.readStanfordProblemsFromJson(dataset);
        double acc = 0.0;
        for (StanfordProblem prob : probs) {
            total.add(prob.id);
            for(int i=0; i<prob.quantities.size(); ++i) {
                t++;
                String gold = prob.expr.findRelevanceLabel(i);
                List<StanfordSchema> schemas = new ArrayList<>();
                schemas.addAll(prob.schema);
                schemas.add(prob.questionSchema);
                boolean pred = irrelevance(schemas, i, prob.tokens);
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

}
