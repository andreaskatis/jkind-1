package jkind.realizability.engines;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.aeval.*;
import jkind.engines.StopException;
import jkind.lustre.*;
import jkind.lustre.builders.NodeBuilder;
import jkind.realizability.engines.fixpoint.RefinedRegion;
import jkind.realizability.engines.messages.RealizableMessage;
import jkind.realizability.engines.messages.UnknownMessage;
import jkind.realizability.engines.messages.UnrealizableMessage;
import jkind.slicing.LustreSlicer;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Specification;
import jkind.util.StreamIndex;
import jkind.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public class RealizabilityFixpointEngine extends RealizabilityEngine {
    private AevalSolver aesolver;
    private RefinedRegion region;
    private ArrayList<String> skolems;
    private String preCondition;

    public RealizabilityFixpointEngine(Specification spec, JRealizabilitySettings settings,
                                   RealizabilityDirector director) {
        super("fixpoint", spec, settings, director);
        this.skolems = new ArrayList<>();
    }

    @Override
    public void main() {
        try {
            if (settings.fixpoint_T) {
                factorizeandcheckRealizable();
            } else {
                for (int k = 0; k < settings.n; k++) {
                    comment("K = " + (k + 1));
                    //checkConsistency(k);
//                    if (k == 0 && settings.fixpoint_T && spec.node.properties.size() > 1) {
//                        splitandcheckRealizable(k);
//                    }
                    checkRealizable(k);
                }
            }
        } catch (StopException se) {
        }
    }

    private void factorizeandcheckRealizable() {
        String factorPrecondition = "true";
        RefinedRegion region = new RefinedRegion("true");
        Map<VarDecl, Integer> factorMap = new LinkedHashMap<>();
        Map<String, String> skolemMap = new LinkedHashMap<>();
        List<Node> fnodes = new ArrayList<>();

        //create sliced nodes per property
        System.out.println(spec.node.properties);
        for (String prop : spec.node.properties) {
            Node fnode = new NodeBuilder(spec.node).clearProperties().addProperty(prop).build();
            fnode = LustreSlicer.slice(fnode, spec.dependencyMap);
            fnodes.add(fnode);
        }

        List<String> realizabilityInputs = spec.node.realizabilityInputs;
        List<VarDecl> realizabilityOutputs = Util.getVarDecls(spec.node);

        realizabilityOutputs.removeIf(vd -> realizabilityInputs.contains(vd.id));
        realizabilityOutputs.removeIf(vd -> spec.node.locals.contains(vd));

        //iterate through each sliced node. Identify the greatest index for which a specific output appears.
        for (VarDecl out : realizabilityOutputs) {
            String outname = out.id;
            for (Node fnode : fnodes) {
                List<String> noderealizabilityInputs = fnode.realizabilityInputs;
                List<VarDecl> noderealizabilityOutputs = Util.getVarDecls(fnode);
                noderealizabilityOutputs.removeIf(vd -> noderealizabilityInputs.contains(vd.id));
                noderealizabilityOutputs.removeIf(vd -> fnode.locals.contains(vd));
                for (VarDecl nout : noderealizabilityOutputs) {
                    if (nout.id.equals(outname)) {
                        factorMap.put(out, fnodes.indexOf(fnode));
                        break;
                    }
                }
            }
        }

        Map<VarDecl, Integer> orderedMap = factorMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        System.out.println(orderedMap);

        if (!orderedMap.isEmpty() && Collections.min(orderedMap.values()) < fnodes.size()) {
            for (int i = 0; i < fnodes.size(); i++) {
                List<VarDecl> factorIds = realizabilityOutputs;
//                List<VarDecl> factorIds = new ArrayList<>();
                List<VarDecl> univIds = new ArrayList<>();

                for (Map.Entry<VarDecl, Integer> entry : orderedMap.entrySet()) {
                    if (entry.getValue() < i) {
                        continue;
                    } else if (entry.getValue() == i) {
                        factorIds.add(entry.getKey());
                    } else {
                        break;
                    }
                }

//                if (!factorIds.isEmpty()) {
                    System.out.println("Outputs for property #" + (i+1) + " : " + factorIds);
                    for (Map.Entry<VarDecl, Integer> entry : orderedMap.entrySet()) {
                        if (entry.getValue() < i) {
                            factorIds.add(entry.getKey());
                        }
                    }
                    List<String> propsublist = spec.node.properties.subList(0, i+1);
                    Node factoredNode = new NodeBuilder(spec.node).clearProperties().addProperties(propsublist).build();
                    factoredNode = LustreSlicer.slice(factoredNode, spec.dependencyMap);
                    Specification factoredNodeSpec = new Specification(factoredNode);

                    try {
                        RealizabilityFixpointSubEngine factoredEngine = new RealizabilityFixpointSubEngine(factoredNodeSpec,
                                settings, director, false, region, factorPrecondition, factorIds);
                        Thread factorThread = new Thread(factoredEngine);
                        factorThread.start();
                        factorThread.join();
                        region = new RefinedRegion(factoredEngine.getFixpointRegion());
                        if (region.getRefinedRegion().equals("Empty") || factoredEngine.getPrecondition().equals("Empty")) {
                            sendUnrealizable(0);
                            throw new StopException();
                        }

                        factorPrecondition = factoredEngine.getPrecondition();

//                        String[] extracted = factoredEngine.getSkolem().split("assert");
//                String simplskolem = "(assert (and " + solver.simplify("(assert " + extracted[extracted.length - 1], null, null) + " true))";
//                        String simplskolem = "(assert " + extracted[extracted.length - 1];
//                        skolems.add(simplskolem);
                    } catch (InterruptedException ie) {

                    }
//                } else {
//                    continue;
//                }
            }
        }

        sendRealizable(0);
//            for (Map.Entry<VarDecl, Integer> entry : orderedMap.entrySet()) {
//                System.out.println(entry);
//                //for each subsequent step, add the precondition to the new engine.
//
//                List<String> propsublist = spec.node.properties.subList(0, entry.getValue() + 1);
//                Node fnode = new NodeBuilder(spec.node).clearProperties().addProperties(propsublist).build();
//                fnode = LustreSlicer.slice(fnode, spec.dependencyMap);
//                Specification fnodespec = new Specification(fnode);
//
//                try {
//                    //Do we really need to run a query over all outputs first? What if we go straight for the skolemization phase?
////                RealizabilityFixpointSubEngine factoredEngine = new RealizabilityFixpointSubEngine(fnodespec, settings, director, true, region, factorPrecondition, entry.getKey());
//                    RealizabilityFixpointSubEngine factoredEngine = new RealizabilityFixpointSubEngine(fnodespec, settings, director, false, region, factorPrecondition, entry.getKey());
//                    Thread factorThread = new Thread(factoredEngine);
//                    factorThread.start();
//                    factorThread.join();
//                    region = new RefinedRegion(factoredEngine.getFixpointRegion());
//                    System.out.println(factoredEngine.getPrecondition());
//
//                    factorPrecondition = factoredEngine.getPrecondition();
//
//                    //we can either collect the skolems and combine in the end OR pass each new skolem into the next iteration. Which one is better?
//                    //the second option might help AE-VAL converge faster!
//                    //first case
////                skolemMap.put(entry.getKey(), factoredEngine.getSkolem());
//                    //second case
//
//                    String[] extracted = factoredEngine.getSkolem().split("assert");
////                String simplskolem = "(assert (and " + solver.simplify("(assert " + extracted[extracted.length - 1], null, null) + " true))";
//                    String simplskolem = "(assert " + extracted[extracted.length - 1];
//                    skolems.add(simplskolem);
//                } catch (InterruptedException ie) {
//
//                }
//            }

//        ---- COMMENTED OUT FOR FIXPOINTCEPTION -----
//        preCondition = factorPrecondition;
//        this.region = region;
////        if (!skolems.isEmpty()) {
//            for (int k = 0; k < settings.n; k++) {
//                comment("K = " + (k + 1));
//                checkRealizable(k);
//            }
//        ---------------------------------

//            director.fixpointImplementation = new SkolemFunction(skolem);
//            sendRealizable(0);
//        } else {
//            throw new StopException();
//        }
    }

    // REMOVED FOR COMPILATION OF FACTORED SYNTHESIS

//    private void splitandcheckRealizable(int k) {
//        List<String> propsublist2 = new ArrayList<>();
//        List<String> propsublist1 = new ArrayList<>();
//        propsublist2.addAll(spec.node.properties);
//        propsublist1.addAll(propsublist2.subList(0,spec.node.properties.size() / 2));
//        propsublist2.removeIf(prop -> propsublist1.contains(prop));
//
//        Node left = new NodeBuilder(spec.node).clearProperties().addProperties(propsublist1).build();
//        Node newleft = LustreSlicer.slice(left, spec.dependencyMap);
//        Specification leftspec = new Specification(newleft);
//
//        Node right = new NodeBuilder(spec.node).clearProperties().addProperties(propsublist2).build();
//        Node newright = LustreSlicer.slice(right, spec.dependencyMap);
//        Specification rightspec = new Specification(newright);
//
//        try {
//            RealizabilityFixpointSubEngine leftEngine =
//                    new RealizabilityFixpointSubEngine(leftspec, settings, director);
//            RealizabilityFixpointSubEngine rightEngine =
//                    new RealizabilityFixpointSubEngine(rightspec, settings, director);
//            Thread leftThread = new Thread(leftEngine);
//            Thread rightThread = new Thread(rightEngine);
//
//            leftThread.start();
//            rightThread.start();
//
//            leftThread.join();
//            rightThread.join();
//            String leftFixpointRegion = leftEngine.getFixpointRegion();
//            String rightFixpointRegion = rightEngine.getFixpointRegion();
////
////            if (leftFixpointRegion != null && rightFixpointRegion != null) {
////                region = new RefinedRegion(solver.simplify(null, leftFixpointRegion, rightFixpointRegion));
////            } else if (leftFixpointRegion == null && rightFixpointRegion != null) {
////                region = new RefinedRegion(solver.simplify(null, null, rightFixpointRegion));
////            } else if (leftFixpointRegion != null && rightFixpointRegion == null) {
////                region = new RefinedRegion(solver.simplify(null, leftFixpointRegion, null));
////            }
////            if (region != null && region.getRefinedRegion().equals("  false\n" +
////                    "  ")) {
////                throw new JKindException("false simplified region");
////            }
//            //before modification of region structure
//            if (leftFixpointRegion != null && rightFixpointRegion != null) {
//                region = new RefinedRegion("(assert (and\n" +
//                        solver.simplify(null, leftFixpointRegion, rightFixpointRegion) + " true))");
//            } else if (leftFixpointRegion == null && rightFixpointRegion != null) {
//                region = new RefinedRegion("(assert (and\n" +
//                        solver.simplify(null, null, rightFixpointRegion) + " true))");
//            } else if (leftFixpointRegion != null && rightFixpointRegion == null) {
//                region = new RefinedRegion("(assert (and\n" +
//                        solver.simplify(null, leftFixpointRegion, null) + " true))");
//            }
//
////            System.out.println(spec.node.properties + " region of subfixpoints: " + region.getRefinedRegion());
//
//            if (region != null && region.getRefinedRegion().equals("(assert (and\n  false\n" +
//                    "  "+ " true))")) {
//                sendUnrealizable(k);
//            }
//        } catch (InterruptedException ie) {
//        }
//    }

    private void checkRealizable(int k) {
        aesolver = new AevalSolver(settings.filename, name + k, aevalscratch);
        aecomment("; Frame = " + (k));
        createQueryVariables(aesolver, region, k);
        System.out.println(spec.node.properties + "main frame" + k);
        AevalResult aeresult = aesolver.realizabilityQuery(getAevalInductiveTransition(0),
            StreamIndex.conjoinEncodings(spec.node.properties, 2), true);
        if (aeresult instanceof ValidResult) {
            System.out.println(spec.node.properties + " : valid, frame" + k);

            director.fixpointImplementation = new SkolemFunction(((ValidResult) aeresult).getSkolem());
            sendRealizable(k);
            throw new StopException();
        } else if (aeresult instanceof InvalidResult) {
            System.out.println(spec.node.properties + " : invalid, frame" + k);

            String result = ((InvalidResult) aeresult).getValidSubset();
            if (result.equals("Empty")) {
               // System.out.println("main frame"+k + " empty refinement");
                sendUnrealizable(k);
            } else {
                String negatedsimplified = "(assert (and " + solver.simplify("(assert (not" + result + ")", null, null) + " true))";
                ValidSubset negatedsubset = new ValidSubset(negatedsimplified);

                if (settings.fixpoint_T) {
                    refineRegion(k, negatedsubset, true);
                } else {
                    refineRegion(k, negatedsubset, true);
                }
            }
        } else if (aeresult instanceof UnknownResult) {
            throw new StopException();
        }

    }

    private void refineRegion(int k, ValidSubset subset, boolean negate) {
        String simplified;
        aesolver = new AevalSolver(settings.filename, name + "subset" + k, aevalscratch);
        aecomment(";Refinement = " + k);
        createSubQueryVariablesAndAssertions(aesolver, subset, k);
        AevalResult aeresult = aesolver.refinementQuery();
        if (aeresult instanceof ValidResult) {
            //System.out.println("valid refinement step main frame");
            sendUnrealizable(k);
            throw new StopException();
        } else if (aeresult instanceof InvalidResult) {
            String result = ((InvalidResult) aeresult).getValidSubset();
            if (result.equals("Empty")) {
                sendUnrealizable(k);
            } else {
                if (region != null) {
                    if (negate) {
                        simplified = "(assert (and\n" + solver.simplify(region.getRefinedRegion(),
                                "(assert (not" + result + ")", null) + " true))";
                    } else {
                        simplified = "(assert (and\n" + solver.simplify(region.getRefinedRegion(),
                                "(assert " + result, null) + " true))";
                    }
                } else {
                    if (negate) {
                        simplified = "(assert (and\n" + solver.simplify(null,
                                "(assert (not" + result + ")", null) + " true))";
                    } else {
                        simplified = "(assert (and\n" + solver.simplify(null,
                                "(assert " + result, null) + " true))";
                    }
                }

                region = new RefinedRegion(simplified);
            }
        } else {
            sendUnknown();
        }
    }

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

        if (settings.scratch) {
            aesolver.scratch.println("; Existentially quantified variables");
        }

        aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL), true);


        List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls());
        List<VarDecl> curroutvars = getOffsetVarDecls(0, getRealizabilityOutputVarDecls());

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

        if (region !=null) {
            aesolver.sendBlockedRegionSPart(region.getRefinedRegion());
        }


        aesolver.assertSPart(getUniversalInputVariablesAssertion(-1));
        aesolver.assertSPart(getUniversalOutputVariablesAssertion(-1));

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for existential part of the formula");
        }
//        aesolver.assertTPart(StreamIndex.conjoinEncodings(spec.node.properties, 0), true);

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

        aesolver.sendSubsetTPart(subset.getValidSubset());


        if (region != null) {
            aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getRefinedRegion(), -1,
                    preoutvars, true));
        }

        if (settings.fixpoint_T) {
//            for (String sk : skolems) {
//                String[] extracted = sk.split("assert");
//                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep("(assert " + extracted[extracted.length - 1], 0, getOffsetVarDecls(0, getRealizabilityOutputVarDecls()), false));
//            }
//            if (!preCondition.equals("true")) {
//                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(preCondition, 0, getOffsetVarDecls(0, getRealizabilityOutputVarDecls()), false));
//            }
        }

    }

    protected void createQueryVariables(AevalSolver aesolver, RefinedRegion region, int k) {
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

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for blocked regions - universal part of the formula");
        }

        if (region != null) {
            aesolver.sendBlockedRegionSPart(region.getRefinedRegion());
        }

        if (region != null) {
            aesolver.sendBlockedRegionSPart(convertOutputsToNextStep(region.getRefinedRegion(), -1,
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
// -- COMMENTS REMOVED TO TEST FIXPOINTCEPTION
        if (region !=null) {
//            if (k!=0) {
                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getRefinedRegion(), -1,
                        getOffsetVarDecls(-1, getRealizabilityOutputVarDecls()), false));
//            } else {
//                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getRefinedRegion(), 0,
//                        getOffsetVarDecls(0, getRealizabilityOutputVarDecls()), false));
//            }
        }
// -----------
        if (settings.fixpoint_T) {
//            for (String sk : skolems) {
//                String[] extracted = sk.split("assert");
//                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep("(assert " + extracted[extracted.length - 1], 0, getOffsetVarDecls(0, getRealizabilityOutputVarDecls()), false));
//            }
            if (!preCondition.equals("true")) {
                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(preCondition, 0, getOffsetVarDecls(0, getRealizabilityOutputVarDecls()), false));
            }
        }
    }

    protected String convertOutputsToNextStep(String region, int k, List<VarDecl> offsetVarDecls, boolean lhs) {
        String converted = region;
        String newvarid;
        if (lhs) {
            converted = converted.replaceAll("\\$~1", "\\$0");
        } else {
            for (VarDecl off : offsetVarDecls) {
                String varid = off.id;
                if (converted.contains(varid)) {
                    if (k == -1) {
                        newvarid = varid.replaceAll("\\$~1", "\\$2");
                    } else {
                        newvarid = varid.replaceAll("\\$"+k, "\\$2");
                    }
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

    public void sendUnrealizable(int k) {
        sendUnrealizable(k, Collections.emptyList());
    }

    public void sendUnrealizable(int k, List<String> conflicts) {
        UnrealizableMessage im = new UnrealizableMessage(k + 1, null, conflicts);
        director.incoming.add(im);
    }

    public void sendUnknown() {
        UnknownMessage um = new UnknownMessage();
        director.incoming.add(um);
    }
}