/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core;

import java.io.Serializable;
import java.util.Random;

/**
 *
 * @author eric
 */
public interface Factory<G> extends Serializable {
  
  public G build(Random random);
  
}
