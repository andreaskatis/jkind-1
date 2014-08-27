package jkind.results;


public final class InvalidRealizability extends Realizability {
		private final Counterexample cex;

		public InvalidRealizability(String name, Counterexample cex, double runtime) {
			super(name, runtime);
			this.cex = cex;
		}

		/**
		 * Counterexample for the property
		 */
		public Counterexample getCounterexample() {
			return cex;
		}
}
