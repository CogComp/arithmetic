package logic;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.stanford.nlp.ling.CoreLabel;
import structure.Node;
import structure.StanfordSchema;
import utils.FeatGen;
import utils.Tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogicFeatGen extends AbstractFeatureGenerator implements Serializable {

	private static final long serialVersionUID = -5902462551801564955L;
	public Lexiconer lm = null;

	public LogicFeatGen(Lexiconer lm) {
		this.lm = lm;
	}

	@Override
	public IFeatureVector getFeatureVector(IInstance prob, IStructure struct) {
		LogicX x = (LogicX) prob;
		LogicY y = (LogicY) struct;
		List<String> features = getFeatures(x, y);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public static List<String> getFeatures(LogicX x, LogicY y) {
		List<String> features = new ArrayList<>();
		features.addAll(getFeatures(x, y.expr, true));
		return features;
	}

	public static List<String> getFeatures(LogicX x, Node node, boolean isTopmost) {
		List<String> features = new ArrayList<>();
		if(node.children.size() == 0) return features;
		if(node.children.get(0).quantIndex < node.children.get(1).quantIndex) {
			features.addAll(getCombinationFeatures(
					x,
					node,
					x.schema.get(node.children.get(0).quantIndex),
					x.schema.get(node.children.get(1).quantIndex),
					x.questionSchema,
					node.infRuleType,
					node.key,
					isTopmost));
		} else {
			features.addAll(getCombinationFeatures(
					x,
					node,
					x.schema.get(node.children.get(1).quantIndex),
					x.schema.get(node.children.get(0).quantIndex),
					x.questionSchema,
					node.infRuleType,
					node.key,
					isTopmost));
		}
		features.addAll(getFeatures(x, node.children.get(0), false));
		features.addAll(getFeatures(x, node.children.get(1), false));
		return features;
	}

	public IFeatureVector getCombinationFeatureVector(LogicX x,
													  Node node,
													  StanfordSchema num1,
													  StanfordSchema num2,
													  StanfordSchema ques,
													  String infRuleType,
													  String key,
													  boolean isTopmost) {
		List<String> features = getCombinationFeatures(
				x, node, num1, num2, ques, infRuleType, key, isTopmost);
		return FeatGen.getFeatureVectorFromListString(features, lm);
	}

	public static List<String> getCombinationFeatures(LogicX x,
													  Node node,
													  StanfordSchema num1,
													  StanfordSchema num2,
													  StanfordSchema ques,
													  String infRuleType,
													  String key,
													  boolean isTopmost) {
		List<String> features = new ArrayList<>();

		List<String> nodeFeatures = new ArrayList<>();
		nodeFeatures.addAll(getNodeFeatures(x, node, num1, num2, ques, infRuleType, key, isTopmost));
		features.addAll(nodeFeatures);

		List<String> infFeatures = new ArrayList<>();
		infFeatures.addAll(getInfTypeFeatures(x, node, num1, num2, ques, infRuleType, isTopmost));
		features.addAll(infFeatures);

		if(!LogicDriver.useInfModel) {
			List<String> keyFeatures = new ArrayList<>();
			keyFeatures.addAll(getKeyFeatures(x, num1, num2, ques, infRuleType, key));
			features.addAll(keyFeatures);
		}
        return features;
	}

	public static List<String> getNodeFeatures(LogicX x,
											   Node node,
											   StanfordSchema num1,
											   StanfordSchema num2,
											   StanfordSchema ques,
											   String infRuleType,
											   String key,
											   boolean isTopmost) {
		List<String> features = new ArrayList<>();
		List<String> addSub = Arrays.asList("ADD", "SUB");
		List<String> mulDiv = Arrays.asList("MUL", "DIV");
		if(addSub.contains(node.label)) {
			if((mulDiv.contains(node.children.get(0).label) ||
					mulDiv.contains(node.children.get(1).label))) {
				features.add("AddConnectingMul");
			}
		}
		if(mulDiv.contains(node.label)) {
			if((addSub.contains(node.children.get(0).label) ||
					addSub.contains(node.children.get(1).label))) {
				features.add("MulConnectingAdd");
			}
		}
		features.add("MidNumber:"+LogicY.midNumber(x.tokens, num1, num2));
		List<CoreLabel> tokens1 = x.tokens.get(num1.sentId);
		int tokenId1 = Tools.getTokenIdFromCharOffset(tokens1, num1.qs.start);
		List<CoreLabel> tokens2 = x.tokens.get(num2.sentId);
		int tokenId2 = Tools.getTokenIdFromCharOffset(tokens2, num2.qs.start);
		if(num1.sentId == num2.sentId) {
			features.add("NumbersPresentInSameSentence");
			if(node.children.get(0).children.size() == 0 &&
					node.children.get(1).children.size() == 0) {
				features.add("LeavesFromSameSentence");
				for(int i=Math.min(tokenId1, tokenId2); i<Math.max(tokenId1, tokenId2); ++i) {
					if(tokens1.get(i).lemma().equals(",") || tokens1.get(i).lemma().equals("and")) {
						features.add(infRuleType.substring(0,4)+"Add");
						break;
					}
				}
				if(!features.contains(infRuleType.substring(0,4)+"Add")) {
					features.add(infRuleType.substring(0,4)+"Mul");
				}
			}
		}
		return features;
	}

	public static List<String> getInfTypeFeatures(LogicX x,
												  Node node,
												  StanfordSchema num1,
												  StanfordSchema num2,
												  StanfordSchema ques,
												  String infRuleType,
												  boolean isTopmost) {
		List<String> features = new ArrayList<>();
		if(infRuleType.startsWith("Rate")) {
			if(infRuleType.startsWith("Rate0")) {
				features.addAll(getRateFeatures(x, num1));
			}
			if(infRuleType.startsWith("Rate1")) {
				features.addAll(getRateFeatures(x, num2));
			}
			if(infRuleType.startsWith("RateQues")) {
				features.addAll(getRateFeatures(x, ques));
			}
		}
		boolean partitionOrVerb = LogicY.isPartitionOrVerb(x, node.label, num1, num2);
		if(node.label.equals("ADD") || node.label.equals("SUB")) {
			features.add("PartitionOrVerb:"+partitionOrVerb);
		}

		return FeatGen.getFeaturesConjWithLabels(
				features,
				"InfRule:"+infRuleType.substring(0,4));
	}


	public static List<String> getRateFeatures(LogicX x, StanfordSchema schema) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens = x.tokens.get(schema.sentId);
		if(schema.qs == null) {
			for(int i=x.questionSpan.getFirst(); i<x.questionSpan.getSecond(); ++i) {
				if (tokens.get(i).lemma().equals("each") ||
						tokens.get(i).lemma().equals("every") ||
						tokens.get(i).lemma().equals("per")) {
					features.add("RateStuffDetected");
					break;
				}
				if(i < x.questionSpan.getSecond()-1) {
					if(tokens.get(i).lemma().equals("how") &&
							tokens.get(i+1).lemma().equals("fast")) {
						features.add("RateStuffDetected");
						break;
					}
				}
				if(i < x.questionSpan.getSecond()-1) {
					if(tokens.get(i).lemma().equals("what") &&
							tokens.get(i+1).lemma().equals("speed")) {
						features.add("RateStuffDetected");
						break;
					}
				}
			}
		} else {
			int tokenId = Tools.getTokenIdFromCharOffset(tokens, schema.qs.start);
			for (int i = Math.max(0, tokenId - 3); i < Math.min(tokenId + 4, tokens.size()); ++i) {
				if (tokens.get(i).lemma().equals("each") ||
						tokens.get(i).lemma().equals("every") ||
						tokens.get(i).lemma().equals("per")) {
					features.add("RateStuffDetected");
					break;
				}
			}
			if(tokenId>=2 && tokens.get(tokenId-1).lemma().equals("of") &&
					tokens.get(tokenId-2).tag().equals("NNS")) {
				features.add("RateStuffDetected");
			}
			if(tokenId>=2 && tokens.get(tokenId-2).lemma().equals("how") &&
					tokens.get(tokenId-1).lemma().equals("many")) {
				features.add("RateStuffDetected");
			}
			if(tokenId < tokens.size()-2 && (tokens.get(tokenId+1).lemma().equals("a") ||
					(tokens.get(tokenId+2).lemma().equals("a") &&
							tokens.get(tokenId+1).tag().startsWith("N")))) {
				features.add("RateStuffDetected");
			}
		}
		if(schema.rate != null && schema.rate.getFirst() >= 0) {
			features.add("RateDetected");
		}
		if(!features.contains("RateDetected")){
			features.add("RateNotDetected");
		}
		boolean rateFound = false;
		for(int i=0; i<x.schema.size(); ++i) {
			if(x.schema.get(i).rate.getFirst() >= 0) {
				rateFound = true;
				break;
			}
		}
		if(!rateFound && x.questionSchema.rate.getFirst() == -1) {
			features.add("RateNotFoundAnywhere");
		}
		return features;
	}



	/***********************************************************************
	 All functions below are used in InfType predictor, any change will effect
	 inference type prediction scores. If you change any of the functions below,
	 retrain InfType predictor before training Logic predictor.
	 ***********************************************************************/

	public static List<String> getPhraseByMode(List<List<CoreLabel>> tokens,
											   StanfordSchema schema,
											   String mode) {
		if(mode.equals("SUBJ")) {
			return Tools.spanToLemmaList(tokens.get(schema.sentId), schema.subject);
		}
		if(mode.equals("OBJ")) {
			return Tools.spanToLemmaList(tokens.get(schema.sentId), schema.object);
		}
		if(mode.equals("UNIT")) {
			return Tools.spanToLemmaList(tokens.get(schema.sentId), schema.unit);
		}
		if(mode.equals("RATE")) {
			return Tools.spanToLemmaList(tokens.get(schema.sentId), schema.rate);
		}
		return new ArrayList<>();
	}

	public static List<String> getKeyFeatures(LogicX x,
											  StanfordSchema num1,
											  StanfordSchema num2,
											  StanfordSchema ques,
											  String infRuleType,
											  String key) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens1 = x.tokens.get(num1.sentId);
		int tokenId1 = Tools.getTokenIdFromCharOffset(tokens1, num1.qs.start);
		List<CoreLabel> tokens2 = x.tokens.get(num2.sentId);
		int tokenId2 = Tools.getTokenIdFromCharOffset(tokens2, num2.qs.start);
		if(!key.equals("0_1") && !key.equals("1_0")) {
			features.add(infRuleType.substring(0, 3) + "_" + key);
		}
		if(infRuleType.startsWith("Verb") || infRuleType.startsWith("Math")) {
			if(key.equals("0_0")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "SUBJ", "SUBJ", infRuleType));
			}
			if(key.equals("0_1")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "SUBJ", "OBJ", infRuleType));
			}
			if(key.equals("1_0")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "OBJ", "SUBJ", infRuleType));
			}
			if(key.equals("QUES")) {
				features.addAll(getPairSchemaFeatures(x, num1, ques, "SUBJ", "SUBJ", infRuleType));
			}
			if(key.equals("QUES_REV")) {
				features.addAll(getPairSchemaFeatures(x, num2, ques, "SUBJ", "SUBJ", infRuleType));
			}
		}
		if(infRuleType.startsWith("Rate")) {
			if(key.equals("0_0")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "UNIT", "UNIT", infRuleType));
				if(infRuleType.contains("Rate0") && Tools.jaccardSim(
						Tools.spanToLemmaList(x.tokens.get(num1.sentId), num1.rate),
						Tools.spanToLemmaList(x.tokens.get(ques.sentId), ques.unit)) > 0) {
					features.add("SupportFromQuestion");
				}
				if(infRuleType.contains("Rate1") && Tools.jaccardSim(
						Tools.spanToLemmaList(x.tokens.get(num2.sentId), num2.rate),
						Tools.spanToLemmaList(x.tokens.get(ques.sentId), ques.unit)) > 0) {
					features.add("SupportFromQuestion");
				}
			}
			if(key.equals("0_1")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "UNIT", "RATE", infRuleType));
				if(Tools.jaccardSim(
						Tools.spanToLemmaList(x.tokens.get(num2.sentId), num2.unit),
						Tools.spanToLemmaList(x.tokens.get(ques.sentId), ques.unit)) > 0) {
					features.add("SupportFromQuestion");
				}
			}
			if(key.equals("1_0")) {
				features.addAll(getPairSchemaFeatures(x, num1, num2, "RATE", "UNIT", infRuleType));
				if(Tools.jaccardSim(
						Tools.spanToLemmaList(x.tokens.get(num1.sentId), num1.unit),
						Tools.spanToLemmaList(x.tokens.get(ques.sentId), ques.unit)) > 0) {
					features.add("SupportFromQuestion");
				}
			}
			if(key.equals("QUES")) {
				features.addAll(getPairSchemaFeatures(x, num2, ques, "UNIT", "RATE", infRuleType));
			}
			if(key.equals("QUES_REV")) {
				features.addAll(getPairSchemaFeatures(x, num1, ques, "UNIT", "RATE", infRuleType));
			}
		}
		if(infRuleType.equals("Partition")) {
			features.addAll(FeatGen.getFeaturesConjWithLabels(
					getPartitionFeatures(x, num1, num2), key.equals("SIBLING")?"SIBLING":"H"));
		}
		for (int i = Math.max(0, tokenId2 - 3); i < Math.min(tokenId2 + 4, tokens2.size()); ++i) {
			if (!tokens2.get(i).tag().startsWith("CD") &&
					!tokens2.get(i).tag().startsWith("N")) {
				features.add(infRuleType.substring(0,4)+key+"2_Unigram_" + tokens2.get(i).lemma());
			}
		}
		return features;
	}

	// Mode has to be one of "SUBJ", "OBJ", "UNIT", "RATE"
	// Note that this is a symmetric similarity function, lets keep it that way
	public static List<String> getPairSchemaFeatures(
			LogicX x, StanfordSchema schema1, StanfordSchema schema2, String mode1, String mode2,
			String infType) {
		List<String> features = new ArrayList<>();
		List<String> phrase1 = getPhraseByMode(x.tokens, schema1, mode1);
		List<String> phrase2 = getPhraseByMode(x.tokens, schema2, mode2);
		if(mode1.equals("UNIT") && phrase1.size() == 0 && schema1.quantId >= 1) {
			StanfordSchema prevSchema = x.schema.get(schema1.quantId-1);
			phrase1 = Tools.spanToLemmaList(x.tokens.get(prevSchema.sentId), prevSchema.unit);
		}
		if(mode2.equals("UNIT") && phrase2.size() == 0 && schema2.quantId >= 1) {
			StanfordSchema prevSchema = x.schema.get(schema2.quantId-1);
			phrase2 = Tools.spanToLemmaList(x.tokens.get(prevSchema.sentId), prevSchema.unit);
		}
		if(mode1.equals("SUBJ") || mode1.equals("OBJ")) {
			if ((phrase1.contains("he") || phrase1.contains("she")) &&
					phrase2.size() > 0) {
				features.add("Pronoun_Person_Present");
			}
			if ((phrase2.contains("he") || phrase2.contains("she")) &&
					phrase1.size() > 0) {
				features.add("Pronoun_Person_Present");
			}
		}
		double sim;
		if(mode1.equals("SUBJ") && mode2.equals("SUBJ")) {
			sim = Math.max(Tools.jaccardSim(phrase1, phrase2), Tools.jaccardSim(
					getPhraseByMode(x.tokens, schema1, "OBJ"),
					getPhraseByMode(x.tokens, schema1, "OBJ")
			));
		} else {
			sim = Tools.jaccardSim(phrase1, phrase2);
		}
		if(sim > 0.2) features.add("NonZeroSimilarity");
		if(sim < 0.2) features.add("AbsolutelyNoMatch");
		List<Pair<String, String>> subjObjCandidates = Arrays.asList(
				new Pair<>("SUBJ", "SUBJ"),
				new Pair<>("SUBJ", "OBJ"),
				new Pair<>("OBJ", "SUBJ"));
		List<Pair<String, String>> unitRateCandidates = Arrays.asList(
				new Pair<>("UNIT", "UNIT"),
				new Pair<>("UNIT", "RATE"),
				new Pair<>("RATE", "UNIT"));
		List<Pair<String, String>> candidates;
		if(mode1.equals("SUBJ") || mode1.equals("OBJ")) {
			candidates = subjObjCandidates;
		} else {
			candidates = unitRateCandidates;
		}
		double maxSim = 0.0;
		for(Pair<String, String> modePair : candidates) {
			double s = Tools.jaccardSim(
					getPhraseByMode(x.tokens, schema1, modePair.getFirst()),
					getPhraseByMode(x.tokens, schema2, modePair.getSecond()));
			if(s > maxSim) maxSim = s;
		}
		if(maxSim > sim+0.001) {
			features.add("BetterCandidatePresent");
			features.add("BetterCandidatePresent"+mode1+mode2);
		} else if(maxSim > 0.1){
			features.add("BestOption");
			features.add("BestOption"+mode1+mode2);
		}
		if(maxSim < 0.1) {
			features.add("NoGoodOption_"+mode1+mode2);
		}
        return features;
	}

	public static List<String> getPartitionFeatures(
			LogicX x, StanfordSchema num1, StanfordSchema num2) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens1 = x.tokens.get(num1.sentId);
		int tokenId1 = Tools.getTokenIdFromCharOffset(tokens1, num1.qs.start);
		for (int i = tokenId1 + 1; i < tokens1.size(); ++i) {
			if (tokens1.get(i).word().equals("remaining") ||
					tokens1.get(i).word().equals("rest")) {
				features.add("RemainingRest");
			}
			if (tokens1.get(i).word().toLowerCase().equals("either")) {
				features.add("Either");
			}
		}
		List<CoreLabel> tokens2 = x.tokens.get(num2.sentId);
		int tokenId2 = Tools.getTokenIdFromCharOffset(tokens2, num2.qs.start);
		for (int i = tokenId2 + 1; i < tokens2.size(); ++i) {
			if (tokens2.get(i).word().equals("remaining") ||
					tokens2.get(i).word().equals("rest")) {
				features.add("RemainingRest");
			}
			if (tokens2.get(i).word().toLowerCase().equals("either")) {
				features.add("Either");
			}
		}
		if(tokens2.get(0).lemma().equals("if") &&
				tokens2.get(tokens2.size()-1).word().equals("?")) {
			boolean comma = false;
			for(int i=0; i<tokenId2; ++i) {
				if(tokens2.get(i).word().equals(",")) {
					comma = true;
					break;
				}
			}
			if(!comma) features.add("InSentenceStartingWithIf");
		}
		for(int i=tokenId2-1; i>=Math.max(0, tokenId2-4); --i) {
			if(tokens2.get(i).lemma().equals("already")) {
				features.add("InSentenceWithAlready");
				break;
			}
		}
		for(int i=tokenId2-1; i>=0; --i) {
			if(tokens2.get(i).word().equals(",")) break;
			if(tokens2.get(i).lemma().equals("but")) {
				features.add("InSentenceWithAlready");
				break;
			}
		}
		for(int i=x.questionSpan.getFirst(); i<x.questionSpan.getSecond(); ++i) {
			CoreLabel token = x.tokens.get(x.questionSchema.sentId).get(i);
			if(token.word().equals("all") || token.word().equals("altogether") ||
					token.word().equals("overall") || token.word().equals("total") ||
					token.word().equals("sum")) {
				features.add("AllPresentInQuestion");
			}
		}
		for(int i=0; i<2; ++i) {
			CoreLabel token = x.tokens.get(x.questionSchema.sentId).get(i);
			if(token.word().equals("all") || token.word().equals("altogether") ||
					token.word().equals("overall") || token.word().equals("total")) {
				features.add("AllPresentInQuestion");
			}
		}
		if(num1.sentId == num2.sentId) {
			features.add("SameSentence");
			if(num1.verb == num2.verb) {
				features.add("VerbSameInstance");
			}
		}
		return features;
	}

	public static List<String> getNeighborhoodFeatures(LogicX x, StanfordSchema schema) {
		List<String> features = new ArrayList<>();
		List<CoreLabel> tokens = x.tokens.get(schema.sentId);
		if(schema.qs == null) {
			for (int i = x.questionSpan.getFirst();
				 i < x.questionSpan.getSecond();
				 ++i) {
				if (!tokens.get(i).tag().startsWith("N")) {
					features.add("Unigram_" + tokens.get(i).lemma());
                    if(i<x.questionSpan.getSecond()-1) {
                        features.add("Bigram_" + tokens.get(i).lemma() + "_" +
                                tokens.get(i+1).tag());
                    }
                    if(i>=1) {
                        features.add("Bigram_" + tokens.get(i-1).tag() + "_" +
                                tokens.get(i).lemma());
                    }
				}
			}
		} else {
			int tokenId = Tools.getTokenIdFromCharOffset(tokens, schema.qs.start);
			for (int i = Math.max(0, tokenId - 2); i < Math.min(tokenId + 3, tokens.size()); ++i) {
				if (!tokens.get(i).tag().startsWith("N")) {
					features.add("Unigram_" + tokens.get(i).lemma());
                    if(i<Math.min(tokenId + 3, tokens.size())-1) {
                        features.add("Bigram_" + tokens.get(i).lemma() + "_" +
                                tokens.get(i+1).tag());
                    }
                    if(i>=1) {
                        features.add("Bigram_" + tokens.get(i-1).tag() + "_" +
                                tokens.get(i).lemma());
                    }
				}
			}
		}
		return features;
	}





}

