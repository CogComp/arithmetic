package demo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.CoreLabel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;

import structure.StanfordProblem;
import structure.QuantSpan;
import utils.Params;
import utils.Tools;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;

public class Derivation {
	
	public static List<Pair<List<String>, List<String>>> rules;
	public static Set<String> allTriggers;
	
	static {
		String str = null;
		try {
			str = FileUtils.readFileToString(new File(Params.patternsFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		rules = new ArrayList<>();
		allTriggers = new HashSet<>();
		Set<String> vars = new HashSet<>();
		for(String ruleString : str.split("\n")) {
			if(!ruleString.contains("-->")) continue;
			List<String> lhs = new ArrayList<>();
			List<String> rhs = new ArrayList<>();
			lhs.addAll(Arrays.asList(ruleString.split("-->")[0].trim().split(" ")));
			rhs.addAll(Arrays.asList(ruleString.split("-->")[1].trim().split(" ")));
			for(String var : lhs) if(var.contains("EXPR")) vars.add(var); else allTriggers.add(var);
			if(rhs.get(rhs.size()-1).equals("REPEAT")) {
				rhs.remove(rhs.size()-1);
				for(int i=0; i<2; ++i) {
					List<String> l = new ArrayList<>();
					List<String> r = new ArrayList<>();
					l.addAll(lhs);
					r.addAll(rhs);
					for(int j=0; j<i; ++j) {
						l.add(lhs.get(lhs.size()-1));
						r.add(rhs.get(rhs.size()-2));
						r.add(rhs.get(rhs.size()-1));
					}
					rules.add(new Pair<>(l, r));
				}
			} else {
				rules.add(new Pair<>(lhs, rhs));
			}
		}
	}
	
	public static List<Pair<String, Double>> parse(List<String> triggers) {
		List<List<List<Pair<String, Double>>>> cky = new ArrayList<>();
		int n = triggers.size();
		for(int i=0; i<=n; ++i) {
			cky.add(new ArrayList<List<Pair<String,Double>>>());
			for(int j=0; j<=n; ++j) {
				cky.get(i).add(new ArrayList<Pair<String,Double>>());
			}
		}
		for(int i=0; i<n; ++i) {
			if(NumberUtils.isNumber(triggers.get(i).trim())) {
				cky.get(i).get(i+1).add(new Pair<>(
						triggers.get(i).trim(), Double.parseDouble(triggers.get(i).trim())));
			}
		}
		for(int i=n-1; i>=0; --i) {
			for(int j=i+2; j<=n; ++j) {
				parse(triggers, i, j, cky);
			}
		}
		return cky.get(0).get(n);
	}
	
	public static void parse(List<String> triggers, int start, int end, 
			List<List<List<Pair<String, Double>>>> cky) {
		for(Pair<List<String>, List<String>> rule : rules) {
			boolean applicable = false;
			for(int i=start; i<end; ++i) {
				if(!triggers.get(i).contains("EXPR") && 
						rule.getFirst().contains(triggers.get(i)) &&
						rule.getFirst().size() <= (end-start)) {
					applicable = true;
					break;
				}
			}
			if(!applicable) continue;
			if(rule.getFirst().size() == 3) {
				for(int i=start+1; i<end; ++i) {
					for(int j=i+1; j<end; ++j) {
						if(isMatch(rule.getFirst().get(0), start, i, cky, triggers) &&
								isMatch(rule.getFirst().get(1), i, j, cky, triggers) &&
								isMatch(rule.getFirst().get(2), j, end, cky, triggers)) {
							List<Pair<String, Double>> exprs = new ArrayList<>();
							if(rule.getFirst().get(0).contains("EXPR")) exprs.add(cky.get(start).get(i).get(0));
							if(rule.getFirst().get(1).contains("EXPR")) exprs.add(cky.get(i).get(j).get(0));
							if(rule.getFirst().get(2).contains("EXPR")) exprs.add(cky.get(j).get(end).get(0));
							cky.get(start).get(end).add(getCombinations(exprs, rule.getSecond().get(1), rule));
						}
					}
				}
			}
			if(cky.get(start).get(end).size() > 0) return;
			if(rule.getFirst().size() == 4) {
				for(int i=start+1; i<end; ++i) {
					for(int j=i+1; j<end; ++j) {
						for(int k=j+1; k<end; ++k) {
							if(isMatch(rule.getFirst().get(0), start, i, cky, triggers) &&
									isMatch(rule.getFirst().get(1), i, j, cky, triggers) &&
									isMatch(rule.getFirst().get(2), j, k, cky, triggers) &&
									isMatch(rule.getFirst().get(2), k, end, cky, triggers)) {
								List<Pair<String, Double>> exprs = new ArrayList<>();
								if(rule.getFirst().get(0).contains("EXPR")) exprs.add(cky.get(start).get(i).get(0));
								if(rule.getFirst().get(1).contains("EXPR")) exprs.add(cky.get(i).get(j).get(0));
								if(rule.getFirst().get(2).contains("EXPR")) exprs.add(cky.get(j).get(k).get(0));
								if(rule.getFirst().get(3).contains("EXPR")) exprs.add(cky.get(k).get(end).get(0));
								cky.get(start).get(end).add(getCombinations(exprs, rule.getSecond().get(1), rule));
							}
						}
					}
				}
			}
			if(cky.get(start).get(end).size() > 0) return;
		}
	}
	
	public static boolean isMatch(String ruleLhsTerm, int start, int end, 
			List<List<List<Pair<String, Double>>>> cky, List<String> triggers) {
		if(ruleLhsTerm.contains("EXPR") && cky.get(start).get(end).size()>0) {
			return true;
		}
		if(start+1 == end && triggers.get(start).equalsIgnoreCase(ruleLhsTerm)) {
			return true;
		}
		return false;
	}
	
	public static Pair<String, Double> getCombinations(
			List<Pair<String, Double>> candidates, String op, Pair<List<String>, List<String>> rule) {
		if(!rule.getSecond().get(0).contains("EXPR1")) {
			List<Pair<String, Double>> candidatesTmp = new ArrayList<>();
			candidatesTmp.addAll(candidates);
			candidates.clear();
			for(Pair<String, Double> candi : candidatesTmp) {
				candidates.add(0, candi);
			}
		}
		String str = "(";
		Double d = candidates.get(0).getSecond();
		for(int i=0; i<candidates.size(); ++i) {
			if(i<candidates.size()-1) str += candidates.get(i).getFirst() + op;
			else str += candidates.get(i).getFirst() + ")";
			if(i>0) {
				if(op.trim().equals("+")) {
					d += candidates.get(i).getSecond();
				}
				if(op.trim().equals("-")) {
					d -= candidates.get(i).getSecond();
				}
				if(op.trim().equals("*")) {
					d *= candidates.get(i).getSecond();
				}
				if(op.trim().equals("/")) {
					d /= candidates.get(i).getSecond();
				}
			}
		}
		return new Pair<>(str, d);
	}
	
	public static List<String> getTriggers(StanfordProblem prob) {
		List<String> triggers = new ArrayList<>();
		for(int i=0; i<prob.tokens.size(); ++i) {
			List<CoreLabel> sent = prob.tokens.get(i);
			for(int j=0; j<sent.size(); ++j) {
				if (allTriggers.contains(sent.get(j).word().toLowerCase())) {
					triggers.add(sent.get(j).word().toLowerCase());
				} else {
					for (QuantSpan qs : prob.quantities) {
						if (Tools.getTokenIdFromCharOffset(sent, qs.start) == i) {
							triggers.add(""+qs.val);
							break;
						}
					}
				}
			}
		}
		return triggers;
	}
	
	public static String solveByDerivation(StanfordProblem prob) {
		List<String> triggers = getTriggers(prob);
		List<Pair<String, Double>> results = parse(triggers);
		if(results.size() == 0) return "";
		else return results.get(0).getFirst()+"="+results.get(0).getSecond();
	}

}
