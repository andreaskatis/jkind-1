package jkind.translation;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.EnumType;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.VarDecl;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.GeneralizedLambda;
import jkind.solvers.GeneralizedStreamDef;
import jkind.solvers.Lambda;
import jkind.solvers.StreamDecl;
import jkind.solvers.StreamDef;
import jkind.util.SexpUtil;
import jkind.util.Util;

public class Lustre2Sexps {
	private StreamDef transition;
	private GeneralizedStreamDef transition_prime;
	private GeneralizedStreamDef p_prime;
	private ArrayList<VarDecl> outputs = new ArrayList<>();
	private final List<StreamDecl> declarations = new ArrayList<>();

	public Lustre2Sexps(Node node) {
		createDefinitions(node);
		createOutputSet(node);
		createTransition(node);
		createTransition_prime(node);
		createP_prime(node);
	}

	private void createDefinitions(Node node) {
		for (VarDecl decl : Util.getVarDecls(node)) {
			declarations.add(new StreamDecl("$" + decl.id, decl.type));
		}
	}
	
	private void createOutputSet(Node node) {
		for (String real : node.realizabilities){
			for (VarDecl out : node.outputs){
					if (!(real.contains("["+out.id+"]")) && !(real.contains("["+out.id+",")) && !(real.contains(" "+out.id+",")) && !(real.contains(" "+out.id+"]")) && !(outputs.contains(new VarDecl("$$"+out.id,out.type)))){
						outputs.add(new VarDecl("$$"+out.id,out.type));
					}
			}
			for (VarDecl local : node.locals){
					if (!(real.contains("["+local.id+"]")) && !(real.contains("["+local.id+",")) && !(real.contains(" "+local.id+",")) && !(real.contains(" "+local.id+"]")) && !(outputs.contains(new VarDecl("$$"+local.id,local.type)))){
						outputs.add(new VarDecl("$$"+local.id,local.type));
					}
			}
			for (VarDecl in : node.inputs){
					if (!(real.contains("["+in.id+"]")) && !(real.contains("["+in.id+",")) && !(real.contains(" "+in.id+",")) && !(real.contains(" "+in.id+"]")) && !(outputs.contains(new VarDecl("$$"+in.id,in.type)))){
						outputs.add(new VarDecl("$$"+in.id,in.type));
					}
			}
		}
	}

	private void createTransition(Node node) {
		Expr2SexpVisitor visitor = new Expr2SexpVisitor(SexpUtil.I);
		List<Sexp> conjuncts = new ArrayList<>();

		for (Equation eq : node.equations) {
			conjuncts.add(equation2Sexp(eq, SexpUtil.I, visitor));
		}

		for (VarDecl input : node.inputs) {
			if (input.type instanceof SubrangeIntType) {
				conjuncts.add(SexpUtil.subrangeConstraint(input.id, SexpUtil.I,
						(SubrangeIntType) input.type));
			} else if (input.type instanceof EnumType) {
				conjuncts.add(SexpUtil.enumConstraint(input.id, SexpUtil.I, (EnumType) input.type));
			}
		}

		for (Expr assertion : node.assertions) {
			conjuncts.add(assertion.accept(visitor));
		}

		Lambda lambda = new Lambda(SexpUtil.I, new Cons("and", conjuncts));
		transition = new StreamDef(Keywords.T, NamedType.BOOL, lambda);
	}
	
	private void createTransition_prime(Node node) {
		List<Sexp> conjuncts = new ArrayList<>();
		Expr2SexpVisitor visitor = new Expr2SexpVisitor(SexpUtil.I, outputs);
		
		for (Equation eq : node.equations) {
			//if (node.properties.contains(eq.lhs.get(0).id)){
				conjuncts.add(equation2SexpReal(eq, SexpUtil.I, visitor));
			//}
			//else {
				//conjuncts.add(equation2Sexp(eq, SexpUtil.I, visitor));
			//}
		}
		
		for (VarDecl input : node.inputs) {
			if (input.type instanceof SubrangeIntType) {
				conjuncts.add(SexpUtil.subrangeConstraint(input.id, SexpUtil.I,
						(SubrangeIntType) input.type));
			}
		}
		
		for (Expr assertion : node.assertions) {
			conjuncts.add(assertion.accept(visitor));
		}
		
		
		
		GeneralizedLambda lambda = new GeneralizedLambda(SexpUtil.I, outputs, new Cons("and", conjuncts));
		transition_prime = new GeneralizedStreamDef(Keywords.T_prime, NamedType.BOOL, lambda);
	}
	
	private void createP_prime(Node node) {
		List<Sexp> conjuncts = new ArrayList<>();
		/*Expr2SexpVisitor visitor = new Expr2SexpVisitor(SexpUtil.I, outputs);
		for (Equation eq : node.equations) {
			if (node.properties.contains(eq.lhs.get(0).id)){
				conjuncts.add(equation2SexpReal(eq, SexpUtil.I, visitor));
			}
		}*/
		for (String prom : node.properties){
			conjuncts.add(new Symbol("$$"+prom));
		}
		
		GeneralizedLambda lambda = new GeneralizedLambda(SexpUtil.I, outputs, new Cons("and", conjuncts));
		p_prime = new GeneralizedStreamDef(Keywords.P_prime, NamedType.BOOL, lambda);
	}

	private Sexp equation2Sexp(Equation eq, Symbol iSym, Expr2SexpVisitor visitor) {
		Sexp body = eq.expr.accept(visitor);
		return new Cons("=", new Cons("$" + eq.lhs.get(0).id, iSym), body);
	}
	
	private Sexp equation2SexpReal(Equation eq, Symbol iSym, Expr2SexpVisitor visitor) {
		Sexp body = eq.expr.accept(visitor);
		return new Cons("=", new Symbol("$$" + eq.lhs.get(0).id), body);
	}

	public StreamDef getTransition() {
		return transition;
	}
	
	public GeneralizedStreamDef getTransition_prime(){
		return transition_prime;
	}

	public List<StreamDecl> getDeclarations() {
		return declarations;
	}
	
	public List<VarDecl> getOutputSet(){
		return outputs;
	}
	
	public GeneralizedStreamDef getP_prime(){
		return p_prime;
	}
}
