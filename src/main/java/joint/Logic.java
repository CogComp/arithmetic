package joint;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.stanford.nlp.ling.CoreLabel;
import reader.Reader;
import structure.StanfordProblem;
import structure.StanfordSchema;
import utils.Params;
import utils.Tools;
import java.util.*;


public class Logic {

    public static List<String> labels = Arrays.asList(
            "ADD", "SUB", "SUB_REV", "MUL", "DIV", "DIV_REV");
    public static List<String> addTokens = Arrays.asList(
            "taller", "more", "older", "higher", "faster");
    public static List<String> subTokens = Arrays.asList(
            "shorter", "less", "younger", "slower");
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
        map.put("0_NUM", -Double.MAX_VALUE);
        map.put("1_NUM", -Double.MAX_VALUE);
        map.put("0_DEN", -Double.MAX_VALUE);
        map.put("1_DEN", -Double.MAX_VALUE);
        map.put("SAME_UNIT", Tools.jaccardSim(num1.unit, num2.unit));
        if(isRate1 > 0.9) {
            map.put("0_NUM", Tools.jaccardSim(num1.unit, num2.unit));
            map.put("0_DEN", Tools.jaccardSim(num1.rate, num2.unit));
        }
        if(isRate2 > 0.9) {
            map.put("1_NUM", Tools.jaccardSim(num1.unit, num2.unit) * isRate2);
            map.put("1_DEN", Tools.jaccardSim(num2.rate, num1.unit));
        }
        return map;
    }

    public static Map<String, Double> partition(LogicInput num1, LogicInput num2) {
        Map<String, Double> map = new HashMap<>();
        map.put("0_HYPO", (Tools.jaccardEntail(num1.subject, num2.subject) +
                1 - Tools.jaccardEntail(num2.subject, num1.subject))/2.0);
        map.put("0_HYPER", (Tools.jaccardEntail(num2.subject, num1.subject) +
                1 - Tools.jaccardEntail(num1.subject, num2.subject))/2.0);
        map.put("1_HYPO", (Tools.jaccardEntail(num1.unit, num2.unit) +
                1 - Tools.jaccardEntail(num2.unit, num1.unit))/2.0);
        map.put("1_HYPER", (Tools.jaccardEntail(num2.unit, num1.unit) +
                1 - Tools.jaccardEntail(num1.unit, num2.unit))/2.0);
        map.put("0_SIBLING", Tools.jaccardSim(num1.subject, num2.subject));
        map.put("1_SIBLING", Tools.jaccardSim(num1.unit, num2.unit));

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
        map.put("ADD", -Double.MAX_VALUE);
        map.put("SUB", -Double.MAX_VALUE);
        map.put("MUL", -Double.MAX_VALUE);
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

    public static Pair<String, Integer> bestAnswerFromLogicSolver(
            LogicInput num1, LogicInput num2, LogicInput ques, boolean isTopmost) {
        Map<Pair<String, Integer>, Double> logicOutput =
                Logic.logicSolver(num1, num2, ques, isTopmost);
        String bestLabel = null;
        Double bestScore = Double.NEGATIVE_INFINITY;
        // Priority of reason : 2 > 3 > 1 (if verbs are matching) > 0
        List<Integer> reasonPriority = Arrays.asList(2, 3, 1, 0);
        for(int reason : reasonPriority) {
            if(reason == 1) {
                if(isTopmost && (!num1.verbLemma.equals(num2.verbLemma) ||
                        !num1.verbLemma.equals(ques.verbLemma) ||
                        !num2.verbLemma.equals(ques.verbLemma))) {
                    continue;
                }
                if(!isTopmost && (!num1.verbLemma.equals(num2.verbLemma))) {
                    continue;
                }
            }
            for(Pair<String, Integer> pair : logicOutput.keySet()) {
                double score = logicOutput.get(pair);
                if (pair.getSecond() == reason && bestScore < score &&
                        score > -100.0) {
                    bestLabel = pair.getFirst();
                    bestScore = score;
                }
            }
            if(bestLabel != null) {
                return new Pair<>(bestLabel, reason);
            }
        }
        return new Pair<>(bestLabel, -1);
    }

    public static Map<Pair<String, Integer>, Double> logicSolver(
            LogicInput num1, LogicInput num2, LogicInput ques, boolean isTopmost) {

        Map<Pair<String, Integer>, Double> scores = new HashMap<>();

        Map<String, Double> cc12 = containerCoref(num1, num2);
        Map<String, Double> cc1ques = containerCoref(num1, ques);
        Map<String, Double> cc2ques = containerCoref(num2, ques);

        Map<String, Double> vc1, vc2, vc_ques;
        vc1 = transformVerbCategoryBasedonContainers(Verbs.verbClassify(num1), cc1ques);
        vc2 = transformVerbCategoryBasedonContainers(Verbs.verbClassify(num2), cc2ques);
        vc_ques = Verbs.verbClassify(ques);

        Map<String, Double> ud12 = unitDependency(num1, num2);
        Map<String, Double> ud1ques = unitDependency(num1, ques);
        Map<String, Double> ud2ques = unitDependency(num2, ques);

        Map<String, Double> part12 = partition(num1, num2);
        Map<String, Double> part1ques = partition(num1, ques);
        Map<String, Double> part2ques = partition(num2, ques);

        Map<String, Double> math1 = math(num1);
        Map<String, Double> math2 = math(num2);
        Map<String, Double> math_ques = math(ques);

        // Make all question properties 0, since its not useful unless its a
        // topmost operation
        if(!isTopmost) {
            for(String key : cc1ques.keySet()) {
                cc1ques.put(key, 0.0);
            }
            for(String key : cc2ques.keySet()) {
                cc2ques.put(key, 0.0);
            }
            for(String key : vc_ques.keySet()) {
                vc_ques.put(key, 0.0);
            }
            for(String key : ud1ques.keySet()) {
                ud1ques.put(key, 0.0);
            }
            for(String key : ud2ques.keySet()) {
                ud2ques.put(key, 0.0);
            }
            for(String key : part1ques.keySet()) {
                part1ques.put(key, 0.0);
            }
            for(String key : part2ques.keySet()) {
                part2ques.put(key, 0.0);
            }
            for(String key : math_ques.keySet()) {
                math_ques.put(key, 0.0);
            }
        }

        // Reason : verb interaction
        // Container coref, unit dep, verb interaction
        Tools.addToHighestMap(scores, new Pair<>("ADD", 0), Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("POSITIVE") + vc_ques.get("STATE"),
                vc1.get("POSITIVE") + vc2.get("POSITIVE") + vc_ques.get("STATE"),
                vc1.get("NEGATIVE") + vc2.get("STATE") + vc_ques.get("STATE"),
                vc1.get("NEGATIVE") + vc2.get("NEGATIVE") + vc_ques.get("STATE")))/3.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB", 0), Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("STATE") + vc_ques.get("NEGATIVE"),
                vc1.get("STATE") + vc2.get("NEGATIVE") + vc_ques.get("STATE"),
                vc1.get("POSITIVE") + vc2.get("NEGATIVE") + vc_ques.get("STATE")))/3.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 0), Collections.max(Arrays.asList(
                vc1.get("STATE") + vc2.get("STATE") + vc_ques.get("POSITIVE"),
                vc1.get("POSITIVE") + vc2.get("STATE") + vc_ques.get("STATE"),
                vc1.get("NEGATIVE") + vc2.get("POSITIVE") + vc_ques.get("STATE")))/3.0);

        // Reason : partition relation
        // Hyponym in wordnet
        Tools.addToHighestMap(scores, new Pair<>("ADD", 1), (part12.get("1_SIBLING") +
                (part1ques.get("1_HYPO")+1-part1ques.get("1_HYPER")) +
                (part2ques.get("1_HYPO")+1-part2ques.get("1_HYPER")))/5.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB", 1), (part2ques.get("1_SIBLING") +
                (part12.get("1_HYPER")+1-part12.get("1_HYPO")) +
                (part1ques.get("1_HYPER")+1-part1ques.get("1_HYPO")))/5.0);
        Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 1), (part1ques.get("1_SIBLING") +
                (part12.get("1_HYPO")+1-part12.get("1_HYPER")) +
                (part2ques.get("1_HYPER")+1-part2ques.get("1_HYPO")))/5.0);

        // Partition of subject
        // Extra 1 is added because of the verb match
        if (num1.verbLemma != null && num2.verbLemma != null &&
                num1.verbLemma.equals(num2.verbLemma)) {
            Tools.addToHighestMap(scores, new Pair<>("ADD", 1), (part12.get("0_SIBLING") +
                    (part1ques.get("0_HYPO")+1-part1ques.get("0_HYPER")) +
                    (part2ques.get("0_HYPO")+1-part2ques.get("0_HYPER")))/5.0);
            Tools.addToHighestMap(scores, new Pair<>("SUB", 1), (part2ques.get("0_SIBLING") +
                    (part12.get("0_HYPER")+1-part12.get("0_HYPO")) +
                    (part1ques.get("0_HYPER")+1-part1ques.get("0_HYPO")))/5.0);
            Tools.addToHighestMap(scores, new Pair<>("SUB_REV", 1), (part1ques.get("0_SIBLING") +
                    (part12.get("0_HYPO")+1-part12.get("0_HYPER")) +
                    (part2ques.get("0_HYPER")+1-part2ques.get("0_HYPO")))/5.0);
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

    public static Map<String, Double> transformVerbCategoryBasedonContainers(
            Map<String, Double> vc, Map<String, Double> ccQuestion) {
        Map<String, Double> map = new HashMap<>();
        map.put("STATE", vc.get("STATE"));
        map.put("POSITIVE", Math.max(vc.get("POSITIVE")+ccQuestion.get("0_0"),
                vc.get("NEGATIVE")+ccQuestion.get("1_0"))/2.0);
        map.put("NEGATIVE", Math.max(vc.get("NEGATIVE")+ccQuestion.get("0_0"),
                vc.get("POSITIVE")+ccQuestion.get("1_0"))/2.0);
        return map;
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

    public static void testLogicSolverWithDefaultExtraction(String dataset) throws Exception {
        Set<Integer> incorrect = new HashSet<>();
        Set<Integer> total = new HashSet<>();
        List<StanfordProblem> probs = Reader.readStanfordProblemsFromJson(dataset);
        SLProblem sp = joint.LogicDriver.getSP(probs);
        for (int i = 0; i < sp.instanceList.size(); i++) {
            LogicX x = (LogicX) sp.instanceList.get(i);
            LogicY gold = (LogicY) sp.goldStructureList.get(i);
            if(x.quantities.size() != 2) continue;
            LogicInput num1 = new LogicInput(1, x.schema.get(0),
                    x.tokens.get(x.schema.get(0).sentId));
            LogicInput num2 = new LogicInput(2, x.schema.get(1),
                    x.tokens.get(x.schema.get(1).sentId));
            LogicInput ques = new LogicInput(0, x.questionSchema,
                    x.questionSchema.sentId >= 0 ?
                            x.tokens.get(x.questionSchema.sentId): null);
            Pair<String, Integer> bestKey = bestAnswerFromLogicSolver(num1, num2, ques, true);
            String op = bestKey.getFirst();
            Double soln = 0.0;
            if(op.equals("ADD")) soln = x.quantities.get(0).val + x.quantities.get(1).val;
            if(op.equals("SUB")) soln = x.quantities.get(0).val - x.quantities.get(1).val;
            if(op.equals("SUB_REV")) soln = x.quantities.get(1).val - x.quantities.get(0).val;
            if(op.equals("MUL")) soln = x.quantities.get(0).val * x.quantities.get(1).val;
            if(op.equals("DIV")) soln = x.quantities.get(0).val / x.quantities.get(1).val;
            if(op.equals("DIV_REV")) soln = x.quantities.get(1).val / x.quantities.get(0).val;
            total.add(x.problemId);
            if(!Tools.safeEquals(gold.expr.getValue(), soln)) {
                incorrect.add(x.problemId);
                System.out.println(x.problemId + " : " + x.text);
                System.out.println();
                for (StanfordSchema schema : x.schema) {
                    System.out.println(schema);
                }
                System.out.println(x.questionSchema);
                System.out.println();
//                System.out.println("Quantities : " + x.quantities);
//                System.out.println();
//                System.out.println("Verb1 : " + Arrays.asList(Verbs.verbClassify(num1)));
//                System.out.println("Verb2 : " + Arrays.asList(Verbs.verbClassify(num2)));
//                System.out.println("VerbQues : " + Arrays.asList(Verbs.verbClassify(ques)));
//                System.out.println();
//                System.out.println("Part12 : " + Arrays.asList(Logic.partition(num1, num2)));
//                System.out.println("Part1Ques : " + Arrays.asList(Logic.partition(num1, ques)));
//                System.out.println("Part2Ques : " + Arrays.asList(Logic.partition(num2, ques)));
//                System.out.println();
//                System.out.println("UD12 : " + Arrays.asList(Logic.unitDependency(num1, num2)));
//                System.out.println("UD1Ques : " + Arrays.asList(Logic.unitDependency(num1, ques)));
//                System.out.println("UD2Ques : " + Arrays.asList(Logic.unitDependency(num2, ques)));
//                System.out.println();
//                System.out.println("Math1 : " + Arrays.asList(Logic.math(num1)));
//                System.out.println("Math2 : " + Arrays.asList(Logic.math(num2)));
//                System.out.println("Math3 : " + Arrays.asList(Logic.math(ques)));
//                System.out.println();
                System.out.println("Gold : " + gold);
                System.out.println("PredOp : " + bestKey.getFirst());
                System.out.println("PredReason : " + bestKey.getSecond());
                System.out.println();
            }
        }
        System.out.println("Strict Accuracy : = 1 - " + incorrect.size() + " / " +
                total.size() + " = " + (1-1.0*incorrect.size()/total.size()));
    }

    public static void main(String args[]) throws Exception {
//        testIrrelevanceWithDefaultExtraction(Params.allArithDir);
        testLogicSolverWithDefaultExtraction(Params.allArithDir);
    }

}
