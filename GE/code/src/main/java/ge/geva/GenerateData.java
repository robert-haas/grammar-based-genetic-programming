/* This script generates mapping examples with the offical GE implementation.

The official implementation of GE by the inventors of the
approach can be found at http://ncra.ucd.ie/Site/GEVA.html
which redirects to https://code.google.com/archive/p/geva/

Here a strongly reduced version of the original codebase is provided,
which can be used with the Maven build system that is often part
of Java IDEs such as Netbeans. This means the directory containing
the file "pom.xml" can be imported as project and then be built and run.
*/

package ge.geva;

import ge.geva.Individuals.GEChromosome;
import ge.geva.Individuals.Phenotype;
import ge.geva.Mapper.DerivationTree;
import ge.geva.Mapper.GEGrammar;
import ge.geva.Mapper.Production;
import ge.geva.Mapper.Rule;
import ge.geva.Mapper.Symbol;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Stream;


public class GenerateData {
    
    private static final Logger logger = Logger.getLogger("OneLogger");

    public static void main(String[] args) throws IOException {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");

        // Run a single example with detailed log messages to compare it with a
        // book example https://doi.org/10.1007/978-1-4615-0447-4 (pp. 37-42)
        // and the highly similar paper example: https://doi.org/10.1109/4235.942529
        logger.setLevel(Level.INFO);
        runMinimalExample();

        // Use different grammars to generate a lot of genotype-phenotype mappings
        // and store them in JSON files as reference for other implementations
        logger.setLevel(Level.OFF);
        runDataGeneration();
    }
    
    public static void runMinimalExample() {
        logger.log(Level.INFO, "Reproduce a paper example of the GE mapping with detailed log messages");
        logger.log(Level.INFO, "======================================================================");

        // Paper
        logger.log(Level.INFO, "\nThe following example was reproduced from https://doi.org/10.1007/978-1-4615-0447-4 (pp. 37-42)");
        logger.log(Level.INFO, "An almost identical example is given in https://doi.org/10.1109/4235.942529");

        // Grammar
        logger.log(Level.INFO, "\n1) Create a grammar\n");
        String filepath = "paper_oneill2001_ws.bnf";
        GEGrammar grammar = new GEGrammar(filepath);
        for (Rule rule : grammar.getRules()) {
            logger.log(Level.INFO, "  {0}", rule);
        }
        logger.log(Level.INFO, "");
        logger.log(Level.INFO, "  Some properties");
        logger.log(Level.INFO, "    Start symbol:           {0}", grammar.getStartSymbol());
        logger.log(Level.INFO, "    Number of nonterminals: {0}", grammar.getRules().size());
        logger.log(Level.INFO, "    Number of terminals:    {0}", grammar.getTerminals().size());
        logger.log(Level.INFO, "    Number of productions:  {0}", grammar.getProductionCount());

        // Genotype
        logger.log(Level.INFO, "\n2) Create a GE genotype (=list of integers)\n");
        int[] codons = new int[]{
            220, 40, 16, 203, 101, 53, 202, 203, 102, 55, 220, 202,
            19, 130, 37, 202, 203, 32, 39, 202, 203, 102};
        int chromosomeSize = codons.length;
        GEChromosome chromosome = new GEChromosome(chromosomeSize, codons);
        logger.log(Level.INFO, "  {0}", chromosome);
        logger.log(Level.INFO, "");
        logger.log(Level.INFO, "  Some properties");
        logger.log(Level.INFO, "    Number of codons:                                {0}", chromosome.getLength());
        logger.log(Level.INFO, "    Codon size (=number of bits, from Integer.SIZE): {0}", chromosome.getCodonSizeBits());
        
        // Mapper (implemented here as a combination of chromosome and grammar)
        logger.log(Level.INFO, "\n3) Create a GE mapper\n");
        int practicallyInfinite = 1_000_000_000;
        chromosome.setMaxChromosomeLength(practicallyInfinite);
        grammar.setMaxDerivationTreeDepth(practicallyInfinite);
        grammar.setMaxWraps(2);
        Phenotype phenotype = new Phenotype();
        grammar.setPhenotype(phenotype);
        logger.log(Level.INFO, "  Maximum number of wraps: {0}", grammar.getMaxWraps());
  
        // Use the mapper: genotype -> derivation tree (-> phenotype)
        logger.log(Level.INFO, "\n4) Use the GE mapping procedure to convert a genotype to a phenotype\n");
        DerivationTree dt = new DerivationTree(grammar, chromosome);
        boolean mappedCompletely = dt.buildDerivationTree();
        logger.log(Level.INFO, "");
        logger.log(Level.INFO, "\n  Derivation tree:\n{0}", dt);
        logger.log(Level.INFO, "\n  Phenotype: {0}", phenotype.getStringNoSpace());
        logger.log(Level.INFO, "  Mapped completely: {0}", mappedCompletely);
    }

    public static void runDataGeneration() throws IOException {
        System.out.printf("\n\n\n");
        System.out.printf("Generate JSON files with genotype-to-phenotype mapping data\n");
        System.out.printf("=============================================================\n");

        // Create grammars
        List grammars = createGrammars();

        // Create genotypes
        List genotypes = createIntegerGenotypesAsStrings();

        // Use different mappers to convert genotypes to strings of a grammar
        generateAndExportMappingData(genotypes, grammars);
    }
    
    public static void generateAndExportMappingData(List<String> genotypes, List<GEGrammar> grammars) throws IOException{
        // Iterate over grammars
        for (int numGrammar=0; numGrammar<grammars.size(); numGrammar++) {
            System.out.printf("\nGrammar " + numGrammar);
            GEGrammar grammar = grammars.get(numGrammar);
            int codonSize = 8;  // fixed by code that generated random genotypes in int range 0-255
            int maxWraps = 30;  // chosen large to only stop infinite recursions but no finite mapping
            List<String> phenotypes = mapGenotypesToPhenotypesGE(genotypes, grammar, maxWraps);
            String method = "GE";
            String parameters = "\n    \"codon_size\": " + codonSize + ",\n    \"max_wraps\": " + maxWraps;
            String filepath = "GE_mappings_" + numGrammar + ".json";
            exportJSONData(filepath, grammar, genotypes, phenotypes, method, parameters);
        }
    }
    
    public static List<String> mapGenotypesToPhenotypesGE(List<String> genotypes, GEGrammar grammar, int maxWraps) {
        String phenotype;
        List phenotypes = new ArrayList<String>();
        for (String genotype : genotypes) {
            phenotype = mapGenotypeToPhenotypeGE(genotype, grammar, maxWraps);
            phenotypes.add(phenotype);
        }
        return phenotypes;
    }
    
    public static String mapGenotypeToPhenotypeGE(String genotype, GEGrammar grammarGiven, int maxWraps){
        GEGrammar grammar = new GEGrammar(grammarGiven);
        int[] intGenotype = stringGenotypeToIntArray(genotype);
        int numCodons = intGenotype.length;
        int practicallyInfinite = 1_000_000_000;
        GEChromosome chromosome = new GEChromosome(numCodons, intGenotype);
        Phenotype phenotype = new Phenotype();
        String phenotypeString;
        
        chromosome.setMaxChromosomeLength(practicallyInfinite);
        grammar.setMaxDerivationTreeDepth(practicallyInfinite);
        grammar.setMaxWraps(maxWraps);
        grammar.setPhenotype(phenotype);
        
        DerivationTree dt = new DerivationTree(grammar, chromosome);
        boolean mappedCompletely = dt.buildDerivationTree();
        if (!mappedCompletely){
            phenotypeString = "MappingException";
        } else {
            phenotypeString = phenotype.getStringNoSpace();
        }
        return phenotypeString;
    }
    
    public static int[] stringGenotypeToIntArray(String genotype){
        return Stream.of(genotype.split(",")).mapToInt(Integer::parseInt).toArray();
    }
    
    public static void exportJSONData(String filepath, GEGrammar grammar, List<String> genotypes, List<String>phenotypes, String method, String parameters) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\n  \"grammar\": {");
        sb.append("\n    \"bnf\": \"" + grammarToJSONString(grammar) + "\",");
        sb.append("\n    \"start_symbol\": \"" + grammar.getStartSymbol() + "\",");
        sb.append("\n    \"nonterminals\": [");
        for (Rule rule : grammar.getRules()){
            sb.append("\n      \"" + rule.getLHS() + "\",");
        }
        sb.deleteCharAt(sb.length() - 1);  // remove last comma
        sb.append("\n    ],");
        sb.append("\n    \"terminals\": [");
        Set<Symbol> uniqueTerminals = new HashSet<Symbol>(grammar.getTerminals());
        for (Symbol terminal : uniqueTerminals){
            sb.append("\n      \"" + terminal + "\",");
        }
        sb.deleteCharAt(sb.length() - 1);  // remove last comma
        sb.append("\n    ]");
        sb.append("\n  },");
        sb.append("\n  \"method\": \"" + method + "\",");
        sb.append("\n  \"parameters\": {" + parameters);
        sb.append("\n  },");
        sb.append("\n  \"genotype_to_phenotype_mappings\": {");
        int i = 0;
        for (; i<genotypes.size()-1; i++) {
            sb.append("\n    \"[" + genotypes.get(i) + "]\": \"" + phenotypes.get(i) + "\",");
        }
        // no comma after final entry
        sb.append("\n    \"" + genotypes.get(i) + "\": \"" + phenotypes.get(i) + "\"");
        sb.append("\n  }");
        sb.append("\n}");
        String jsonString = sb.toString();
        writeToTextFile(jsonString, filepath);
    }
    
    public static String grammarToJSONString(GEGrammar grammar) {
        StringBuilder sb = new StringBuilder();
        for (Rule rule : grammar.getRules()) {
            sb.append(rule.getLHS() + " ::= ");
            for (Production production : rule){
                for (Symbol symbol : production){
                    sb.append(symbol + " ");
                }
                sb.append("| ");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append("\\n");
        }
        return sb.toString();
    }
    
    public static void writeToTextFile(String text, String filepath) throws IOException{
        FileWriter writer = new FileWriter(filepath);
        writer.write(text);
        writer.close();
    }
    
    public static List<GEGrammar> createGrammars() {
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
        GEGrammar grammar;
        List grammars = new ArrayList<GEGrammar>();
        File file;
        for (String filename : filenames){
          String filepath = filename;
          grammar = new GEGrammar(filepath);
          grammars.add(grammar);
        }
        return grammars;
    }
    
    public static List<String> createIntegerGenotypesAsStrings() throws FileNotFoundException {
        File file = new File("src/main/resources/genotypes.txt");
        Scanner scanner = new Scanner(file);
        scanner.useDelimiter(System.getProperty("line.separator"));
        ArrayList<String> genotypes = new ArrayList<String>();
        while (scanner.hasNext()){
            genotypes.add(scanner.next());
        }
        scanner.close();
        return genotypes;
    }
}
