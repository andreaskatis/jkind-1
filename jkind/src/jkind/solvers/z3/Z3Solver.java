package jkind.solvers.z3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jkind.JKindException;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.MaxSatSolver;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.solvers.UnsatResult;
import jkind.solvers.smtlib2.SmtLib2Solver;
import jkind.solvers.smtlib2.SolverOutOfMemoryException;


public class Z3Solver extends SmtLib2Solver implements MaxSatSolver {
	private final boolean linear;
	private int actCount = 1;

	public Z3Solver(String scratchBase, boolean linear) {
		super(scratchBase);
		this.linear = linear;
	}

	@Override
	protected String getSolverName() {
		return "Z3";
	}

	@Override
	protected String[] getSolverOptions() {
		return new String[] { "-smt2", "-in" };
	}

	@Override
	public void initialize() {
		setOption("produce-models", true);
		setOption("produce-unsat-cores", true);
		setOption("smt.core.minimize", true);
		setOption("sat.core.minimize", true);

		// The following option can be added
		// when the reported bugs in Z3 resurfaces:
		// https://github.com/Z3Prover/z3/issues/158
		// setOption("smt.core.validate", true);
	}

	public void setOption(String option, boolean value) {
		send("(set-option :" + option + " " + value + ")");
	}

	@Override
	public Result query(Sexp sexp) {
		Result result;

		if (linear) {
			Symbol literal = createActivationLiteral("act", actCount++);
			send(new Cons("assert", new Cons("=>", literal, new Cons("not", sexp))));
			send(new Cons("check-sat", literal));
		} else {
			push();
			send(new Cons("assert", new Cons("not", sexp)));
			send(new Cons("check-sat"));
//			send(new Cons("check-sat-using qfnra-nlsat"));
		}

		try {
			String status = readFromSolver();
			if (isSat(status)) {
				send("(get-model)");
				result = new SatResult(parseModel(readFromSolver()));
			} else if (isUnsat(status)) {
				result = new UnsatResult();
			} else {
				// Even for unknown we can sometimes get a partial model
				send("(get-model)");

				String content = readFromSolver();
				if (content == null) {
					return new UnknownResult();
				} else {
					result = new UnknownResult(parseModel(content));
				}
			}
		} catch (SolverOutOfMemoryException e) {
			return new UnknownResult();
		}

		if (!linear) {
			pop();
		}

		return result;
	}

	@Override
	public Result quickCheckSat(List<Symbol> activationLiterals) {
		send(new Cons("check-sat", activationLiterals));
		String status = readFromSolver();
		if (isSat(status)) {
			return new SatResult();
		} else if (isUnsat(status)) {
			return new UnsatResult(getUnsatCore(activationLiterals));
		} else {
			return new UnknownResult();
		}
	}

	public Result checkMaximal() {
		send("(set-option :sat.phase always_true)");
		send("(check-sat-using sat)");
		String status = readFromSolver();

		if (isSat(status)) {
			send("(get-model)");
			return new SatResult(parseModel(readFromSolver()));
		} else if (isUnsat(status)) {
			return new UnsatResult();
		} else {
			return new UnknownResult();
		}
	}

	public Result checkMinimal() {
		send("(set-option :sat.phase always_false)");
		send("(check-sat-using sat)");
		String status = readFromSolver();

		if (isSat(status)) {
			send("(get-model)");
			return new SatResult(parseModel(readFromSolver()));
		} else if (isUnsat(status)) {
			return new UnsatResult();
		} else {
			return new UnknownResult();
		}
	}

	public Result checkValuation(List<Symbol> positiveLits, List<Symbol> negativeLits, boolean getModel) {
		String arg = "(check-sat ";
		for (Symbol s : positiveLits) {
			arg += s.toString() + " ";
		}
		for (Symbol s : negativeLits) {
			arg += "(not " + s.toString() + ") ";
		}
		arg += ")";
		send(arg);
		String status = readFromSolver();

		if (isSat(status)) {
			if (getModel) {
				send("(get-model)");
				return new SatResult(parseModel(readFromSolver()));
			} else {
				return new SatResult();
			}
		} else if (isUnsat(status)) {
			return new UnsatResult();

		} else {
			return new UnknownResult();
		}

	}

	/**
	 * similar to quickCheckSat, but focused on
	 *     1- either the SAT model or unsat-core
	 *     2- or just the return Type of Result
	 */
	public Result checkSat(List<Symbol> activationLiterals, boolean getModel, boolean getCore) {
		send(new Cons("check-sat", activationLiterals));
		String status = readFromSolver();

		if (isSat(status)) {
			if (getModel) {
				send("(get-model)");
				return new SatResult(parseModel(readFromSolver()));
			} else {
				return new SatResult();
			}
		} else if (isUnsat(status)) {
			if (getCore) {
				return new UnsatResult(getUnsatCore(activationLiterals));
			} else {
				return new UnsatResult();
			}
		} else {
			return new UnknownResult();
		}
	}

	@Override
	protected List<Symbol> getUnsatCore(List<Symbol> activationLiterals) {
		List<Symbol> unsatCore = new ArrayList<>();
		send("(get-unsat-core)");
		for (String s : readCore().split(" ")) {
			if (!s.isEmpty()) {
				unsatCore.add(new Symbol(s));
			}
		}
		return unsatCore;
	}

	private String readCore() {
		String line = "";
		try {
			line = fromSolver.readLine();
			comment(getSolverName() + ": " + line);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line.substring(1, line.length() - 1);
	}

	public Sexp qeQuery(Sexp formula, boolean isRef) {
		push();
		//send(";\n;" + (isRef ? "---- Refinement step ----" : "---- Main step ----") + "\n;");
		assertSexp(formula);
		send(new Cons("apply", new Cons("par-or",
				new Symbol("qe2"), new Cons("then", new Symbol("qe-light"), new Symbol("qe2")), new Cons("then", new Symbol("qe-light"), new Symbol("qe")))));		
//		send(new Cons("apply", new Cons("par-or",
//				new Symbol("qe2"), new Cons("then", new Symbol("qe-light"), new Symbol("qe")))));
//				new Symbol("qe2"), new Cons("then", new Symbol("qe-light"), new Cons("!", new Symbol("qe"), new Symbol(":qe-nonlinear true"))))));
//		send(new Cons("apply", new Symbol("qe2")));
//		send(new Cons("apply", new Cons("!", new Symbol("qe"), new Symbol(":qe-nonlinear true"))));
		// send(new Cons("apply", new Cons("then", new Symbol("qe-light"), new Symbol("qe"))));
//		send(new Cons("apply", new Cons("then", new Symbol("qe-light"), new Symbol("qe2"))));
		String result = readFromSolver();
		pop();
		String regexString = Pattern.quote("(goal\n  ") + "(?s)(.*?)" + "(\\n\\s\\s)?" + Pattern.quote(":");
		Pattern pattern = Pattern.compile(regexString);
		Matcher matcher = pattern.matcher(result);
		if (matcher.find()) {
			String qeResult = matcher.group(1);
			return qeResult.equals("") ? new Symbol("true") :
					(qeResult.equals("false") ? new Symbol("false") :
							new Cons("and", new Symbol(qeResult)));
		} else {
			throw new JKindException("Error extracting formula from Z3 quantifier elimination.\n");
		}
	}

//	public String qeQuery(Sexp outputs, Sexp transition, Sexp properties, Sexp fixpoint, Sexp fixpointNext, boolean isRef) {
//		push();
//		send(";\n;" + (isRef ? "---- Refinement step ----" : "---- Main step ----") + "\n;\n");
////		String simplified = this.simplify("(assert (and " + transition.toString() + "\n" + properties.toString() + "\n" +
////				fixpoint.toString() + "\n" + fixpointNext.toString() + "))", null, null);
////		String simplified = this.simplify("(assert (=> (and " + fixpoint.toString() + ")\n" + "(and " + transition.toString() + "\n" + properties.toString() + "\n" +
////				fixpointNext.toString() + ")))", null, null);
////		Sexp query = new Cons("exists", outputs, new Cons("and", new Symbol(simplified)));
//		Sexp exPart = new Cons("exists", outputs, new Cons("and", transition, properties, fixpointNext));
//		Sexp query = new Cons ("=>", fixpoint, exPart);
//		assertSexp(query);
//
//		send(new Cons("apply", new Cons("par-or",
////				new Cons("then", new Symbol("ctx-solver-simplify"), new Symbol("qe"), new Symbol("ctx-solver-simplify")),
////				new Cons("then", new Symbol("ctx-solver-simplify"), new Symbol("qe2"), new Symbol("ctx-solver-simplify")),
//////				new Cons("then", new Symbol("ctx-solver-simplify"), new Symbol("qe-light"), new Symbol("ctx-solver-simplify")),
////				new Cons("then", new Symbol("ctx-solver-simplify"), new Symbol("qe_rec"), new Symbol("ctx-solver-simplify")),
////
////				new Cons("then", new Symbol("qe"), new Symbol("ctx-solver-simplify")),
////				new Cons("then", new Symbol("qe2"), new Symbol("ctx-solver-simplify")),
//////				new Cons("then", new Symbol("qe-light"), new Symbol("ctx-solver-simplify")),
////				new Cons("then", new Symbol("qe_rec"), new Symbol("ctx-solver-simplify")))));
//
//				new Symbol("qe2"), new Cons("then", new Symbol("qe-light"), new Symbol("qe")))));
//
////		send(new Cons("apply", new Cons("and-then", new Symbol("qe-light"), new Symbol("qe"))));
//
////		send(new Cons("apply", new Cons("par-or",
////				new Symbol("qe2"), new Symbol("qe-light"), new Symbol("qe_rec"))));
////		send(new Cons("apply", new Cons("then", new Cons("par-or",
////				 new Symbol("qe2"), new Symbol("qe-light"), new Symbol("qe_rec")), new Symbol("ctx-solver-simplify"))));
////		send(new Cons("apply", new Cons("then", new Symbol("ctx-solver-simplify"), new Symbol("qe2"))));
////		if (isRef) {
////		send(new Cons("apply", new Cons("par-or",
////				new Cons("then", new Symbol("qe"), new Symbol("ctx-solver-simplify")),
////				new Cons("then", new Symbol("qe2"), new Symbol("ctx-solver-simplify")),
////				new Cons("then", new Symbol("qe-light"), new Symbol("ctx-solver-simplify")),
////				new Cons("then", new Symbol("qe_rec"), new Symbol("ctx-solver-simplify")))));
////		} else {
////			send(new Cons("apply", new Cons("then", new Symbol("qe2"), new Symbol("ctx-solver-simplify"))));
////		}
//
////		send(new Cons("apply", new Cons("or-else",
////				new Cons("then", new Cons("try-for", new Symbol("ctx-solver-simplify"), new Symbol("5000")),
////					new Symbol("qe2")),
////				new Symbol("qe2"))));
////		send(new Cons("apply", new Symbol("qe2")));
////		send(new Cons("apply", new Symbol("qe_rec")));
//		String result = readFromSolver();
//		pop();
//		String regexString = Pattern.quote("(goal\n  ") + "(?s)(.*?)" + Pattern.quote(":");
//		Pattern pattern = Pattern.compile(regexString);
//		Matcher matcher = pattern.matcher(result);
//		if (matcher.find()) {
//			String qeResult = matcher.group(1);
//			return qeResult.equals("") ? "true" : qeResult;
//		} else {
//			throw new JKindException("Error extracting formula from Z3 quantifier elimination.\n");
//		}
//	}

	public Result realizabilityQuery(Sexp outputs, Sexp transition, Sexp properties, int timeoutMs) {
		push();
		if (timeoutMs > 0) {
			send(new Cons("set-option", new Symbol(":timeout"), Sexp.fromInt(timeoutMs)));
		}
		Sexp query = new Cons("not", new Cons("and", transition, properties));
		if (outputs != null) {
			query = new Cons("forall", outputs, query);
		}
		assertSexp(query);
		send(new Cons("check-sat-using", new Cons("or-else", new Symbol("default"), new Symbol("smt"))));
		String status = readFromSolver();
		if (isSat(status)) {
			send("(get-model)");
			pop();
			return new SatResult(parseModel(readFromSolver()));
		} else if (isUnsat(status)) {
			pop();
			return new UnsatResult();
		} else {
			pop();
			return new UnknownResult();
		}
	}

	public Result realizabilityQuery(Sexp outputs, Sexp transition, Sexp properties) {
		return realizabilityQuery(outputs, transition, properties, 0);
	}

	public Sexp simplify(Sexp formula) {
		push();
		assertSexp(formula);
		send(new Cons("apply", new Symbol("ctx-solver-simplify")));
		String result = readFromSolver();
		pop();
		String regexString = Pattern.quote("(goal\n  ") + "(?s)(.*?)" + "(\\n\\s\\s)?" + Pattern.quote(":");
		Pattern pattern = Pattern.compile(regexString);
		Matcher matcher = pattern.matcher(result);

		if (matcher.find()) {
			String simplified = matcher.group(1);

			return simplified.equals("") ? new Symbol("true") :
					(simplified.equals("false") ? new Symbol("false") :
							//adding "true" below because AE-VAL's version of Z3 doesn't do well with unary expressions over "and"
							new Cons("and", new Symbol(simplified), new Symbol("true")));
		} else {
			throw new JKindException("Error extracting simplified formula from Z3.\n");
		}
	}

//    public String simplify(String region, String left, String right) {
//        push();
//        if (region != null) {
//            send(region);
//        }
//        if (left != null) {
//            send(left);
//        }
//        if (right != null) {
//            send(right);
//        }
//        send("(apply ctx-solver-simplify)");
//        String result = readFromSolver();
//        pop();
//        String regexString = Pattern.quote("(goal\n  ") + "(?s)(.*?)" + Pattern.quote(":");
//        Pattern pattern = Pattern.compile(regexString);
//        Matcher matcher = pattern.matcher(result);
//        if (matcher.find()) {
//            String simplified = matcher.group(1);
//            return simplified;
//        } else {
//            throw new JKindException("Error extracting simplified formula from Z3.\n");
//        }
//    }

	public boolean initialStatesQuery(Sexp transition, Sexp properties, Sexp preRegion, Sexp postRegion) {
		push();
		boolean sat = false;
		Sexp transitionAssert = new Cons("and", transition, properties);
		assertSexp(transitionAssert);
		assertSexp(preRegion);
		assertSexp(postRegion);
		send("(check-sat)");
		String status = readFromSolver();
		if (isSat(status)) {
			sat = true;
		}
		pop();
		return sat;
	}

//    public boolean initialStatesQuery(Sexp transition, Sexp properties, String preRegion, String postRegion) {
//		boolean sat = false;
//		Sexp transitionAssert = new Cons("and", transition, properties);
//		assertSexp(transitionAssert);
//		send(preRegion);
//		send(postRegion);
//		send("(check-sat)");
//		String status = readFromSolver();
//		if (isSat(status)) {
//			sat = true;
//		}
//		pop();
//		return sat;
//	}

	@Override
	public void assertSoft(Sexp sexp) {
		send(new Cons("assert-soft", sexp));
	}

	@Override
	public Result maxsatQuery(Sexp query) {
		return query(query);
	}

}
