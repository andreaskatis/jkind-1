package jkindreal.processes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.invariant.Invariant;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.solvers.Model;
import jkind.solvers.NumericValue;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnsatResult;
import jkind.translation.Keywords;
import jkind.translation.Specification;
import jkind.util.SexpUtil;
import jkindreal.processes.BaseProcessReal;
import jkindreal.processes.DirectorReal;
import jkindreal.processes.messages.BaseStepMessage;
import jkindreal.processes.messages.InductiveCounterexampleMessageReal;
import jkindreal.processes.messages.InvalidRealizabilityMessage;
import jkindreal.processes.messages.MessageReal;
import jkindreal.processes.messages.UnknownMessageReal;
import jkindreal.processes.messages.ValidRealizabilityMessage;

public class InductiveProcessReal extends ProcessReal {
	private int kLimit = 0;
	private BaseProcessReal baseProcess;
	private List<Invariant> invariants = new ArrayList<>();
	//private ReduceProcess reduceProcess;

	public InductiveProcessReal(Specification spec, JRealizabilitySettings settings, DirectorReal realDirector) {
		super("Inductive", spec, settings, realDirector);
	}

	public void setBaseProcess(BaseProcessReal baseProcess2) {
		this.baseProcess = baseProcess2;
	}

	@Override
	public void main() {
		int k;
		for (k = 0; k <= settings.n; k++) {
			debug("K = " + k);
			processMessagesAndWait(k);
			assertTransitionAndInvariants(k);
			if (!realizabilities.isEmpty()) {
				checkRealizabilities(k);
			}
			if (realizabilities.isEmpty()) {
				break;
			}
		}
		//sendStop();
	}

	@Override
	protected void initializeSolver() {
		super.initializeSolver();
		declareN();
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
					properties.removeAll(unknownMessage.unknown);
				} else {
					throw new JKindException("Unknown message type in inductive process: "
							+ message.getClass().getCanonicalName());
				}
			}
		} catch (InterruptedException e) {
			throw new JKindException("Interrupted while waiting for message", e);
		}
	}

	private void assertInvariants(List<Invariant> invariants, int i) {
		for (Invariant invariant : invariants) {
			assertInvariant(invariant, i);
		}
	}

	private void assertInvariant(Invariant invariant, int i) {
		solver.send(new Cons("assert", invariant.instantiate(getIndex(i))));
	}

	private void assertTransitionAndInvariants(int offset) {
		solver.send(new Cons("assert", new Cons(Keywords.T, getIndex(offset))));
		assertInvariants(invariants, offset);
	}
	
	private void assertRealizability(int k) {
		solver.send(new Cons("assert", new Cons("and",new Cons(Keywords.T, getIndex(k)), SexpUtil.conjoinStreams(spec.node.properties, getIndex(k)))));
	}
	
	
	private void checkRealizabilities(int k) {
		List<String> possiblyValid = new ArrayList<>(realizabilities);
		//List<List<String>> invalids = new ArrayList<>();
		
		//supports one realizability command for now.
		while (!possiblyValid.isEmpty()) {
			Result result = solver.realizability_query(spec.translation.getOutputSet(), getIndex(k));

			if (result instanceof SatResult) {
				Model model = ((SatResult) result).getModel();
				BigInteger n = getN(model);
				//Iterator<List<String>> iterator = possiblyValid.iterator();
				//while (iterator.hasNext()) {
					//List<String> p = iterator.next();
					sendInductiveCounterexample_Realizability(possiblyValid.toString(), n, k +1, model);
					//iterator.remove();
					//invalids.add(possiblyValid.get(0));
					//sendInvalidRealizability(invalids, k);
					//possiblyValid.remove(0);
				//}
			} else if (result instanceof UnsatResult) {
				//realizabilities.removeAll(possiblyValid);
				realizabilities.remove(0);
				assertRealizability(k);
				sendValidRealizability(possiblyValid, k);
				return;
			}
		}
	}
	
	private BigInteger getN(Model model) {
		NumericValue value = (NumericValue) model.getValue(Keywords.N);
		return new BigInteger(value.toString());
	}

	private Sexp getIndex(int offset) {
		return new Cons("+", Keywords.N, Sexp.fromInt(offset));
	}

	private void sendValidRealizability(List<String> valid, int k) {
		baseProcess.incoming.add(new ValidRealizabilityMessage(valid, k, invariants));
		
		//if (reduceProcess != null) {
			//reduceProcess.incoming.add(new ValidRealizabilityMessage(valid, k, invariants));
		//} else {
			director.incoming.add(new ValidRealizabilityMessage(valid, k, invariants));
		//}
	}

	private void sendInductiveCounterexample_Realizability(String realizability, BigInteger n, int k, Model model) {
		if (settings.inductiveCounterexamples) {
			director.incoming.add(new InductiveCounterexampleMessageReal(realizability, n, k, model));
		}
	}
	
	//private void sendStop() {
	//	if (reduceProcess != null) {
	//		reduceProcess.incoming.add(new StopMessage());
	//	}
	//}
}
