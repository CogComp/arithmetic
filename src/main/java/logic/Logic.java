package logic;

import structure.Problem;
import utils.Tools;
import java.util.*;

public class Logic {

    public static List<String> labels = Arrays.asList("ADD", "SUB", "SUB_REV", "MUL", "DIV", "DIV_REV", "NONE");

    public Map<String, Double> containerCoref(Problem problem, int index1, int index2) {
        List<String> subj1 = Tools.consToList(problem.schema.quantSchemas.get(index1).subject);
        List<String> obj1 = Tools.consToList(problem.schema.quantSchemas.get(index1).object);
        List<String> subj2 = Tools.consToList(problem.schema.quantSchemas.get(index2).subject);
        List<String> obj2 = Tools.consToList(problem.schema.quantSchemas.get(index2).object);
        Map<String, Double> map = new HashMap<>();
        map.put("0_0", Tools.jaccardSim(subj1, subj2));
        map.put("0_1", Tools.jaccardSim(subj1, obj2));
        map.put("1_0", Tools.jaccardSim(obj1, subj2));
        map.put("1_1", Tools.jaccardSim(obj1, obj2));
        return map;
    }

    public Map<String, Double> verbClassify(Problem prob, int index) {
        return null;
    }

    public Map<String, Double> unitDependency(Problem prob, int index1, int index2) {
        return null;
    }

    public Map<String, Double> partition(Problem prob, int index1, int index2) {
        return null;
    }

    public Map<String, Double> math(Problem prob, int index) {
        return null;
    }

    public Map<String, Double> lca(Problem problem, int index1, int index2) {
        String verbLemma1 = problem.schema.quantSchemas.get(index1).verbLemma;
        String verbLemma2 = problem.schema.quantSchemas.get(index1).verbLemma;
        String verbLemmaQues = null;

        Map<String, Double> cc12 = containerCoref(problem, index1, index2);
        Map<String, Double> cc1ques = containerCoref(problem, index1, -1);
        Map<String, Double> cc2ques = containerCoref(problem, index2, -1);

        Map<String, Double> vc1 = verbClassify(problem, index1);
        Map<String, Double> vc2 = verbClassify(problem, index2);
        Map<String, Double> vc_ques = verbClassify(problem, -1);

        Map<String, Double> ud12 = unitDependency(problem, index1, index2);
        Map<String, Double> ud1ques = unitDependency(problem, index1, -1);
        Map<String, Double> ud2ques = unitDependency(problem, index2, -1);

        Map<String, Double> part12 = partition(problem, index1, index2);
        Map<String, Double> part1ques = partition(problem, index1, -1);
        Map<String, Double> part2ques = partition(problem, index2, -1);

        Map<String, Double> math1 = math(problem, index1);
        Map<String, Double> math2 = math(problem, index2);
        Map<String, Double> math_ques = math(problem, -1);

        List<Double> add = new ArrayList<>();
        List<Double> sub = new ArrayList<>();
        List<Double> sub_rev = new ArrayList<>();
        List<Double> mul = new ArrayList<>();
        List<Double> div = new ArrayList<>();
        List<Double> div_rev = new ArrayList<>();

        // Reason : verb interaction
        // Container coref, unit dep, verb interaction
        add.add((ud12.get("SAME_UNIT") + cc12.get("0_0") + Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("POSITIVE"),
                vc1.get("POSITIVE") + vc2.get("POSITIVE"),
                vc1.get("NEGATIVE") + vc2.get("STATE"),
                vc1.get("NEGATIVE") + vc2.get("NEGATIVE"))))/4.0);
        add.add((ud12.get("SAME_UNIT") + cc12.get("0_1") + Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("NEGATIVE"),
                vc1.get("POSITIVE") + vc2.get("NEGATIVE"))))/4.0);
        add.add((ud12.get("SAME_UNIT") + cc12.get("1_0") + Collections.max(Arrays.asList(
                vc2.get("STATE") + vc1.get("POSITIVE"),
                vc2.get("POSITIVE") + vc1.get("NEGATIVE"))))/4.0);
        sub.add((ud12.get("SAME_UNIT") + cc12.get("0_0") + Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("STATE"),
                vc1.get("STATE") + vc2.get("NEGATIVE"),
                vc1.get("POSITIVE") + vc2.get("NEGATIVE"))))/4.0);
        sub.add((ud12.get("SAME_UNIT") + cc12.get("0_1") + Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("POSITIVE"),
                vc1.get("POSITIVE") + vc2.get("POSITIVE"))))/4.0);
        sub_rev.add((ud12.get("SAME_UNIT") + cc12.get("1_0") + vc1.get("NEGATIVE") + vc2.get("STATE"))/4.0);

        // Reason : partition relation
        // Hyponym in wordnet
        add.add(part12.get("1_SIBLING"));
        sub.add(part12.get("1_HYPER"));
        sub_rev.add(part12.get("1_HYPO"));
        sub_rev.add(part1ques.get("1_SIBLING"));
        sub.add(part2ques.get("1_SIBLING"));
        add.add(Math.max(part1ques.get("1_HYPO"), part2ques.get("1_HYPO")));
        sub.add(part1ques.get("1_HYPER"));
        sub_rev.add(part2ques.get("1_HYPER"));

        // Partition of subject
        if (verbLemma1 != null && verbLemma2 != null && verbLemmaQues != null &&
                verbLemma1.equals(verbLemma2) && verbLemma1.equals(verbLemmaQues)) {
            add.add(part1ques.get("0_HYPO"));
            add.add(part1ques.get("1_HYPO"));
        }

        // Reason : math
        // Container coref, math
        add.add((cc12.get("0_1") + math2.get("ADD"))/2.0);
        add.add((cc12.get("1_0") + math1.get("ADD"))/2.0);
        sub.add((cc12.get("0_0") + math2.get("ADD"))/2.0);
        sub_rev.add((cc12.get("0_0") + math1.get("ADD"))/2.0);

        sub.add((cc12.get("0_1") + math2.get("SUB"))/2.0);
        sub_rev.add((cc12.get("1_0") + math1.get("SUB"))/2.0);
        add.add((cc12.get("0_0") + math2.get("SUB"))/2.0);
        add.add((cc12.get("0_0") + math1.get("SUB"))/2.0);

        mul.add((cc12.get("0_1") + math2.get("MUL"))/2.0);
        mul.add((cc12.get("1_0") + math1.get("MUL"))/2.0);
        div.add((cc12.get("0_0") + math2.get("MUL"))/2.0);
        div_rev.add((cc12.get("0_0") + math1.get("MUL"))/2.0);

        sub.add((cc1ques.get("0_0") + cc2ques.get("0_1") + math_ques.get("ADD")) / 3.0);
        sub_rev.add((cc1ques.get("0_1") + cc2ques.get("0_0") + math_ques.get("ADD")) / 3.0);
        sub_rev.add((cc1ques.get("0_0") + cc2ques.get("0_1") + math_ques.get("SUB")) / 3.0);
        sub.add((cc1ques.get("0_1") + cc2ques.get("0_0") + math_ques.get("SUB")) / 3.0);
        div.add((cc1ques.get("0_0") + cc2ques.get("0_1") + math_ques.get("MUL")) / 3.0);
        div_rev.add((cc1ques.get("0_1") + cc2ques.get("0_0") + math_ques.get("MUL")) / 3.0);

        // Reason : rate
        // Unit dep
        mul.add(Math.max(ud12.get("0_DEN"), ud12.get("1_DEN")));
        div_rev.add(ud12.get("0_NUM"));
        div.add(ud12.get("1_NUM"));
        div.add((ud1ques.get("1_NUM") + ud2ques.get("1_DEN"))/2.0);
        div_rev.add((ud1ques.get("1_DEN") + ud2ques.get("1_NUM"))/2.0);
        return null;
    }

}
