/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.util;

import java.util.Objects;

/**
 *
 * @author eric
 */
public class Triplet<F, S, T> extends Pair<F, S> {

  private final T third;

  public Triplet(F first, S second, T third) {
    super(first, second);
    this.third = third;
  }

  public T getThird() {
    return third;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 67 * hash + Objects.hashCode(this.third);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Triplet<?, ?, ?> other = (Triplet<?, ?, ?>) obj;
    if (!Objects.equals(this.third, other.third)) {
      return false;
    }
    return super.equals(obj);
  }

  @Override
  public String toString() {
    return "<" + getFirst() + ", " + getSecond() + ", " + third + '>';
  }

}
