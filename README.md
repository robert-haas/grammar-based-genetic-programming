# Grammar-based genetic programming

The aim of this repository is to take a close look at the **genotype-to-phenotype mapping procedures** of different grammar-based genetic programming systems (GBGP) in order to be able to reimplement them precisely. Currently covered are the following systems:

- Grammatical Evolution (GE)
- Weighted Hierarchical Grammatical Evolution (WHGE)
- Dynamic Structured Grammatical Evolution (DSGE)


## What is provided in this repository?

- **Reduced reference implementations**: Thankfully the inventors of each system prepared both academic journal articles as explanation and code repositories as reference implementation. These implementations often contain a lot of functionality on top of the core algorithm, which can look overwhelmingly complex on first sight. In this repository, strongly reduced versions of the original codebases are provided, which are just sufficient to read a grammar in BNF notation and perform a genotype-to-phenotype mapping. This reduction was done to a) remove a lot of external dependencies and b) simplify the systems drastically to observe only their essential parts in action. To further improve understanding, log messages were added at all relevant points in the mapping procedures, which can be turned on or off by setting the log level correspondingly. This repository contains a directory for each system (``GE``, ``WHGE``, ``DSGE``) and the reduced reference implementations can be found in the ``code`` subdirectories.

- **Reproduced paper examples**: Each system was published with at least one step-by-step mapping example to demonstrate its inner workings. These examples were successfully reproduced here with the reduced reference implementations. The log messages were collected in text files named ``paper_example.txt`` and can be found in the ``generated_data`` subdirectories in this repository. These files may help to develop a precise understanding of each mapping procedure.

- **Datasets of genotype-phenotype mappings**: Hundreds of randomly generated genotypes in combination with 20 different grammars were used as input for the genotype-to-phenotype mapping procedures and the output phenotypes were collected. These input-output examples are provided as simple JSON files (one for each grammar and parameter set) and can be found in the ``generated_data/mappings`` subdirectories in this repository. This synthetic dataset may serve as reference against which new implementations can be tested.


## Where can relevant literature and code be found?

- Grammar-based genetic programming (GBGP), also known as grammar-guided genetic programming (GGGP, G3P)

  - Reviews that cover many approaches: [McKay et al. in 2010](https://doi.org/10.1007/s10710-010-9109-y), [O’Neill et al. in 2010](https://doi.org/10.1007/s10710-010-9113-2)

    The central question all these approaches try to answer is how the expressive power of [context-free grammars (CFG)](https://en.wikipedia.org/wiki/Context-free_grammar) can be used in combination with the flexibility of search algorithms from the field of [Evolutionary Computation (EC)](https://en.wikipedia.org/wiki/Evolutionary_computation) or its subfield [Genetic Programming (GP)](https://en.wikipedia.org/wiki/Genetic_programming).

- Grammatical Evolution (GE)

  - First conference paper:
    [Ryan, Collins, O’Neill in 1998](https://doi.org/10.1007/BFb0055930)
  - Follow-up journal article:
    [O’Neill, Ryan in 2001](https://doi.org/10.1109/4235.942529)
  - Website by Michael O'Neill:
    [grammatical-evolution.org](http://www.grammatical-evolution.org)
  - Reference implementation:
    [GEVA](http://ncra.ucd.ie/Site/GEVA.html) written in Java
    
    - Journal article on GEVA:
      [O'Neill et al. in 2008](https://doi.org/10.1145/1527063.1527066)
    - Exact version used here:
      [v2.0 from Jun 22, 2011](https://code.google.com/archive/p/geva/downloads)
    

- Weighted Hierarchical Grammatical Evolution (WHGE)

  - First conference paper:
    [Medvet in 2017](https://doi.org/10.1145/3067695.3075972)
  - Follow-up journal article:
    [Bartoli, Castelli, Medvet in 2018](https://doi.org/10.1109/TCYB.2018.2876563)
  - Reference implementation:
    [evolved-ge](https://github.com/ericmedvet/evolved-ge) written in Java

- Dynamic Structured Grammatical Evolution (DSGE)

  - First conference paper:
    [Assunção, Lourenço, Machado, Ribeiro in 2017](https://doi.org/10.1145/3071178.3071286)
  - Follow-up book chapter:
    [Lourenço et al. in 2018](https://doi.org/10.1007/978-3-319-78717-6_6)
  - Reference implementation:
    [dsge](https://github.com/nunolourenco/dsge) written in Python 3
