package jkindreal.processes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkindreal.processes.messages.BaseStepMessage;
import jkindreal.processes.messages.InvalidRealizabilityMessage;
import jkindreal.processes.messages.MessageReal;
import jkindreal.processes.messages.StopMessage;
import jkindreal.processes.messages.UnknownMessageReal;
import jkindreal.processes.messages.ValidRealizabilityMessage;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.solvers.Model;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.translation.Keywords;
import jkind.translation.Specification;
import jkind.util.SexpUtil;

public class BaseProcessReal extends ProcessReal {
	private InductiveProcessReal inductiveProcess;
	private ProcessReal cexProcess;

	public BaseProcessReal(Specification spec, JRealizabilitySettings settings, DirectorReal realDirector) {
		super("Base", spec, settings, realDirector);
	}

	public void setInductiveProcess(InductiveProcessReal inductiveProcess) {
		this.inductiveProcess = inductiveProcess;
	}

	public void setCounterexampleProcess(ProcessReal cexProcess) {
		this.cexProcess = cexProcess;
	}

	@Override
	public void main() {
		int k;
		for (k = 1; k <= settings.n - 1; k++) {
			debug("K = " + k);
			processMessages();
			if (realizabilities.isEmpty()) {
				break;
			}
			assertTransition(k);
			if (!realizabilities.isEmpty()) {
				checkRealizabilities(k);
				assertRealizabilities(k);
			}
		}
		sendStop();
	}

	private void processMessages() {
		while (!incoming.isEmpty()) {
			MessageReal message = incoming.poll();
			if (message instanceof ValidRealizabilityMessage) {
				ValidRealizabilityMessage validrealizabilityMessage = (ValidRealizabilityMessage) message;
				realizabilities.removeAll(validrealizabilityMessage.valid);
			} else {
				throw new JKindException("Unknown message type in base process: "
						+ message.getClass().getCanonicalName());
			}
		}
	}

	private void assertTransition(int k) {
		solver.send(new Cons("assert", new Cons(Keywords.T, Sexp.fromInt(k - 1))));
	}
	/* 
	private void assertTransition_prime(int k) {
		solver.send(new Cons("assert", new Cons(Keywords.T_prime, Sexp.fromInt(k - 1))));
	}*/
	
	
	private void checkRealizabilities(int k) {
		Result result;
		do {
			result = solver.realizability_query(spec.translation.getOutputSet(), Sexp.fromInt(k-1));
			if (result instanceof SatResult) {
				Model model = ((SatResult) result).getModel();
				List<String> invalid = new ArrayList<>();
				Iterator<String> iterator = realizabilities.iterator();
				while (iterator.hasNext()) {
					String p = iterator.next();
					invalid.add(p);
					iterator.remove();
				}
				sendInvalidRealizability(invalid, k, model);
			} else if (result instanceof UnknownResult) {
				sendUnknown(realizabilities);
				realizabilities.clear();
			}
			
		} while (!realizabilities.isEmpty() && result instanceof SatResult);

		sendBaseStep(k);
	}

	private void sendInvalidRealizability(List<String> invalid, int k, Model model) {
		InvalidRealizabilityMessage im = new InvalidRealizabilityMessage(invalid, k, model);
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
		if (inductiveProcess != null) {
			inductiveProcess.incoming.add(new BaseStepMessage(k));
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
			solver.send(new Cons("assert", new Cons("and",new Cons(Keywords.T, Sexp.fromInt(k - 1)), 
					SexpUtil.conjoinStreams(spec.node.properties, Sexp.fromInt(k - 1)))));
		}
	}
	
	private void sendStop() {
		if (cexProcess != null) {
			cexProcess.incoming.add(new StopMessage());
		}
	}

}
