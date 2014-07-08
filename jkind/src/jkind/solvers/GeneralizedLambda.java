package jkind.solvers;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.VarDecl;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;

public class GeneralizedLambda {
	final private Symbol arg;
	final private List<VarDecl> outs;
	final private Sexp body;

	public GeneralizedLambda(Symbol arg, List<VarDecl> outputs, Sexp body) {
		this.arg = arg;
		this.outs = outputs;
		this.body = body;
	}

	public GeneralizedLambda(Symbol arg, Sexp body) {
		this.arg = arg;
		this.body = body;
		this.outs = null;
	}

	public Symbol getArg() {
		return arg;
	}

	public List<VarDecl> getouts() {
		return outs;
	}

	public Sexp getBody() {
		return body;
	}
	public Sexp instantiate(Sexp actual) {
		return substitute(body, arg, actual);
	}

	private static Sexp substitute(Sexp sexp, Sexp x, Sexp t) {
		if (sexp instanceof Cons) {
			Cons cons = (Cons) sexp;
			List<Sexp> args = new ArrayList<>();
			for (Sexp arg : cons.args) {
				args.add(substitute(arg, x, t));
			}
			return new Cons(substitute(cons.head, x, t), args);
		} else if (sexp instanceof Symbol) {
			Symbol symbol = (Symbol) sexp;
			if (sexp.equals(x)) {
				return t;
			} else {
				return symbol;
			}
		} else {
			throw new IllegalArgumentException();
		}
	}
}
