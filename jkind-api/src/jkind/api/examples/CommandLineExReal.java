package jkind.api.examples;

import java.io.File;

import jkind.api.RealApi;
import jkind.api.results.JKindResultRealizability;
import jkind.api.results.RealizabilityResult;

import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * This example illustrates how to call the JKind API and process the results
 */
public class CommandLineExReal {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Must specify Lustre file as argument");
			System.exit(-1);
		}

		/*
		 * We take a Lustre file as input, though in practice programmers will
		 * often construct their Lustre programmatically.
		 */
		File file = new File(args[0]);

		/*
		 * The results of JKind API execution will be stored in here. A
		 * programmer may attach listeners to this object to dynamically respond
		 * to results.
		 */
		JKindResultRealizability result = new JKindResultRealizability(file.getName());

		/*
		 * The progress monitor is used to cancel execution if needed. We will
		 * not use that feature here.
		 */
		NullProgressMonitor monitor = new NullProgressMonitor();

		/*
		 * This triggers the actual execution of analysis. Options can be set on
		 * the JKindApi object if desired.
		 */
		new RealApi().execute(file, result, monitor);

		/*
		 * Process some of the results
		 */
		for (RealizabilityResult re : result.getRealizabilityResults()) {
			System.out.println(re.getName() + " - " + re.getStatus());
		}

		/*
		 * Dumps the results to an Excel file
		 */
		File xlsFile = new File(args[0] + ".xls");
		result.toExcel(xlsFile);
		System.out.println("Complete results written to " + xlsFile);
	}
}
