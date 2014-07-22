package jkind;

import jkind.analysis.Level;
import jkind.analysis.StaticAnalyzer;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.slicing.DependencyMap;
import jkind.translation.Specification;
import jkind.translation.Translate;
import jkindreal.processes.DirectorReal;

public class JRealizability {
	public static void main(String[] args) {
		try {
			JRealizabilitySettings settings = JRealizabilityArgumentParser.parse(args);
			String filename = settings.filename;
			Program program = Main.parseLustre(filename);
			
			Level nonlinear = Level.WARNING;
			StaticAnalyzer.check(program, nonlinear);

			Node main = Translate.translate(program);
			DependencyMap dependencyMap = new DependencyMap(main, main.properties);
			Specification spec = new Specification(filename, main, dependencyMap);
			new DirectorReal(settings, spec).run();
			System.exit(0); // Kills all threads
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}
}