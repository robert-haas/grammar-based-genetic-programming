/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core;

/**
 *
 * @author eric
 */
public class LeavesJoiner<T> implements PhenotypePrinter<T> {

  @Override
  public String toString(Node<T> node) {
    StringBuilder sb = new StringBuilder();
    for (Node<T> leaf : node.leafNodes()) {
      sb.append(leaf.getContent().toString());
    }
    return sb.toString();
  }

}
