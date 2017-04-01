package logic;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import structure.QuantitySchema;
import structure.StanfordSchema;
import utils.Tools;
import java.util.*;


class LogicInput {

    public List<String> subject, subjectPos, object, objectPos, unit, unitPos, rate;
    public String verbLemma, math;
    public int mode; // 0 for ques, 1 for quantity1, 2 for quantity2
    public IntPair subjSpan, objSpan, unitSpan, rateSpan;
    public int verbIndex;
    public int sentId;

    public LogicInput(int mode) {
        this.mode = mode;
        this.sentId = -1;
        subject = new ArrayList<>();
        subjectPos = new ArrayList<>();
        object = new ArrayList<>();
        objectPos = new ArrayList<>();
        unit = new ArrayList<>();
        unitPos = new ArrayList<>();
        rate = new ArrayList<>();
    }

    public LogicInput(int mode, List<String> subject, List<String> object,
                      List<String> unit, List<String> rate, String verbLemma,
                      String math, TextAnnotation ta, List<Constituent> posTags,
                      List<String> lemmas) {
        this.mode = mode;
        this.sentId = -1;
        this.subject = subject;
        this.object = object;
        this.unit = unit;
        this.rate = rate;
        this.verbLemma = verbLemma;
        this.math = math;
        subjectPos = Tools.populatePos(subject, ta, posTags, lemmas);
        objectPos = Tools.populatePos(object, ta, posTags, lemmas);
        unitPos = Tools.populatePos(unit, ta, posTags, lemmas);
    }

    public LogicInput(int mode, QuantitySchema quantSchema, TextAnnotation ta,
                      List<Constituent> posTags, List<String> lemmas) {
        this.mode = mode;
        this.sentId = -1;
        subject = Tools.consToList(quantSchema.subject);
        object = Tools.consToList(quantSchema.object);
        unit = Arrays.asList(quantSchema.unit.split(" "));
        rate = Tools.consToList(quantSchema.rateUnit);
        verbLemma = quantSchema.verbLemma;
        math = quantSchema.math;
        subjectPos = Tools.populatePos(subject, ta, posTags, lemmas);
        objectPos = Tools.populatePos(object, ta, posTags, lemmas);
        unitPos = Tools.populatePos(unit, ta, posTags, lemmas);
    }

    public LogicInput(int mode, IntPair subject, IntPair object,
                      IntPair unit, IntPair rate, int verb, int math,
                      List<CoreLabel> tokens) {
        this.mode = mode;
        this.sentId = -1;
        if (tokens == null) return;
        this.subject = Tools.spanToLemmaList(tokens, subject);
        this.object = Tools.spanToLemmaList(tokens, object);
        this.unit = Tools.spanToLemmaList(tokens, unit);
        this.rate = Tools.spanToLemmaList(tokens, rate);
        if (verb >= 0) {
            this.verbLemma = tokens.get(verb).lemma();
        }
        if(math >= 0) {
            this.math = tokens.get(math).lemma();
        }
        subjectPos = Tools.populatePos(tokens, subject);
        objectPos = Tools.populatePos(tokens, object);
        unitPos = Tools.populatePos(tokens, unit);
    }

    public LogicInput(int mode, StanfordSchema schema, List<CoreLabel> tokens) {
        this.mode = mode;
        if (tokens == null) return;
        this.sentId = schema.sentId;
        this.subject = Tools.spanToLemmaList(tokens, schema.subject);
        this.object = Tools.spanToLemmaList(tokens, schema.object);
        this.unit = Tools.spanToLemmaList(tokens, schema.unit);
        this.unit.remove("many");
        this.unit.remove("much");
        this.rate = Tools.spanToLemmaList(tokens, schema.rate);
        if (schema.verb >= 0) {
            this.verbLemma = tokens.get(schema.verb).lemma();
        }
        if (schema.math >= 0) {
            this.math = tokens.get(schema.math).lemma();
        }
        subjectPos = Tools.populatePos(tokens, schema.subject);
        objectPos = Tools.populatePos(tokens, schema.object);
        unitPos = Tools.populatePos(tokens, schema.unit);
    }
}

public class Logic {

    public static List<String> labels = Arrays.asList(
            "ADD", "SUB", "SUB_REV", "MUL", "DIV", "DIV_REV", "NONE");

    public static List<String> addTokens = Arrays.asList("taller", "more", "older", "higher", "faster");
    public static List<String> subTokens = Arrays.asList("shorter", "less", "younger", "slower");
    public static List<String> mulTokens = Arrays.asList("times");

    public static int maxNumInferenceTypes = 4;

    public static Map<String, Double> containerCoref(LogicInput num1, LogicInput num2) {
        Map<String, Double> map = new HashMap<>();
        map.put("0_0", Tools.jaccardSim(num1.subject, num2.subject));
        map.put("0_1", Tools.jaccardSim(num1.subject, num2.object));
        map.put("1_0", Tools.jaccardSim(num1.object, num2.subject));
        map.put("1_1", Tools.jaccardSim(num1.object, num2.object));
        return map;
    }

    public static Map<String, Double> unitDependency(LogicInput num1, LogicInput num2) {
        int isRate1 = 0, isRate2 = 0;
        if (num1.rate != null && num1.rate.size() > 0) {
            isRate1 = 1;
        }
        if (num2.rate != null && num2.rate.size() > 0) {
            isRate2 = 1;
        }
        Map<String, Double> map = new HashMap<>();
        map.put("SAME_UNIT", Tools.jaccardSim(num1.unit, num2.unit));
        map.put("0_NUM", Tools.jaccardSim(num1.unit, num2.unit)*isRate1);
        map.put("1_NUM", Tools.jaccardSim(num1.unit, num2.unit)*isRate2);
        map.put("0_DEN", Tools.jaccardSim(num1.rate, num2.unit));
        map.put("1_DEN", Tools.jaccardSim(num2.rate, num1.unit));
        map.put("NO_REL", 1 - Tools.jaccardSim(num1.unit, num2.unit));
        return map;
    }

    public static Map<String, Double> partition(LogicInput num1, LogicInput num2) {
        Map<String, Double> map = new HashMap<>();
        map.put("0_HYPO", Tools.jaccardEntail(num1.subject, num2.subject));
        map.put("0_HYPER", Tools.jaccardEntail(num2.subject, num1.subject));
        map.put("1_HYPO", Tools.jaccardEntail(num1.unit, num2.unit));
        map.put("1_HYPER", Tools.jaccardEntail(num2.unit, num1.unit));
        map.put("1_SIBLING", 0.0);

        String wordnetIndicator = Tools.wordnetIndicator(
                num1.unit, num2.unit, num1.unitPos, num2.unitPos);
        if (wordnetIndicator == null) {
            return map;
        }
        if (wordnetIndicator.equals("Hyponyms")) {
            map.put("1_HYPO", 1.0);
        }
        if (wordnetIndicator.equals("Hypernyms")) {
            map.put("1_HYPER", 1.0);
        }
        if (wordnetIndicator.equals("Siblings") || wordnetIndicator.equals("Antonyms")) {
            map.put("1_SIBLING", 1.0);
        }
        return map;
    }

    public static Map<String, Double> math(LogicInput num) {
        Map<String, Double> map = new HashMap<>();
        map.put("ADD", 0.0);
        map.put("SUB", 0.0);
        map.put("MUL", 0.0);
        if (addTokens.contains(num.math)) {
            map.put("ADD", 1.0);
        }
        if (subTokens.contains(num.math)) {
            map.put("SUB", 1.0);
        }
        if (mulTokens.contains(num.math)) {
            map.put("MUL", 1.0);
        }
        return map;
    }

    public static Map<Pair<String, Integer>, Double> logicSolver(
            LogicInput num1, LogicInput num2, LogicInput ques) {

        Map<Pair<String, Integer>, Double> scores = new HashMap<>();

        Map<String, Double> cc12 = containerCoref(num1, num2);
        Map<String, Double> cc1ques = containerCoref(num1, ques);
        Map<String, Double> cc2ques = containerCoref(num2, ques);

        Map<String, Double> vc1 = Verbs.verbClassify(num1);
        Map<String, Double> vc2 = Verbs.verbClassify(num2);
        Map<String, Double> vc_ques = Verbs.verbClassify(ques);

        Map<String, Double> ud12 = unitDependency(num1, num2);
        Map<String, Double> ud1ques = unitDependency(num1, ques);
        Map<String, Double> ud2ques = unitDependency(num2, ques);

        Map<String, Double> part12 = partition(num1, num2);
        Map<String, Double> part1ques = partition(num1, ques);
        Map<String, Double> part2ques = partition(num2, ques);

        Map<String, Double> math1 = math(num1);
        Map<String, Double> math2 = math(num2);
        Map<String, Double> math_ques = math(ques);

        // Reason : verb interaction
        // Container coref, unit dep, verb interaction
        Tools.addToHighestMap(scores, new Pair<>("ADD", 0),
                (ud12.get("SAME_UNIT") + cc12.get("0_0") + Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("POSITIVE"),
                vc1.get("POSITIVE") + vc2.get("POSITIVE"),
                vc1.get("NEGATIVE") + vc2.get("STATE"),
                vc1.get("NEGATIVE") + vc2.get("NEGATIVE"))))/4.0);
        Tools.addToHighestMap(scores, new Pair<>("ADD", 0),
                (ud12.get("SAME_UNIT") + cc12.get("0_1") + Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("NEGATIVE"),
                vc1.get("POSITIVE") + vc2.get("NEGATIVE"))))/4.0);
        Tools.addToHighestMap(scores, new Pair<>("ADD", 0),
                (ud12.get("SAME_UNIT") + cc12.get("1_0") + Collections.max(Arrays.asList(
                vc2.get("STATE") + vc1.get("POSITIVE"),
                vc2.get("POSITIVE") + vc1.get("NEGATIVE"))))/4.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB", 0),
                (ud12.get("SAME_UNIT") + cc12.get("0_0") + Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("STATE"),
                vc1.get("STATE") + vc2.get("NEGATIVE"),
                vc1.get("POSITIVE") + vc2.get("NEGATIVE"))))/4.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB", 0),
                (ud12.get("SAME_UNIT") + cc12.get("0_1") + Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("POSITIVE"),
                vc1.get("POSITIVE") + vc2.get("POSITIVE"))))/4.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 0),
                (ud12.get("SAME_UNIT") + cc12.get("1_0") +
                        vc1.get("NEGATIVE") + vc2.get("STATE"))/4.0);

        // Reason : partition relation
        // Hyponym in wordnet
        Tools.addToHighestMap(scores, new Pair<>("ADD", 1), part12.get("1_SIBLING"));
        Tools.addToHighestMap(scores, new Pair<>("SUB", 1), part12.get("1_HYPER"));
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 1), part12.get("1_HYPO"));
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 1),part1ques.get("1_SIBLING"));
        Tools.addToHighestMap(scores, new Pair<>("SUB", 1), part2ques.get("1_SIBLING"));
        Tools.addToHighestMap(scores, new Pair<>("ADD", 1),
                Math.max(part1ques.get("1_HYPO"), part2ques.get("1_HYPO")));
        Tools.addToHighestMap(scores, new Pair<>("SUB", 1), part1ques.get("1_HYPER"));
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 1), part2ques.get("1_HYPER"));

        // Partition of subject
        if (num1.verbLemma != null && num2.verbLemma != null &&
                ques.verbLemma != null && num1.verbLemma.equals(num2.verbLemma) &&
                num1.verbLemma.equals(ques.verbLemma)) {
            Tools.addToHighestMap(scores, new Pair<>("ADD", 1), part1ques.get("0_HYPO"));
            Tools.addToHighestMap(scores, new Pair<>("ADD", 1), part1ques.get("1_HYPO"));
        }

        // Reason : math
        // Container coref, math
        Tools.addToHighestMap(scores, new Pair<>("ADD", 2), (cc12.get("0_1") + math2.get("ADD"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("ADD", 2), (cc12.get("1_0") + math1.get("ADD"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB", 2), (cc12.get("0_0") + math2.get("ADD"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 2), (cc12.get("0_0") + math1.get("ADD"))/2.0);

        Tools.addToHighestMap(scores, new Pair<>("SUB", 2), (cc12.get("0_1") + math2.get("SUB"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 2), (cc12.get("1_0") + math1.get("SUB"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("ADD", 2), (cc12.get("0_0") + math2.get("SUB"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("ADD", 2), (cc12.get("0_0") + math1.get("SUB"))/2.0);

        Tools.addToHighestMap(scores, new Pair<>("MUL", 2), (cc12.get("0_1") + math2.get("MUL"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("MUL", 2), (cc12.get("1_0") + math1.get("MUL"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("DIV", 2), (cc12.get("0_0") + math2.get("MUL"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("DIV_REV", 2), (cc12.get("0_0") + math1.get("MUL"))/2.0);

        Tools.addToHighestMap(scores, new Pair<>("SUB", 2),
                (cc1ques.get("0_0") + cc2ques.get("0_1") + math_ques.get("ADD")) / 3.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 2),
                (cc1ques.get("0_1") + cc2ques.get("0_0") + math_ques.get("ADD")) / 3.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 2),
                (cc1ques.get("0_0") + cc2ques.get("0_1") + math_ques.get("SUB")) / 3.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB", 2),
                (cc1ques.get("0_1") + cc2ques.get("0_0") + math_ques.get("SUB")) / 3.0);
        Tools.addToHighestMap(scores, new Pair<>("DIV", 2),
                (cc1ques.get("0_0") + cc2ques.get("0_1") + math_ques.get("MUL")) / 3.0);
        Tools.addToHighestMap(scores, new Pair<>("DIV_REV", 2),
                (cc1ques.get("0_1") + cc2ques.get("0_0") + math_ques.get("MUL")) / 3.0);

        // Reason : rate
        // Unit dep
        Tools.addToHighestMap(scores, new Pair<>("MUL", 3),
                Math.max(ud12.get("0_DEN"), ud12.get("1_DEN")));
        if (num1.rate != null && num1.rate.size() > 0) {
            Tools.addToHighestMap(scores, new Pair<>("DIV_REV", 3), ud12.get("0_NUM"));
        }
        if (num2.rate != null && num2.rate.size() > 0) {
            Tools.addToHighestMap(scores, new Pair<>("DIV", 3), ud12.get("1_NUM"));
        }
        Tools.addToHighestMap(scores, new Pair<>("DIV", 3),
                (ud1ques.get("1_NUM") + ud2ques.get("1_DEN"))/2.0);
        Tools.addToHighestMap(scores, new Pair<>("DIV_REV", 3),
                (ud1ques.get("1_DEN") + ud2ques.get("1_NUM"))/2.0);
        return scores;
    }

}
