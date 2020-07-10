/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.mapper;

import it.units.malelab.ege.core.mapper.AbstractMapper;
import it.units.malelab.ege.core.mapper.MappingException;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.util.Pair;
import it.units.malelab.ege.util.Utils;
import it.units.malelab.ege.ge.genotype.SGEGenotype;
import it.units.malelab.ege.core.Grammar;
import static it.units.malelab.ege.ge.mapper.StandardGEMapper.BIT_USAGES_INDEX_NAME;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author eric
 */
public class SGEMapper<T> extends AbstractMapper<SGEGenotype<T>, T> {

  private final Grammar<Pair<T, Integer>> nonRecursiveGrammar;
  private final Map<Pair<T, Integer>, List<Integer>> geneBounds;
  private final int maxDepth;
  private Map<Pair<T, Integer>, Integer> geneFirstIndexes;

  public SGEMapper(int maxDepth, Grammar<T> grammar) {
    super(grammar);
    this.maxDepth = maxDepth;
    nonRecursiveGrammar = Utils.resolveRecursiveGrammar(grammar, maxDepth);
    geneBounds = new LinkedHashMap<>();
    geneFirstIndexes = new LinkedHashMap<>();
    int counter = 0;
    for (Map.Entry<Pair<T, Integer>, List<List<Pair<T, Integer>>>> entry : nonRecursiveGrammar.getRules().entrySet()) {
      List<Integer> bounds = new ArrayList<>();
      for (int i = 0; i<maximumExpansions(entry.getKey(), nonRecursiveGrammar); i++) {
        bounds.add(entry.getValue().size());
      }
      geneBounds.put(entry.getKey(), bounds);
      geneFirstIndexes.put(entry.getKey(), counter);
      counter = counter+bounds.size();
    }
  }

  public Node<T> map(SGEGenotype<T> genotype, Map<String, Object> report) throws MappingException {
    int[] usages = new int[genotype.size()];
    //map
    Multiset<Pair<T, Integer>> expandedSymbols = LinkedHashMultiset.create();
    Node<Pair<T, Integer>> tree = new Node<>(nonRecursiveGrammar.getStartingSymbol());
    while (true) {
      Node<Pair<T, Integer>> nodeToBeReplaced = null;
      for (Node<Pair<T, Integer>> node : tree.leafNodes()) {
        if (nonRecursiveGrammar.getRules().keySet().contains(node.getContent())) {
          nodeToBeReplaced = node;
          break;
        }
      }
      if (nodeToBeReplaced == null) {
        break;
      }
      //get codon
      List<Integer> values = genotype.getGenes().get(nodeToBeReplaced.getContent());
      int value = values.get(expandedSymbols.count(nodeToBeReplaced.getContent()));
      int usageIndex = geneFirstIndexes.get(nodeToBeReplaced.getContent())+expandedSymbols.count(nodeToBeReplaced.getContent());
      usages[usageIndex] = usages[usageIndex]+1;
      List<List<Pair<T, Integer>>> options = nonRecursiveGrammar.getRules().get(nodeToBeReplaced.getContent());
      int optionIndex = value;
      //add children
      for (Pair<T, Integer> symbol : options.get(optionIndex)) {
        Node<Pair<T, Integer>> newChild = new Node<>(symbol);
        nodeToBeReplaced.getChildren().add(newChild);
      }
      expandedSymbols.add(nodeToBeReplaced.getContent());
    }
    report.put(BIT_USAGES_INDEX_NAME, usages);
    return transform(tree);
  }
  
  private Node<T> transform(Node<Pair<T, Integer>> pairNode) {
    Node<T> node = new Node<>(pairNode.getContent().getFirst());
    for (Node<Pair<T, Integer>> pairChild : pairNode.getChildren()) {
      node.getChildren().add(transform(pairChild));
    }
    return node;
  }

  private <E> int maximumExpansions(E nonTerminal, Grammar<E> g) {
    //assume non recursive grammar
    if (nonTerminal.equals(g.getStartingSymbol())) {
      return 1;
    }
    int count = 0;
    for (Map.Entry<E, List<List<E>>> rule : g.getRules().entrySet()) {
      int maxCount = Integer.MIN_VALUE;
      for (List<E> option : rule.getValue()) {
        int optionCount = 0;
        for (E optionSymbol : option) {
          if (optionSymbol.equals(nonTerminal)) {
            optionCount = optionCount + 1;
          }
        }
        maxCount = Math.max(maxCount, optionCount);
      }
      if (maxCount > 0) {
        count = count + maxCount * maximumExpansions(rule.getKey(), g);
      }
    }
    return count;
  }

  public Map<Pair<T, Integer>, List<Integer>> getGeneBounds() {
    return geneBounds;
  }

  @Override
  public String toString() {
    return "SGEMapper{" + "maxDepth=" + maxDepth + '}';
  }
  
}
