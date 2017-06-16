package logic;

import edu.stanford.nlp.ling.CoreLabel;
import structure.StanfordSchema;
import utils.Params;
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
            "Partition", "Verb", "Rate0", "Rate1", "RateQues", "SimpleInterest");

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
            if(vc2.equals("POSITIVE") || vc2.equals("CONSTRUCT")) op = "ADD";
            if(vc2.equals("NEGATIVE") || vc2.equals("DESTROY")) op = "SUB";
        }
        if(vc1.equals("POSITIVE") || vc1.equals("CONSTRUCT")) {
            if(vc2.equals("STATE")) op = "SUB";
            if(vc2.equals("POSITIVE") || vc2.equals("CONSTRUCT")) op = "ADD";
            if(vc2.equals("NEGATIVE") || vc2.equals("DESTROY")) op = "SUB";
        }
        if(vc1.equals("NEGATIVE") || vc1.equals("DESTROY")) {
            if(vc2.equals("STATE")) op = "ADD";
            if(vc2.equals("POSITIVE") || vc2.equals("CONSTRUCT")) op = "SUB";
            if(vc2.equals("NEGATIVE") || vc2.equals("DESTROY")) op = "ADD";
        }
        if(!vc1.equals("CONSTRUCT") && !vc1.equals("DESTROY") &&
                !vc2.equals("CONSTRUCT") && !vc2.equals("DESTROY")) {
            if (key.equals("0_1") || key.equals("1_0")) {
                if (op.startsWith("SUB")) return "ADD";
                if (op.startsWith("ADD")) return "SUB";
            }
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
        if(num2.verb >= 1) {
            String before = tokens.get(num2.sentId).get(num2.verb - 1).word().toLowerCase();
            if (num2.verb >= 1 && (before.equals("not") || before.equals("hadn't") ||
                    before.equals("didn't"))) {
                flipVerbOrder = true;
            }
        }
        return  Logic.verb(
                tokens.get(num1.sentId).get(num1.verb).lemma(),
                tokens.get(num2.sentId).get(num2.verb).lemma(),
                Tools.spanToLemmaList(tokens.get(num1.sentId), num1.unit),
                Tools.spanToLemmaList(tokens.get(num2.sentId), num2.unit),
                key,
                flipVerbOrder);

    }

    // Classification for partition: SIBLING, HYPO, HYPER
    public static String simpleInterest(String key) {
        if(key.equals("NotInterest")) return "MUL";
        if(key.equals("InterestFirst")) return "DIV";
        if(key.equals("InterestSecond")) return "DIV_REV";
        return null;
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
        if(Params.simpleInterest && infType.equals("SimpleInterest")) {
            keys.addAll(Arrays.asList("NotInterest", "InterestFirst", "InterestSecond"));
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

}
