package jkind.api.results;

import jkind.results.InvalidRealizability;
import jkind.results.Realizability;
import jkind.results.UnknownRealizability;
import jkind.results.ValidRealizability;

public class RealizabilityResult extends AnalysisResult {
	private int elapsed;
	private Status status;
	private Realizability realizability;
	private final Renaming renaming;
	private boolean invertStatus = false;

	public RealizabilityResult(String name, Renaming renaming, boolean invertStatus) {
		this(name, renaming);
		this.invertStatus = invertStatus;
	}

	public RealizabilityResult(String name, Renaming renaming) {
		super(name);
		this.elapsed = 0;
		this.status = Status.WAITING;
		this.realizability = null;
		this.renaming = renaming;
	}

	public int getElapsed() {
		return elapsed;
	}

	public Status getStatus() {
		return status;
	}

	public Realizability getRealizability() {
		return realizability;
	}

	public void setRealizability(Realizability original) {
		if (renaming == null) {
			realizability = original;
		} else {
			realizability = renaming.rename(original);
		}
		if (realizability instanceof ValidRealizability) {
			setStatus(invertStatus ? Status.INVALID : Status.VALID);
		} else if (realizability instanceof InvalidRealizability) {
			setStatus(invertStatus ? Status.VALID : Status.INVALID);
		} else if (realizability instanceof UnknownRealizability) {
			setStatus(Status.UNKNOWN);
		}
	}

	public void start() {
		setStatus(Status.WORKING);
	}

	public void tick() {
		if (status == Status.WORKING) {
			pcs.firePropertyChange("elapsed", elapsed, ++elapsed);
		}
	}

	public void cancel() {
		if (status == Status.WORKING || status == Status.WAITING) {
			setStatus(Status.CANCELED);
		}
	}

	public void done() {
		if (status == Status.WORKING || status == Status.WAITING) {
			setStatus(Status.ERROR);
		}
	}

	private void setStatus(Status status) {
		pcs.firePropertyChange("status", this.status, this.status = status);
	}

	@Override
	public String toString() {
		return name + " - " + status;
	}
}
