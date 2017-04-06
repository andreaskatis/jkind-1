package jkind.realizability.engines;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.aeval.*;
import jkind.engines.StopException;
import jkind.lustre.Expr;
import jkind.lustre.LustreUtil;
import jkind.lustre.NamedType;
import jkind.lustre.VarDecl;
import jkind.realizability.engines.fixpoint.BlockedRegion;
import jkind.realizability.engines.messages.RealizableMessage;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Specification;
import jkind.util.StreamIndex;
import jkind.util.Util;

import java.util.ArrayList;
import java.util.List;

public class RealizabilityFixpointEngine extends RealizabilityEngine {
    private AevalSolver aesolver;
//    private List<ValidSubset> subsets = new ArrayList<>();
    private List<BlockedRegion> regions = new ArrayList<>();


    public RealizabilityFixpointEngine(Specification spec, JRealizabilitySettings settings,
                                   RealizabilityDirector director) {
        super("fixpoint", spec, settings, director);
    }

    @Override
    public void main() {
        try {
            for (int k = 0; k < settings.n; k++) {
                comment("K = " + (k + 1));
                //checkConsistency(k);
                checkRealizable(k);
            }
        } catch (StopException se) {
        }
    }

    private void checkRealizable(int k) {
        aesolver = new AevalSolver(settings.filename, name + k, aevalscratch);
        aecomment("; Frame = " + (k));
        createQueryVariables(aesolver, regions, -1);
        AevalResult aeresult = aesolver.realizabilityQuery(getAevalFixpointTransition(),
                    StreamIndex.conjoinEncodings(spec.node.properties, 2));
        //Transition without curr state assertions
//        AevalResult aeresult = aesolver.realizabilityQuery(getAevalFixpointTransition(),
//                    StreamIndex.conjoinEncodings(spec.node.properties, 3));
        if (aeresult instanceof ValidResult) {
            director.fixpointImplementation = new SkolemFunction(((ValidResult) aeresult).getSkolem());
            sendRealizable(k);
            throw new StopException();
        } else if (aeresult instanceof InvalidResult){
            ValidSubset subset = new ValidSubset(((InvalidResult) aeresult).getValidSubset());
//            subsets.add(subset);
            BlockedRegion region = extractBlockedRegion(k, subset);
            regions.add(region);
            //TODO: Run Z3 to get cex here instead.
        } else if (aeresult instanceof UnknownResult){
            throw new StopException();
        }
    }

    private BlockedRegion extractBlockedRegion(int k, ValidSubset subset){
        aesolver = new AevalSolver(settings.filename, name + "subset"+ k, aevalscratch);
        aecomment(";Refinement = " + k);
        createSubQueryVariablesAndAssertions(aesolver, subset);
        AevalResult aeresult = aesolver.refinementQuery();
        if (aeresult instanceof ValidResult) {
            //TODO: Run Z3 to get cex here instead.
            throw new JKindException("Unrealizable. Use realizability check for cex.");
        } else if (aeresult instanceof InvalidResult){
            ValidSubset toblock = new ValidSubset(((InvalidResult) aeresult).getValidSubset());
            String subsetassertion = toblock.getValidSubset().split("assert")[1];
            BlockedRegion region = new BlockedRegion("(assert (not" + subsetassertion + ")");
            return region;
        } else {
            throw new JKindException("Unknown");
        }
    }

//    private void reduceAndSendUnrealizable(int k, Model model) {
//        Sexp realizabilityOutputs = getRealizabilityOutputs(k);
//        Sexp transition = getTransition(k, k == 0);
//        List<String> conflicts = new ArrayList<>(spec.node.properties);
//
//        for (String curr : spec.node.properties) {
//            conflicts.remove(curr);
//            Result result = solver.realizabilityQuery(realizabilityOutputs, transition,
//                    StreamIndex.conjoinEncodings(conflicts, k), REDUCE_TIMEOUT_MS);
//
//            if (result instanceof SatResult) {
//                model = ((SatResult) result).getModel();
//            } else {
//                conflicts.add(curr);
//            }
//        }
//
//        sendUnrealizable(k, model, conflicts);
//    }

    protected void createSubQueryVariablesAndAssertions(AevalSolver aesolver, ValidSubset subset) {
        if (settings.scratch) {
            aesolver.scratch.println("; Transition relation");
        }
        aesolver.defineTVar(spec.getFixpointTransitionRelation(), true);

        if (settings.scratch) {
            aesolver.scratch.println("; Universally quantified variables");
        }

        List<VarDecl> preoutvars = getOffsetVarDecls(-1, getRealizabilityOutputVarDecls());

        for (VarDecl out : preoutvars) {
            aesolver.defineSVar(out);
            aesolver.defineTVar(out, false);
        }

        List<VarDecl> curroutvars = getOffsetVarDecls(0, getRealizabilityOutputVarDecls());

        for (VarDecl out : curroutvars) {
            aesolver.defineSVar(out);
            aesolver.defineTVar(out, false);
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Existentially quantified variables");
        }

        aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL), true);

        List<VarDecl> preinvars = getOffsetVarDecls(-1, getRealizabilityInputVarDecls());
        List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls());
//        invars.addAll(getOffsetVarDecls(1, getRealizabilityInputVarDecls()));


        for (VarDecl in : preinvars) {
            aesolver.defineTVar(in, true);
        }

        for (VarDecl in : currinvars) {
            aesolver.defineTVar(in, true);
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for universal part of the formula");
        }


//      It appears that the blocked regions are exact. That is, the first blocked region
//      contains all the bad states in its space. Thus, a refinement query on the intersection
//      of true and the negated first region will return a "false" valid subset.

//        for (BlockedRegion r : regions) {
//            aesolver.sendBlockedRegionSPart(r.getBlockedRegion());
//        }

        aesolver.assertSPart(getUniversalOutputVariablesAssertion(-1));
        aesolver.assertSPart(getUniversalOutputVariablesAssertion(0));

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for existential part of the formula");
        }

        aesolver.assertTPart(getTransition(0, INIT), true);
//        aesolver.assertTPart(StreamIndex.conjoinEncodings(spec.node.properties, 1), true);
//        aesolver.assertTPart(getAssertions(), true);
//        aesolver.assertTPart(getNextStepAssertions(), true);

//        for (ValidSubset s : subsets) {
//            String subsetassertion = s.getValidSubset().split("assert")[1];
//            String negatedsubset = "(assert (not" + subsetassertion + ")";
//            aesolver.sendSubsetTPart(negatedsubset);
//        }


//        String subsetassertion = (subset.getValidSubset()).replaceAll("\\$1", "\\$0").split("assert")[1];
        String subsetassertion = (subset.getValidSubset()).split("assert")[1];
        String negatedsubset = "(assert (not" + subsetassertion + ")";
        aesolver.sendSubsetTPart(negatedsubset);
    }

    protected void createQueryVariables(AevalSolver aesolver, List<BlockedRegion> regions, int k) {
        if (settings.scratch) {
            aesolver.scratch.println("; Transition relation");
        }
        aesolver.defineSVar(spec.getFixpointTransitionRelation());
        aesolver.defineTVar(spec.getFixpointTransitionRelation(), true);

        if (settings.scratch) {
            aesolver.scratch.println("; Universally quantified variables");
        }

        aesolver.defineSVar(new VarDecl(INIT.str, NamedType.BOOL));
        aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL), false);

        List<VarDecl> preinvars = getOffsetVarDecls(k, getRealizabilityInputVarDecls());
        List<VarDecl> currinvars = getOffsetVarDecls(k+1, getRealizabilityInputVarDecls());
        List<VarDecl> offsetoutvars = getOffsetVarDecls(
                k+3, getRealizabilityOutputVarDecls());
        for (VarDecl in : preinvars) {
            aesolver.defineSVar(in);
            aesolver.defineTVar(in, false);
        }

        List<VarDecl> preoutvars = getOffsetVarDecls(k, getRealizabilityOutputVarDecls());
        for (VarDecl vd : preoutvars) {
            aesolver.defineSVar(vd);
            aesolver.defineTVar(vd, false);
        }

        for (VarDecl in : currinvars) {
            aesolver.defineSVar(in);
            aesolver.defineTVar(in, false);
        }

        List<VarDecl> curroutvars = getOffsetVarDecls(k+1, getRealizabilityOutputVarDecls());
        for (VarDecl vd : curroutvars) {
            aesolver.defineSVar(vd);
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Existentially quantified variables");
        }
        for (VarDecl out : offsetoutvars) {
            aesolver.defineTVar(out, true);
        }

        for (VarDecl vd : Util.getVarDecls(spec.node)) {
            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
            if (constraint != null) {
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(k)));
            }
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for universal part of the formula");
        }

        aesolver.assertSPart(getTransition(0, INIT));
//        aesolver.assertSPart(getAssertions());
//        aesolver.assertSPart(getUniversalVariablesAssertion());

        if (regions != null) {
            if (settings.scratch) {
                aesolver.scratch.println("; Assertions for blocked regions - universal part of the formula");
            }

            for (BlockedRegion r : regions) {
                aesolver.sendBlockedRegionSPart(r.getBlockedRegion());
            }
            if (settings.scratch) {
                aesolver.scratch.println("; Assertions for blocked regions - existential part of the formula");
            }


            for (BlockedRegion r : regions) {
                String newregion = convertOutputsToNextStep(r.getBlockedRegion());
                aesolver.sendBlockedRegionTPart(newregion);
            }
        }
    }

    protected String convertOutputsToNextStep(String region) {
        String convertedone = region.replaceAll("\\$0", "\\$2");
        //String convertedone = convertedzero.replaceAll("\\$1", "\\$3");
        return convertedone;
    }

    private void sendRealizable(int k) {
        RealizableMessage rm = new RealizableMessage(k);
        director.incoming.add(rm);
    }
}
