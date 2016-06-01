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
import jkind.analysis.LinearChecker;
import jkind.lustre.Expr;
import jkind.lustre.LustreUtil;
import jkind.lustre.NamedType;
import jkind.lustre.VarDecl;
import jkind.realizability.engines.messages.Message;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.aeval.AevalSolver;
import jkind.solvers.z3.Z3Solver;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Specification;
import jkind.util.StreamIndex;
import jkind.util.Util;

public abstract class RealizabilityEngine implements Runnable {
	protected final String name;
	protected final Specification spec;
	protected final JRealizabilitySettings settings;
	protected final RealizabilityDirector director;

	protected PrintWriter aevalscratch;



	protected Z3Solver solver;
	protected final BlockingQueue<Message> incoming = new LinkedBlockingQueue<>();


	// The director process will read this from another thread, so we
	// make it volatile
	protected volatile Throwable throwable;

	public RealizabilityEngine(String name, Specification spec,
			JRealizabilitySettings settings, RealizabilityDirector director) {
		this.name = name;
		this.spec = spec;
		this.settings = settings;
		this.director = director;

		this.aevalscratch = getaevalScratch();

	}

	protected abstract void main();

	@Override
	public final void run() {
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
		solver = new Z3Solver(getScratchBase(), LinearChecker.isLinear(spec.node));
		solver.initialize();
		solver.define(spec.getTransitionRelation());
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
		aesolver.defineSVar(spec.getTransitionRelation());
		aesolver.defineTVar(spec.getTransitionRelation());

		if (check == "extend") {
			aesolver.defineSVar(new VarDecl(INIT.str, NamedType.BOOL));
			aesolver.defineGuardVar(new VarDecl(INIT.str, NamedType.BOOL));
			aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL));
		}
		for (int i = -1; i <= k-1; i = i +1) {
			for (VarDecl vd : getOffsetVarDecls(i)) {
				aesolver.defineSVar(vd);
				aesolver.defineGuardVar(vd);
				if (i == k-1) {
					aesolver.defineTVar(vd);
				}
			}
		}

		for (VarDecl vd : getOffsetVarDecls(k)) {
			aesolver.defineSVar(vd);
			aesolver.defineGuardVar(vd);
		}

		List<VarDecl> offsetinvars = getOffsetVarDecls(
				k, getRealizabilityInputVarDecls());
		List<VarDecl> offsetoutvars = getOffsetVarDecls(
									k+2, getRealizabilityOutputVarDecls());
		for (VarDecl in : offsetinvars) {
			aesolver.defineTVar(in);
		}

		for (VarDecl out : offsetoutvars) {
			aesolver.defineSkolVar(out);
			aesolver.defineTVar(out);
		}

		for (VarDecl vd : Util.getVarDecls(spec.node)) {
			Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
			if (constraint != null) {
				aesolver.assertSPart(constraint.accept(new Lustre2Sexp(k)));
			}
		}

		if (k > 0) {
			aesolver.assertSPart(getTransition(k-1,k-1==0));
			aesolver.assertSPart(StreamIndex.conjoinEncodings(spec.node.properties, k-1));
		}
	}

	protected void assertGuardandSkolVars(AevalSolver aesolver, int k, String check) {

		Symbol zero = new Symbol("0");
		List<Sexp> guardargs = new ArrayList<>();
		List<Sexp> skolargs = new ArrayList<>();
		if (check == "extend") {
			guardargs.add(new Cons("=", INIT, INIT));
		}
		List<VarDecl> realouts = getOffsetVarDecls(k+2,
				getRealizabilityOutputVarDecls());
		for (int i = -1; i <= k; i=i+1) {
			for (VarDecl vd : getOffsetVarDecls(i)) {
				Symbol name = new Symbol(vd.id);
				guardargs.add(new Cons("=", name, name));
			}
		}
		for (VarDecl out : realouts) {
			Symbol name = new Symbol(out.id);
			skolargs.add(new Cons("=", name, name));
		}
		guardargs.add(new Cons("=", zero, zero));
		skolargs.add(new Cons("=", zero, zero));
		aesolver.assertGuards(new Cons("&&", guardargs));
		aesolver.assertSkolvars(new Cons("&&", skolargs));
		return;
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
		args.addAll(getSymbols(getOffsetVarDecls(k,
				getRealizabilityInputVarDecls())));
		args.addAll(getSymbols(getOffsetVarDecls(k+2,
				getRealizabilityOutputVarDecls())));
		return new Cons(spec.getTransitionRelation().getName(), args);
	}

	protected Sexp getAevalInductiveTransition(int k) {
		if (k == 0) {
			return getAevalTransition(0, INIT);
		} else {
			return getAevalTransition(k, false);
		}
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

	private List<VarDecl> getRealizabilityOutputVarDecls() {
		List<String> realizabilityInputs = spec.node.realizabilityInputs;
		List<VarDecl> all = Util.getVarDecls(spec.node);
		all.removeIf(vd -> realizabilityInputs.contains(vd.id));
		return all;
	}

	private List<VarDecl> getRealizabilityInputVarDecls() {
		List<String> realizabilityInputs = spec.node.realizabilityInputs;
		List<VarDecl> all = Util.getVarDecls(spec.node);
		all.removeIf(vd -> !realizabilityInputs.contains(vd.id));
		return all;
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
		if (settings.scratch && settings.synthesis) {

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
}