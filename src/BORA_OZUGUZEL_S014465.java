// package project2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class BORA_OZUGUZEL_S014465 {

	public static final String FILE_NAME = "G1.txt";
	public static String filePath;

	public static void main(String[] args) {
		try {
			String potentialNonTerminalSymbolsAsString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
			ContextFreeGrammar cfg = new ContextFreeGrammar(readFile(FILE_NAME), potentialNonTerminalSymbolsAsString);
			// System.out.println(cfg);
			ContextFreeGrammar cnf = cfg.createCnfFromCfg();
			System.out.println(cnf);

		} catch (NullPointerException e) {
			System.out.println("NullPointerException");
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("No available Non-Terminal Variable Symbols left.");
		}
	}

	public static Scanner readFile(String fileName) {
		try {
			File file = new File(fileName);
			if (file.exists()) {
				filePath = file.getAbsolutePath();
			} else {
				findFile(FILE_NAME, new File("."));
			}
			return new Scanner(new File(filePath));
		} catch (Exception e) {
			System.out.println("Exception.");
		}
		return null;
	}

	public static void findFile(String fileName, File dir) {
		File[] list = dir.listFiles();
		if (list != null) {
			for (File f : list) {
				if (f.isDirectory()) {
					findFile(fileName, f);
				} else if (fileName.equals(f.getName())) {
					filePath = f.getAbsolutePath();
				}
			}
		}
	}
}

class ContextFreeGrammar implements Cloneable {
	HashSet<Character> nonTerminalSymbols;
	HashSet<Character> terminalSymbols;
	List<Rule> rules;
	char startVariable;
	HashSet<Character> potentialNonTerminalSymbols;

	static final char epsilon = 'e';

	public ContextFreeGrammar(Scanner sc, String potentialNonTerminalSymbolsAsString) {
		this.nonTerminalSymbols = new HashSet<Character>();
		this.terminalSymbols = new HashSet<Character>();
		this.rules = new ArrayList<Rule>();
		this.potentialNonTerminalSymbols = new HashSet<Character>();
		for (char symbol : potentialNonTerminalSymbolsAsString.toCharArray())
			this.potentialNonTerminalSymbols.add(symbol);
		int partitionState = 0;
		while (sc.hasNextLine()) {
			String currentLine = sc.nextLine();
			if (currentLine.isBlank() || currentLine.isEmpty()) {
				continue;
			}
			if (currentLine.equals("NON-TERMINAL")) {
				partitionState = 1;
				continue;
			} else if (currentLine.equals("TERMINAL")) {
				partitionState = 2;
				continue;
			} else if (currentLine.equals("RULES")) {
				partitionState = 3;
				continue;
			} else if (currentLine.equals("START")) {
				partitionState = 4;
				continue;
			} else {

				if (partitionState == 1) {
					this.potentialNonTerminalSymbols.remove(currentLine.charAt(0));
					this.nonTerminalSymbols.add(currentLine.charAt(0));
				} else if (partitionState == 2) {
					this.terminalSymbols.add(currentLine.charAt(0));
				} else if (partitionState == 3) {
					StringTokenizer st = new StringTokenizer(currentLine, ":");
					if (st.hasMoreTokens()) {
						char leftHandSide = st.nextToken().charAt(0);
						String rightHandSide = st.nextToken();
						this.rules.add(new Rule(leftHandSide, rightHandSide));
					}
				} else if (partitionState == 4) {
					this.startVariable = currentLine.charAt(0);
					partitionState = 5;
					break;
				}

			}
		}
	}

	@SuppressWarnings("unchecked")
	public ContextFreeGrammar(HashSet<Character> nonTerminalSymbols, HashSet<Character> terminalSymbols,
			ArrayList<Rule> rules, char startVariable, HashSet<Character> potentialNonTerminalSymbols) {
		this.nonTerminalSymbols = (HashSet<Character>) nonTerminalSymbols.clone();
		this.terminalSymbols = (HashSet<Character>) terminalSymbols.clone();
		this.rules = (ArrayList<Rule>) rules.clone();
		this.startVariable = startVariable;
		this.potentialNonTerminalSymbols = (HashSet<Character>) potentialNonTerminalSymbols.clone();
	}

	@Override
	public ContextFreeGrammar clone() {
		ContextFreeGrammar cfg = new ContextFreeGrammar(this.nonTerminalSymbols, this.terminalSymbols,
				(ArrayList<Rule>) this.rules, this.startVariable, this.potentialNonTerminalSymbols);
		return cfg;
	}

	public ContextFreeGrammar createCnfFromCfg() {

		ContextFreeGrammar cnf = null;
		cnf = this.clone();

		// Add new start variable.

		char newStartVariable = cnf.getNewNonTerminalSymbol();
		cnf.rules.add(new Rule(newStartVariable, "" + this.startVariable));
		cnf.startVariable = newStartVariable;

		// Remove epsilon rules add new generated rules.

		while (true) {
			HashSet<Rule> epsilonRules = cnf.rules.stream()
					.filter(r -> r.leftHandSide != this.startVariable && r.rightHandSide.equals("" + epsilon))
					.collect(Collectors.toCollection(HashSet<Rule>::new));
			if (epsilonRules.size() == 0) {
				break;
			}

			for (Rule epsilonRule : epsilonRules) {
				List<HashSet<Rule>> newRuleSets = cnf.rules.stream()
						.filter(r -> r.rightHandSide.contains("" + epsilonRule.leftHandSide))
						.map(r -> generateNewRulesFromEpsilonRule(epsilonRule, r)).collect(Collectors.toList());
				for (HashSet<Rule> newRuleSet : newRuleSets) {
					for (Rule newRule : newRuleSet) {
						addRuleToCfg(newRule, cnf);
					}
				}
				removeRuleFromCfg(epsilonRule, cnf);
			}
		}

		// Remove unit rules add new generated rules.

		while (true) {
			final HashSet<Character> tempNonTerminalSymbols = cnf.nonTerminalSymbols;
			Optional<Character> randomUnitRuleLeftHandSide = cnf.rules.stream().filter(
					r -> r.rightHandSide.length() == 1 && tempNonTerminalSymbols.contains(r.rightHandSide.charAt(0)))
					.map(r -> r.leftHandSide).findFirst();
			if (randomUnitRuleLeftHandSide.isEmpty()) {
				break;
			}

			HashSet<Rule> unitRules = cnf.rules.stream()
					.filter(r -> r.leftHandSide == randomUnitRuleLeftHandSide.get() && r.rightHandSide.length() == 1
							&& tempNonTerminalSymbols.contains(r.rightHandSide.charAt(0)))
					.collect(Collectors.toCollection(HashSet<Rule>::new));

			for (Rule unitRule : unitRules) {
				HashSet<Rule> newRuleSet = cnf.rules.stream()
						.filter(r -> r.leftHandSide == unitRule.rightHandSide.charAt(0))
						.map(r -> new Rule(unitRule.leftHandSide, r.rightHandSide))
						.collect(Collectors.toCollection(HashSet::new));
				for (Rule newRule : newRuleSet) {
					addRuleToCfg(newRule, cnf);
				}
				final List<Rule> tempRules = cnf.rules;
				removeRuleFromCfg(unitRule, cnf);
				HashSet<Rule> disconnectedRules = cnf.rules.stream()
						.filter(r -> unitRule.rightHandSide.equals("" + r.leftHandSide)
								&& tempRules.stream().noneMatch(r2 -> r2.rightHandSide.contains("" + r.leftHandSide)))
						.collect(Collectors.toCollection(HashSet<Rule>::new));
				for (Rule diconnectedRule : disconnectedRules) {
					removeRuleFromCfg(diconnectedRule, cnf);
				}
			}
		}

		// Fix for the empty set.

		if (cnf.rules.size() == 1 && cnf.rules.get(0).leftHandSide == cnf.startVariable
				&& cnf.rules.get(0).rightHandSide.equals("" + this.startVariable)) {
			cnf.rules.remove(0);
			cnf.rules.add(new Rule(cnf.startVariable, "" + cnf.startVariable));
		}

		// Change rest of the non conforming rules.

		HashSet<Rule> repeatableRules = new HashSet<Rule>();

		final HashSet<Character> tempTerminalSymbols = cnf.terminalSymbols;
		HashSet<Rule> nonConformingRules = cnf.rules.stream()
				.filter(r -> r.rightHandSide.length() == 2
						&& (tempTerminalSymbols.contains(r.rightHandSide.charAt(0))
								|| tempTerminalSymbols.contains(r.rightHandSide.charAt(1)))
						|| r.rightHandSide.length() > 2)
				.collect(Collectors.toCollection(HashSet<Rule>::new));

		for (Rule nonConformingRule : nonConformingRules) {
			String tempRightHandSide = "";
			for (int i = 0; i < nonConformingRule.rightHandSide.length(); i++) {
				char c = nonConformingRule.rightHandSide.charAt(i);
				if (cnf.terminalSymbols.contains(c)) {
					Rule r = createRuleFromRightHandSide("" + c, repeatableRules, cnf);
					repeatableRules.add(r);
					c = r.leftHandSide;
				}
				tempRightHandSide += c;
			}

			tempRightHandSide = groupCharactersByTwo(nonConformingRule.leftHandSide, tempRightHandSide,
					tempRightHandSide.length(), repeatableRules, cnf);

			removeRuleFromCfg(nonConformingRule, cnf);

		}

		// Sort the rules by the oder the non-terminals appear from the right hand side
		// of previous rules starting from the rules with start variable.

		cnf.rules = cnf.rules.stream().sorted(new RuleSorter(cnf.nonTerminalSymbols, cnf.rules, cnf.startVariable))
				.toList();

		return cnf;
	}

	public String groupCharactersByTwo(char leftHandSide, String str, int length, HashSet<Rule> repeatableRules,
			ContextFreeGrammar cnf) {
		String s = "";
		if (str.length() == 1) {
			s += str;
		}
		if (str.length() >= 2) {
			Rule r = null;
			if (length > 2) {
				r = createRuleFromRightHandSide(str.substring(0, 2), repeatableRules, cnf);
				repeatableRules.add(r);
			} else {
				r = new Rule(leftHandSide, str.substring(0, 2));
				cnf.addRuleToCfg(r, cnf);
			}
			s += r.leftHandSide;
		}
		if (str.length() > 2) {
			s += groupCharactersByTwo(leftHandSide, str.substring(2), length, repeatableRules, cnf);
		}
		if (s.length() >= 2 && !str.equals(s)) {
			groupCharactersByTwo(leftHandSide, s, s.length(), repeatableRules, cnf);
		}
		return s;
	}

	public Rule createRuleFromRightHandSide(String tempRightHandSide, HashSet<Rule> repeatableRules,
			ContextFreeGrammar cfg) {
		Optional<Rule> existingRule = null;
		if (!(repeatableRules == null)) {
			existingRule = cfg.rules.stream().filter(r -> repeatableRules.stream().anyMatch(r2 -> r2.equals(r))
					&& r.rightHandSide.equals(tempRightHandSide)).findFirst();
		}
		if (!(existingRule == null) && existingRule.isPresent()) {
			return existingRule.get();
		} else {
			Rule newRule = new Rule(cfg.getNewNonTerminalSymbol(), tempRightHandSide);
			cfg.addRuleToCfg(newRule, cfg);
			return newRule;
		}
	}

	public char getNewNonTerminalSymbol() {

		Character[] potentialNonTerminalSymbolsAsArray = this.potentialNonTerminalSymbols
				.toArray(new Character[this.potentialNonTerminalSymbols.size()]);
		int randomArrayIndex = 0;
		if (this.potentialNonTerminalSymbols.size() > 0) {
			Random random = new Random();
			randomArrayIndex = random.nextInt(0, this.potentialNonTerminalSymbols.size());
		}
		char newNonTerminalSymbol = potentialNonTerminalSymbolsAsArray[randomArrayIndex];
		this.potentialNonTerminalSymbols.remove(newNonTerminalSymbol);
		this.nonTerminalSymbols.add(newNonTerminalSymbol);
		return newNonTerminalSymbol;
	}

	public HashSet<Rule> generateNewRulesFromEpsilonRule(Rule epsilonRule, Rule tempRule) {
		HashSet<Rule> result = new HashSet<Rule>();

		List<Integer> charIndexList = new ArrayList<Integer>();
		String tempRightHandSide2 = tempRule.rightHandSide;
		for (int i = 0; i < tempRightHandSide2.length(); i++) {
			if (tempRightHandSide2.charAt(i) == epsilonRule.leftHandSide) {
				charIndexList.add(i);
			}
		}
		char[] charReplacement = new char[] { epsilon, epsilonRule.leftHandSide };

		HashSet<Rule> tempResult = new HashSet<Rule>();
		repeatingPermutation(charIndexList, charReplacement, tempRule, tempResult);
		result.addAll(tempResult);

		return result;
	}

	public void repeatingPermutationRecursion(String str, char[] data, int last, int index, List<Integer> charIndexList,
			Rule tempRule, HashSet<Rule> result) {
		int length = str.length();
		for (int i = 0; i < length; i++) {
			data[index] = str.charAt(i);
			if (index == last) {
//				System.out.println(new String(data));
				String tempRightHandSide = tempRule.rightHandSide;
				for (int j = charIndexList.size() - 1; j >= 0; j--) {
					String charAsString = (data[j] == epsilon) ? "" : "" + data[j];
					if ((charIndexList.get(j) + 1) < tempRule.rightHandSide.length()) {
						tempRightHandSide = tempRightHandSide.substring(0, charIndexList.get(j)) + charAsString
								+ tempRightHandSide.substring(charIndexList.get(j) + 1);
					} else {
						tempRightHandSide = tempRightHandSide.substring(0, charIndexList.get(j)) + charAsString;

					}
				}
				if (!tempRightHandSide.equals(tempRule.rightHandSide) && !tempRightHandSide.equals("")) {
					result.add(new Rule(tempRule.leftHandSide, tempRightHandSide));
				}
			} else {
				repeatingPermutationRecursion(str, data, last, index + 1, charIndexList, tempRule, result);
			}

		}
	}

	public void repeatingPermutation(List<Integer> charIndexList, char[] charReplacement, Rule tempRule,
			HashSet<Rule> result) {
		int length = charIndexList.size();
		char[] data = new char[length + 1];
		char[] temp = charReplacement;
		Arrays.sort(temp);
		String tempRightHandSide = new String(temp);
		repeatingPermutationRecursion(tempRightHandSide, data, length - 1, 0, charIndexList, tempRule, result);
	}

	public void addRuleToCfg(Rule newRule, ContextFreeGrammar cfg) {
		if (cfg.rules.stream().noneMatch(r -> r.equals(newRule))) {
			cfg.rules.add(newRule);
		}
	}

	public void removeRuleFromCfg(Rule ruleToBeRemoved, ContextFreeGrammar cfg) {
		cfg.rules.remove(ruleToBeRemoved);
		if (cfg.rules.stream().noneMatch(r -> r.leftHandSide == ruleToBeRemoved.leftHandSide)) {
			cfg.nonTerminalSymbols.remove(ruleToBeRemoved.leftHandSide);
		}
	}

	@Override
	public String toString() {
		String result = "NON-TERMINAL\n";
		for (char nonTerminalSymbol : this.nonTerminalSymbols) {
			result += nonTerminalSymbol + "\n";
		}
		result += "TERMINAL\n";
		for (char terminalSymbol : this.terminalSymbols) {
			result += terminalSymbol + "\n";
		}
		result += "RULES\n";
		for (Rule rule : this.rules) {
			result += rule + "\n";
		}
		return result + "START\n" + this.startVariable;
	}
}

class Rule {
	char leftHandSide;
	String rightHandSide;
	static final char epsilon = 'e';

	public Rule(char leftHandSide, String rightHandSide) {
		this.leftHandSide = leftHandSide;
		this.rightHandSide = (rightHandSide.isBlank() || rightHandSide.isEmpty()) ? "" + epsilon : rightHandSide;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Rule)) {
			return false;
		}
		Rule r = (Rule) o;
		if (this.leftHandSide == r.leftHandSide && this.rightHandSide.equals(r.rightHandSide)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return this.leftHandSide + ":" + this.rightHandSide;
	}
}

class RuleSorter implements Comparator<Rule> {
	HashSet<Character> nonTerminalSymbols;
	List<Rule> rules;
	char startVariable;
	List<Character> variableOrder;

	RuleSorter(HashSet<Character> nonTerminalSymbols, List<Rule> rules, char startVariable) {
		this.nonTerminalSymbols = nonTerminalSymbols;
		this.rules = rules;
		this.startVariable = startVariable;
		this.variableOrder = new ArrayList<Character>();
		this.variableOrder.add(this.startVariable);
		int variableIndex = 0;
		while (this.variableOrder.size() < this.nonTerminalSymbols.size()) {
			final int tempVariableIndex = variableIndex;
			List<String> rightHandSidesOfRules = this.rules.stream()
					.filter(r -> r.leftHandSide == this.variableOrder.get(tempVariableIndex)).map(r -> r.rightHandSide)
					.toList();
			for (String rightHandSide : rightHandSidesOfRules) {
				for (int i = 0; i < rightHandSide.length(); i++) {
					char symbol = rightHandSide.charAt(i);
					if (this.nonTerminalSymbols.contains(symbol) && !this.variableOrder.contains(symbol)) {
						this.variableOrder.add(symbol);
					}
				}
			}
			variableIndex++;
		}
	}

	@Override
	public int compare(Rule r1, Rule r2) {
		int result = 0;
		result = ((Integer) this.variableOrder.indexOf(r1.leftHandSide))
				.compareTo(this.variableOrder.indexOf(r2.leftHandSide));
		if (result != 0) {

		} else if (r1.rightHandSide.length() != r2.rightHandSide.length()) {
			result = ((Integer) r2.rightHandSide.length()).compareTo(r1.rightHandSide.length());
		} else {
			result = r1.rightHandSide.compareTo(r2.rightHandSide);
		}
		return result;
	}
}