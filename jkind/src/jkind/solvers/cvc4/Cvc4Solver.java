package jkind.solvers.cvc4;

import java.util.List;

import jkind.lustre.VarDecl;
import jkind.sexp.Sexp;
import jkind.solvers.Result;
import jkind.solvers.smtlib2.SmtLib2Solver;

public class Cvc4Solver extends SmtLib2Solver {
	public Cvc4Solver() {
		super(new ProcessBuilder("cvc4", "--lang", "smt"), "CVC4");
	}

	@Override
	public void initialize() {
		send("(set-option :produce-models true)");
		send("(set-option :incremental true)");
		send("(set-option :rewrite-divk true)");
		send("(set-logic AUFLIRA)");
	}
	
	@Override
	public Result realizability_query(List<VarDecl> outs, Sexp k) {
		
		throw new IllegalArgumentException("CVC4 not supported for realizability checks");
		
	}
}
