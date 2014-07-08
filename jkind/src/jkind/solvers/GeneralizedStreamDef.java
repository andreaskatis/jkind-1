package jkind.solvers;

import java.util.List;

import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;

public class GeneralizedStreamDef {
	final private StreamDecl decl;
	final private GeneralizedLambda lambda;

	public GeneralizedStreamDef(Symbol id, Type type, GeneralizedLambda lambda) {
		this.decl = new StreamDecl(id, type);
		this.lambda = lambda;
	}

	public GeneralizedStreamDef(String string, Type type, GeneralizedLambda lambda) {
		this(new Symbol(string), type, lambda);
	}

	public Symbol getId() {
		return decl.getId();
	}

	public GeneralizedLambda getLambda() {
		return lambda;
	}

	public Sexp instantiate(Sexp arg) {
		return lambda.instantiate(arg);
	}
	

	public Sexp getArg() {
		return lambda.getArg();
	}

	public List<VarDecl> getouts(){
		return lambda.getouts();
	}
	
	public Type getType() {
		return decl.getType();
	}

	public StreamDecl getDecl() {
		return decl;
	}

	public Sexp getBody() {
		return lambda.getBody();
	}
}
