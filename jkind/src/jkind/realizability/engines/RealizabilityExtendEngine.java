package jkind.realizability.engines;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.aeval.AevalResult;
import jkind.aeval.AevalSolver;
import jkind.aeval.SkolemRelation;
import jkind.aeval.ValidResult;
import jkind.engines.StopException;
import jkind.lustre.NamedType;
import jkind.lustre.VarDecl;
\import jkind.solvers.Model;
import jkind.realizability.engines.messages.BaseStepMessage;
import jkind.realizability.engines.messages.ExtendCounterexampleMessage;
import jkind.realizability.engines.messages.InconsistentMessage;
import jkind.realizability.engines.messages.Message;
import jkind.realizability.engines.messages.RealizableMessage;
import jkind.realizability.engines.messages.UnknownMessage;
import jkind.realizability.engines.messages.UnrealizableMessage;
import jkind.realizability.engines.messages.ExtendImplementationMessage;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.solvers.UnsatResult;
import jkind.translation.Specification;
import jkind.util.StreamIndex;

import java.util.ArrayList;

public class RealizabilityExtendEngine extends RealizabilityEngine {
	private int kLimit = 0;
	private RealizabilityBaseEngine baseEngine;
	private AevalSolver aesolver;


	public RealizabilityExtendEngine(Specification spec, JRealizabilitySettings settings,
			RealizabilityDirector director) {
		super("extend", spec, settings, director);
	}

	public void setBaseEngine(RealizabilityBaseEngine baseEngine) {
		this.baseEngine = baseEngine;
	}

	@Override
	protected void initializeSolver() {
		super.initializeSolver();
		solver.define(new VarDecl(INIT.str, NamedType.BOOL));
	}


	@Override
	public void main() {
		try {
			createVariables(-1);
			for (int k = 0; k <= settings.n; k++) {
				comment("K = " + k);
				processMessagesAndWait(k);
				createVariables(k);
				assertTransition(k);
				checkRealizabilities(k);
				assertProperties(k);
			}
		} catch (StopException se) {
		}
	}

	private void processMessagesAndWait(int k) {
		try {
			while (!incoming.isEmpty() || k > kLimit) {
				Message message = incoming.take();
				if (message instanceof UnrealizableMessage) {
					throw new StopException();
				} else if (message instanceof InconsistentMessage) {
					throw new StopException();
				} else if (message instanceof BaseStepMessage) {
					BaseStepMessage baseStepMessage = (BaseStepMessage) message;
					kLimit = baseStepMessage.step;
				} else if (message instanceof UnknownMessage) {
					throw new StopException();
				} else {
					throw new JKindException("Unknown message type in inductive process: "
							+ message.getClass().getCanonicalName());
				}
			}
		} catch (InterruptedException e) {
			throw new JKindException("Interrupted while waiting for message", e);
		}
	}

	private void assertTransition(int k) {
		solver.assertSexp(getInductiveTransition(k));
	}

	private void assertProperties(int k) {
		solver.assertSexp(StreamIndex.conjoinEncodings(spec.node.properties, k));
	}

	private void checkRealizabilities(int k) {
		Result result = solver.realizabilityQuery(getRealizabilityOutputs(k),
				getInductiveTransition(k), StreamIndex.conjoinEncodings(spec.node.properties, k));

		if (result instanceof UnsatResult) {
			sendRealizable(k);

			//Existential variables need different
			//naming due to AE-VAL's different variable
			//scope mechanism. Properties are part of the
			//outputs so these should be renamed as well.
			//New names can be derived if we simply use the
			//next value of k for this AE-VAL call.
			aesolver = new AevalSolver(settings.filename);
			aesolver.comment("K = " + (k + 1));
			createAevalVariables(aesolver, k);
			assertGuardandSkolVars(aesolver, k);
			AevalResult aeresult = aesolver.synthesize(getRealizabilityOutputs(k+1), getAevalTransition(k, k == 0),
					StreamIndex.conjoinEncodings(spec.node.properties, k+1));
			if (aeresult instanceof ValidResult) {
				implementation.add(((ValidResult) aeresult).getSkolem());
			} else {
				//case where Z3 result conflicts with AE-VAL
				//what should be happening?
				throw new StopException();
			}

			sendImplementation(implementation, k);

			throw new StopException();
		} else if (result instanceof SatResult) {
			Model model = ((SatResult) result).getModel();
			sendExtendCounterexample(k + 1, model);
		} else if (result instanceof UnknownResult) {
			throw new StopException();
		}
	}

	private void sendRealizable(int k) {
		RealizableMessage rm = new RealizableMessage(k);
		baseEngine.incoming.add(rm);
		director.incoming.add(rm);
	}

	private void sendExtendCounterexample(int k, Model model) {
		if (settings.extendCounterexample) {
			director.incoming.add(new ExtendCounterexampleMessage(k, model));
		}
	}

	private void sendImplementation(ArrayList<SkolemRelation> implementation, int k) {
		if (settings.synthesis) {
			director.incoming.add(new ExtendImplementationMessage(k, implementation));
		}
	}
}