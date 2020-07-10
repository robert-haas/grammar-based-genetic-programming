/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.ge.genotype;

import com.google.common.collect.Range;
import it.units.malelab.ege.core.ConstrainedSequence;
import it.units.malelab.ege.core.Sequence;
import it.units.malelab.ege.util.Utils;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author eric
 */
public class BitsGenotype implements ConstrainedSequence<Boolean> {

  private final int length;
  private final BitSet bitSet;

  private final static Set<Boolean> DOMAIN;

  static {
    Set<Boolean> domain = new LinkedHashSet<>();
    domain.add(Boolean.TRUE);
    domain.add(Boolean.FALSE);
    DOMAIN = Collections.unmodifiableSet(domain);
  }

  public BitsGenotype(String bits) {
    this(bits.length());
    for (int i = 0; i < length; i++) {
      bitSet.set(i, bits.charAt(i) != '0');
    }
  }

  public BitsGenotype(int nBits) {
    this.length = nBits;
    bitSet = new BitSet(nBits);
  }

  public BitsGenotype(int length, BitSet bitSet) {
    this.length = length;
    this.bitSet = bitSet.get(0, length);
  }

  @Override
  public int size() {
    return length;
  }

  public BitsGenotype slice(int fromIndex, int toIndex) {
    checkIndexes(fromIndex, toIndex);
    return new BitsGenotype(toIndex - fromIndex, bitSet.get(fromIndex, toIndex));
  }

  public int count() {
    return bitSet.cardinality();
  }

  public int toInt() {
    BitsGenotype genotype = this;
    if (length > Integer.SIZE / 2) {
      genotype = compress(Integer.SIZE / 2);
    }
    if (genotype.bitSet.toLongArray().length <= 0) {
      return 0;
    }
    return (int) genotype.bitSet.toLongArray()[0];
  }

  public void set(int fromIndex, BitsGenotype other) {
    checkIndexes(fromIndex, fromIndex + other.size());
    for (int i = 0; i < other.size(); i++) {
      bitSet.set(fromIndex + i, other.bitSet.get(i));
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(length + ":");
    for (int i = 0; i < length; i++) {
      if (i > 0 && i % 8 == 0) {
        sb.append('-');
      }
      sb.append(bitSet.get(i) ? '1' : '0');
    }
    return sb.toString();
  }

  @Override
  public Boolean get(int index) {
    checkIndexes(index, index + 1);
    return bitSet.get(index);
  }

  public void flip() {
    bitSet.flip(0, length);
  }

  public void flip(int index) {
    checkIndexes(index, index + 1);
    bitSet.flip(index);
  }

  public void flip(int fromIndex, int toIndex) {
    checkIndexes(fromIndex, toIndex);
    bitSet.flip(fromIndex, toIndex);
  }

  private void checkIndexes(int fromIndex, int toIndex) {
    if (fromIndex >= toIndex) {
      throw new ArrayIndexOutOfBoundsException(String.format("from=%d >= to=%d", fromIndex, toIndex));
    }
    if (fromIndex < 0) {
      throw new ArrayIndexOutOfBoundsException(String.format("from=%d < 0", fromIndex));
    }
    if (toIndex > length) {
      throw new ArrayIndexOutOfBoundsException(String.format("to=%d > length=%d", toIndex, length));
    }
  }

  public BitSet asBitSet() {
    BitSet copy = new BitSet(length);
    copy.or(bitSet);
    return copy;
  }

  public BitsGenotype compress(int newLength) {
    BitsGenotype compressed = new BitsGenotype(newLength);
    List<BitsGenotype> slices = slices(Utils.slices(Range.closedOpen(0, length), newLength));
    for (int i = 0; i < slices.size(); i++) {
      compressed.bitSet.set(i, slices.get(i).count() > slices.get(i).size() / 2);
    }
    return compressed;
  }

  public List<BitsGenotype> slices(final List<Range<Integer>> ranges) {
    List<BitsGenotype> genotypes = new ArrayList<>(ranges.size());
    for (Range<Integer> range : ranges) {
      genotypes.add(slice(range));
    }
    return genotypes;
  }

  public BitsGenotype slice(Range<Integer> range) {
    if ((range.upperEndpoint() - range.lowerEndpoint()) == 0) {
      return new BitsGenotype(0);
    }
    return slice(range.lowerEndpoint(), range.upperEndpoint());
  }

  public BitsGenotype append(BitsGenotype genotype) {
    BitsGenotype resultGenotype = new BitsGenotype(length + genotype.length);
    if (length > 0) {
      resultGenotype.set(0, this);
    }
    resultGenotype.set(length, genotype);
    return resultGenotype;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 47 * hash + this.length;
    hash = 47 * hash + Objects.hashCode(this.bitSet);
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
    final BitsGenotype other = (BitsGenotype) obj;
    if (this.length != other.length) {
      return false;
    }
    if (!Objects.equals(this.bitSet, other.bitSet)) {
      return false;
    }
    return true;
  }

  @Override
  public Set<Boolean> domain(int index) {
    return DOMAIN;
  }

  @Override
  public Sequence<Boolean> clone() {
    return new BitsGenotype(length, bitSet);
  }

  @Override
  public void set(int index, Boolean t) {
    checkIndexes(index, index + 1);
    bitSet.set(index, t);
  }

}
