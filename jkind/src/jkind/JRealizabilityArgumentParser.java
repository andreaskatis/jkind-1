package jkind;

import jkind.aeval.AevalSolver;
import jkind.engines.SolverUtil;
import jkind.lustre.Node;
import jkind.lustre.builders.NodeBuilder;
import jkind.realizability.JRealizabilitySolverOption;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class JRealizabilityArgumentParser extends ArgumentParser {
	private static final String EXCEL = "excel";
	private static final String EXTEND_CEX = "extend_cex";
	private static final String N = "n";
	private static final String REDUCE = "reduce";
	private static final String SCRATCH = "scratch";
	private static final String TIMEOUT = "timeout";
	private static final String XML = "xml";
	private static final String SYNTHESIS = "synthesis";
	private static final String FIXPOINT = "fixpoint";
    private static final String COMPACT = "compact";
    private static final String ALLINCLUSIVE = "all-inclusive";
    private static final String SOLVER = "solver";
    private static final String NONDET = "nondet";

	private final JRealizabilitySettings settings;

	private JRealizabilityArgumentParser() {
		this("JRealizability", new JRealizabilitySettings());
	}

	private JRealizabilityArgumentParser(String name, JRealizabilitySettings settings) {
		super(name, settings);
		this.settings = settings;
	}

	@Override
	protected Options getOptions() {
		Options options = super.getOptions();
		options.addOption(EXCEL, false, "generate results in Excel format");
		options.addOption(EXTEND_CEX, false, "report extend counterexample");
		options.addOption(N, true, "number of iterations (default 200)");
		options.addOption(REDUCE, false, "reduce conflicting properties in case of unrealizable");
		options.addOption(SCRATCH, false, "produce files for debugging purposes");
		options.addOption(TIMEOUT, true, "maximum runtime in seconds (default 100)");
		options.addOption(XML, false, "generate results in XML format");
		options.addOption(SYNTHESIS, false, "synthesize implementation from realizable contract (default engine : k-induction, use with -fixpoint for fixpoint-based synthesis)");
		options.addOption(FIXPOINT, false, "use fixpoint algorithm for realizability/synthesis");
        options.addOption(COMPACT, false, "Attempt to synthesize a more compact implementation");
        options.addOption(ALLINCLUSIVE, false, "Attempt to synthesize an all-inclusive implementation (for contracts with disjunctive/implicative properties)");
        options.addOption(SOLVER, true, "SMT solver for k-induction realizability checking. Current options : z3, aeval");
        options.addOption(NONDET, false, "synthesize nondeterministic implementation (default engine : k-induction, use with -fixpoint for fixpoint-based synthesis)");
		return options;
	}

	public static JRealizabilitySettings parse(String[] args) {
		JRealizabilityArgumentParser parser = new JRealizabilityArgumentParser();
		parser.parseArguments(args);
		return parser.settings;
	}

	@Override
	protected void parseCommandLine(CommandLine line) {
        if (line.hasOption(VERSION)) {
            Output.println(name + " " + Main.VERSION);
            printDetectedSolvers();
            System.exit(0);
        }
		super.parseCommandLine(line);

		ensureExclusive(line, EXCEL, XML);

		if (line.hasOption(EXCEL)) {
			settings.excel = true;
		}
		
		if (line.hasOption(EXTEND_CEX)) {
			settings.extendCounterexample = true;
		}

		if (line.hasOption(N)) {
			settings.n = parseNonnegativeInt(line.getOptionValue(N));
		}
		
		if (line.hasOption(REDUCE)) {
			settings.reduce = true;
		}

		if (line.hasOption(TIMEOUT)) {
			settings.timeout = parseNonnegativeInt(line.getOptionValue(TIMEOUT));
		}

		if (line.hasOption(SCRATCH)) {
			settings.scratch = true;
		}

		if (line.hasOption(XML)) {
			settings.xml = true;
		}

		if (line.hasOption(SYNTHESIS)) {
			settings.synthesis = true;
            settings.solver = JRealizabilitySolverOption.AEVAL;
		}

		if (line.hasOption(FIXPOINT)) {
			settings.fixpoint = true;
            settings.solver = JRealizabilitySolverOption.AEVAL;
        }

        if (line.hasOption(COMPACT)) {
            settings.synthesis = true;
            settings.compact = true;
            settings.solver = JRealizabilitySolverOption.AEVAL;
        }

        if (line.hasOption(ALLINCLUSIVE)) {
            settings.synthesis = true;
            settings.allinclusive = true;
            settings.solver = JRealizabilitySolverOption.AEVAL;
        }

        if (line.hasOption(NONDET)) {
            settings.synthesis = true;
            settings.nondet = true;
            settings.solver = JRealizabilitySolverOption.AEVAL;
        }

        if (line.hasOption(SOLVER)) {
            settings.solver = getSolverOption(line.getOptionValue(SOLVER));
        }
	}

    private void printDetectedSolvers() {
        String detected = Arrays.stream(SolverOption.values()).filter(this::solverIsAvailable)
                .map(Object::toString).collect(joining(", "));
        System.out.println("Detected solvers: " + detected);
    }

    private boolean solverIsAvailable(SolverOption solverOption) {
        try {
            switch (solverOption) {
                case AEVAL:
                    AevalSolver ae = new AevalSolver(null, null, null);
                    break;
                default:
                    Node emptyNode = new NodeBuilder("empty").build();
                    SolverUtil.getSolver(solverOption, null, emptyNode);
            }
        } catch (JKindException e) {
            return false;
        }
        return true;
    }

    private static JRealizabilitySolverOption getSolverOption(String solver) {
        List<JRealizabilitySolverOption> options = Arrays.asList(JRealizabilitySolverOption.values());
        for (JRealizabilitySolverOption option : options) {
            if (solver.equals(option.toString())) {
                return option;
            }
        }

        Output.error("unknown solver: " + solver);
        Output.println("Valid options: " + options);
        System.exit(ExitCodes.INVALID_OPTIONS);
        return null;
    }
}
