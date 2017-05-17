package utils;

import java.util.*;

import edu.illinois.cs.cogcomp.bigdata.mapdb.MapDB;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.nlp.util.SimpleCachingPipeline;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.data.list.*;
import net.didion.jwnl.dictionary.Dictionary;
import structure.SimpleQuantifier;
import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.annotation.handler.IllinoisChunkerHandler;
import edu.illinois.cs.cogcomp.annotation.handler.IllinoisLemmatizerHandler;
import edu.illinois.cs.cogcomp.annotation.handler.IllinoisNerHandler;
import edu.illinois.cs.cogcomp.annotation.handler.IllinoisPOSHandler;
import edu.illinois.cs.cogcomp.annotation.handler.StanfordDepHandler;
import edu.illinois.cs.cogcomp.annotation.handler.StanfordParseHandler;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.tokenizer.IllinoisTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.CcgTextAnnotationBuilder;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.ParserAnnotator;

public class Tools {
	
	public static SimpleQuantifier quantifier;
	public static AnnotatorService pipeline;
	public static StanfordCoreNLP stanfordPipeline;
	public static Map<String, List<CoreMap>> cache;

	static {
		try {
			quantifier = new SimpleQuantifier();
			cache = MapDB.newDefaultDb("cache", "cache").make().getHashMap("cache");

			System.out.println("Initializing Wordnet ...");
			JWNL.initialize();


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<CoreMap> annotateWithStanfordCoreNLP(String text) {
		if (cache.containsKey(text)) {
			return cache.get(text);
		} else {
			Annotation annotation = new Annotation(text);
			stanfordPipeline.annotate(annotation);
			List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
			cache.put(text, sentences);
			return sentences;
		}
	}

	public static boolean safeEquals(Double d1, Double d2) {
		if(d1 == null && d2 == null) return true;
		if(d1 == null || d2 == null) {
			return false;
		}
		if(d1 > d2 - 0.0001 && d1 < d2 + 0.0001) {
			return true;
		}
		return false;
	}
	
	public static boolean contains(List<Double> arr, Double key) {
		for(Double d : arr) {
			if(Tools.safeEquals(d, key)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean equals(List<Double> arr1, List<Double> arr2) {
		if(arr1 == null || arr2 == null) return false;
		if(arr1.size() != arr2.size()) return false;
		for(Double d1 : arr1) {
			boolean found = false;
			for(Double d2 : arr2) {
				if(Tools.safeEquals(d1, d2)) {
					found = true;
				}
			}
			if(!found) return false;
		}
		return true;
	}
	
	// Returns id of first question sentence, -1 if none found
	public static int getQuestionSentenceId(TextAnnotation ta) {
		int questionSentId = 0;
		for(int i=0; i<ta.getNumberOfSentences(); ++i) {
			Sentence sent = ta.getSentence(i);
			if(sent.getText().trim().endsWith("?")) {
				questionSentId = i;
				break;
			}
		}
		return questionSentId;
	}
	
	public static int getNumTokenMatches(
			List<String> tknList1, List<String> tknList2) {
		int sim = 0;
		Set<String> s1 = new HashSet<>();
		Set<String> s2 = new HashSet<>();
		Set<String> unImportant = new HashSet<String>(
				Arrays.asList("a","an","the"));
		for(String tkn1 : tknList1) {
			s1.add(tkn1);
			for(String tkn2 : tknList2) {
				s2.add(tkn2);
			}
		}
		for(String tkn1 : s1) {
			if(unImportant.contains(tkn1)) continue;
			for(String tkn2 : s2) {
				if(tkn1.toLowerCase().equals(tkn2.toLowerCase())) {
					sim += 1;
					break;
				}
			}
		}
		return sim;
	}
	
	public static List<String> getTokensList(Constituent cons) {
		List<String> tokens = new ArrayList<String>();
		TextAnnotation ta = cons.getTextAnnotation();
		int startIndex = cons.getStartSpan();
		int endIndex = cons.getEndSpan();
		for(int i=startIndex; i<endIndex; ++i) {
			tokens.add(ta.getToken(i));
		}
		return tokens;
	}
	
	public static void printCons(List<Constituent> constituents) {
		for(Constituent cons : constituents) {
			System.out.println(cons.getLabel()+" : "+cons.getSurfaceForm()+" : "+cons.getSpan());
		}
	}

	public static double jaccardSim(List<String> phrase1, List<String> phrase2) {
		if((phrase1 == null || phrase1.size() == 0) &&
				(phrase2 == null || phrase2.size() == 0)) {
			return 0.0;
		}
		if(phrase1 != null && phrase2 != null) {
			if (phrase1.contains("$") || phrase1.contains("money") || phrase1.contains("dollar") ||
					phrase1.contains("buck") || phrase1.contains("cent")) {
				if (phrase2.contains("$") || phrase2.contains("money") || phrase2.contains("dollar") ||
						phrase2.contains("buck") || phrase2.contains("cent")) {
					return 1.0;
				}
			}
		}
		if(phrase1 != null && (phrase1.contains("he") || phrase1.contains("she") ||
				phrase1.contains("this") || phrase1.contains("they"))) {
			return 1.0;
		}
		if(phrase2 != null && (phrase2.contains("he") || phrase2.contains("she") ||
				phrase2.contains("this") || phrase2.contains("they"))) {
			return 1.0;
		}
		Set<String> tokens1 = new HashSet<>();
		tokens1.addAll(phrase1);
		Set<String> tokens2 = new HashSet<>();
		tokens2.addAll(phrase2);
		int union = tokens1.size();
		int intersection = 0;
		for (String token : tokens2) {
			if (tokens1.contains(token)) {
				intersection++;
			} else {
				union++;
			}
		}
		return intersection*1.0/union;
	}

	public static double relevanceSim(List<String> phrase1, List<String> phrase2) {
		if((phrase1 == null || phrase1.size() == 0) &&
				(phrase2 == null || phrase2.size() == 0)) {
			return 0.0;
		}
		if(phrase1 != null && phrase2 != null) {
			if (phrase1.contains("$") || phrase1.contains("money") || phrase1.contains("dollar") ||
					phrase1.contains("buck") || phrase1.contains("cent")) {
				if (phrase2.contains("$") || phrase2.contains("money") || phrase2.contains("dollar") ||
						phrase2.contains("buck") || phrase2.contains("cent")) {
					return 1.0;
				}
			}
		}
		if(phrase1 != null && (phrase1.contains("he") || phrase1.contains("she") ||
				phrase1.contains("this") || phrase1.contains("they"))) {
			return 1.0;
		}
		if(phrase2 != null && (phrase2.contains("he") || phrase2.contains("she") ||
				phrase2.contains("this") || phrase2.contains("they"))) {
			return 1.0;
		}
		Set<String> tokens1 = new HashSet<>();
		tokens1.addAll(phrase1);
		Set<String> tokens2 = new HashSet<>();
		tokens2.addAll(phrase2);
		int union = tokens1.size();
		int intersection = 0;
		for (String token : tokens2) {
			if (tokens1.contains(token)) {
				intersection++;
			} else {
				union++;
			}
		}
		return intersection*1.0/Math.min(phrase1.size(), phrase2.size());
	}

	public static double jaccardEntail(List<String> phrase1, List<String> phrase2) {
		if(phrase1 == null || phrase2 == null ||
				phrase1.size() == 0 || phrase2.size() == 0) {
			return 0.0;
		}
		if(phrase1.get(phrase1.size()-1).equalsIgnoreCase("her") ||
				phrase1.contains("him")  || phrase1.contains("them") ||
				phrase1.contains("he") || phrase1.contains("she") ) {
			return 1.0;
		}
		if(phrase2.contains("them") || phrase2.contains("they")) {
			return 1.0;
		}
		Set<String> tokens1 = new HashSet<>();
		tokens1.addAll(phrase1);
		Set<String> tokens2 = new HashSet<>();
		tokens2.addAll(phrase2);
		int intersection = 0;
		for (String token : tokens2) {
			if (tokens1.contains(token)) {
				intersection++;
			}
		}
		return intersection*1.0/tokens2.size();
	}

	public static List<String> consToList(Constituent cons) {
		List<String> tokens = new ArrayList<>();
		if (cons == null) {
			return tokens;
		}
		for(int i=cons.getStartSpan(); i<cons.getEndSpan(); ++i) {
			tokens.add(cons.getTextAnnotation().getToken(i));
		}
		return tokens;
	}

	public static List<String> spanToLemmaList(List<CoreLabel> tokens, IntPair span) {
		List<String> lemmas = new ArrayList<>();
		if (span == null || span.getFirst() == -1) {
			return lemmas;
		}
		for(int i=span.getFirst(); i<span.getSecond(); ++i) {
			if(tokens.get(i).lemma().equals("more") ||
					tokens.get(i).lemma().equals("much") ||
					tokens.get(i).lemma().equals("many") ||
					tokens.get(i).word().equals("his") ||
					(tokens.get(i).word().equals("her") &&
							span.getSecond()-span.getFirst()>1)) {
				continue;
			}
			lemmas.add(tokens.get(i).lemma());
		}
		return lemmas;
	}

	public static List<String> populatePos(List<CoreLabel> tokens, IntPair span) {
		List<String> seqPos = new ArrayList<>();
		for(int i=span.getFirst(); i<span.getSecond(); ++i) {
			seqPos.add(tokens.get(i).tag());
		}
		return seqPos;
	}

	public static IndexWord getIndexWord(String token, String posTag) {
		POS pos= null;
		if (posTag.startsWith("N")) {
			pos = POS.NOUN;
		}
		if (posTag.startsWith("J")) {
			pos = POS.ADJECTIVE;
		}
		if (posTag.startsWith("V")) {
			pos = POS.VERB;
		}
		if(pos == null) {
			return null;
		}
		IndexWord word = null;
		try {
			word = Dictionary.getInstance().lookupIndexWord(pos, token);
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		return word;
	}

	public static String wordnetIndicator(List<String> tokens1, List<String> tokens2,
										  List<String> posTags1, List<String> posTags2,
										  Map<Pair<String, String>, String> cache) {
		if (tokens1 == null || tokens2 == null || posTags1 == null || posTags2 == null) {
			return null;
		}
		List<String> colors = Arrays.asList("white", "black", "red", "green", "yellow", "brown", "blue", "gray");
		for (int i=0; i<tokens1.size(); ++i) {
			for (int j=0; j<tokens2.size(); ++j) {
				if (colors.contains(tokens1.get(i)) && colors.contains(tokens2.get(j)) &&
						!tokens1.get(i).equalsIgnoreCase(tokens2.get(j))) {
					return "Siblings";
				}
				String wn;
				// Optimization on time, call wordnet only when not found in cache
				if(cache == null) {
					wn = wordNetIndicator(tokens1.get(i), tokens2.get(j),
							posTags1.get(i), posTags2.get(j));
				} else {
					wn = cache.getOrDefault(new Pair<>(tokens1.get(i), tokens2.get(j)), null);
				}
				if (wn != null) {
					return wn;
				}
			}
		}
		return null;
	}

	public static String wordNetIndicator(String lemma1, String lemma2, String pos1, String pos2) {
		if (lemma1.equalsIgnoreCase(lemma2)) {
			return null;
		}
		IndexWord word1 = getIndexWord(lemma1, pos1);
		IndexWord word2 = getIndexWord(lemma2, pos2);
		if(word1 == null || word2 == null) return null;
		try {
			for(Synset synset1 : word1.getSenses()) {
                PointerTargetNodeList list = PointerUtils.getInstance().getAntonyms(synset1);
                for (Iterator itr = list.iterator(); itr.hasNext(); ) {
					Synset antonym = ((PointerTargetNode) itr.next()).getSynset();
                    for (Synset synset2 : word2.getSenses()) {
                        if (synset2.equals(antonym)) {
                            return "Antonyms";
                        }
                    }
                }
            }
			for(Synset synset1 : word1.getSenses()) {
				PointerTargetNodeList list = PointerUtils.getInstance().getDirectHypernyms(synset1);
				for (Iterator itr = list.iterator(); itr.hasNext(); ) {
					Synset hypernym1 = ((PointerTargetNode) itr.next()).getSynset();
					for (Synset synset2 : word2.getSenses()) {
						if (synset2.equals(hypernym1)) {
							return "Hyponyms";
						}
					}
					PointerTargetNodeList list1 = PointerUtils.getInstance().getDirectHypernyms(hypernym1);
					for (Iterator itr1 = list1.iterator(); itr1.hasNext(); ) {
						Synset hypernym2 = ((PointerTargetNode) itr1.next()).getSynset();
						for (Synset synset2 : word2.getSenses()) {
							if (synset2.equals(hypernym2)) {
								return "Hyponyms";
							}
						}
					}
				}
			}
			for(Synset synset1 : word1.getSenses()) {
				PointerTargetNodeList list = PointerUtils.getInstance().getDirectHyponyms(synset1);
				for (Iterator itr = list.iterator(); itr.hasNext(); ) {
					Synset hyponym1 = ((PointerTargetNode) itr.next()).getSynset();
					for (Synset synset2 : word2.getSenses()) {
						if (synset2.equals(hyponym1)) {
							return "Hypernyms";
						}
					}
					PointerTargetNodeList list1 = PointerUtils.getInstance().getDirectHyponyms(hyponym1);
					for (Iterator itr1 = list1.iterator(); itr1.hasNext(); ) {
						Synset hyponym2 = ((PointerTargetNode) itr1.next()).getSynset();
						for (Synset synset2 : word2.getSenses()) {
							if (synset2.equals(hyponym2)) {
								return "Hypernyms";
							}
						}
					}
				}
			}
			for(Synset synset1 : word1.getSenses()) {
				PointerTargetNodeList list =  PointerUtils.getInstance().getDirectHypernyms(synset1);
				for (Iterator itr = list.iterator(); itr.hasNext();) {
					Synset hyper = ((PointerTargetNode) itr.next()).getSynset();
					PointerTargetNodeList list1 =  PointerUtils.getInstance().getDirectHyponyms(hyper);
					for (Iterator itr1 = list1.iterator(); itr1.hasNext();) {
						Synset sibling = ((PointerTargetNode) itr1.next()).getSynset();
						for (Synset synset2 : word2.getSenses()) {
							if (synset2.equals(sibling) && !synset2.equals(synset1)) {
								return "Siblings";
							}
						}
					}
				}
			}
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int getSentenceIdFromCharOffset(List<List<CoreLabel>> tokens, int charOffset) {
		for(int i=0; i<tokens.size(); ++i) {
			if (charOffset >= tokens.get(i).get(0).beginPosition() &&
					charOffset < tokens.get(i).get(tokens.get(i).size()-1).endPosition()) {
				return i;
			}
		}
		return -1;
	}

	public static int getTokenIdFromCharOffset(List<CoreLabel> tokens, int charOffset) {
		for(int i=0; i<tokens.size(); ++i) {
			if (charOffset >= tokens.get(i).beginPosition() &&
					charOffset < tokens.get(i).endPosition()) {
				return i;
			}
		}
		return -1;
	}

	public static <T> T getKeyForMaxValue(Map<T, Double> map) {
		Double bestScore = -Double.MAX_VALUE;
		T best = null;
		for(T key : map.keySet()) {
			if(map.get(key) > bestScore) {
				bestScore = map.get(key);
				best = key;
			}
		}
		return best;
	}

	public static IntPair getMaximalNounPhraseSpan(
			List<CoreLabel> tokens, int tokenId) {
		int start = tokenId, end = tokenId+1;
		for(int i=tokenId+1; i<tokens.size(); ++i) {
			if(tokens.get(i).tag().startsWith("N")) {
				end = i+1;
			} else {
				break;
			}
		}
		for(int i=tokenId-1; i>=0; --i) {
			if(tokens.get(i).tag().startsWith("N")) {
				start = i;
			} else if (tokens.get(i).tag().startsWith("J")) {
				start = i;
				break;
			} else {
				break;
			}
		}
		return new IntPair(start, end);
	}

	public static void initIllinoisTools() throws Exception {
		ResourceManager rm = new ResourceManager(Params.pipelineConfig);
		IllinoisTokenizer tokenizer = new IllinoisTokenizer();
		TextAnnotationBuilder taBuilder = new CcgTextAnnotationBuilder(tokenizer);
		IllinoisPOSHandler pos = new IllinoisPOSHandler();
		IllinoisChunkerHandler chunk = new IllinoisChunkerHandler();
		IllinoisNerHandler nerConll = new IllinoisNerHandler(rm, ViewNames.NER_CONLL);
		IllinoisLemmatizerHandler lemma = new IllinoisLemmatizerHandler(rm);

		Properties stanfordProps = new Properties();
		stanfordProps.put("annotators", "pos, parse");
		stanfordProps.put("parse.originalDependencies", true);

		POSTaggerAnnotator posAnnotator = new POSTaggerAnnotator("pos", stanfordProps);
		ParserAnnotator parseAnnotator = new ParserAnnotator("parse", stanfordProps);

		StanfordParseHandler parser = new StanfordParseHandler(posAnnotator, parseAnnotator);
		StanfordDepHandler depParser = new StanfordDepHandler(posAnnotator, parseAnnotator);

		Map<String, Annotator> extraViewGenerators = new HashMap<>();

		extraViewGenerators.put(ViewNames.POS, pos);
		extraViewGenerators.put(ViewNames.SHALLOW_PARSE, chunk);
		extraViewGenerators.put(ViewNames.LEMMA, lemma);
		extraViewGenerators.put(ViewNames.NER_CONLL, nerConll);
		extraViewGenerators.put(ViewNames.PARSE_STANFORD, parser);
		extraViewGenerators.put(ViewNames.DEPENDENCY_STANFORD, depParser);

		Map<String, Boolean> requestedViews = new HashMap<>();
		for (String view : extraViewGenerators.keySet())
			requestedViews.put(view, false);

		Tools.pipeline = new SimpleCachingPipeline(taBuilder, extraViewGenerators, rm);
	}

	public static void initStanfordTools() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
		Tools.stanfordPipeline = new StanfordCoreNLP(props);
	}

	public static void main(String args[]) {
		String wn = wordnetIndicator(Arrays.asList("red", "apples"),
				Arrays.asList("green", "apples"), Arrays.asList("N", "N"),
				Arrays.asList("N", "N"), null);
		System.out.println("Wordnet says "+wn);
		wn = wordnetIndicator(Arrays.asList("win"), Arrays.asList("lose"),
				Arrays.asList("V"), Arrays.asList("V"), null);
		System.out.println("Wordnet says "+wn);
	}
}
