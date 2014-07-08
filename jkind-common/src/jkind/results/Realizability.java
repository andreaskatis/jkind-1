package jkind.results;

public abstract class Realizability {
	protected final String name;
	protected final double runtime;

	public Realizability(String real, double runtime) {
		this.name = real;
		this.runtime = runtime;
	}

	/**
	 * Get the name of the realizability
	 */
	public String getName() {
		return name;
	}
}
