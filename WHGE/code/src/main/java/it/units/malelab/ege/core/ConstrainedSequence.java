/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core;

import java.util.Set;

/**
 *
 * @author eric
 */
public interface ConstrainedSequence<T> extends Sequence<T> {
  
  public Set<T> domain(int index);
  
}
