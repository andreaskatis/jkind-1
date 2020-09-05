JKind
=====

JKind is an SMT-based infinite-state model checker for safety
properties in Lustre. JKind uses parallel cooperating engines
including k-induction, property directed reachability, and
template-based invariant generation.

Downloads
---------

JKind is written in Java and requires at least [Java
8](https://java.com/download). The latest release of JKind is available on the
[releases page](https://github.com/agacek/jkind/releases). This includes the
JKind model checker as well as the JRealizability, JLustre2Excel, and
JLustre2Kind tools.

Design Goals
------------

JKind is designed to be cross-platform, reliable, and easy to
extend. Power and performance are secondary goals. Additionally,
JKind attempts to be mostly compatible with pkind and [Kind
2](http://kind2-mc.github.io/kind2/), though this varies over
time due to developments in both systems.


Alternative Solvers (optional)
------------------------------

By default, JKind is packaged with [SMTInterpol](http://ultimate.informatik.uni-freiburg.de/smtinterpol/) 
as its underlying SMT solver. Advanced users may wish to install alternative solvers such as 
[Z3](https://github.com/Z3Prover/z3),
[Yices (version 1)](http://yices.csl.sri.com/download-yices1.shtml), 
[Yices 2](http://yices.csl.sri.com/index.shtml),
[CVC4](http://cvc4.cs.nyu.edu/web/), or
[MathSAT](http://mathsat.fbk.eu/).

Minimal IVCs enumeration (optional)
-----------------------------------
JKind supports enumeration of All Minimal Inductive Validity Cores (All-MIVCs) to provide a full enumeration of all minimal set of model elements necessary for the inductive proofs of a safety property. 
Both the offline enumeration (as described in [1]) and the online enumeration (as described in [2]) have been implemented, and the offline enumeration is the algorithm made available for general use. 
To use the MIVC enumeration, run JKind with the following arguments: 
```
-all_ivcs -solver z3
```
In addition, use the -timeout argument to limit the time for the enumeration, e.g., 
```
-timeout 1800
```

[1] E. Ghassabani, M. W. Whalen, and A. Gacek. Efficient generation of all minimal inductive validity cores. 2017 Formal Methods in Computer Aided Design (FMCAD), pages 31–38, 2017.
[2] J. Bendik, E. Ghassabani, M. Whalen, and I. Cerna. Online enumeration of all minimal inductive validity cores. In International Conference on Software Engineering and Formal Methods, pages 189–204. Springer, 2018.

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
