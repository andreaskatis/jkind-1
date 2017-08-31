package jkind.realizability.engines;

import jkind.JRealizabilitySettings;
import jkind.aeval.*;
import jkind.engines.StopException;
import jkind.lustre.Expr;
import jkind.lustre.LustreUtil;
import jkind.lustre.NamedType;
import jkind.lustre.VarDecl;
import jkind.realizability.engines.fixpoint.BlockedRegion;
import jkind.realizability.engines.messages.RealizableMessage;
import jkind.realizability.engines.messages.UnknownMessage;
import jkind.realizability.engines.messages.UnrealizableMessage;
import jkind.solvers.z3.Z3Solver;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Specification;
import jkind.util.StreamIndex;
import jkind.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RealizabilityFixpointEngine extends RealizabilityEngine {
    private AevalSolver aesolver;
//    private List<ValidSubset> subsets = new ArrayList<>();
    private List<BlockedRegion> regions = new ArrayList<>();
    private BlockedRegion region;


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
        createQueryVariables(aesolver, region, k);
//        createQueryVariables(aesolver, regions, k);
        //
        AevalResult aeresult = aesolver.realizabilityQuery(getAevalInductiveTransition(0),
                    StreamIndex.conjoinEncodings(spec.node.properties, 2));
        if (aeresult instanceof ValidResult) {
            //
            //trying z3's simplify
//            String[] result = ((ValidResult) aeresult).getSkolem().split("\\(assert");
//            String simplified = "(assert "+ solver.simplifyOutput(result[0] + "(simplify " + result[1]) + ")";
//            director.fixpointImplementation = new SkolemFunction(result[0] + simplified);
            director.fixpointImplementation = new SkolemFunction(((ValidResult) aeresult).getSkolem());
            sendRealizable(k);
            throw new StopException();
        } else if (aeresult instanceof InvalidResult){
            //testing z3's ctx-solver-simplify
            String result = ((InvalidResult) aeresult).getValidSubset();
            //testing z3's simplify
//            String result = ((InvalidResult) aeresult).getValidSubset().replace("assert", "simplify");
            //testing simplification of negation
            String simplified = "(assert (and\n"+ solver.simplifyOutput(null, result.replace("assert", "assert (not") + ")") + " true))";
//            String simplified = "(assert (and\n"+ solver.simplifyOutput(result) + " true))";
            ValidSubset subset = new ValidSubset(simplified);
//            ValidSubset subset = new ValidSubset(((InvalidResult) aeresult).getValidSubset());
            extractBlockedRegion(k, subset);
        } else if (aeresult instanceof UnknownResult){
            throw new StopException();
        }
    }

    private void extractBlockedRegion(int k, ValidSubset subset) {
        String simplified;
        aesolver = new AevalSolver(settings.filename, name + "subset"+ k, aevalscratch);
        aecomment(";Refinement = " + k);
        createSubQueryVariablesAndAssertions(aesolver, subset, k);
        AevalResult aeresult = aesolver.refinementQuery();
        if (aeresult instanceof ValidResult) {
            sendUnrealizable(k);
        } else if (aeresult instanceof InvalidResult){
            //testing ctx-solver-simplify
            String result = ((InvalidResult) aeresult).getValidSubset();
            //testing z3's simplify
//            String result = ((InvalidResult) aeresult).getValidSubset().replace("assert", "simplify");
            //testing simplification of negation
            if (region != null) {
                simplified = "(assert (and\n" + solver.simplifyOutput(region.getBlockedRegion(), result.replace("assert", "assert (not") + ")") + " true))";
            } else {
                simplified = "(assert (and\n" + solver.simplifyOutput(null, result.replace("assert", "assert (not") + ")") + " true))";
            }
//            String simplified = "(assert (and\n"+ solver.simplifyOutput(result) + " true))";
            ValidSubset toblock = new ValidSubset(simplified);
//            ValidSubset toblock = new ValidSubset(((InvalidResult) aeresult).getValidSubset());
            if ((toblock.getValidSubset().equals("Empty"))) {
                sendUnrealizable(k);
            } else {
//                String subsetassertion = toblock.getValidSubset().split("\\(assert")[1];
//                BlockedRegion region = new BlockedRegion("(assert (not" + subsetassertion + ")");
//
//                BlockedRegion region = new BlockedRegion(toblock.getValidSubset());
//                regions.add(region);
                region = new BlockedRegion(toblock.getValidSubset());
            }

        } else {
            sendUnknown();
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

    protected void createSubQueryVariablesAndAssertions(AevalSolver aesolver, ValidSubset subset, int k) {

        aesolver.defineTVar(spec.getFixpointTransitionRelation(), true);


        if (settings.scratch) {
            aesolver.scratch.println("; Universally quantified variables");
        }

        List<VarDecl> preinvars = getOffsetVarDecls(-1, getRealizabilityInputVarDecls());
        List<VarDecl> preoutvars = getOffsetVarDecls(-1, getRealizabilityOutputVarDecls());


        for (VarDecl in : preinvars) {
            aesolver.defineTVar(in, false);
            aesolver.defineSVar(in);
        }

        for (VarDecl out : preoutvars) {
            aesolver.defineSVar(out);
            aesolver.defineTVar(out, false);
        }
        //testing locals on the left
//        List<VarDecl> offsetoutvars = getOffsetVarDecls(0, getLocalVarDecls());
//        offsetoutvars.addAll(getOffsetVarDecls(0, getOutputVarDecls()));
//
//        for (VarDecl out : offsetoutvars) {
//            aesolver.defineSVar(out);
//            aesolver.defineTVar(out, false);
//        }

        if (settings.scratch) {
            aesolver.scratch.println("; Existentially quantified variables");
        }

        aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL), true);


        List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls());
        List<VarDecl> curroutvars = getOffsetVarDecls(0, getRealizabilityOutputVarDecls());
        //testing locals on the left
//        List<VarDecl> curroutvars = getOffsetVarDecls(0, getRealizabilityTrueOutputVarDecls());


        for (VarDecl in : currinvars) {
            aesolver.defineTVar(in, true);
        }

        for (VarDecl out : curroutvars) {
            aesolver.defineTVar(out, true);
        }


        if (settings.scratch) {
            aesolver.scratch.println("; Constraints for universal part of the formula");
        }

        for (VarDecl vd : Util.getVarDecls(spec.node)) {
            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
            if (constraint != null) {
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(-1)));
            }
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for universal part of the formula");
        }


//        for (BlockedRegion r : regions) {
//            aesolver.sendBlockedRegionSPart(r.getBlockedRegion());
//        }
        if (region !=null) {
            aesolver.sendBlockedRegionSPart(region.getBlockedRegion());
        }
        aesolver.assertSPart(getUniversalInputVariablesAssertion(-1));
        aesolver.assertSPart(getUniversalOutputVariablesAssertion(-1));
        //testing locals on the left
        //aesolver.assertSPart(getUniversalCurrLocalsAssertion(0));

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for existential part of the formula");
        }
        aesolver.assertTPart(getTransition(0, INIT), true);
        aesolver.assertTPart(getNextStepAssertions(), true);

        if (settings.scratch) {
            aesolver.scratch.println("; Constraints for existential part of the formula");
        }



        for (VarDecl vd : Util.getVarDecls(spec.node)) {
            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
            if (constraint != null) {
                aesolver.assertTPart(constraint.accept(new Lustre2Sexp(0)), true);
            }
        }

        //String subsetassertion = (subset.getValidSubset()).split("\\(assert")[1];
        //String negatedsubset = "(assert (not" + subsetassertion + ")";
        //aesolver.sendSubsetTPart(negatedsubset);

        //testing simplification of negation
        aesolver.sendSubsetTPart(subset.getValidSubset());

//        for (BlockedRegion r : regions) {
//            String newregion = convertOutputsToNextStep(r.getBlockedRegion(),
//                    preoutvars, true);
//            aesolver.sendBlockedRegionTPart(newregion);
//        }
        if (region != null) {
            aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getBlockedRegion(),
                    preoutvars, true));
        }
    }

//    protected void createQueryVariables(AevalSolver aesolver, List<BlockedRegion> regions, int k) {
    protected void createQueryVariables(AevalSolver aesolver, BlockedRegion region, int k) {
        if (settings.scratch) {
            aesolver.scratch.println("; Transition relation");
        }

        aesolver.defineSVar(spec.getFixpointTransitionRelation());
        aesolver.defineTVar(spec.getFixpointTransitionRelation(), false);


        if (settings.scratch) {
            aesolver.scratch.println("; Universally quantified variables");
        }

        aesolver.defineSVar(new VarDecl(INIT.str, NamedType.BOOL));
        aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL), false);

        List<VarDecl> dummyoutvars = getOffsetVarDecls(0, getRealizabilityOutputVarDecls());
        List<VarDecl> preinvars = getOffsetVarDecls(-1, getRealizabilityInputVarDecls());
        List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls());
        //testing locals on the left
//        List<VarDecl> offsetoutvars = getOffsetVarDecls(0, getLocalVarDecls());
//        offsetoutvars.addAll(getOffsetVarDecls(0, getOutputVarDecls()));
//        offsetoutvars.addAll(getOffsetVarDecls(2, getRealizabilityTrueOutputVarDecls()));
        List<VarDecl> offsetoutvars = getOffsetVarDecls(2, getRealizabilityOutputVarDecls());

        for (VarDecl vd : dummyoutvars) {
            aesolver.defineSVar(vd);
        }

        for (VarDecl in : preinvars) {
            aesolver.defineSVar(in);
            aesolver.defineTVar(in, false);
        }

        List<VarDecl> preoutvars = getOffsetVarDecls(-1, getRealizabilityOutputVarDecls());

        for (VarDecl vd : preoutvars) {
            aesolver.defineSVar(vd);
            aesolver.defineTVar(vd, false);
        }

        for (VarDecl in : currinvars) {
            aesolver.defineSVar(in);
            aesolver.defineTVar(in, false);
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Existentially quantified variables");
        }
        for (VarDecl out : offsetoutvars) {
            aesolver.defineTVar(out, true);
        }


        if (settings.scratch) {
            aesolver.scratch.println("; Assertion for T - universal part");
        }

        aesolver.assertSPart(getTransition(0, INIT));

        if (settings.scratch) {
            aesolver.scratch.println("; Constraints for universal part of the formula");
        }

        aesolver.assertSPart(getNextStepAssertions());

        for (VarDecl vd : getRealizabilityOutputVarDecls()) {
            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
            if (constraint != null) {
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(-1)));
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(0)));
            }
        }

        for (VarDecl vd : getRealizabilityInputVarDecls()) {
            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
            if (constraint != null) {
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(-1)));
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(0)));
            }
        }

        //aesolver.assertSPart(getUniversalVariablesAssertion());

        if (regions != null) {
            if (settings.scratch) {
                aesolver.scratch.println("; Assertions for blocked regions - universal part of the formula");
            }

//            for (BlockedRegion r : regions) {
//                aesolver.sendBlockedRegionSPart(r.getBlockedRegion());
//            }
            if (region != null) {
                aesolver.sendBlockedRegionSPart(region.getBlockedRegion());
            }
//            for (BlockedRegion r : regions) {
//                String newregion = convertOutputsToNextStep(r.getBlockedRegion(),
//                        preoutvars,true);
//                aesolver.sendBlockedRegionSPart(newregion);
//            }
            if (region != null) {
                aesolver.sendBlockedRegionSPart(convertOutputsToNextStep(region.getBlockedRegion(),
                        preoutvars, true));
            }
            if (settings.scratch) {
                aesolver.scratch.println("; Constraints for existential part of the formula");
            }

            for (VarDecl vd : getRealizabilityOutputVarDecls()) {
                Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
                if (constraint != null) {
                    aesolver.assertTPart(constraint.accept(new Lustre2Sexp(2)), true);
                }
            }

            if (settings.scratch) {
                aesolver.scratch.println("; Assertions for blocked regions - existential part of the formula");
            }

//            for (BlockedRegion r : regions) {
//                String newregion = convertOutputsToNextStep(r.getBlockedRegion(),
//                        getOffsetVarDecls(-1, getRealizabilityOutputVarDecls()),  false);
//                        //testing locals on the left
////                        getOffsetVarDecls(-1, getRealizabilityTrueOutputVarDecls()),  false);
//                aesolver.sendBlockedRegionTPart(newregion);
//            }
            if (region !=null) {
                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getBlockedRegion(),
                        getOffsetVarDecls(-1, getRealizabilityOutputVarDecls()), false));
            }
        }
    }

    protected String convertOutputsToNextStep(String region, List<VarDecl> offsetVarDecls, boolean lhs) {
        String converted = region;
        if (lhs) {
            converted = converted.replaceAll("\\$~1", "\\$0");
        } else {
            for (VarDecl off : offsetVarDecls) {
                String varid = off.id;
                if (converted.contains(varid)) {
                    String newvarid = varid.replaceAll("\\$~1", "\\$2");
                    converted = converted.replace(varid, newvarid);
                }
            }
        }
        return converted;
    }

    private void sendRealizable(int k) {
        RealizableMessage rm = new RealizableMessage(k);
        director.incoming.add(rm);
    }

    private void sendUnrealizable(int k) {
        sendUnrealizable(k, Collections.emptyList());
    }

    private void sendUnrealizable(int k, List<String> conflicts) {
        UnrealizableMessage im = new UnrealizableMessage(k + 1, null, conflicts);
        director.incoming.add(im);
    }

    private void sendUnknown() {
        UnknownMessage um = new UnknownMessage();
        director.incoming.add(um);
    }
}