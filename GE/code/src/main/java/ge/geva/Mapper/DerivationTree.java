/*
Grammatical Evolution in Java
Release: GEVA-v2.0.zip
Copyright (C) 2008 Michael O'Neill, Erik Hemberg, Anthony Brabazon, Conor Gilligan 
Contributors Patrick Middleburgh, Eliott Bartley, Jonathan Hugosson, Jeff Wrigh

Separate licences for asm, bsf, antlr, groovy, jscheme, commons-logging, jsci is included in the lib folder. 
Separate licence for rieps is included in src/com folder.

This licence refers to GEVA-v2.0.

This software is distributed under the terms of the GNU General Public License.


This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
/>.
*/

/*
 * DerivationTree.java
 *
 * Created on 17 October 2006, 13:32
 *
 * DerrivationTree
 *
 */

package ge.geva.Mapper;

import ge.geva.Exceptions.MalformedGrammarException;
import ge.geva.Individuals.GEChromosome;
import ge.geva.Util.Enums;
import ge.geva.Util.Constants;
import ge.geva.Util.Structures.IntIterator;
import ge.geva.Util.Structures.NimbleTree;
import ge.geva.Util.Structures.TreeNode;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class is used in the mapping from genotype to phenotype in GE.
 * @author EHemberg
 */
public class DerivationTree extends NimbleTree<Symbol> implements Derivation {
    final protected GEGrammar grammy;
    final protected GEChromosome genny;
    protected int wrapCount = 0;
    protected IntIterator genIter;
    protected int geneCnt;
    protected int currentCodonValue = -1;
    
    private static final Logger logger = Logger.getLogger("OneLogger");

    @Override
    protected TreeNode<Symbol> newNode(){
	return new DerivationNode();
    }
    
    /**
     * Create new Derivation Tree
     * @param gram mapper
     * @param gen input
     */
    public DerivationTree(GEGrammar gram, GEChromosome gen){
        super();
        this.getRoot().setData(gram.getStartSymbol());
        this.genny = gen;
        this.grammy = gram;
        this.genIter = genny.iterator();
        this.wrapCount = 1;
        this.geneCnt = 0;
    }

    /**
     * Copy Constructor
     * @param copy Derivation Tree to copy
     */
    public DerivationTree(DerivationTree copy) {
	super(copy);
	this.grammy = new GEGrammar(copy.grammy);
	this.genny = new GEChromosome(copy.genny);
	this.wrapCount = copy.wrapCount;
	this.geneCnt = copy.geneCnt;
      }

    /**
     * Build a derivation tree. Using the grammar as a mapper and the input is the genotype.
     * @return boolean Did the tree map completely
     */
    public boolean buildDerivationTree(){
        assert this.getCurrentNode() instanceof DerivationNode
             : this.getCurrentNode().getClass().getName();
        logger.log(Level.INFO, "Called: buildDerivationTree()");
        DerivationNode t;
        Boolean validBuild;
        t = (DerivationNode)this.getCurrentNode();
        this.grammy.findRule(t.getData());
	validBuild = growNode(t);
        this.genny.setUsedGenes(this.geneCnt);
        //System.out.println(this.geneCnt);
	//System.out.println(this.wrapCount);
        return validBuild;
    }
    
    /** Extract range for value from non-terminal specification,
     * where specification is in the format <GECodonValue{x, y, z}> (set-mode)
     * or <GECodonValue(x, y)> (range-mode -- range can be open, closed, or half-open).
     * @param s symbol string
     * @param codon codon value
     * @return string value
     * @throws MalformedGrammarException The specification s is malformed
     */
    protected String getGECodonValue(String s, int codon) throws MalformedGrammarException {

	String c = Constants.GE_CODON_VALUE_PARSING;
	if (s.indexOf("{") != -1) {
	    // in "set"-mode: uses set-notation {x, y, z}
	    if (s.indexOf("}") == -1) {
		throw new MalformedGrammarException("Bad GECodonValue specification: " + s);
	    }
	    
	    String GECodonValueRE = c + "\\{(.*)\\}" + ">";
	    Pattern pattern = Pattern.compile(GECodonValueRE);
	    Matcher matcher = pattern.matcher(s);
	    if (matcher.find()) {
		if (matcher.groupCount() == 1) {
		    String content = matcher.group(1);
		    String [] setValues = content.split(",");

		    // Success: found good values in set-mode. Return.
		    return setValues[codon % setValues.length].trim();
		} else {
		    throw new MalformedGrammarException("Bad GECodonValue specification (found too many matches): " + s);
		}
	    } else {
		throw new MalformedGrammarException("Bad GECodonValue specification (didn't find a match): " + s);
	    }
	} 

	else if (s.indexOf("(") != -1 || s.indexOf("[") != -1) {
	    // in "range"-mode: uses open, closed, or half-open ranges:
	    // [x, y], (x, y), [x, y), or (x, y]
	    if (s.indexOf(")") == -1 && s.indexOf("]") == -1) {
		throw new MalformedGrammarException("Bad GECodonValue specification (didn't find closing brackets): " + s);
	    }

	    // first group gets lower-boundary indicator ( or [;
	    // second group gets content, eg 4.1, 4.9;
	    // third group gets upper-boundary indicator ) or ].
	    String GECodonValueRE = c + "([\\(\\[])(.*)([\\)\\]])" + ">";
	    Pattern pattern = Pattern.compile(GECodonValueRE);
	    Matcher matcher = pattern.matcher(s);
	    String content;
	    String [] boundaryValues;
	    if (matcher.find()) {
// 		for (int i = 0; i <= matcher.groupCount(); i++) {
// 		    System.out.println("debug: i = " + i + "; group = " + matcher.group(i));
// 		}

		if (matcher.groupCount() != 3) {
		    throw new MalformedGrammarException("Bad GECodonValue specification (didn't find opening and closing brackets and boundaries): " + s);
		}

		// Get the text
		content = matcher.group(2);
		boundaryValues = content.split(",");
		if (boundaryValues.length != 2) {
		    throw new MalformedGrammarException("Bad GECodonValue specification (didn't find upper and lower boundaries): " + s);
		}
	    } else {
		throw new MalformedGrammarException("Bad GECodonValue specification (didn't find a match): " + s);
	    }

	    int low, high;
	    double lowd, highd;
	    boolean intMode;
	    try {
		intMode = true;
		low = Integer.parseInt(boundaryValues[0].trim());
		high = Integer.parseInt(boundaryValues[1].trim());
		// dummy values, these won't be used
		lowd = 0.0;
		highd = 0.0;
	    } catch (Exception e) {
		intMode = false;
		lowd = Double.parseDouble(boundaryValues[0].trim());
		highd = Double.parseDouble(boundaryValues[1].trim());
		low = 0;
		high = genny.getMaxCodonValue();
	    }
	    
	    if (matcher.group(1).equals("(")) {
		// it's an open range: exclude the low value
		// System.out.println("range semi-open at the bottom");
		low += 1;
		// FIXME what a hack
		lowd += 0.0001; 
	    }
	    if (matcher.group(3).equals(")")) {
		// it's an open range: exclude the high value
		// System.out.println("range semi-open at the top");
		high -= 1;
		// FIXME what a hack
		highd -= 0.0001;
	    }
	    
	    if (intMode == true) {
		if (low > high) {
		    throw new MalformedGrammarException("Bad GECodonValue specification (bad boundaries): " + s);
		}

		// Success: found good boundaries in integer mode: return.
		return String.valueOf(low + (codon % (high - low + 1)));
	    } else {
		if (lowd > highd) {
		    throw new MalformedGrammarException("Bad GECodonValue specification (bad boundaries): " + s);
		}
		// Success: found good boundaries in double-mode: return.
		return String.valueOf(lowd + (highd - lowd) * (codon) / Double.valueOf((genny.getMaxCodonValue() - 0)));
	    }
	    
	} 

	// may be in legacy mode
	else {
	    System.out.println("Warning: DerivationTree attempting to use GECodonValue legacy format");
	    return getGECodonValueLegacyFormat(s, codon);
	}
    }

    /** Extract range for value from non-terminal specification,
     * where specification is in the legacy format <GECodonValue-3+17>
     * @param s symbol string
     * @param codon codon value
     * @return string value
     */
    String getGECodonValueLegacyFormat(String s, int codon) {
        int low = 0;
        int high = -1;
        int i = Constants.GE_CODON_VALUE_PARSING.length(); //Start value for codon counter
        String codon_value;
        char currentChar = s.charAt(i);// currentChar is first character after "<GECodonValue"
        // Look for range definitions
        while(currentChar != '>'){
            if(currentChar == '-'){// Low range specification
                currentChar = s.charAt(i++);
                while((currentChar >= '0') && (currentChar <= '9')){
                    low = (low * 10) + (currentChar - '0');
                    currentChar = s.charAt(i++);
                }
            } else if(currentChar == '+'){// High range specification
                currentChar = s.charAt(i++);
                while((currentChar >= '0') && (currentChar <= '9')){
                    if(high == -1){
                        high = 0;
                    }
                    high = (high * 10) + (currentChar - '0');
                    currentChar = s.charAt(i++);
                }
            } else{// Ignore errors
                currentChar = s.charAt(i++);
            }
        }
        // High range was not specified, so set it to maximum
        if(high == -1){
            high = genny.getMaxCodonValue();
        }
        if(high == low){// Catch division by zero
            codon_value = String.valueOf(low);
        } else{
            codon = (codon%(high-low+1))+low;
            codon_value = String.valueOf(codon);
        }
        return codon_value;
    }
    
    /**
     * Grows the nodes of the tree in a recursive procedure.
     * @param t start node
     * @return validity of growth
     **/
    @SuppressWarnings({"LoopStatementThatDoesntLoop"})
    protected boolean growNode(DerivationNode t) {
        logger.log(Level.INFO, "  Called: growNode()");
        Symbol s = t.getData();

        if (s.getType().toString() == "NTSymbol") {
            logger.log(Level.INFO, "    Nonterminal symbol: {0}", s);
        } else {
            logger.log(Level.INFO, "    Terminal symbol: {0}", s);
        }

        if(this.getDepth() > grammy.getMaxDerivationTreeDepth()) {
            System.out.println("maxDerivationTreeDepth exceeded:" + this.getDepth()+" > "+grammy.maxDerivationTreeDepth);
            return false;
        }
        if(this.geneCnt > this.genny.getMaxChromosomeLength()) {
            System.out.println("maxGEChromosomeLength exceeded:" + this.geneCnt+">"+genny.getMaxChromosomeLength());
            return false;
        } 

        if(s.getType()==Enums.SymbolType.NTSymbol){
            Rule r = this.grammy.findRule(s);
            logger.log(Level.INFO, "    Rules: {0}", r);

            int numProd = r.size();
            logger.log(Level.INFO, "      Number of possible productions: {0}", numProd);

            if(!this.genIter.hasNext()){
                this.wrapCount++;
                logger.log(Level.INFO, "      Wrap event {0}", this.wrapCount);
                this.genIter = genny.iterator();
            }
	    
            //Use a codon if there is more than one production or if
            //the production is GECodonValue
            while(this.wrapCount <= this.grammy.getMaxWraps() 
		  || numProd==1
                    && !r.get(0).get(0).getSymbolString().startsWith(Constants.GE_CODON_VALUE_PARSING)) {
                Production p;

                if(numProd > 1) {
                    logger.log(Level.INFO, "      Multiple productions, using a codon to choose one:");
                    this.currentCodonValue = this.genIter.next();
                    logger.log(Level.INFO, "        Codon value: {0}", this.currentCodonValue);
		    t.setCodonIndex(geneCnt, this.currentCodonValue, this.currentCodonValue % numProd);
                    this.geneCnt++;
                    logger.log(Level.INFO, "        Chosen rule index: {0}", this.currentCodonValue % numProd);
                    p = r.get(this.currentCodonValue % numProd);
                    logger.log(Level.INFO, "        Production: {0}", p);
                } 
		else {
                    logger.log(Level.INFO, "      Only one production, using it without consuming a codon.");
		    if(r.get(0).get(0).getType() == Enums.SymbolType.NTSymbol
		       && r.get(0).get(0).getSymbolString().startsWith(Constants.GE_CODON_VALUE_PARSING)){
			t.setCodonIndex(geneCnt, this.currentCodonValue, 0);
		    }
                    logger.log(Level.INFO, "        Chosen rule index: {0}", 0);
                    p = r.get(0);
                    logger.log(Level.INFO, "        Production: {0}", p);
                }
                logger.log(Level.INFO, "      Number of used codons: {0}", this.geneCnt);
                logger.log(Level.INFO, "      Number of used wraps:  {0}", this.wrapCount);

                Iterator<Symbol> symIt = p.iterator();
                DerivationNode newTree;
                Symbol newSym;
                while(symIt.hasNext()) {
                    newSym = symIt.next();

                    // Check for GECodonValue
                    if(newSym.getType() == Enums.SymbolType.NTSymbol 
		       && newSym.getSymbolString().startsWith(Constants.GE_CODON_VALUE_PARSING)) {
                        String value;
			
                        // Create a new symbol from the GE codon value
                        // GE Codon uses a codon from the genotype
                        if(!this.genIter.hasNext()){
			    this.genIter = genny.iterator();
                            this.wrapCount++;
                            if (this.wrapCount >= this.grammy.getMaxWraps()) {
                                return false;
                            }
                        }

                        this.geneCnt++;
                        this.currentCodonValue = this.genIter.next();
                        try {
			    value = getGECodonValue(newSym.getSymbolString(), currentCodonValue);

			    // Added to update the codon used to pick
			    // the derivation node (This is mainly for
			    // derivation node/tree toString calls)
			    t.setCodonIndex(geneCnt-1, this.currentCodonValue, 0);

			} catch (MalformedGrammarException e) {
			    System.out.println("Malformed GECodonValue specification in grammar");
			    return false;
			}
                        newSym = new Symbol(value, Enums.SymbolType.TSymbol);
                    }

                    newTree = new DerivationNode(t, newSym);
                    t.add(newTree);
                    this.setCurrentLevel(this.getCurrentLevel()+1);
                    if(this.getDepth()<this.getCurrentLevel()) {
                        this.setDepth(this.getCurrentLevel());
                    }
		    newTree.setDepth(this.getCurrentLevel());
                    if(!growNode(newTree)) {
                        return false;
                    }
                    this.setCurrentLevel(this.getCurrentLevel()-1);
                }
                return true;
            }
            return false;
        } else {
            this.grammy.getPhenotype().add(s);

            return true;
        }
    }

    /**
     * Get the number of wraps used
     * @return int The number of wraps used
     */
    public int getWrapCount() {
        return wrapCount;
    }

    /**
     * Set the number of wraps used
     * @param wrapCount The number of wraps used
     */
    public void setWrapCount(int wrapCount) {
	this.wrapCount = wrapCount;
    }

    /**
     * Get the input used, called genes
     * @return int The number of inputs used
     */
    public int getGeneCnt() {
        return geneCnt;
    }
    
    public boolean derive() {
        return this.buildDerivationTree();
    }
}