package joint;

import edu.stanford.nlp.ling.CoreLabel;
import structure.StanfordSchema;
import utils.Tools;
import java.util.*;

public class LogicNew {

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
            if(op.equals("ADD")) {
                if(vc1.equals("STATE")) return "SUB_REV";
                else return "SUB";
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
        if(LogicNew.addTokens.contains(math)) {
            return order+"ADD";
        }
        if(LogicNew.subTokens.contains(math)) {
            return order+"SUB";
        }
        if(LogicNew.mulTokens.contains(math)) {
            return order+"MUL";
        }
        return null;
    }

}
