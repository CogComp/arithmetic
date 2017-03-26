package utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.data.list.*;
import net.didion.jwnl.data.relationship.Relationship;
import net.didion.jwnl.data.relationship.RelationshipFinder;
import net.didion.jwnl.data.relationship.RelationshipList;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.commons.io.FileUtils;
import structure.QuantSpan;
import structure.Schema;
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
import edu.illinois.cs.cogcomp.nlp.util.SimpleCachingPipeline;
import edu.illinois.cs.cogcomp.nlp.utility.CcgTextAnnotationBuilder;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.ParserAnnotator;

public class Tools {
	
	public static SimpleQuantifier quantifier;
	public static AnnotatorService pipeline;
	public static Map<String, double[]> vectors;

	static {
		try {
			ResourceManager rm = new ResourceManager(Params.pipelineConfig);
			
	        IllinoisTokenizer tokenizer = new IllinoisTokenizer();
	        TextAnnotationBuilder taBuilder = new CcgTextAnnotationBuilder( tokenizer );
	        IllinoisPOSHandler pos = new IllinoisPOSHandler();
	        IllinoisChunkerHandler chunk = new IllinoisChunkerHandler();
	        IllinoisNerHandler nerConll = new IllinoisNerHandler( rm, ViewNames.NER_CONLL );
	        IllinoisLemmatizerHandler lemma = new IllinoisLemmatizerHandler( rm );

	        Properties stanfordProps = new Properties();
	        stanfordProps.put( "annotators", "pos, parse") ;
	        stanfordProps.put("parse.originalDependencies", true);

	        POSTaggerAnnotator posAnnotator = new POSTaggerAnnotator( "pos", stanfordProps );
	        ParserAnnotator parseAnnotator = new ParserAnnotator( "parse", stanfordProps );

	        StanfordParseHandler parser = new StanfordParseHandler( posAnnotator, parseAnnotator );
	        StanfordDepHandler depParser = new StanfordDepHandler( posAnnotator, parseAnnotator );

	        Map< String, Annotator> extraViewGenerators = new HashMap<String, Annotator>();

	        extraViewGenerators.put( ViewNames.POS, pos );
	        extraViewGenerators.put( ViewNames.SHALLOW_PARSE, chunk );
	        extraViewGenerators.put( ViewNames.LEMMA, lemma );
	        extraViewGenerators.put( ViewNames.NER_CONLL, nerConll );
	        extraViewGenerators.put( ViewNames.PARSE_STANFORD, parser );
	        extraViewGenerators.put( ViewNames.DEPENDENCY_STANFORD, depParser );

	        Map< String, Boolean > requestedViews = new HashMap<String, Boolean>();
	        for ( String view : extraViewGenerators.keySet() )
	            requestedViews.put( view, false );

	        pipeline =  new SimpleCachingPipeline(taBuilder, extraViewGenerators, rm);
			quantifier = new SimpleQuantifier();

			System.out.println("Reading vectors ...");
			vectors = readVectors(Params.vectorsFile);

			System.out.println("Initializing Wordnet ...");
			JWNL.initialize();


		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static double sigmoid(double x) {
		return 1.0/(1+Math.pow(Math.E, -x))*2.0-1.0;
	}
	
	public static boolean doesIntersect(IntPair ip1, IntPair ip2) {
		if(ip1.getFirst() <= ip2.getFirst() && ip2.getFirst() < ip1.getSecond()) {
			return true;
		}
		if(ip2.getFirst() <= ip1.getFirst() && ip1.getFirst() < ip2.getSecond()) {
			return true;
		}
		return false;
	}
	
	// is ip2 subset of ip1
	public static boolean doesContain(IntPair big, IntPair small) {
		if(big.getFirst() <= small.getFirst() && small.getSecond() <= big.getSecond()) {
			return true;
		}
		return false;
	}
	

	public static boolean doesContainNotEqual(IntPair big, IntPair small) {
		if(big.getFirst() == small.getFirst() && big.getSecond() == small.getSecond()) {
			return false;
		}
		if(big.getFirst() <= small.getFirst() && small.getSecond() <= big.getSecond()) {
			return true;
		}
		return false;
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

	public static int getTokenIndex(QuantSpan qs, TextAnnotation ta) {
		return ta.getTokenIdFromCharacterOffset(qs.start);
	}
	
	public static List<Double> uniqueNumbers(List<QuantSpan> quantSpans) {
		List<Double> uniqueNos = new ArrayList<>();
		for(int i=0; i<quantSpans.size(); i++) {
			QuantSpan qs = quantSpans.get(i);
			boolean allow = true;
			for(int j=0; j<i; j++) {
				if(Tools.safeEquals(qs.val, quantSpans.get(j).val)) {
					allow = false;
					break;
				}
			}
			if(allow) uniqueNos.add(qs.val);
		}
		return uniqueNos;
	}
	
	public static List<QuantSpan> getRelevantQuantSpans(
			Double d, List<QuantSpan> quantSpans) {
		List<QuantSpan> relevantSpans = new ArrayList<QuantSpan>();
		for(QuantSpan qs : quantSpans) {
			if(Tools.safeEquals(d, qs.val)) {
				relevantSpans.add(qs);
			}
		}
		return relevantSpans;
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
	
	public static boolean areAllTokensInSameSentence(
			TextAnnotation ta, List<Integer> tokenIds) {
		Set<Integer> sentenceIds = new HashSet<>();
		for(Integer tokenId : tokenIds) {
			sentenceIds.add(ta.getSentenceFromToken(tokenId).getSentenceId());
		}
		if(sentenceIds.size() == 1) return true;
		return false;
	}
	
	public static Integer max(List<Integer> intList) {
		Integer max = Integer.MIN_VALUE;
		for(Integer i : intList) {
			if(max < i) {
				max = i;
			}
		}
		return max;
	}
	
	public static Integer min(List<Integer> intList) {
		Integer min = Integer.MAX_VALUE;
		for(Integer i : intList) {
			if(min > i) {
				min = i;
			}
		}
		return min;
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

	public static int editDist(String[] str1 , String[] str2 , int m ,int n) {
		// Create a table to store results of subproblems
		int dp[][] = new int[m+1][n+1];
		// Fill d[][] in bottom up manner
		for (int i=0; i<=m; i++)
		{
			for (int j=0; j<=n; j++)
			{
				// If first string is empty, only option is to
				// isnert all characters of second string
				if (i==0)
					dp[i][j] = j;  // Min. operations = j

					// If second string is empty, only option is to
					// remove all characters of second string
				else if (j==0)
					dp[i][j] = i; // Min. operations = i

					// If last characters are same, ignore last char
					// and recur for remaining string
				else if (str1[i-1].equalsIgnoreCase(str2[j-1]))
					dp[i][j] = dp[i-1][j-1];

					// If last character are different, consider all
					// possibilities and find minimum
				else
					dp[i][j] = 1 + Math.min(Math.min(
							dp[i][j-1],  // Insert
							dp[i-1][j]),  // Remove
							dp[i-1][j-1]); // Replace
			}
		}

		return dp[m][n];
	}
	
	public static List<String> getTokensListWithCorefReplacements(
			Constituent cons, Schema schema) {
		List<String> tokens = new ArrayList<String>();
		TextAnnotation ta = cons.getTextAnnotation();
		int startIndex = cons.getStartSpan();
		int endIndex = cons.getEndSpan();
		for(int i=startIndex; i<endIndex; ++i) {
			if(schema.coref.containsKey(i)) {
				IntPair ip = schema.coref.get(i);
				for(int j=ip.getFirst(); j<ip.getSecond(); ++j) {
					tokens.add(ta.getToken(j));
				}
			} else {
				tokens.add(ta.getToken(i));
			}
		}
		return tokens;
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
	
	public static boolean implicitRateDetected(TextAnnotation ta, 
			List<QuantSpan> quantities, int quantIndex1, int quantIndex2) {
		int tokenId1 = ta.getTokenIdFromCharacterOffset(
				quantities.get(quantIndex1).start);
		int tokenId2 = ta.getTokenIdFromCharacterOffset(
				quantities.get(quantIndex2).start);
		if(ta.getSentenceFromToken(tokenId1).getSentenceId() == 
				ta.getSentenceFromToken(tokenId2).getSentenceId()) {
			boolean allow = true;
			for(int i=Math.min(tokenId1, tokenId2); i<=Math.max(tokenId1, tokenId2); ++i) {
				if(ta.getToken(i).equals(",") || ta.getToken(i).equals("and")) {
					allow = false;
				}
			}
			if(allow) {
				return true;
			}
		}
		return false;
	}
	
	public static void printCons(List<Constituent> constituents) {
		for(Constituent cons : constituents) {
			System.out.println(cons.getLabel()+" : "+cons.getSurfaceForm()+" : "+cons.getSpan());
		}
	}

	public static double jaccardSim(List<String> phrase1, List<String> phrase2) {
		Set<String> tokens1 = new HashSet<>();
		tokens1.addAll(phrase1);
		Set<String> tokens2 = new HashSet<>();
		tokens2.addAll(phrase2);
		int union = tokens1.size(), intersection = 0;
		for (String token : tokens2) {
			if (tokens1.contains(token)) {
				intersection++;
			} else {
				union++;
			}
		}
		return intersection*1.0/union;

	}

	public static double jaccardEntail(List<String> phrase1, List<String> phrase2) {
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

	public static <K> void addToHighestMap(Map<K, Double> map, K key, Double val) {
		if(!map.containsKey(key) || (map.get(key) < val)) {
			map.put(key, val);
		}
	}

	public static Map<String, double[]> readVectors(String vectorFile) throws IOException {
		Map<String, double[]> vectors = new HashMap<>();
		for(String line : FileUtils.readLines(new File(vectorFile))) {
			String strArr[] = line.split(" ");
			double d[] = new double[strArr.length-1];
			for(int i=1; i<strArr.length; ++i) {
				d[i-1] = Double.parseDouble(strArr[i]);
			}
			vectors.put(strArr[0].trim(), d);
		}
		return vectors;
	}

	public static double getVectorSim(String word1, String word2) {
		if (word1 == null || word2 == null) {
			return 0.0;
		}
		if(vectors.containsKey(word1) && vectors.containsKey(word2)) {
			double[] v1 = vectors.get(word1);
			double[] v2 = vectors.get(word2);
			double dot = 0.0, norm1 = 0.0, norm2 = 0.0;
			for (int i=0; i<v1.length; ++i) {
				dot += (v1[i]*v2[i]);
				norm1 += (v1[i]*v1[i]);
				norm2 += (v2[i]*v2[i]);
			}
			return dot / (norm1 * norm2);
		}
		return 0.0;
	}

	public static List<String> populatePos(List<String> seq,
										   TextAnnotation ta,
										   List<Constituent> posTags,
										   List<String> lemmas) {
		List<String> seqPos = new ArrayList<>();
		boolean flag;
		for(String item : seq) {
			flag = false;
			for (int i=0; i<ta.size(); ++i) {
				if (ta.getToken(i).equalsIgnoreCase(item)) {
					seqPos.add(posTags.get(i).getLabel());
					flag = true;
					break;
				}
			}
			if (flag) continue;
			for (int i=0; i<lemmas.size(); ++i) {
				if (lemmas.get(i).equalsIgnoreCase(item)) {
					seqPos.add(posTags.get(i).getLabel());
					flag = true;
					break;
				}
			}
			if (flag) continue;
			seqPos.add("UNK");
		}
		if(seqPos.size() != seq.size()) {
			System.out.println("Problem in populatePos");
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
										  List<String> posTags1, List<String> posTags2) {
		List<String> colors = Arrays.asList("white", "black", "red", "green", "yellow", "brown", "blue", "gray");
		for (int i=0; i<tokens1.size(); ++i) {
			IndexWord word1 = getIndexWord(tokens1.get(i), posTags1.get(i));
			if (word1 == null) continue;
			for (int j=0; j<tokens2.size(); ++j) {
				if (colors.contains(tokens1.get(i)) && colors.contains(tokens1.get(j))) {
					return "Siblings";
				}
				IndexWord word2 = getIndexWord(tokens2.get(j), posTags2.get(j));
				if (word2 == null) continue;
				String wn = wordNetIndicator(word1, word2);
				if (wn != null) {
					return wn;
				}
			}
		}
		return null;
	}

	public static String wordNetIndicator(IndexWord word1, IndexWord word2) {
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

	public static void main(String args[]) {
		String wn = wordnetIndicator(Arrays.asList("red", "apples"),
				Arrays.asList("green", "apples"), Arrays.asList("N", "N"),
				Arrays.asList("N", "N"));
		System.out.println("Wordnet says "+wn);
		wn = wordnetIndicator(Arrays.asList("win"), Arrays.asList("lose"),
				Arrays.asList("V"), Arrays.asList("V"));
		System.out.println("Wordnet says "+wn);
	}
}
