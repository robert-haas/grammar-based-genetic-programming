"""This script generates mapping examples with the offical DSGE implementation.

The official implementation of DSGE by the inventors of the
approach can be found at https://github.com/nunolourenco/dsge

Here a strongly reduced version of the original codebase is used,
which can be found in the directory dsge_reduced.


USAGE: python3 generate_data.py

"""

import json
import logging
import os
import random

from dsge_reduced.grammar import Grammar


logging.basicConfig(level=logging.INFO, format='%(message)s')
LOGGER = logging.getLogger()

DIRPATH = os.path.join('input', 'grammars')

random.seed(1729)  # an attempt to ensure the same random genotypes are generated, not perfect


def run_minimal_example():
    """Run a small, verbose example to reproduce the results shown in first paper on DSGE in 2017.

    References
    ----------
    - https://doi.org/10.1145/3071178.3071286

    """
    logging.info('Reproduce a paper example of the DSGE mapping with detailed log messages');
    logging.info('========================================================================\n');
    
    # Grammar
    logging.info('1) Create a grammar\n')
    filepath = os.path.join(DIRPATH, 'paper_assuncao2017.bnf')
    grammar = Grammar(filepath, init_depth=6, max_depth=17)
    logging.info(str(grammar))

    # Genotype
    logging.info('\n2) Create a DSGE genotype (=list of lists of integers)\n')
    genotype = [[0], [0], [1], [0, 0, 1], [2, 5, 9]]
    logging.info(genotype)

    # Mapping
    logging.info('\n\n3) Use the DSGE mapping procedure to convert a genotype to a phenotype\n')
    mapping_numbers = [0] * len(genotype)
    phenotype, depth = grammar.mapping(genotype, mapping_numbers)
    logging.info('\nResults')
    logging.info('- Depth of the derivation tree: %s', depth)
    logging.info('- Phenotype: %s', phenotype)


def run_data_generation():
    """Create a lot of genotype-to-phenotype mapping examples with different grammars."""
    logging.info('\n\n')
    logging.info('Generate JSON files with genotype-to-phenotype mapping data');
    logging.info('===========================================================\n');

    def save_as_json(filepath, grammar, max_depth, gen_phe_mappings):
        data = {
            'grammar': {
                'bnf': str(grammar),
                'start_symbol': grammar.start_rule[0],
                'nonterminals': list(grammar.ordered_non_terminals),
                'terminals': list(grammar.terminals),
            },
            'method': 'DSGE',
            'parameters': {
                'max_depth': max_depth,
            },
            'genotype_to_phenotype_mappings': gen_phe_mappings,
        }
        with open(filepath, 'w') as fp:
            json.dump(data, fp, indent=2)

    
    max_depth = 12
    num_mappings = 2000
    for cnt, filename in enumerate(sorted(os.listdir(DIRPATH))):
        # Status message
        LOGGER.setLevel(logging.INFO)
        logging.info('Grammar %s: %s', cnt, filename)
        LOGGER.setLevel(logging.CRITICAL)
        # Grammar
        filepath = os.path.join(DIRPATH, filename)
        grammar = Grammar(filepath, init_depth=2, max_depth=max_depth)
        num_nonterminals = grammar.count_number_of_non_terminals()
        # Mappings
        gen_phe_mappings = dict()
        while True:
            # Use an empty genotype, so that the mapping method builds one randomly on the fly
            genotype = [[] for _ in range(num_nonterminals)]
            mapping_numbers = [0] * num_nonterminals
            phenotype, depth = grammar.mapping(genotype, mapping_numbers)
            gen_phe_mappings[str(genotype)] = phenotype
            if len(gen_phe_mappings) >= num_mappings:
                break
        # Export the results as JSON file
        save_as_json('DSGE_mappings_{}.json'.format(cnt), grammar, max_depth, gen_phe_mappings)


# Run a single example with detailed log messages to compare it with
# example from paper in 2018
run_minimal_example()


# Use different grammars to generate a lot of genotype-phenotype mappings
# and store them in JSON files as reference for other implementations
run_data_generation()
