import os

with open(os.path.join(os.path.dirname(__file__), 'speed_grammar.ebnf'), 'r') as fp:
    SPEED_GRAMMAR = fp.read()
