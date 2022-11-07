package jkind.realizability.engines;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.SolverOption;
import jkind.analysis.LinearChecker;
import jkind.lustre.Expr;
import jkind.lustre.LustreUtil;
import jkind.lustre.NamedType;
import jkind.lustre.VarDecl;
import jkind.realizability.JRealizabilitySolverOption;
import jkind.realizability.engines.messages.Message;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.aeval.AevalSolver;
import jkind.solvers.Model;
import jkind.solvers.z3.Z3Solver;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Specification;
import jkind.util.SexpUtil;
import jkind.util.StreamIndex;
import jkind.util.Util;

public abstract class RealizabilityEngine implements Runnable {
	protected final String name;
	protected final Specification spec;
	protected final JRealizabilitySettings settings;
	protected final RealizabilityDirector director;

	protected PrintWriter aevalscratch;

	//for diagnosis
	protected String result = "NONE";
	protected int cexLength = 0;
	protected Model model;


	protected Z3Solver solver;
	protected final BlockingQueue<Message> incoming = new LinkedBlockingQueue<>();


	// The director process will read this from another thread, so we
	// make it volatile
	protected volatile Throwable throwable;

	public RealizabilityEngine(String name, Specification spec, JRealizabilitySettings settings,
			RealizabilityDirector director) {
		this.name = name;
		this.spec = spec;
		this.settings = settings;
		this.director = director;

		this.aevalscratch = getaevalScratch();

	}

	protected abstract void main();

	@Override
	public void run() {
		try {
			initializeSolver();
			main();
		} catch (Throwable t) {
			throwable = t;
		} finally {
			if (solver != null) {
				solver.stop	();
				solver = null;
			}
			if (aevalscratch != null) {
				aevalscratch.close();
				aevalscratch = null;
			}
		}
	}

	protected void initializeSolver() {
//		solver = new Z3Solver(getScratchBase(), LinearChecker.isLinear(spec.node));
		solver = new Z3Solver(getScratchBase(), false);
		solver.initialize();
		solver.declare(spec.functions);
		solver.define(spec.getTransitionRelation());

        if(settings.fixpoint) {
            solver.define(new VarDecl(INIT.str, NamedType.BOOL));
            List<VarDecl> dummyoutvars = getOffsetVarDecls(0, getRealizabilityOutputVarDecls());
            List<VarDecl> preinvars = getOffsetVarDecls(-1, getRealizabilityInputVarDecls());
            List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls());

            for (VarDecl vd : dummyoutvars) {
                solver.define(vd);
            }

            for (VarDecl in : preinvars) {
                solver.define(in);
            }

            List<VarDecl> preoutvars = getOffsetVarDecls(-1, getRealizabilityOutputVarDecls());

            for (VarDecl vd : preoutvars) {
                solver.define(vd);
            }

            for (VarDecl in : currinvars) {
                solver.define(in);
            }

			if (settings.synthesis || settings.solver == JRealizabilitySolverOption.AEVAL) {
				List<VarDecl> skolemvars = getOffsetVarDecls(2, getRealizabilityOutputVarDecls());
				for (VarDecl in : skolemvars) {
					solver.define(in);
				}
			}
        }
	}

	protected String getScratchBase() {
		if (settings.scratch) {
			return settings.filename + "." + name;
		} else {
			return null;
		}
	}

	public Throwable getThrowable() {
		return throwable;
	}

	/** Utility */

	protected void comment(String str) {
		solver.comment(str);
	}

	public String getName() {
		return name;
	}

	protected void createVariables(int k) {
		for (VarDecl vd : getOffsetVarDecls(k)) {
			solver.define(vd);
		}

		for (VarDecl vd : Util.getVarDecls(spec.node)) {
			Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
			if (constraint != null) {
				solver.assertSexp(constraint.accept(new Lustre2Sexp(k)));
			}
		}
	}

	protected void createAevalVariables(AevalSolver aesolver, int k, String check) {
		if (settings.scratch) {
			aesolver.scratch.println("; Transition relation");
		}

		aesolver.defineSVar(spec.getTransitionRelation());
        //The following does not allow to solve realizable but non well-separated contracts.
		aesolver.defineTVar(spec.getTransitionRelation(), false);
		if (settings.scratch) {
			aesolver.scratch.println("; Universally quantified variables");
		}
		if (check == "extend") {
			aesolver.defineSVar(new VarDecl(INIT.str, NamedType.BOOL));
			aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL), false);
		}
		for (int i = -1; i <= k-1; i = i +1) {
			for (VarDecl vd : getOffsetVarDecls(i)) {
				aesolver.defineSVar(vd);
				if (i == k-1) {
					aesolver.defineTVar(vd, false);
				}
			}
		}

		for (VarDecl vd : getOffsetVarDecls(k)) {
			aesolver.defineSVar(vd);
		}

		List<VarDecl> offsetinvars = getOffsetVarDecls(
				k, getRealizabilityInputVarDecls());
		List<VarDecl> offsetoutvars = getOffsetVarDecls(
									k+2, getRealizabilityOutputVarDecls());
		for (VarDecl in : offsetinvars) {
			aesolver.defineTVar(in, false);
		}

		if (settings.scratch) {
			aesolver.scratch.println("; Existentially quantified variables");
		}
		for (VarDecl out : offsetoutvars) {
			aesolver.defineTVar(out, true);
		}

		for (VarDecl vd : Util.getVarDecls(spec.node)) {
			Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
			if (constraint != null) {
				aesolver.assertSPart(constraint.accept(new Lustre2Sexp(k)));
			}
		}

		if (settings.scratch) {
			aesolver.scratch.println("; Assertions for universal part of the formula");
		}
		if (k > 0) {
			if (check == "extend") {
				for (int i = 0; i < k; i++) {
					if (i == 0) {
						aesolver.assertSPart(getTransition(i, INIT));
					} else {
						aesolver.assertSPart(getTransition(i, false));
					}
					aesolver.assertSPart(StreamIndex.conjoinEncodings(spec.node.properties, i));
				}
			} else {
				for (int i = 0; i < k; i++) {
					aesolver.assertSPart(getTransition(i, i == 0));
					aesolver.assertSPart(StreamIndex.conjoinEncodings(spec.node.properties, i));
				}
			}
		}
	}

	protected Sexp getNonInitialStates(Sexp init){
		return new Cons("not", init);
	}

	protected Sexp getUniversalOutputVariablesAssertion(int k){
		List<Sexp> conjuncts = new ArrayList<>();
		List<Sexp> equatities = new ArrayList<>();
		conjuncts.addAll(getSymbols(getOffsetVarDecls(k, getRealizabilityOutputVarDecls())));
		for (Sexp c : conjuncts) {
			equatities.add(new Cons("=", c, c));
		}
		return SexpUtil.conjoin(equatities);
	}


	protected Sexp getUniversalInputVariablesAssertion(int k){
		List<Sexp> conjuncts = new ArrayList<>();
		List<Sexp> equalities = new ArrayList<>();
        conjuncts.addAll(getSymbols(getOffsetVarDecls(k, getRealizabilityInputVarDecls())));
        for (Sexp c : conjuncts) {
			equalities.add(new Cons("=", c, c));
		}
		return SexpUtil.conjoin(equalities);
	}


	protected Sexp getAssertions() {
		Sexp assertions = Lustre2Sexp.getConjunctedAssertions(spec.node);
		return assertions;
	}

	protected Sexp getNextStepAssertions() {
		Sexp assertions = Lustre2Sexp.getNextStepConjunctedAssertions(spec.node);
		return assertions;
	}

	protected List<VarDecl> getOffsetVarDecls(int k) {
		return getOffsetVarDecls(k, Util.getVarDecls(spec.node));
	}

	protected List<VarDecl> getOffsetVarDecls(int k, List<VarDecl> varDecls) {
		List<VarDecl> result = new ArrayList<>();
		for (VarDecl vd : varDecls) {
			StreamIndex si = new StreamIndex(vd.id, k);
			result.add(new VarDecl(si.getEncoded().str, vd.type));
		}
		return result;
	}

    protected List<VarDecl> getAevalOffsetVarDecls(int k, List<VarDecl> varDecls, List<VarDecl> inputDecls, List<VarDecl> outputDecls) {
        List<VarDecl> result = new ArrayList<>();
        List<String> inputNames = new ArrayList<>();
        List<String> outputNames = new ArrayList<>();
        for (VarDecl vd : inputDecls) {
            inputNames.add(vd.id);
        }
        for (VarDecl vd : outputDecls) {
            outputNames.add(vd.id);
        }
        for (VarDecl vd : varDecls) {
            StreamIndex si;
            if (inputNames.contains(vd.id)) {
                si = new StreamIndex(vd.id, k);
                result.add(new VarDecl(si.getEncoded().str, vd.type));
            } else {
                si = new StreamIndex(vd.id, k + 2);
                result.add(new VarDecl(si.getEncoded().str, vd.type));
            }
        }
        return result;
    }

	protected Sexp getTransition(int k, Sexp init) {
		List<Sexp> args = new ArrayList<>();
		args.add(init);
		args.addAll(getSymbols(getOffsetVarDecls(k - 1)));
		args.addAll(getSymbols(getOffsetVarDecls(k)));
		return new Cons(spec.getTransitionRelation().getName(), args);
	}

	protected Sexp getTransition(int k, boolean init) {
		return getTransition(k, Sexp.fromBoolean(init));
	}

	protected Sexp getAevalTransition(int k, Sexp init) {
		List<Sexp> args = new ArrayList<>();
		args.add(init);
		args.addAll(getSymbols(getOffsetVarDecls(k - 1)));

        if (settings.fixpoint) {
            args.addAll(getSymbols(getAevalOffsetVarDecls(0, Util.getVarDecls(spec.node), getRealizabilityInputVarDecls(), getRealizabilityOutputVarDecls())));
        } else {
            args.addAll(getSymbols(getAevalOffsetVarDecls(k, Util.getVarDecls(spec.node), getRealizabilityInputVarDecls(), getRealizabilityOutputVarDecls())));
        }
		return new Cons(spec.getTransitionRelation().getName(), args);
	}

	protected Sexp getAevalInductiveTransition(int k) {
		if (k == 0) {
			return getAevalTransition(0, INIT);
		} else {
			return getAevalTransition(k, false);
		}
	}

	protected Sexp getAevalFixpointTransition() {
		List<Sexp> args = new ArrayList<>();
		args.add(INIT);
		args.addAll(getSymbols(getOffsetVarDecls(-1)));
		args.addAll(getSymbols(getOffsetVarDecls(0, getRealizabilityInputVarDecls())));
		args.addAll(getSymbols(getOffsetVarDecls(2, getRealizabilityOutputVarDecls())));
		return new Cons(spec.getTransitionRelation().getName(), args);
	}

	protected Sexp getAevalTransition(int k, boolean init) {
		return getAevalTransition(k, Sexp.fromBoolean(init));
	}

	protected Sexp getRealizabilityOutputs(int k) {
		List<VarDecl> realizabilityOutputVarDecls = getRealizabilityOutputVarDecls();
		if (realizabilityOutputVarDecls.isEmpty()) {
			return null;
		} else {
			return varDeclsToQuantifierArguments(realizabilityOutputVarDecls, k);
		}
	}

	protected Sexp getInductiveTransition(int k) {
		if (k == 0) {
			return getTransition(0, INIT);
		} else {
			return getTransition(k, false);
		}
	}

	protected static final Symbol INIT = Lustre2Sexp.INIT;

	public List<VarDecl> getRealizabilityOutputVarDecls() {
		List<String> realizabilityInputs = spec.node.realizabilityInputs;
		List<VarDecl> all = Util.getVarDecls(spec.node);

		all.removeIf(vd -> realizabilityInputs.contains(vd.id));
		return all;
	}

	public List<VarDecl> getRealizabilityInputVarDecls() {
		List<String> realizabilityInputs = spec.node.realizabilityInputs;
		List<VarDecl> all = Util.getVarDecls(spec.node);
		all.removeIf(vd -> !realizabilityInputs.contains(vd.id));
		return all;
	}

	protected Sexp varDeclsToRefinementQuantifierArguments(List<VarDecl> varDecls, int k) {
		List<Sexp> args = new ArrayList<>();
		args.add(new Cons("%init", new Symbol("Bool")));
		for (VarDecl vd : varDecls) {
			Symbol name = new StreamIndex(vd.id, k).getEncoded();
			Symbol type = solver.type(vd.type);
			args.add(new Cons(name, type));
		}
		return new Cons(args);
	}

	protected Sexp varDeclsToQuantifierArguments(List<VarDecl> varDecls, int k) {
		List<Sexp> args = new ArrayList<>();
		for (VarDecl vd : varDecls) {
			Symbol name = new StreamIndex(vd.id, k).getEncoded();
			Symbol type = solver.type(vd.type);
			args.add(new Cons(name, type));
		}
		return new Cons(args);
	}

	protected List<Sexp> getSymbols(List<VarDecl> varDecls) {
		List<Sexp> result = new ArrayList<>();
		for (VarDecl vd : varDecls) {
			result.add(new Symbol(vd.id));
		}
		return result;
	}

	private PrintWriter getaevalScratch( ) {
		if (settings.scratch && (settings.solver == JRealizabilitySolverOption.AEVAL || settings.synthesis)) {

			String filename = settings.filename + ".aeval" + "." + name + ".smt2";
			try {
				return new PrintWriter(new FileOutputStream(filename), true);
			} catch (FileNotFoundException e) {
				throw new JKindException("Unable to open scratch file: " + filename, e);
			}
		} else {
			return null;
		}
	}

	public void aecomment(String str) {
		if (aevalscratch != null) {
			aevalscratch.println(str);
		}
	}

	protected abstract void setResult(int k, String result, Model model);

	public String getResult() {
		return result;
	}

	public int getCexLength() { return cexLength; }

	public Model getModel() {
		return model;
	}
}