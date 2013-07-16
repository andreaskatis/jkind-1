package jkind.solvers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import jkind.JKindException;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.util.BiMap;
import jkind.util.Util;

public abstract class Solver {
	public abstract void initialize();
	
	protected abstract void send(String str);
	
	public void send(Sexp sexp) {
		send(encode(sexp).toString());
	}

	public abstract void send(StreamDecl decl);
	public abstract void send(StreamDef def);
	public abstract void send(VarDecl decl);
	
	public abstract Label weightedAssert(Sexp sexp, int weight);
	public abstract Label labelledAssert(Sexp sexp);
	public abstract void retract(Label label);
	
	public abstract Result query(Sexp sexp);
	public abstract Result maxsatQuery(Sexp sexp);
	
	public abstract void push();
	public abstract void pop();

	/** Backend */
	
	protected Process process;
	protected BufferedWriter toSolver;
	protected BufferedReader fromSolver;
	
	protected Solver(ProcessBuilder pb) {
		pb.redirectErrorStream(true);
		try {
			process = pb.start();
		} catch (IOException e) {
			throw new JKindException("Unable to start solver", e);
		}
		addShutdownHook();
		toSolver = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		fromSolver = new BufferedReader(new InputStreamReader(process.getInputStream()));
	}
	

	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
			@Override
			public void run() {
				Solver.this.stop();
			}
		});
	}

	public synchronized void stop() {
		/**
		 * This must be synchronized since two threads (a Process or a shutdown
		 * hook) may try to stop the solver at the same time
		 */

		if (process != null) {
			process.destroy();
			process = null;
		}
	}
	
	/** Utility */

	public void send(List<StreamDecl> decls) {
		for (StreamDecl decl : decls) {
			send(decl);
		}
	}
	
	/** Encoding for quoted ids */
	
	protected BiMap<Symbol, Symbol> encoding = new BiMap<>();
	
	protected Sexp encode(Sexp sexp) {
		if (sexp instanceof Cons) {
			Cons cons = (Cons) sexp;
			return new Cons(encode(cons.head), encodeList(cons.args));
		} else if (sexp instanceof Symbol) {
			Symbol symbol = (Symbol) sexp;
			return encode(symbol);
		} else {
			throw new IllegalArgumentException("Unknown sexp");
		}
	}
	
	private List<Sexp> encodeList(List<Sexp> list) {
		List<Sexp> result = new ArrayList<>();
		for (Sexp sexp : list) {
			result.add(encode(sexp));
		}
		return result;
	}
	
	protected Symbol encode(Symbol original) {
		if (encoding.containsKey(original)) {
			return encoding.get(original);
		} else if (Util.isQuotedStream(original.sym)) {
			Symbol encoded = new Symbol(convertQuotedStream(original.sym));
			encoding.put(original, encoded);
			return encoded;
		} else {
			return original;
		}
	}
	
	final private String QUOTED_ID_PREFIX = "%quoted";
	private int quotedIdCounter = 0;

	protected String convertQuotedStream(String qid) {
		return "$" + QUOTED_ID_PREFIX + quotedIdCounter++;
	}
	
	public String decode(String encoded) {
		Symbol sym = new Symbol(encoded);
		if (encoding.containsValue(sym)) {
			return encoding.inverse().get(sym).sym;
		} else {
			return encoded;
		}
	}

	/** Debugging */
	
	protected PrintWriter debug;
	
	public void setDebug(PrintWriter debug) {
		this.debug = debug;
	}

	public void debug(String str) {
		if (debug != null) {
			debug.println(str);
		}
	}
}
