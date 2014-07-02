package jkind.solvers.cvc4;

import java.util.ArrayList;

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
	public Result realizability_query(ArrayList<VarDecl> outs, Sexp k) {
		// TODO Auto-generated method stub
		return null;
	}
}
