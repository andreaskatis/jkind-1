package jkindreal.processes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkindreal.processes.messages.BaseStepMessage;
import jkindreal.processes.messages.StopMessage;
import jkind.sexp.Sexp;
import jkind.sexp.Cons;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.solvers.smtlib2.SmtLib2Model;
import jkind.translation.Specification;
import jkind.util.StreamIndex;
import jkindreal.processes.messages.InvalidRealizabilityMessage;
import jkindreal.processes.messages.MessageReal;
import jkindreal.processes.messages.UnknownMessageReal;
import jkindreal.processes.messages.ValidRealizabilityMessage;

public class BaseProcessReal extends ProcessReal {
	private InductiveProcessReal inductiveProcess;
	private ProcessReal cexProcess;
	private List<String> validRealizabilities = new ArrayList<>();

	public BaseProcessReal(Specification spec, JRealizabilitySettings settings, DirectorReal director) {
		super("Base", spec, settings, director);
	}

	public void setInductiveProcess(InductiveProcessReal inductiveProcess) {
		this.inductiveProcess = inductiveProcess;
	}

	public void setCounterexampleProcess(ProcessReal cexProcess) {
		this.cexProcess = cexProcess;
	}

	@Override
	public void main() {
		createVariables(-1);
		for (int k = 0; k < settings.n; k++) {
			debug("K = " + (k + 1));
			processMessages();
			if (realizabilities.isEmpty()) {
				break;
			}
			createVariables(k);
			checkRealizabilities(k);
			assertRealizabilities(k);
		}
		sendStop();
	}

	private void processMessages() {
		while (!incoming.isEmpty()) {
			MessageReal message = incoming.poll();
			if (message instanceof ValidRealizabilityMessage) {
				ValidRealizabilityMessage validMessage = (ValidRealizabilityMessage) message;
				realizabilities.removeAll(validMessage.valid);
				validRealizabilities.addAll(validMessage.valid);
			} else {
				throw new JKindException("Unknown message type in base process: "
						+ message.getClass().getCanonicalName());
			}
		}
	}

	private void checkRealizabilities(int k) {
		Result result;
		do {
			result = solver.realizability_query(getInputs(k), getOutputs(k), getTransition(k, Sexp.fromBoolean(k == 0)), StreamIndex.conjoinEncodings(properties,  k));
			if (result instanceof SatResult) {
				SmtLib2Model model = (SmtLib2Model) ((SatResult) result).getModel();
				List<String> invalid = new ArrayList<>();
				Iterator<String> iterator = realizabilities.iterator();
				while (iterator.hasNext()) {
					String p = iterator.next();
					invalid.add(p);
					iterator.remove();
				}
				sendInvalid(invalid, k, model);
			} else if (result instanceof UnknownResult) {
				sendUnknown(realizabilities);
				realizabilities.clear();
			}
		} while (!realizabilities.isEmpty() && result instanceof SatResult);
		sendBaseStep(k);
	}

	private void sendInvalid(List<String> invalid, int k, SmtLib2Model model) {
		InvalidRealizabilityMessage im = new InvalidRealizabilityMessage(invalid, k + 1, model);
		if (cexProcess != null) {
			cexProcess.incoming.add(im);
		} else {
			director.incoming.add(im);
		}

		if (inductiveProcess != null) {
			inductiveProcess.incoming.add(im);
		}
	}

	private void sendBaseStep(int k) {
		BaseStepMessage bsm = new BaseStepMessage(k+1);
		director.incoming.add(bsm);
		if (inductiveProcess != null) {
			inductiveProcess.incoming.add(bsm);
		}
	}

	private void sendUnknown(List<String> unknown) {
		UnknownMessageReal um = new UnknownMessageReal(unknown);
		director.incoming.add(um);
		if (inductiveProcess != null) {
			inductiveProcess.incoming.add(um);
		}
	}
	
	private void assertRealizabilities(int k) {
		if (!realizabilities.isEmpty()) {
			solver.send(new Cons("assert", new Cons("and", getTransition(k, Sexp.fromBoolean(k == 0)), 
					StreamIndex.conjoinEncodings(properties, k))));
		}
		if (!(validRealizabilities.isEmpty())) {
			solver.send(new Cons("assert", new Cons("and", getTransition(k, Sexp.fromBoolean(k == 0)), 
					StreamIndex.conjoinEncodings(validRealizabilities, k))));
		}
	}

	private void sendStop() {
		if (cexProcess != null) {
			cexProcess.incoming.add(new StopMessage());
		}
	}
}