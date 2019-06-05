JRealizability
=====

Forked repository of the <a href="https://github.com/agacek/jkind">JKind</a> model checker, with added support for realizability checking and synthesis of reactive implementations. May or may not be up-to-date with the latest version of JKind.

Use the <a href="https://github.com/andrewkatis/jkind-1/releases">releases</a> page to download a pre-built binary.

If you would like to translate synthesized implementations into C/Lustre, you can use the <a href="https://github.com/andrewkatis/SMTLib2C">SMTLIB2C</a> translation tool.

Usage
=====
JKind requires Lustre programs as input. For realizability checking, you should provide specification in the form of an Assume-Guarantee contract. You can find example contracts under the `testing/realizability` folder, as well as in our <a href="https://github.com/andrewkatis/synthesis-benchmarks">benchmark collection</a>. Use `jrealizability` for a list of command options. The following options are of most interest:

- `-solver` sets the underlying solver to use for realizability checking using the k-induction engine. Supported solvers : Microsoft Z3, <a href="https://github.com/grigoryfedyukovich/aeval">AE-VAL</a> (Z3 not supported for fixpoint engine or synthesis tasks. For nondeterministic synthesis, see `nondet` option).
- `-synthesis` Given a realizable contract, use this option to synthesize an implementation in the form of a Skolem function in SMT-LIB 2.0 format. Default engine : k-induction
- `-fixpoint` Enable the fixpoint engine instead of k-induction for realizability checking / synthesis
- `-compact` Attempt to synthesize a more compact implementation. Enables `-synthesis` by default.
- `-allinclusive` Attempt to synthesize an implementation that covers all possible cases. Particularly useful when the contract contains properties in disjunctive / implicative form. Enables `-synthesis` by default.
- `-nondet` Synthesize an implementation that supports nondeterministic behavior. <a href="https://github.com/andrewkatis/fuzzersynthesis">For this option you NEED a modified version of AE-VAL</a>. Enables `-synthesis` by default.