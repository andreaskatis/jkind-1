package jkind.results;

public abstract class Realizability {
	protected final String name;

	public Realizability(String real) {
		this.name = real;
	}

	/**
	 * Get the name of the realizability
	 */
	public String getName() {
		return name;
	}
}
