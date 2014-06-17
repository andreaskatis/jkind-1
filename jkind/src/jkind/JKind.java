package jkind;

import java.util.ArrayList;
import java.util.List;

import jkind.analysis.Level;
import jkind.analysis.StaticAnalyzer;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.processes.Director;
import jkind.slicing.DependencyMap;
import jkind.slicing.LustreSlicer;
import jkind.translation.Specification;
import jkind.translation.Translate;

public class JKind {
	public static void main(String[] args) {
		try {
			JKindSettings settings = JKindArgumentParser.parse(args);
			String filename = settings.filename;
			Program program = Main.parseLustre(filename);
			
			Level nonlinear = settings.solver == SolverOption.Z3 ? Level.WARNING : Level.ERROR;
			StaticAnalyzer.check(program, nonlinear);

			Node main = Translate.translate(program);
			DependencyMap dependencyMap = new DependencyMap(main, append(main.properties, main.eventuallies));
			main = LustreSlicer.slice(main, dependencyMap);
			Specification spec = new Specification(filename, main, dependencyMap);
			new Director(settings, spec).run();
			System.exit(0); // Kills all threads
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private static List<String> append(List<String> a, List<String> b) {
		List<String> result = new ArrayList<>();
		result.addAll(a);
		result.addAll(b);
		return result;
	}
}
