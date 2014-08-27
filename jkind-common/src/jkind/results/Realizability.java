package jkind.results;

public abstract class Realizability {
	private final String name;
	private final double runtime;

	public Realizability(String real, double runtime) {
		this.name = real;
		this.runtime = runtime;
	}
	
	public Realizability(String name) {
		this.name = name;
		this.runtime = 0;
	}
	

	/**
	 * Get the name of the realizability
	 */
	public String getName() {
		return name;
	}
	
	/**
	 *  Runtime of the property measured in seconds
	 */
	public double getRuntime() {
		return runtime;
	}
}
