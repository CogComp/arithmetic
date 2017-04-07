package logic;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import structure.QuantitySchema;
import structure.StanfordSchema;
import utils.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogicInput {

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
