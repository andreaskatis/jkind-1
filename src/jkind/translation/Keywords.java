package jkind.translation;

import jkind.sexp.Symbol;

public class Keywords {
	// 'n' used in induction
	final public static Symbol N = new Symbol("n");
	
	// Base transition relation
	final public static Symbol TB = new Symbol("TB");

	// Inductive transition relation (contains additional constraints)
	final public static Symbol TI = new Symbol("TI");
}
