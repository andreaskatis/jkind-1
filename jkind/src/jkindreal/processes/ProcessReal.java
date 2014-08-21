package jkindreal.processes;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.lustre.EnumType;
import jkind.lustre.NamedType;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.VarDecl;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.Solver;
import jkind.solvers.z3.Z3Solver;
import jkind.translation.Specification;
import jkind.translation.TransitionRelation;
import jkind.util.SexpUtil;
import jkind.util.Util;
import jkindreal.processes.messages.MessageReal;

public abstract class ProcessReal implements Runnable {
	protected Specification spec;
	protected JRealizabilitySettings settings;
	protected DirectorReal director;
	protected List<String> properties;
	protected List<String> realizabilities;


	protected Solver solver;
	protected BlockingQueue<MessageReal> incoming = new LinkedBlockingQueue<>();

	private String name;
	private PrintWriter scratch;

	// The director process will read this from another thread, so we
	// make it volatile
	private volatile Throwable throwable;

	public ProcessReal(String name, Specification spec, JRealizabilitySettings settings, DirectorReal director) {
		this.name = name;
		this.spec = spec;
		this.settings = settings;
		this.director = director;
		this.properties = new ArrayList<>(spec.node.properties);
		this.realizabilities = new ArrayList<>(spec.node.realizabilities);
		this.scratch = getScratch(spec.filename, name);
	}

	private PrintWriter getScratch(String base, String proc) {
		String filename = base + "." + proc.toLowerCase() + "." + getSolverExtension();
		if (settings.scratch) {
			try {
				return new PrintWriter(new FileOutputStream(filename), true);
			} catch (FileNotFoundException e) {
				throw new JKindException("Unable to open scratch file: " + filename, e);
			}
		} else {
			return null;
		}
	}

	private String getSolverExtension() {
			return "smt2";
	}


	protected abstract void main();

	@Override
	final public void run() {
		try {
			initializeSolver();
			main();
		} catch (Throwable t) {
			throwable = t;
		} finally {
			if (solver != null) {
				solver.stop();
				solver = null;
			}
			if (scratch != null) {
				scratch.close();
			}
		}
	}

	protected void initializeSolver() {
		
		solver = new Z3Solver();

		if (settings.scratch) {
			solver.setDebug(scratch);
		}

		solver.initialize();
		solver.define(spec.transitionRelation);
		solver.define(new VarDecl(INIT.str, NamedType.BOOL));
	}

	public Throwable getThrowable() {
		return throwable;
	}

	/** Debug methods */

	protected void debug(String str) {
		if (scratch != null) {
			scratch.print("; ");
			scratch.println(str);
		}
	}

	public String getName() {
		return name;
	}

	/** Utility */

	protected void createVariables(int k) {
		for (VarDecl vd : getOffsetVarDecls(k)) {
			solver.define(vd);
		}

		// Constrain input by type
		if (k >= 0) {
			for (VarDecl vd : getOffsetInputVarDecls(k)) {
				if (vd.type instanceof SubrangeIntType) {
					SubrangeIntType subrangeType = (SubrangeIntType) vd.type;
					solver.send(new Cons("assert", SexpUtil.subrangeConstraint(vd.id, subrangeType)));
				} else if (vd.type instanceof EnumType) {
					EnumType enumType = (EnumType) vd.type;
					solver.send(new Cons("assert", SexpUtil.enumConstraint(vd.id, enumType)));
				}
			}
		}
	}

	protected List<VarDecl> getOffsetVarDecls(int k) {
		return getOffsetVarDecls(k, Util.getVarDecls(spec.node));
	}

	protected List<VarDecl> getOffsetInputVarDecls(int k) {
		return getOffsetVarDecls(k, spec.node.inputs);
	}

	protected List<VarDecl> getOffsetVarDecls(int k, List<VarDecl> varDecls) {
		List<VarDecl> result = new ArrayList<>();
		for (VarDecl vd : varDecls) {
			result.add(SexpUtil.offset(vd, k));
		}
		return result;
	}

	protected static final Symbol INIT = new Symbol("%init");

	protected void defineInductiveInit() {
		solver.define(new VarDecl(INIT.str, NamedType.BOOL));
	}

	protected Sexp getTransition(int k, Sexp init) {
		List<Sexp> args = new ArrayList<>();
		args.add(init);
		args.addAll(getSymbols(getOffsetVarDecls(k - 1)));
		args.addAll(getSymbols(getOffsetVarDecls(k)));
		return new Cons(TransitionRelation.T, args);
	}
	
	protected Sexp getInputs(int k) {
		List<Sexp> args = new ArrayList<>();
		for (String real : realizabilities) {
			List<String> inputs = Arrays.asList(real.substring(1, real.length()-1).split("\\s*,\\s*"));
			for (String in : inputs){
				for (VarDecl element : spec.node.inputs) {
					if (element.id.startsWith(in)) {
						args.add(new Cons(SexpUtil.offset(element, k).id, new Symbol(SexpUtil.offset(element, k).type.toString().substring(0, 1).toUpperCase()+SexpUtil.offset(element, k).type.toString().substring(1))));
					}
				}
			}
		}
		return new Cons(args);
	}
	
	protected Sexp getOutputs(int k) {
		List<Sexp> args = new ArrayList<>();
		for (String real : realizabilities) {
			List<String> inputs = Arrays.asList(real.substring(1, real.length()-1).split("\\s*,\\s*"));
			for (VarDecl element : spec.node.inputs) {
				for (int i = 0; i < inputs.size(); i++) {
					if ((!element.id.startsWith(inputs.get(i))) && (i==inputs.size()-1)) {
						args.add(new Cons(SexpUtil.offset(element, k).id, new Symbol(SexpUtil.offset(element, k).type.toString().substring(0, 1).toUpperCase()+SexpUtil.offset(element, k).type.toString().substring(1))));
					} else if ((!element.id.startsWith(inputs.get(i))) && (i < inputs.size()-1)) {
						continue;
					} else {
						break;
					}
				}
			}
			for (VarDecl element : spec.node.outputs) {
				for (int i = 0; i < inputs.size(); i++) {
					if ((!element.id.startsWith(inputs.get(i))) && (i==inputs.size()-1)) {
						args.add(new Cons(SexpUtil.offset(element, k).id, new Symbol(SexpUtil.offset(element, k).type.toString().substring(0, 1).toUpperCase()+SexpUtil.offset(element, k).type.toString().substring(1))));
					} else if ((!element.id.startsWith(inputs.get(i))) && (i < inputs.size()-1)) {
						continue;
					} else {
						break;
					}
				}
			}
			for (VarDecl element : spec.node.locals) {
				for (int i = 0; i < inputs.size(); i++) {
					if ((!element.id.startsWith(inputs.get(i))) && (i==inputs.size()-1)) {
						args.add(new Cons(SexpUtil.offset(element, k).id, new Symbol(SexpUtil.offset(element, k).type.toString().substring(0, 1).toUpperCase()+SexpUtil.offset(element, k).type.toString().substring(1))));
					} else if ((!element.id.startsWith(inputs.get(i))) && (i < inputs.size()-1)) {
						continue;
					} else {
						break;
					}
				}
			}
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
}