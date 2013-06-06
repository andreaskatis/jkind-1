package jkind.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.invariant.Invariant;
import jkind.lustre.Node;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;

public class Util {
	public static List<VarDecl> getVarDecls(Node node) {
		List<VarDecl> decls = new ArrayList<VarDecl>();
		decls.addAll(node.inputs);
		decls.addAll(node.outputs);
		decls.addAll(node.locals);
		return decls;
	}

	public static Map<String, Type> getTypeMap(Node node) {
		Map<String, Type> map = new HashMap<String, Type>();
		for (VarDecl v : getVarDecls(node)) {
			map.put(v.id, v.type);
		}
		return map;
	}

	public static List<String> getIds(List<VarDecl> decls) {
		List<String> ids = new ArrayList<String>();
		for (VarDecl decl : decls) {
			ids.add(decl.id);
		}
		return ids;
	}

	public static Map<String, Node> getNodeTable(List<Node> nodes) {
		Map<String, Node> nodeTable = new HashMap<String, Node>();
		for (Node node : nodes) {
			nodeTable.put(node.id, node);
		}
		return nodeTable;
	}

	public static Sexp subrangeConstraint(String id, Sexp index, SubrangeIntType subrange) {
		/**
		 * It seems to be more efficient for the SMT solvers if we explode a
		 * subrange into equalities rather than treating it as two inequalities
		 */

		Sexp var = new Cons("$" + id, index);
		List<Sexp> choices = new ArrayList<Sexp>();
		for (BigInteger i = subrange.low; i.compareTo(subrange.high) <= 0; i = i
				.add(BigInteger.ONE)) {
			choices.add(new Cons("=", Sexp.fromBigInt(i), var));
		}
		return new Cons("or", choices);
	}

	public static Sexp conjoin(Collection<? extends Sexp> fns, Sexp i) {
		if (fns.isEmpty()) {
			return new Symbol("true");
		}

		List<Sexp> args = new ArrayList<Sexp>();
		for (Sexp fn : fns) {
			args.add(new Cons(fn, i));
		}
		return new Cons("and", args);
	}

	public static Sexp conjoinStreams(Collection<String> ids, Sexp i) {
		List<Sexp> symbols = new ArrayList<Sexp>();
		for (String id : ids) {
			symbols.add(new Symbol("$" + id));
		}
		return conjoin(symbols, i);
	}

	public static Sexp conjoinInvariants(Collection<Invariant> invariants, Sexp i) {
		if (invariants.isEmpty()) {
			return new Symbol("true");
		}

		List<Sexp> sexps = new ArrayList<Sexp>();
		for (Invariant invariant : invariants) {
			sexps.add(invariant.instantiate(i));
		}
		return new Cons("and", sexps);
	}

	final public static Symbol I = new Symbol("i");
}
