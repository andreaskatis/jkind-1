package jkind.realizability.engines;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jkind.ExitCodes;
import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.Main;
import jkind.Output;
import jkind.aeval.SkolemFunction;
import jkind.realizability.JRealizabilitySolverOption;
import jkind.realizability.engines.messages.BaseStepMessage;
import jkind.realizability.engines.messages.ExtendCounterexampleMessage;
import jkind.realizability.engines.messages.InconsistentMessage;
import jkind.realizability.engines.messages.Message;
import jkind.realizability.engines.messages.RealizableMessage;
import jkind.realizability.engines.messages.UnknownMessage;
import jkind.realizability.engines.messages.UnrealizableMessage;
import jkind.realizability.writers.ConsoleWriter;
import jkind.realizability.writers.ExcelWriter;
import jkind.realizability.writers.Writer;
import jkind.realizability.writers.XmlWriter;
import jkind.results.Counterexample;
import jkind.results.layout.RealizabilityNodeLayout;
import jkind.slicing.ModelSlicer;
import jkind.solvers.Model;
import jkind.translation.Specification;
import jkind.util.CounterexampleExtractor;

public class RealizabilityDirector {
	private JRealizabilitySettings settings;
	private Specification spec;
	private Writer writer;

	private PrintWriter writerImplementation;
	protected ArrayList<SkolemFunction> baseImplementation;
	protected SkolemFunction extendImplementation;
	public SkolemFunction fixpointImplementation;
	protected int k;

	private int baseStep = 0;
	private ExtendCounterexampleMessage extendCounterexample;
	private boolean done = false;

	private List<RealizabilityEngine> engines = new ArrayList<>();
	private List<Thread> threads = new ArrayList<>();

	protected BlockingQueue<Message> incoming = new LinkedBlockingQueue<>();

	public RealizabilityDirector(JRealizabilitySettings settings, Specification spec) {
		this.settings = settings;
		this.spec = spec;
		this.writer = getWriter(spec);
		this.writerImplementation = getImplementationWriter();
		this.baseImplementation = new ArrayList<>();
		this.k = 0;
	}

	private Writer getWriter(Specification spec) {
		try {
			if (settings.excel) {
				return new ExcelWriter(settings.filename + ".xls", spec.node);
			} else if (settings.xml) {
//				if (settings.synthesis && !settings.fixpoint) {
//					return new XmlWriter(settings.filename + "_jsyn.xml", spec.typeMap);
//				} else if (settings.fixpoint || settings.fixpoint_T) {
//					return new XmlWriter(settings.filename + "_jsynvg.xml", spec.typeMap);
//				} else {
					return new XmlWriter(settings.filename + ".xml", spec.typeMap);
//				}
			} else {
				return new ConsoleWriter(new RealizabilityNodeLayout(spec.node));
			}
		} catch (IOException e) {
			throw new JKindException("Unable to open output file", e);
		}
	}

	private PrintWriter getImplementationWriter() {
		if (settings.synthesis) {
			String filename = settings.filename.split("\\.")[0] + "_skolem.smt2";
			try {
				return new PrintWriter(new FileOutputStream(filename), true);
			} catch (FileNotFoundException e) {
				throw new JKindException("Unable to open scratch file: " + filename, e);
			}
		} else {
			return null;
		}
	}

	public void writeImplementation(int k, ArrayList<SkolemFunction> base, SkolemFunction extend) {
		if (writerImplementation != null) {

			Iterator<String> inputs = spec.node.realizabilityInputs.iterator();
			Iterator<String> props = spec.node.properties.iterator();

			if (inputs.hasNext()) {
				writerImplementation.print(";-- INPUTS: ");
				writerImplementation.print(inputs.next());
			}
			while (inputs.hasNext()) {
				writerImplementation.print(", "+inputs.next());
			}

			writerImplementation.print("\n");

			if (props.hasNext()) {
				writerImplementation.print(";-- PROPERTIES: ");
				writerImplementation.print(props.next());
			}
			while (props.hasNext()) {
				writerImplementation.print(", "+props.next());
			}

			writerImplementation.print("\n");

			if (k !=0) {
				for (int step = 0; step < k; step++) {
					writerImplementation.println(";Skolem function for base " + step);
					writerImplementation.println(base.get(step).getSkolemRelation());
				}
			}

			if (extend != null) {
				writerImplementation.println(";Skolem function for extend");
				writerImplementation.println(extend.getSkolemRelation());
			}
		}
	}

	public void writeFixpointImplementation(SkolemFunction impl) {
		if (writerImplementation != null && impl != null) {

			Iterator<String> inputs = spec.node.realizabilityInputs.iterator();
			Iterator<String> props = spec.node.properties.iterator();

			if (inputs.hasNext()) {
				writerImplementation.print(";-- INPUTS: ");
				writerImplementation.print(inputs.next());
			}
			while (inputs.hasNext()) {
				writerImplementation.print(", "+inputs.next());
			}

			writerImplementation.print("\n");

			if (props.hasNext()) {
				writerImplementation.print(";-- PROPERTIES: ");
				writerImplementation.print(props.next());
			}
			while (props.hasNext()) {
				writerImplementation.print(", "+props.next());
			}

			writerImplementation.print("\n");
			writerImplementation.println(";Skolem function for fixpoint");
			writerImplementation.println(impl.getSkolemRelation());
		}
	}

	public int run() {
		printHeader();
		writer.begin();
		startThreads();

		long startTime = System.currentTimeMillis();
		long timeout = startTime + ((long) settings.timeout) * 1000;
		while (System.currentTimeMillis() < timeout && !done && someThreadAlive()
				&& !someEngineFailed()) {
			processMessages(startTime);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
        processMessages(startTime);
		if (settings.synthesis) {
            if (settings.fixpoint) {
                writeFixpointImplementation(fixpointImplementation);
            } else {
                writeImplementation(k, baseImplementation, extendImplementation);
            }
		}

		if (!done) {
			writer.writeUnknown(baseStep, convertExtendCounterexample(), getRuntime(startTime));
		}

		writer.end();
		return reportFailures();
	}

	private boolean someThreadAlive() {
		for (Thread thread : threads) {
			if (thread.isAlive()) {
				return true;
			}
		}

		return false;
	}

	private boolean someEngineFailed() {
		for (RealizabilityEngine process : engines) {
			if (process.getThrowable() != null) {
				return true;
			}
		}

		return false;
	}

	private int reportFailures() {
		int exitCode = 0;
		for (RealizabilityEngine process : engines) {
			if (process.getThrowable() != null) {
				Throwable t = process.getThrowable();
				Output.println(process.getName() + " process failed");
				Output.printStackTrace(t);
				exitCode = ExitCodes.UNCAUGHT_EXCEPTION;
			}
		}
		return exitCode;
	}

	private void printHeader() {
		Output.println("==========================================");
		Output.println("  JRealizability " + Main.VERSION);
		Output.println("==========================================");
		Output.println();
	}

	private void startThreads() {

		if (settings.fixpoint) {
			RealizabilityFixpointEngine fixpointEngine = new RealizabilityFixpointEngine(spec, settings, this);
			registerProcess(fixpointEngine);
		} else {

			RealizabilityBaseEngine baseEngine = new RealizabilityBaseEngine(spec, settings, this);
			registerProcess(baseEngine);

			RealizabilityExtendEngine extendEngine = new RealizabilityExtendEngine(spec, settings, this);
			baseEngine.setExtendEngine(extendEngine);
			extendEngine.setBaseEngine(baseEngine);
			registerProcess(extendEngine);
		}

		for (Thread thread : threads) {
			thread.start();
		}
	}

	private void registerProcess(RealizabilityEngine process) {
		engines.add(process);
		threads.add(new Thread(process, process.getName()));
	}

	private void processMessages(long startTime) {
		while (!done && !incoming.isEmpty()) {
			Message message = incoming.poll();
			double runtime = getRuntime(startTime);
			if (message instanceof RealizableMessage) {
				RealizableMessage rm = (RealizableMessage) message;
				done = true;
				k = rm.k;
                if(settings.fixpoint) {
                    writer.writeFixpointRealizable(rm.k, runtime);
                } else {
                    writer.writeRealizable(rm.k, runtime);
                }
			} else if (message instanceof UnrealizableMessage) {
				UnrealizableMessage um = (UnrealizableMessage) message;
				done = true;
				if (settings.solver == JRealizabilitySolverOption.AEVAL) {
					writer.writeFixpointUnrealizable(um.k, um.properties, runtime);
				} else {
					Model sliced = slice(um.model, um.properties);
					Counterexample cex = extractCounterexample(um.k, sliced);
					writer.writeUnrealizable(cex, um.properties, runtime);
				}
			} else if (message instanceof ExtendCounterexampleMessage) {
				extendCounterexample = (ExtendCounterexampleMessage) message;
			} else if (message instanceof UnknownMessage) {
				done = true;
				writer.writeUnknown(baseStep, null, runtime);
			} else if (message instanceof BaseStepMessage) {
				BaseStepMessage bsm = (BaseStepMessage) message;
				writer.writeBaseStep(bsm.step);
				baseStep = bsm.step;
			} else if (message instanceof InconsistentMessage) {
				InconsistentMessage im = (InconsistentMessage) message;
				done = true;
				writer.writeInconsistent(im.k, runtime);
			} else {
				throw new JKindException("Unknown message type in director: "
						+ message.getClass().getCanonicalName());
			}
		}
	}

	private double getRuntime(long startTime) {
		return (System.currentTimeMillis() - startTime) / 1000.0;
	}

	private Model slice(Model model, List<String> properties) {
		if (properties.isEmpty()) {
			return model;
		}
		return ModelSlicer.slice(model, spec.dependencyMap.get(properties));
	}

	private Counterexample convertExtendCounterexample() {
		if (extendCounterexample == null) {
			return null;
		}

		return extractCounterexample(extendCounterexample.k, extendCounterexample.model);
	}

	private Counterexample extractCounterexample(int k, Model model) {
		return CounterexampleExtractor.extract(spec, k, model);
	}
}