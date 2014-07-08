package jkind.results;


public final class InvalidRealizability extends Realizability {
		private final Counterexample cex;

		public InvalidRealizability(String real, Counterexample cex, double runtime) {
			super(real, runtime);
			this.cex = cex;
		}

		/**
		 * Counterexample for the property
		 */
		public Counterexample getCounterexample() {
			return cex;
		}

		/**
		 * Runtime of falsification measured in seconds
		 */
		public double getRuntime() {
			return runtime;
		}

}
