package jkindreal.processes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.invariant.Invariant;
import jkind.lustre.Type;
import jkind.lustre.values.Value;
import jkindreal.processes.messages.CounterexampleMessageReal;
import jkindreal.processes.messages.InductiveCounterexampleMessageReal;
import jkindreal.processes.messages.InvalidRealizabilityMessage;
import jkindreal.processes.messages.MessageReal;
import jkindreal.processes.messages.ValidRealizabilityMessage;
import jkind.results.Counterexample;
import jkind.results.Signal;
import jkind.results.layout.NodeLayout;
import jkind.slicing.CounterexampleSlicer;
import jkind.solvers.Model;
import jkind.solvers.StreamDecl;
import jkind.solvers.StreamDef;
import jkind.translation.Specification;
import jkind.util.Util;
import jkind.writers.ConsoleWriter;
import jkind.writers.ExcelWriter;
import jkind.writers.Writer;
import jkind.writers.XmlWriter;

public class DirectorReal {
	private JRealizabilitySettings settings;
	private Specification spec;
	private Writer writer;

	
	private List<String> remainingRealizabilities = new ArrayList<>();
	private List<String> validRealizabilities = new ArrayList<>();
	private List<String> invalidRealizabilities = new ArrayList<>();
	private Map<String, InductiveCounterexampleMessageReal> inductiveCounterexamples = new HashMap<>();
	private Map<String, StreamDecl> declarations;

	private BaseProcessReal baseProcess;
	private InductiveProcessReal inductiveProcess;

	private List<ProcessReal> processes = new ArrayList<>();
	private List<Thread> threads = new ArrayList<>();

	protected BlockingQueue<MessageReal> incoming = new LinkedBlockingQueue<>();

	public DirectorReal(JRealizabilitySettings settings, Specification spec) {
		this.settings = settings;
		this.spec = spec;
		this.writer = getWriter(spec);
		this.remainingRealizabilities.addAll(spec.node.realizabilities);
		this.declarations = new HashMap<>();
		for (StreamDecl decl : spec.translation.getDeclarations()) {
			this.declarations.put(decl.getId().toString(), decl);
		}
	}

	private Writer getWriter(Specification spec) {
		try {
			if (settings.excel) {
				return new ExcelWriter(spec.filename + ".xls", spec.node);
			} else if (settings.xml) {
				return new XmlWriter(spec.filename + ".xml", spec.typeMap,settings.xmlToStdout);
			} else {
				return new ConsoleWriter(new NodeLayout(spec.node));
			}
		} catch (IOException e) {
			throw new JKindException("Unable to open output file", e);
		}
	}

	public void run() {
		printHeader();
		writer.begin();
		startThreads();

		long startTime = System.currentTimeMillis();
		long timeout = startTime + ((long) settings.timeout) * 1000;
		while (System.currentTimeMillis() < timeout && !remainingRealizabilities.isEmpty()
				&& someThreadAlive() && !someProcessFailed()) {
			processMessages(startTime);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		processMessages(startTime);
		
		//Do we need to check for "unknown" realizabilities"?...
		/*if (!remainingRealizabilities.isEmpty()) {
			writer.writeUnknown_Realizability(remainingRealizabilities, convertInductiveCounterexamples());
		}*/

		writer.end();
		printSummary();
		reportFailures();
	}

	private boolean someThreadAlive() {
		for (Thread thread : threads) {
			if (thread.isAlive()) {
				return true;
			}
		}

		return false;
	}

	private boolean someProcessFailed() {
		for (ProcessReal process : processes) {
			if (process.getThrowable() != null) {
				return true;
			}
		}

		return false;
	}

	private void reportFailures() {
		for (ProcessReal process : processes) {
			if (process.getThrowable() != null) {
				Throwable t = process.getThrowable();
				System.out.println(process.getName() + " process failed");
				t.printStackTrace(System.out);
			}
		}
	}

	private void printHeader() {
		System.out.println("==========================================");
		System.out.println("  JAVA KIND");
		System.out.println("==========================================");
		System.out.println();
		System.out
		.println("There are " + remainingRealizabilities.size() + " realizabilities to be checked.");
		System.out.println("REALIZABILITIES TO BE CHECKED: " + remainingRealizabilities);
		System.out.println();
	}

	private void startThreads() {
		baseProcess = new BaseProcessReal(spec, settings, this);
		registerProcess(baseProcess);

		if (settings.useInductiveProcess) {
			inductiveProcess = new InductiveProcessReal(spec, settings, this);
			baseProcess.setInductiveProcess(inductiveProcess);
			inductiveProcess.setBaseProcess(baseProcess);
			registerProcess(inductiveProcess);
		}

		/*if (settings.useInvariantProcess) {
			invariantProcess = new InvariantProcess(spec, settings);
			invariantProcess.setInductiveProcess(inductiveProcess);
			inductiveProcess.setInvariantProcess(invariantProcess);
			registerProcess(invariantProcess);
		}*/

		/*if (settings.reduceInvariants) {
			reduceProcess = new ReduceProcess(spec, settings, this);
			inductiveProcess.setReduceProcess(reduceProcess);
			registerProcess(reduceProcess);
		}*/

		/*if (settings.smoothCounterexamples) {
			smoothProcess = new SmoothProcess(spec, settings, this);
			baseProcess.setCounterexampleProcess(smoothProcess);
			registerProcess(smoothProcess);
		}*/

		/*if (settings.intervalGeneralization) {
			intervalProcess = new IntervalProcess(spec, settings, this);
			if (smoothProcess == null) {
				baseProcess.setCounterexampleProcess(intervalProcess);
			} else {
				smoothProcess.setCounterexampleProcess(intervalProcess);
			}
			registerProcess(intervalProcess);
		}*/

		for (Thread thread : threads) {
			thread.start();
		}
	}

	private void registerProcess(ProcessReal invariantProcess2) {
		processes.add(invariantProcess2);
		threads.add(new Thread(invariantProcess2, invariantProcess2.getName()));
	}

	private void processMessages(long startTime) {
		while (!incoming.isEmpty()) {
			MessageReal message = incoming.poll();
			double runtime = (System.currentTimeMillis() - startTime) / 1000.0;
			if (message instanceof ValidRealizabilityMessage) {
				ValidRealizabilityMessage vm = (ValidRealizabilityMessage) message;
				remainingRealizabilities.removeAll(vm.valid);
				validRealizabilities.addAll(vm.valid);
				inductiveCounterexamples.keySet().removeAll(vm.valid);
				List<Invariant> invariants = vm.invariants;
				writer.writeValidRealizability(vm.valid, vm.k, runtime, invariants);
			} else if (message instanceof InvalidRealizabilityMessage){
				InvalidRealizabilityMessage im = (InvalidRealizabilityMessage) message;
				remainingRealizabilities.removeAll(im.invalid);
				invalidRealizabilities.addAll(im.invalid);
				inductiveCounterexamples.keySet().removeAll(im.invalid);
				CounterexampleSlicer cexSlicer = new CounterexampleSlicer(spec.dependencyMap);
				for (String invalidReal : im.invalid) {
					//for(String prop : spec.node.properties) {
						Model slicedModel = cexSlicer.slicereal(invalidReal, im.model);
						Counterexample cex = extractCounterexample(im.k, BigInteger.ZERO, slicedModel);
						writer.writeInvalidRealizability(invalidReal, cex, runtime);
					//}					
				}	
			} else if (message instanceof CounterexampleMessageReal){
				CounterexampleMessageReal cm = (CounterexampleMessageReal) message;
				remainingRealizabilities.remove(cm.invalid);
				invalidRealizabilities.addAll(cm.invalid);
				inductiveCounterexamples.keySet().remove(cm.invalid);
				writer.writeInvalidRealizability(cm.invalid.toString(), cm.cex, runtime);
			} else if (message instanceof InductiveCounterexampleMessageReal) {
				InductiveCounterexampleMessageReal icmr = (InductiveCounterexampleMessageReal) message;
				inductiveCounterexamples.put(icmr.realizability, icmr);
			} else {
				throw new JKindException("Unknown message type in director: "
						+ message.getClass().getCanonicalName());
			}
		}
	}

	private void printSummary() {
		System.out.println("    -------------------------------------");
		System.out.println("    --^^--        SUMMARY          --^^--");
		System.out.println("    -------------------------------------");
		System.out.println();
		
		
		if (!validRealizabilities.isEmpty()) {
			System.out.println("VALID REALIZABILITIES: " + validRealizabilities);
			System.out.println();
		}
		if (!invalidRealizabilities.isEmpty()) {
			System.out.println("INVALID REALIZABILITIES: " + invalidRealizabilities);
			System.out.println();
		}
	}

	private Counterexample extractCounterexample(int k, BigInteger offset, Model model) {
		Counterexample cex = new Counterexample(k);
		model.setDeclarations(declarations);
		model.setDefinitions(Collections.<String, StreamDef> emptyMap());
		for (String fn : new TreeSet<>(model.getFunctions())) {
			cex.addSignal(extractSignal(fn, k, offset, model));
		}
		return cex;
	}

	private Signal<Value> extractSignal(String fn, int k, BigInteger offset, Model model) {
		String name = fn.substring(1);
		Signal<Value> signal = new Signal<>(name);
		Type type = spec.typeMap.get(name);

		for (int i = 0; i < k; i++) {
			BigInteger key = BigInteger.valueOf(i).add(offset);
			jkind.solvers.Value value = model.getFunctionValue(fn, key);
			if (value != null) {
				signal.putValue(i, Util.parseValue(Util.getName(type), value.toString()));
			}
		}

		return signal;
	}
}
