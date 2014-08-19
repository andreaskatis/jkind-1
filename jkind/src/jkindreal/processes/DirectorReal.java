package jkindreal.processes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.Output;
import jkind.invariant.Invariant;
import jkind.lustre.EnumType;
import jkind.lustre.Type;
import jkind.lustre.values.EnumValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.Value;
import jkind.results.Counterexample;
import jkind.results.Signal;
import jkind.results.layout.NodeLayout;
import jkind.solvers.Model;
import jkind.translation.Specification;
import jkind.util.SexpUtil;
import jkind.writers.ConsoleWriter;
import jkind.writers.ExcelWriter;
import jkind.writers.Writer;
import jkind.writers.XmlWriter;
import jkindreal.processes.messages.CounterexampleMessageReal;
import jkindreal.processes.messages.InductiveCounterexampleMessageReal;
import jkindreal.processes.messages.InvalidRealizabilityMessage;
import jkindreal.processes.messages.MessageReal;
import jkindreal.processes.messages.UnknownMessageReal;
import jkindreal.processes.messages.ValidRealizabilityMessage;

public class DirectorReal {
	private JRealizabilitySettings settings;
	private Specification spec;
	private Writer writer;

	private List<String> remainingRealizabilities = new ArrayList<>();
	private List<String> validRealizabilities = new ArrayList<>();
	private List<String> invalidRealizabilities = new ArrayList<>();
	private List<String> unknownRealizabilities = new ArrayList<>();
	private Map<String, InductiveCounterexampleMessageReal> inductiveCounterexamples = new HashMap<>();

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
	}

	private Writer getWriter(Specification spec) {
		try {
			if (settings.excel) {
				return new ExcelWriter(spec.filename + ".xls", spec.node);
			} else if (settings.xml) {
				return new XmlWriter(spec.filename + ".xml", spec.typeMap, settings.xmlToStdout);
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

		unknownRealizabilities.addAll(remainingRealizabilities);
		remainingRealizabilities.clear();
		if (!unknownRealizabilities.isEmpty()) {
			writer.writeUnknown(unknownRealizabilities, convertInductiveCounterexamples());
		}

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
				Output.println(process.getName() + " process failed");
				Output.printStackTrace(t);
			}
		}
	}

	private void printHeader() {
		if (!settings.xmlToStdout) {
			Output.println("==========================================");
			Output.println("  JAVA KIND");
			Output.println("==========================================");
			Output.println();
			Output.println("There are " + remainingRealizabilities.size() + " realizabilities to be checked.");
			Output.println("REALIZABILITIES TO BE CHECKED: " + remainingRealizabilities);
			Output.println();
		}
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
		
		for (Thread thread : threads) {
			thread.start();
		}
	}

	private void registerProcess(ProcessReal process) {
		processes.add(process);
		threads.add(new Thread(process, process.getName()));
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
			} else if (message instanceof InvalidRealizabilityMessage) {
				InvalidRealizabilityMessage im = (InvalidRealizabilityMessage) message;
				remainingRealizabilities.removeAll(im.invalid);
				invalidRealizabilities.addAll(im.invalid);
				inductiveCounterexamples.keySet().removeAll(im.invalid);
				for (String props : spec.node.properties) {
					Model slicedModel = im.model.slice(spec.dependencyMap.get(props));
					Counterexample cex = extractCounterexample(im.k, slicedModel);
					writer.writeInvalidRealizability(props, cex, runtime);
				}
			} else if (message instanceof CounterexampleMessageReal) {
				CounterexampleMessageReal cm = (CounterexampleMessageReal) message;
				remainingRealizabilities.remove(cm.invalid);
				invalidRealizabilities.addAll(cm.invalid);
				inductiveCounterexamples.keySet().remove(cm.invalid);
				writer.writeInvalidRealizability(cm.invalid.toString(), cm.cex, runtime);
			} else if (message instanceof InductiveCounterexampleMessageReal) {
				InductiveCounterexampleMessageReal icm = (InductiveCounterexampleMessageReal) message;
				inductiveCounterexamples.put(icm.realizability, icm);
			} else if (message instanceof UnknownMessageReal) {
				UnknownMessageReal um = (UnknownMessageReal) message;
				remainingRealizabilities.removeAll(um.unknown);
				unknownRealizabilities.addAll(um.unknown);
			} else {
				throw new JKindException("Unknown message type in director: "
						+ message.getClass().getCanonicalName());
			}
		}
	}

	private void printSummary() {
		if (!settings.xmlToStdout) {
			Output.println("    -------------------------------------");
			Output.println("    --^^--        SUMMARY          --^^--");
			Output.println("    -------------------------------------");
			Output.println();
			if (!validRealizabilities.isEmpty()) {
				Output.println("VALID REALIZABILITIES: " + validRealizabilities);
				Output.println();
			}
			if (!invalidRealizabilities.isEmpty()) {
				Output.println("INVALID REALIZABILITIES: " + invalidRealizabilities);
				Output.println();
			}
			if (!unknownRealizabilities.isEmpty()) {
				Output.println("UNKNOWN REALIZABILITIES: " + unknownRealizabilities);
				Output.println();
			}
		}
	}

	private Map<String, Counterexample> convertInductiveCounterexamples() {
		Map<String, Counterexample> result = new HashMap<>();

		for (String real : inductiveCounterexamples.keySet()) {
			InductiveCounterexampleMessageReal icm = inductiveCounterexamples.get(real);
			Model slicedModel = icm.model.slice(spec.dependencyMap.get(icm.realizability));
			result.put(real, extractCounterexample(icm.k, slicedModel));
		}

		return result;
	}

	private Counterexample extractCounterexample(int k, Model model) {
		Counterexample cex = new Counterexample(k);
		for (String var : model.getVariableNames()) {
			String base = SexpUtil.getBaseName(var);
			int offset = SexpUtil.getOffset_real(var);
			if (offset >= 0 && !base.startsWith("%")) {
				Signal<Value> signal = cex.getOrCreateSignal(base);
				Value value = convert(base, model.getValue(var));
				signal.putValue(offset, value);
			}
		}
		return cex;
	}

	private Value convert(String base, Value value) {
		Type type = spec.typeMap.get(base);
		if (type instanceof EnumType) {
			EnumType et = (EnumType) type;
			IntegerValue iv = (IntegerValue) value;
			return new EnumValue(et.values.get(iv.value.intValue()));
		}
		return value;
	}
}