package jkindreal.processes;

import java.util.ArrayList;
import java.util.List;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.invariant.Invariant;
import jkindreal.processes.messages.BaseStepMessage;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.solvers.Model;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.solvers.UnsatResult;
import jkind.translation.Specification;
import jkind.util.StreamIndex;
import jkindreal.processes.messages.InductiveCounterexampleMessageReal;
import jkindreal.processes.messages.InvalidRealizabilityMessage;
import jkindreal.processes.messages.MessageReal;
import jkindreal.processes.messages.UnknownMessageReal;
import jkindreal.processes.messages.ValidRealizabilityMessage;

public class InductiveProcessReal extends ProcessReal {
	private int kLimit = 0;
	private BaseProcessReal baseProcess;
	private List<Invariant> invariants = new ArrayList<>();

	public InductiveProcessReal(Specification spec, JRealizabilitySettings settings, DirectorReal director) {
		super("Inductive", spec, settings, director);
	}

	public void setBaseProcess(BaseProcessReal baseProcess) {
		this.baseProcess = baseProcess;
	}

	@Override
	public void main() {
		createVariables(-1);
		for (int k = 0; k <= settings.n; k++) {
			debug("K = " + k);
			processMessagesAndWait(k);
			createVariables(k);
			checkRealizabilities(k);
			if (realizabilities.isEmpty()) {
				break;
			}
			assertRealizabilities(k);
		}
	}

	private void processMessagesAndWait(int k) {
		try {
			while (!incoming.isEmpty() || k > kLimit) {
				MessageReal message = incoming.take();
				if (message instanceof InvalidRealizabilityMessage) {
					InvalidRealizabilityMessage invalidMessage = (InvalidRealizabilityMessage) message;
					realizabilities.removeAll(invalidMessage.invalid);
				} else if (message instanceof BaseStepMessage) {
					BaseStepMessage baseStepMessage = (BaseStepMessage) message;
					kLimit = baseStepMessage.step;
				} else if (message instanceof UnknownMessageReal) {
					UnknownMessageReal unknownMessage = (UnknownMessageReal) message;
					realizabilities.removeAll(unknownMessage.unknown);
				} else {
					throw new JKindException("Unknown message type in inductive process: "
							+ message.getClass().getCanonicalName());
				}
			}
		} catch (InterruptedException e) {
			throw new JKindException("Interrupted while waiting for message", e);
		}
	}


	private void checkRealizabilities(int k) {
		List<String> reals = new ArrayList<>(realizabilities);
		//supports one realizability command for now.
		while (!reals.isEmpty()) {
			Result result = solver.realizability_query(getInputs(k), getOutputs(k), getTransition(k, INIT), StreamIndex.conjoinEncodings(properties,  k));
			
			if (result instanceof SatResult) {
				Model model = ((SatResult) result).getModel();
				sendInductiveCounterexample(reals.get(0), k +1, model);
				reals.remove(0);
			} else if (result instanceof UnsatResult) {
				realizabilities.remove(0);
				sendValidRealizability(reals, k);
				return;
			} else if (result instanceof UnknownResult) {
				realizabilities.remove(0);
				return;
			}
		}
	}
	
	private void assertRealizabilities(int k) {
		if (!realizabilities.isEmpty()) {
			solver.send(new Cons("assert", new Cons("and", getTransition(k, Sexp.fromBoolean(k == 0)), 
					StreamIndex.conjoinEncodings(properties, k))));
		}
	}
	
	private void sendValidRealizability(List<String> valid, int k) {
		baseProcess.incoming.add(new ValidRealizabilityMessage(valid, k, invariants));
		director.incoming.add(new ValidRealizabilityMessage(valid, k, invariants));
	}

	private void sendInductiveCounterexample(String r, int k, Model model) {
		if (settings.inductiveCounterexamples) {
			director.incoming.add(new InductiveCounterexampleMessageReal(r, k, model));
		}
	}
}