/* This script generates mapping examples with the offical WHGE implementation.

The official implementation of WHGE by the inventors of the approach
can be found at https://github.com/ericmedvet/evolved-ge

Here a strongly reduced version of the original codebase is provided,
which can be used with the Maven build system that is often part
of Java IDEs such as Netbeans. This means the directory containing
the file "pom.xml" can be imported as project and then be built and run.
*/

package it.units.malelab.ege;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.units.malelab.ege.core.Grammar;
import it.units.malelab.ege.core.LeavesJoiner;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.mapper.Mapper;
import it.units.malelab.ege.core.mapper.MappingException;
import it.units.malelab.ege.ge.genotype.BitsGenotype;
import it.units.malelab.ege.ge.mapper.PiGEMapper;
import it.units.malelab.ege.ge.mapper.StandardGEMapper;
import it.units.malelab.ege.ge.mapper.WeightedHierarchicalMapper;
import java.io.FileNotFoundException;
import java.util.Scanner;


public class GenerateData {

  private static final Logger logger = Logger.getLogger("OneLogger");

  public static void main(String[] args) throws IOException, InterruptedException, MappingException {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");

    // Run a single example with detailed log messages to compare it with
    // example from paper in 2018
    logger.setLevel(Level.INFO);
    runMinimalExample();

    // Use different grammars to generate a lot of genotype-phenotype mappings
    // and store them in JSON files as reference for other implementations
    logger.setLevel(Level.OFF);
    runDataGeneration();
  }

  public static void runMinimalExample() throws IOException {
    logger.log(Level.INFO, "Reproduce a paper example of the WHGE mapping with detailed log messages");
    logger.log(Level.INFO, "========================================================================");

    // Grammar
    logger.log(Level.INFO, "\n1) Create a grammar\n");
    File file = new File("input/grammars/paper_bartoli2018_ws.bnf");
    Grammar grammar = it.units.malelab.ege.util.Utils.parseFromFile(file);
    logger.log(Level.INFO, "{0}", grammar);

    // Genotype
    logger.log(Level.INFO, "2) Create a WHGE genotype (=bitstring)\n");
    BitsGenotype genotype = new BitsGenotype("111001111111000010100101011100010110010100000111");
    logger.log(Level.INFO, "{0}", genotype);

    // Mapper
    logger.log(Level.INFO, "\n3) Create a WHGE mapper\n");
    int maxDepth = 2;
    Mapper mapper = new WeightedHierarchicalMapper(maxDepth, grammar);
    logger.log(Level.INFO, "Mapper: {0}", mapper);

    // Use the mapper: genotype -> derivation tree (-> phenotype)
    logger.log(Level.INFO, "\n4) Use the WHGE mapping procedure to convert a genotype to a phenotype\n");
    Map<String, Object> report = new LinkedHashMap<>();
    try {
      Node tree = mapper.map(genotype, report);
      LeavesJoiner lj = new LeavesJoiner();
      String phenotype = lj.toString(tree);
      logger.log(Level.INFO, "\n\nResults");
      logger.log(Level.INFO, "- Derivation tree: {0}", tree);
      logger.log(Level.INFO, "- Leave nodes: {0}", tree.leafNodes());
      logger.log(Level.INFO, "- Phenotype: {0}", phenotype);
    } catch (MappingException ex) {
      logger.log(Level.WARNING, "Mapping failed. No phenotype was found.");
    }
  }


  public static void runDataGeneration() throws IOException {
    System.out.printf("\n\n\n");
    System.out.printf("Generate JSON files with genotype-to-phenotype mapping data\n");
    System.out.printf("=============================================================\n");

    // Create grammars
    List grammars = createGrammars();

    // Create genotypes
    List genotypes = createBitGenotyes();

    // Use different mappers to convert genotypes to strings of a grammar
    generateAndExportMappingData(genotypes, grammars);
  }

  public static void generateAndExportMappingData(List<String> genotypes, List<Grammar> grammars) throws IOException {
    // Iterate over grammars
    for (int numGrammar=0; numGrammar<grammars.size(); numGrammar++) {
      System.out.printf("\nGrammar " + numGrammar);
      Grammar grammar = grammars.get(numGrammar);

      /*
      System.out.printf("\n- GE");
      int maxWraps = 10;
      for (int codonSize=4; codonSize<=8; codonSize+=4){
        List<String> phenotypes = mapGenotypesToPhenotypesGE(genotypes, grammar, codonSize, maxWraps);
        String method = "GE";
        String parameters = "\n    \"codon_size\": " + codonSize + ",\n    \"max_wraps\": " + maxWraps;
        String filepath = "GE_mappings_" + numGrammar + "_codon_size_" + codonSize + ".json";
        exportJSONData(filepath, grammar, genotypes, phenotypes, method, parameters);
      }

      System.out.printf("\n- PiGE");
      for (int codonSize=4; codonSize<=8; codonSize+=4){
        List<String> phenotypes = mapGenotypesToPhenotypesPiGE(genotypes, grammar, codonSize, maxWraps);
        String method = "piGE";
        String parameters = "\n    \"codon_size\": " + codonSize + ",\n    \"max_wraps\": " + maxWraps;
        String filepath = "PiGE_mappings_" + numGrammar + "_codon_size_" + codonSize + ".json";
        exportJSONData(filepath, grammar, genotypes, phenotypes, method, parameters);
      }
      */

      //System.out.printf("\n- WHGE");
      for (int maxDepth=1; maxDepth<4; maxDepth+=1){
        List<String> phenotypes = mapGenotypesToPhenotypesWHGE(genotypes, grammar, maxDepth);
        String method = "WHGE";
        String parameters = "\n    \"max_depth\": " + maxDepth;
        String filepath = "WHGE_mappings_" + numGrammar + "_max_depth_" + maxDepth + ".json";
        exportJSONData(filepath, grammar, genotypes, phenotypes, method, parameters);
      }
    }
  }

  public static void exportJSONData(String filepath, Grammar grammar, List<String> genotypes, List<String>phenotypes, String method, String parameters) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\n  \"grammar\": {");
    sb.append("\n    \"bnf\": \"" + grammar.toString().replace("*::=", "::=").replace("\n", "\\n") + "\",");
    sb.append("\n    \"start_symbol\": \"" + grammar.getStartingSymbol() + "\",");
    sb.append("\n    \"nonterminals\": [");
    for (Object nt : grammar.getRules().keySet()) {
        sb.append("\n      \"" + nt + "\",");
    }
    sb.deleteCharAt(sb.length() - 1);  // remove last comma
    sb.append("\n     ]");
    // terminals are skipped here, because they can't be extracted easily
    sb.append("\n  },");
    sb.append("\n  \"method\": \"" + method + "\",");
    sb.append("\n  \"parameters\": {" + parameters);
    sb.append("\n  },");
    sb.append("\n  \"genotype_to_phenotype_mappings\": {");
    int i = 0;
    for (; i<genotypes.size()-1; i++) {
      sb.append("\n    \"" + genotypes.get(i) + "\": \"" + phenotypes.get(i) + "\",");
    }
    // no comma after final entry
    sb.append("\n    \"" + genotypes.get(i) + "\": \"" + phenotypes.get(i) + "\"");
    sb.append("\n  }");
    sb.append("\n}");
    String jsonString = sb.toString();
    writeToTextFile(jsonString, filepath);
  }

  public static List<String> mapGenotypesToPhenotypesGE(List<String> genotypes, Grammar grammar, int codonLength, int maxWraps) {
    Mapper mapper = new StandardGEMapper(codonLength, maxWraps, grammar);
    Map<String, Object> report = new LinkedHashMap<>();
    String phenotype;
    List phenotypes = new ArrayList<String>();
    for (String genotype : genotypes) {
      BitsGenotype bitsGenotype = new BitsGenotype(genotype);
      try {
        Node tree = mapper.map(bitsGenotype, report);
        phenotype = new LeavesJoiner().toString(tree);
      } catch (MappingException ex) {
        phenotype = "MappingException";
      }
      phenotypes.add(phenotype);
    }
    return phenotypes;
  }

  public static List<String> mapGenotypesToPhenotypesPiGE(List<String> genotypes, Grammar grammar, int codonLength, int maxWraps) {
    Mapper mapper = new PiGEMapper(codonLength, maxWraps, grammar);
    Map<String, Object> report = new LinkedHashMap<>();
    String phenotype;
    List phenotypes = new ArrayList<String>();
    for (String genotype : genotypes) {
      BitsGenotype bitsGenotype = new BitsGenotype(genotype);
      try {
        Node tree = mapper.map(bitsGenotype, report);
        phenotype = new LeavesJoiner().toString(tree);
      } catch (MappingException ex) {
        phenotype = "MappingException";
      }
      phenotypes.add(phenotype);
    }
    return phenotypes;
  }

  public static List<String> mapGenotypesToPhenotypesWHGE(List<String> genotypes, Grammar grammar, int maxDepth) {
    Mapper mapper = new WeightedHierarchicalMapper(maxDepth, grammar);
    Map<String, Object> report = new LinkedHashMap<>();
    String phenotype;
    List phenotypes = new ArrayList<String>();
    for (String genotype : genotypes) {
      BitsGenotype bitsGenotype = new BitsGenotype(genotype);
      try {
        Node tree = mapper.map(bitsGenotype, report);
        phenotype = new LeavesJoiner().toString(tree);
      } catch (MappingException ex) {
        phenotype = "MappingException";
      }
      phenotypes.add(phenotype);
    }
    return phenotypes;
  }

  public static void writeToTextFile(String text, String filepath) throws IOException{
    FileWriter writer = new FileWriter(filepath);
    writer.write(text);
    writer.close();
  }

  public static List<Grammar> createGrammars() throws IOException{
    String dirpath = "input/grammars/";
    List<String> filenames = Arrays.asList(
      "abcdf_ws.bnf",
      "algebraic_expression_xyz_ws.bnf",
      "bytes_ws.bnf",
      "digits_1_ws.bnf",
      "digits_2_ws.bnf",
      "digits_3_ws.bnf",
      "dna_ws.bnf",
      "ipv4_ws.bnf",
      "letters_1_ws.bnf",
      "letters_2_ws.bnf",
      "letters_3_ws.bnf",
      "one_max_1_ws.bnf",
      "one_max_2_ws.bnf",
      "one_max_3_ws.bnf",
      "paper_assuncao2017_ws.bnf",
      "paper_bartoli2018_ws.bnf",
      "paper_fagan2010_ws.bnf",
      "paper_oneill2001_ws.bnf",
      "parentheses_ws.bnf",
      "regression_ws.bnf"
    );
    Grammar<String> grammar;
    List grammars = new ArrayList<Grammar>();
    File file;
    for (String filename : filenames){
      String filepath = dirpath + filename;
      file = new File(filepath);
      grammar = it.units.malelab.ege.util.Utils.parseFromFile(file);
      grammars.add(grammar);
    }
    return grammars;
  }

  public static List<String> createBitGenotyes() throws FileNotFoundException {
    File file = new File("input/genotypes.txt");
    Scanner scanner = new Scanner(file);
    ArrayList<String> genotypes = new ArrayList<String>();
    while (scanner.hasNext()){
        genotypes.add(scanner.next());
    }
    scanner.close();
    return genotypes;
  }
}
