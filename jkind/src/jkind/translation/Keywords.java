package jkind.translation;

import jkind.sexp.Symbol;

public class Keywords {
	// 'n' used in induction
	final public static Symbol N = new Symbol("n");
	
	// Transition relation
	final public static Symbol T = new Symbol("T");

	
	// Transition relation for realizability checking.
	final public static Symbol T_prime = new Symbol("T_prime");

	
	// Set of properties defined at T_prime transition relation.
	//Important to check that they hold at the next step, during realizability checking.
	final public static Symbol P_prime = new Symbol("P_prime");
}
