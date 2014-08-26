package jkind.api.results;

import java.util.Map.Entry;

import jkind.lustre.values.Value;
import jkind.results.Counterexample;
import jkind.results.InvalidProperty;
import jkind.results.InvalidRealizability;
import jkind.results.Property;
import jkind.results.Realizability;
import jkind.results.Signal;
import jkind.results.UnknownProperty;
import jkind.results.UnknownRealizability;
import jkind.results.ValidProperty;
import jkind.results.ValidRealizability;

/**
 * An class for renaming and removing variables from analysis results
 * 
 * @see MapRenaming
 */
public abstract class Renaming {
	/**
	 * Returns the new name for a given name, or null if the original name
	 * should be hidden. This method should always return the same result when
	 * given the same input.
	 * 
	 * @param original
	 *            Original variable name
	 * @return the new variable name or null if variable should be hidden
	 */
	public abstract String rename(String original);

	/**
	 * Rename property and signals (if present), possibly omitting some
	 * 
	 * @param property
	 *            Property to be renamed
	 * @return Renamed version of the property, or <code>null</code> if there is
	 *         no renaming for the property
	 */
	public Property rename(Property property) {
		if (property instanceof ValidProperty) {
			return rename((ValidProperty) property);
		} else if (property instanceof InvalidProperty) {
			return rename((InvalidProperty) property);
		} else if (property instanceof UnknownProperty) {
			return rename((UnknownProperty) property);
		} else {
			return null;
		}
	}

	/**
	 * Rename valid property and signals (if present), possibly omitting some
	 * 
	 * Note: Invariants (if present) will not be renamed
	 * 
	 * @param property
	 *            Property to be renamed
	 * @return Renamed version of the property, or <code>null</code> if there is
	 *         no renaming for the property
	 */
	public ValidProperty rename(ValidProperty property) {
		String name = rename(property.getName());
		if (name == null) {
			return null;
		}

		return new ValidProperty(name, property.getK(), property.getRuntime(),
				property.getInvariants());
	}

	/**
	 * Rename invalid property and signals (if present), possibly omitting some
	 * 
	 * @param property
	 *            Property to be renamed
	 * @return Renamed version of the property, or <code>null</code> if there is
	 *         no renaming for the property
	 */
	public InvalidProperty rename(InvalidProperty property) {
		String name = rename(property.getName());
		if (name == null) {
			return null;
		}

		return new InvalidProperty(name, rename(property.getCounterexample()),
				property.getRuntime());
	}

	/**
	 * Rename unknown property and signals (if present), possibly omitting some
	 * 
	 * @param property
	 *            Property to be renamed
	 * @return Renamed version of the property, or <code>null</code> if there is
	 *         no renaming for the property
	 */
	public UnknownProperty rename(UnknownProperty property) {
		String name = rename(property.getName());
		if (name == null) {
			return null;
		}

		return new UnknownProperty(name, rename(property.getInductiveCounterexample()));
	}
	
	
	/**
	 * Rename realizability and signals (if present), possibly omitting some
	 * 
	 * @param realizability
	 *            Realizability to be renamed
	 * @return Renamed version of the realizability, or <code>null</code> if there is
	 *         no renaming for the realizability
	 */
	public Realizability rename(Realizability realizability) {
		if (realizability instanceof ValidRealizability) {
			return rename((ValidRealizability) realizability);
		} else if (realizability instanceof InvalidRealizability) {
			return rename((InvalidRealizability) realizability);
		} else if (realizability instanceof UnknownRealizability) {
			return rename((UnknownRealizability) realizability);
		} else {
			return null;
		}
	}

	/**
	 * Rename valid realizability and signals (if present), possibly omitting some
	 * 
	 * Note: Invariants (if present) will not be renamed
	 * 
	 * @param realizability
	 *            Realizability to be renamed
	 * @return Renamed version of the realizability, or <code>null</code> if there is
	 *         no renaming for the realizability
	 */
	public ValidRealizability rename(ValidRealizability realizability) {
		String name = rename(realizability.getName());
		if (name == null) {
			return null;
		}

		return new ValidRealizability(name, realizability.getK(), realizability.getRuntime(),
				realizability.getInvariants());
	}

	/**
	 * Rename invalid realizability and signals (if present), possibly omitting some
	 * 
	 * @param realizability
	 *            Realizability to be renamed
	 * @return Renamed version of the realizability, or <code>null</code> if there is
	 *         no renaming for the realizability
	 */
	public InvalidRealizability rename(InvalidRealizability realizability) {
		String name = rename(realizability.getName());
		if (name == null) {
			return null;
		}

		return new InvalidRealizability(name, rename(realizability.getCounterexample()),
				realizability.getRuntime());
	}

	/**
	 * Rename signals in a counterexample, possibly omitting some
	 * 
	 * @param cex
	 *            Counterexample to be renamed
	 * @return Renamed version of the counterexample
	 */
	private Counterexample rename(Counterexample cex) {
		if (cex == null) {
			return null;
		}
		
		Counterexample result = new Counterexample(cex.getLength());
		for (Signal<Value> signal : cex.getSignals()) {
			Signal<Value> newSignal = rename(signal);
			if (newSignal != null) {
				result.addSignal(newSignal);
			}
		}
		return result;
	}

	/**
	 * Rename signal
	 * @param <T>
	 * 
	 * @param signal
	 *            The signal to be renamed
	 * @return Renamed version of the signal or <code>null</code> if there is no
	 *         renaming for it
	 */
	private <T extends Value> Signal<T> rename(Signal<T> signal) {
		String name = rename(signal.getName());
		if (name == null) {
			return null;
		}
		
		Signal<T> newSignal = new Signal<>(name); 
		for (Entry<Integer, T> entry : signal.getValues().entrySet()) {
			newSignal.putValue(entry.getKey(), entry.getValue());
		}
		return newSignal;
	}
}
