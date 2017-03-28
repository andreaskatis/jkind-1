package jkind.realizability.engines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.aeval.*;
import jkind.engines.StopException;
import jkind.realizability.engines.messages.BaseStepMessage;
import jkind.realizability.engines.messages.InconsistentMessage;
import jkind.realizability.engines.messages.Message;
import jkind.realizability.engines.messages.RealizableMessage;
import jkind.realizability.engines.messages.UnknownMessage;
import jkind.realizability.engines.messages.UnrealizableMessage;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.Model;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.solvers.UnsatResult;
import jkind.translation.Specification;
import jkind.util.StreamIndex;


public class RealizabilityBaseEngine extends RealizabilityEngine {
	private RealizabilityExtendEngine extendEngine;
	private static final int REDUCE_TIMEOUT_MS = 200;
	private AevalSolver aesolver;




	public RealizabilityBaseEngine(Specification spec, JRealizabilitySettings settings,
			RealizabilityDirector director) {
		super("base", spec, settings, director);
	}

	public void setExtendEngine(RealizabilityExtendEngine extendEngine) {
		this.extendEngine = extendEngine;
	}

	@Override
	public void main() {
		try {
			createVariables(-1);
			for (int k = 0; k < settings.n; k++) {
				comment("K = " + (k + 1));
				processMessages();
				createVariables(k);
				assertTransition(k);
				checkConsistency(k);
				checkRealizable(k);
				assertProperties(k);
			}
		} catch (StopException se) {
		}
	}

	private void processMessages() {
		while (!incoming.isEmpty()) {
			Message message = incoming.poll();
			if (message instanceof RealizableMessage) {
				throw new StopException();
			}
			throw new JKindException("Unknown message type in base process: "
					+ message.getClass().getCanonicalName());
		}
	}

	private void assertTransition(int k) {
		solver.assertSexp(getTransition(k, k == 0));
	}

	private void assertProperties(int k) {
		solver.assertSexp(StreamIndex.conjoinEncodings(spec.node.properties, k));
	}

	private void checkConsistency(int k) {
		Result result = solver.query(new Symbol("false"));
		if (result instanceof UnsatResult) {
			sendInconsistent(k);
			throw new StopException();
		}
	}

	private void checkRealizable(int k) {
//		Result result = solver.realizabilityQuery(getRealizabilityOutputs(k),
//				getTransition(k, k == 0), StreamIndex.conjoinEncodings(spec.node.properties, k));
//
//		if (result instanceof UnsatResult) {
//			sendBaseStep(k);
//			if (settings.synthesis) {
//				aesolver = new AevalSolver(settings.filename, name + k, aevalscratch);
////				aecomment("; K = " +  k);
//				aecomment("; K = " + (k + 1));
//				createAevalVariables(aesolver, k, name);
//				aesolver.assertSPart(getTransition(k, k == 0));
//				AevalResult aeresult = aesolver.realizabilityQuery(getAevalTransition(k, k == 0),
//						StreamIndex.conjoinEncodings(spec.node.properties, k + 2));
//				if (aeresult instanceof ValidResult) {
//					director.baseImplementation.add(new SkolemFunction(((ValidResult) aeresult).getSkolem()));
//				} else {
//					//case where Z3 result conflicts with AE-VAL
//					throw new JKindException("Conflicting results between Z3 and AE-VAL");
//				}
//			}
//		}
//
//		if (result instanceof SatResult) {
//			Model model = ((SatResult) result).getModel();
//			if (settings.reduce) {
//				reduceAndSendUnrealizable(k, model);
//			} else {
//				sendUnrealizable(k, model);
//			}
//		} else if (result instanceof UnknownResult) {
//			sendUnknown();
//		}

		//can simply say "do regular realizability check but if k!=0 && settings.synthesis,
		if (settings.synthesis) {

			//This already happens a level above with checkconsistency
//			if (k!=0) {
//				Result result = solver.realizabilityQuery(getRealizabilityOutputs(k),
//						getTransition(k, k == 0), StreamIndex.conjoinEncodings(spec.node.properties, k));
//				if (result instanceof UnsatResult) {
//					sendBaseStep(k);
//				}
//
//				if (result instanceof SatResult) {
//					Model model = ((SatResult) result).getModel();
//					if (settings.reduce) {
//						reduceAndSendUnrealizable(k, model);
//					} else {
//						sendUnrealizable(k, model);
//					}
//				} else if (result instanceof UnknownResult) {
//					sendUnknown();
//				}
//			}

			aesolver = new AevalSolver(settings.filename, name + k, aevalscratch);
//			aecomment("; K = " +  k);
			aecomment("; K = " + (k + 1));
			createAevalVariables(aesolver, k, name);
			aesolver.assertSPart(getTransition(k, k == 0));
			// assert input and state to ensure
			AevalResult aeresult = aesolver.realizabilityQuery(getAevalTransition(k, k == 0),
					StreamIndex.conjoinEncodings(spec.node.properties, k + 2));
			if (aeresult instanceof ValidResult) {
				sendBaseStep(k);
				director.baseImplementation.add(new SkolemFunction(((ValidResult) aeresult).getSkolem()));
			} else if (aeresult instanceof InvalidResult){
				//we can possibly run a Z3 check here instead, to get the counterexample.
				throw new JKindException("Unrealizable. Use realizability check for cex.");
			} else {
				throw new JKindException("Unknown");
			}
		} else {
			Result result = solver.realizabilityQuery(getRealizabilityOutputs(k),
					getTransition(k, k == 0), StreamIndex.conjoinEncodings(spec.node.properties, k));
			if (result instanceof UnsatResult) {
				sendBaseStep(k);
			}

			if (result instanceof SatResult) {
				Model model = ((SatResult) result).getModel();
				if (settings.reduce) {
					reduceAndSendUnrealizable(k, model);
				} else {
					sendUnrealizable(k, model);
				}
			} else if (result instanceof UnknownResult) {
				sendUnknown();
			}
		}
	}

	private void reduceAndSendUnrealizable(int k, Model model) {
		Sexp realizabilityOutputs = getRealizabilityOutputs(k);
		Sexp transition = getTransition(k, k == 0);
		List<String> conflicts = new ArrayList<>(spec.node.properties);

		for (String curr : spec.node.properties) {
			conflicts.remove(curr);
			Result result = solver.realizabilityQuery(realizabilityOutputs, transition,
					StreamIndex.conjoinEncodings(conflicts, k), REDUCE_TIMEOUT_MS);

			if (result instanceof SatResult) {
				model = ((SatResult) result).getModel();
			} else {
				conflicts.add(curr);
			}
		}

		sendUnrealizable(k, model, conflicts);
	}

	private void sendBaseStep(int k) {
		BaseStepMessage bsm = new BaseStepMessage(k + 1);
		director.incoming.add(bsm);
		extendEngine.incoming.add(bsm);
	}

	private void sendInconsistent(int k) {
		InconsistentMessage im = new InconsistentMessage(k + 1);
		director.incoming.add(im);
		extendEngine.incoming.add(im);
	}

	private void sendUnrealizable(int k, Model model) {
		sendUnrealizable(k, model, Collections.emptyList());
	}

	private void sendUnrealizable(int k, Model model, List<String> conflicts) {
		UnrealizableMessage im = new UnrealizableMessage(k + 1, model, conflicts);
		director.incoming.add(im);
		extendEngine.incoming.add(im);
	}

	private void sendUnknown() {
		UnknownMessage um = new UnknownMessage();
		director.incoming.add(um);
		extendEngine.incoming.add(um);
	}

}