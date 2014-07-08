package jkind.api.results;

import jkind.api.results.JKindResultRealizability;

public class TickerReal extends Thread {
	private JKindResultRealizability result;
	private boolean done;

	public TickerReal(JKindResultRealizability result) {
		super("TickerReal");
		this.result = result;
		this.done = false;
	}

	@Override
	public void run() {
		try {
			while (!done) {
				Thread.sleep(1000);
				result.tick();
			}
		} catch (InterruptedException e) {
		}
	}

	public void done() {
		done = true;
	}
}
