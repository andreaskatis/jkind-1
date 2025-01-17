package jkind.realizability.engines;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.aeval.*;
import jkind.engines.StopException;
import jkind.lustre.*;
import jkind.lustre.builders.NodeBuilder;
import jkind.realizability.JRealizabilitySolverOption;
import jkind.realizability.engines.fixpoint.RefinedRegion;
import jkind.realizability.engines.messages.InconsistentMessage;
import jkind.realizability.engines.messages.RealizableMessage;
import jkind.realizability.engines.messages.UnknownMessage;
import jkind.realizability.engines.messages.UnrealizableMessage;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.slicing.LustreSlicer;
import jkind.solvers.Model;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnsatResult;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Specification;
import jkind.util.StreamIndex;
import jkind.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public class RealizabilityFixpointEngine extends RealizabilityEngine {
    private AevalSolver aesolver;
    private RefinedRegion region;
    private Sexp simpTrans;
    private ArrayList<String> skolems;
    private String preCondition;

    public RealizabilityFixpointEngine(Specification spec, JRealizabilitySettings settings,
                                   RealizabilityDirector director) {
        super("fixpoint", spec, settings, director);
        this.skolems = new ArrayList<>();
        this.region = new RefinedRegion(new Symbol("true"));
    }

    public RealizabilityFixpointEngine(Specification spec, JRealizabilitySettings settings,
                                       RealizabilityDirector director, String name) {
        super(name, spec, settings, director);
        this.skolems = new ArrayList<>();
        this.region = new RefinedRegion(new Symbol("true"));
    }

    @Override
    public void main() {
        try {
//            if (settings.fixpoint_T) {
//                factorizeandcheckRealizable();
//            } else {
                for (int k = 0; k < settings.n; k++) {
                    comment("K = " + k);
                    if (k == 0) {
                        checkConsistency(k);
                    }
                    if (settings.solver == JRealizabilitySolverOption.AEVAL) {
                        checkRealizable(k);
                    } else {
                        checkRealizableWithQE(k);
                    }


                }
//            }
        } catch (StopException se) {
        }
    }

    private void checkConsistency(int k) {
        solver.push();
        solver.assertSexp(getTransition(0, Sexp.fromBoolean(k == 0)));
        Result result = solver.query(new Symbol("false"));
        if (result instanceof UnsatResult) {
            sendInconsistent(k);
            throw new StopException();
        }
        solver.pop();
    }

    // private void factorizeandcheckRealizable() {
    //     String factorPrecondition = "true";
    //     RefinedRegion region = new RefinedRegion("(assert true)");
    //     Map<VarDecl, Integer> factorMap = new LinkedHashMap<>();
    //     Map<VarDecl, Integer> trueOutputsMap = new LinkedHashMap<>();
    //     Map<String, String> skolemMap = new LinkedHashMap<>();
    //     List<Node> fnodes = new ArrayList<>();
    //     List<String> locIds = new ArrayList<>();

    //     //create sliced nodes per property
    //     for (String prop : spec.node.properties) {
    //         Node fnode = new NodeBuilder(spec.node).clearProperties().addProperty(prop).build();
    //         fnode = LustreSlicer.slice(fnode, spec.dependencyMap);
    //         fnodes.add(fnode);
    //     }

    //     List<String> realizabilityInputs = spec.node.realizabilityInputs;
    //     List<VarDecl> realizabilityOutputs = Util.getVarDecls(spec.node);

    //     realizabilityOutputs.removeIf(vd -> realizabilityInputs.contains(vd.id));
    //     realizabilityOutputs.removeIf(vd -> spec.node.properties.contains(vd.id));

    //     for (VarDecl loc : spec.node.locals) {
    //         locIds.add(loc.id);
    //     }
    //     List <VarDecl> trueOutputs = new ArrayList<>();
    //     trueOutputs.addAll(realizabilityOutputs);
    //     trueOutputs.removeIf(vd -> locIds.contains(vd.id));

    //     for (VarDecl out : realizabilityOutputs) {
    //         String outname = out.id;
    //         for (Node fnode : fnodes) {
    //             List<String> noderealizabilityInputs = fnode.realizabilityInputs;
    //             List<VarDecl> noderealizabilityOutputs = Util.getVarDecls(fnode);
    //             noderealizabilityOutputs.removeIf(vd -> noderealizabilityInputs.contains(vd.id));
    //             for (VarDecl nout : noderealizabilityOutputs) {
    //                 if (nout.id.equals(outname)) {
    //                     factorMap.put(out, fnodes.indexOf(fnode) + 1);
    //                     break;
    //                 }
    //             }
    //         }
    //     }

    //     for (VarDecl out : trueOutputs) {
    //         String outname = out.id;
    //         for (Node fnode : fnodes) {
    //             List<String> noderealizabilityInputs = fnode.realizabilityInputs;
    //             List<VarDecl> noderealizabilityOutputs = Util.getVarDecls(fnode);
    //             noderealizabilityOutputs.removeIf(vd -> noderealizabilityInputs.contains(vd.id));
    //             for (VarDecl nout : noderealizabilityOutputs) {
    //                 if (nout.id.equals(outname)) {
    //                     trueOutputsMap.put(out, fnodes.indexOf(fnode) + 1);
    //                     break;
    //                 }
    //             }
    //         }
    //     }

    //     Map<VarDecl, Integer> orderedMap = factorMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
    //             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    //     int factorCounter =0;
    //     if (!orderedMap.isEmpty() && Collections.min(orderedMap.values()) < fnodes.size()) {
    //         int lastProperty = 0;
    //         for (int i = 1; i <= fnodes.size(); i++) {
    //             List<VarDecl> factorIds = new ArrayList<>();
    //             List<VarDecl> uncomputedIds = new ArrayList<>();

    //             for (Map.Entry<VarDecl, Integer> entry : orderedMap.entrySet()) {
    //                 if (entry.getValue() <= i && entry.getValue() > lastProperty) {
    //                     factorIds.add(entry.getKey());
    //                 } else if (entry.getValue() <= lastProperty) {
    //                     continue;
    //                 } else {
    //                         uncomputedIds.add(entry.getKey());//                        }
    //                 }
    //             }

    //             if (!factorIds.isEmpty()) {
    //                 factorCounter++;
    //                 List<String> propsublist = spec.node.properties.subList(lastProperty, i);
    //                 Node factoredNode = new NodeBuilder(spec.node).clearProperties().addProperties(propsublist).build();
    //                 factoredNode = LustreSlicer.slice(factoredNode, spec.dependencyMap);
    //                 Specification factoredNodeSpec = new Specification(factoredNode);

    //                 List<String> visitedpropsublist = spec.node.properties.subList(0, i);
    //                 Node visitedFactorsNode = new NodeBuilder(spec.node).clearProperties().addProperties(visitedpropsublist).build();
    //                 visitedFactorsNode = LustreSlicer.slice(visitedFactorsNode, spec.dependencyMap);
    //                 Specification visitedFactorsNodeSpec = new Specification(visitedFactorsNode);

    //                 try {
    //                     RealizabilityFixpointSubEngine factoredEngine = new RealizabilityFixpointSubEngine(visitedFactorsNodeSpec, factoredNodeSpec,
    //                             settings, director, true, region, factorPrecondition, factorIds, uncomputedIds);
    //                     Thread factorThread = new Thread(factoredEngine);
    //                     factorThread.start();
    //                     factorThread.join();
    //                     if (region != null) {
    //                         region = new RefinedRegion(factoredEngine.getFixpointRegion());
    //                         if (region.getRefinedRegion().equals("Empty")) {
    //                             if (settings.diagnose) {
    //                                 setResult(0,"UNREALIZABLE", null);
    //                             } else {
    //                                 sendUnrealizable(0);
    //                             }
    //                             throw new StopException();
    //                         }
    //                     }
    //                     String[] extracted = factoredEngine.getSkolem().split("assert");
    //                     String simplskolem = "(assert " + extracted[extracted.length - 1];
    //                     skolems.add(simplskolem);
    //                 } catch (InterruptedException ie) {

    //                 }
    //                 lastProperty = i;
    //             } else {
    //                 continue;
    //             }
    //         }
    //         String implementation = "";
    //         for (String skolem : skolems) {
    //             implementation = implementation + skolem;
    //         }
    //         director.fixpointImplementation = new SkolemFunction(implementation);
    //         sendRealizable(factorCounter);
    //     } else if (Collections.min(orderedMap.values()) == fnodes.size()) {
    //         for (int k = 0; k < settings.n; k++) {
    //             comment("K = " + (k + 1));
    //             checkRealizable(k);
    //         }
    //     }
    //     if (preCondition != null && !preCondition.equals("true")) {
    //         preCondition = factorPrecondition;
    //     }
    //     if (!region.getRefinedRegion().equals("true")) {
    //         this.region = region;
    //     }
    // }

    //If the QE result R is such that `T => R` is unsat, then R only contains constraints that come as a result of simplifying the original assumptions.
    //This can occur in cases where an assumption is the output value of a Lustre node N.
    //Essentially, QE computes an R that is defined over input variables, eliminating the variables that correspond to node N's input and output
    // (in a similar way to how inlining the node would work).

    //One way to skip this query would be to assume that T is true, but that seems to have a negative performance impact
    //on bigger contracts.

//    private boolean checkAgainstAssumptions(Sexp qeResult, Sexp transition) {
//        Result queryResult = solver.query(new Cons("or", new Cons("not", transition), qeResult));
//        return (queryResult instanceof UnsatResult);
//        return false;
//    }

    private void synthesizeImplementation() {
        aesolver = new AevalSolver(settings.filename, name, aevalscratch);
        createQueryVariables(aesolver, region);
        AevalResult aeresult = aesolver.realizabilityQuery(getAevalInductiveTransition(0),
                StreamIndex.conjoinEncodings(spec.node.properties, 2), settings.synthesis, settings.aevalopt, settings.nondet,
                settings.compact, settings.allinclusive);
        if (aeresult instanceof ValidResult) {
            if (settings.synthesis) {
                director.fixpointImplementation = new SkolemFunction(((ValidResult) aeresult).getSkolem());
            }
        } else {
            throw new JKindException("Synthesis step failed");
        }
    }

    private void checkRealizableWithQE(int k) {
        Sexp trueRegion = region.getRefinedRegion();
        Sexp trueRegionNext = region == null ? new Symbol("true"):
                new Cons("and", new Symbol(convertOutputsToNextStep(trueRegion.toString(), 0,
                getOffsetVarDecls(-1, getRealizabilityOutputVarDecls()), false)));

        if (k == 0) {
            simpTrans = solver.simplify(getInductiveTransition(0));
        }

        Sexp query = new Cons("and", simpTrans, StreamIndex.conjoinEncodings(spec.node.properties, 0), trueRegionNext);
        if (getRealizabilityOutputs(0) != null) {
            query = new Cons("exists", getRealizabilityOutputs(0), query);
        }
        query = new Cons("=>", (k == 0 ? new Cons("and", simpTrans, trueRegion) : trueRegion), query);

        Sexp qeResult = solver.qeQuery(query, false);
        Sexp negatedQeResult = solver.simplify(new Cons("not", qeResult));

        if (qeResult.toString().equals("false")) {
            if (settings.diagnose) {
                setResult(k,"UNREALIZABLE", null);
            } else {
                sendUnrealizable(k);
                throw new StopException();
            }
        } else if (qeResult.toString().equals("true") || negatedQeResult.toString().equals("false")) {
            if (settings.diagnose) {
                setResult(k,"REALIZABLE", null);
            } else {

                if (settings.synthesis) {
                    synthesizeImplementation();
                }

                if (settings.traceLength == 0) {
                    sendRealizable(k);
                } else {
                    Model model = extractTrace(trueRegion, k);
                    sendRealizable(k, model);
                }
                throw new StopException();
            }
        } else {
            //refine
            List<VarDecl> vars = new ArrayList<>();
            vars.addAll(getRealizabilityInputVarDecls());
            vars.addAll(getRealizabilityOutputVarDecls());

            Sexp refinementQuery = new Cons("=>", trueRegion,
                    new Cons("exists", varDeclsToRefinementQuantifierArguments(vars, 0),
                            new Cons("and", simpTrans, negatedQeResult)));

            Sexp refinementResult = solver.qeQuery(refinementQuery,true);

            if (solver.simplify(refinementResult).toString().equals("true")) {
                if (settings.diagnose) {
                    setResult(k,"UNREALIZABLE", null);
                } else {
                    sendUnrealizable(k);
                    throw new StopException();
                }
            } else {
                Sexp negatedRefRes = solver.simplify(new Cons("not", refinementResult));
                Result isFixpoint = solver.query(new Cons("=>", trueRegion, negatedRefRes));
                if (isFixpoint instanceof UnsatResult) {
                    if (solver.initialStatesQuery(getTransition(0, true),
                            StreamIndex.conjoinEncodings(spec.node.properties, 0),
                            negatedRefRes,
                            new Symbol(convertOutputsToNextStep(negatedRefRes.toString(), 0, getOffsetVarDecls(-1, getRealizabilityOutputVarDecls()), false)))) {
                        if (settings.diagnose) {
                            setResult(k,"REALIZABLE", null);
                        } else {
                            if (settings.synthesis) {
                                synthesizeImplementation();
                            }

                            if (settings.traceLength == 0) {
                                sendRealizable(k + 1);
                            } else {
                                Model model = extractTrace(trueRegion, k + 1);
                                sendRealizable(k + 1, model);
                            }
                            throw new StopException();
                        }
                    } else {
                        if (settings.diagnose) {
                            setResult(k,"UNREALIZABLE", null);
                        } else {
                            sendUnrealizable(k);
                            throw new StopException();
                        }
                    }
                } else {
                    region = new RefinedRegion(negatedRefRes);
                }
            }
        }

    }

    private Model extractTrace(Sexp trueRegion, int k) {
        List<Symbol> regions = new ArrayList<>();
        List<Symbol> transitions = new ArrayList<>();
        List<Symbol> properties = new ArrayList<>();
        List<VarDecl> varDecls = new ArrayList<>();

        int traceLength = settings.traceLength > -1 ? settings.traceLength : k;

        solver.push();

        regions.add(new Symbol(trueRegion.toString()));
        transitions.add(new Symbol(getTransition(0, true).toString()));
        properties.add(new Symbol(StreamIndex.conjoinEncodings(spec.node.properties, 0).toString()));

        for (int i = 1; i < traceLength; i++){
            varDecls.addAll(getOffsetVarDecls(i, getRealizabilityInputVarDecls()));
            varDecls.addAll(getOffsetVarDecls(i, getRealizabilityOutputVarDecls()));
            String converted = trueRegion.toString();
            converted = converted.replaceAll("\\$~1", "\\$"+i);
            regions.add(new Symbol("(and " + converted + ")"));
            transitions.add(new Symbol(getTransition(i, false).toString()));
            properties.add(new Symbol(StreamIndex.conjoinEncodings(spec.node.properties, i).toString()));
        }

        for (VarDecl in : varDecls) {
            solver.define(in);
        }

        List<Symbol> allSexps = new ArrayList<>();
        allSexps.addAll(transitions);
        allSexps.addAll(properties);
        allSexps.addAll(regions);

        Result result = solver.checkSat(allSexps, true, false);
        Model model = ((SatResult) result).getModel();
        solver.pop();
        return model;
    }

    private void checkRealizable(int k) {
        aesolver = new AevalSolver(settings.filename, name + k, aevalscratch);
        aecomment("; Frame = " + (k));
        createQueryVariables(aesolver, region);

        AevalResult aeresult = aesolver.realizabilityQuery(getAevalInductiveTransition(0),
                StreamIndex.conjoinEncodings(spec.node.properties, 2), false, settings.aevalopt, settings.nondet,
                settings.compact, settings.allinclusive);

        if (aeresult instanceof ValidResult) {
            if (settings.synthesis) {
                aeresult = aesolver.realizabilityQuery(getAevalInductiveTransition(0),
                        StreamIndex.conjoinEncodings(spec.node.properties, 2), true, false, settings.nondet,
                        settings.compact, settings.allinclusive);
                director.fixpointImplementation = new SkolemFunction(((ValidResult) aeresult).getSkolem());
            }
            if (checkInitialStates()) {
                if (settings.diagnose) {
                    setResult(k,"REALIZABLE", null);
                } else {
                    sendRealizable(k);
                }
            } else {
                if (settings.diagnose) {
                    setResult(k,"UNREALIZABLE", null);
                } else {
                    sendUnrealizable(k);
                }
            }
            throw new StopException();
        } else if (aeresult instanceof InvalidResult) {
            Sexp result = ((InvalidResult) aeresult).getValidSubset();
            if (result.toString().equals("Empty")) {
                if (settings.diagnose) {
                    setResult(k,"UNREALIZABLE", null);
                } else {
                    sendUnrealizable(k);
                }
            } else {
                Sexp negatedsimplified = solver.simplify(new Cons("not", result));
                ValidSubset negatedsubset = new ValidSubset(negatedsimplified);
                refineRegion(k, negatedsubset);
            }
        } else if (aeresult instanceof UnknownResult) {
            throw new StopException();
        }

    }

    private boolean checkInitialStates() {
        if (region != null) {
            return solver.initialStatesQuery(getAevalTransition(0, true),
                    StreamIndex.conjoinEncodings(spec.node.properties, 2), region.getRefinedRegion(),
                    new Symbol(convertOutputsToNextStep(
                            region.getRefinedRegion().toString(),-1, getOffsetVarDecls(-1, getRealizabilityOutputVarDecls()), false)));
        }
        return true;
    }

    private void refineRegion(int k, ValidSubset subset) {
        Sexp simplified;
        aesolver = new AevalSolver(settings.filename, name + "subset" + k, aevalscratch);
        aecomment(";Refinement = " + k);
        createSubQueryVariablesAndAssertions(aesolver, subset, k);
        AevalResult aeresult = aesolver.refinementQuery(settings.aevalopt);
        if (aeresult instanceof ValidResult) {
            if (settings.diagnose) {
                setResult(k,"UNREALIZABLE", null);
            } else {
                sendUnrealizable(k);
            }
            throw new StopException();
        } else if (aeresult instanceof InvalidResult) {
            Sexp result = ((InvalidResult) aeresult).getValidSubset();
            if (result.toString().equals("false")) {
                if (settings.diagnose) {
                    setResult(k,"UNREALIZABLE", null);
                } else {
                    sendUnrealizable(k);
                }
            } else {
                simplified = solver.simplify(new Cons("and", region.getRefinedRegion(), new Cons("not", result)));
                region = new RefinedRegion(simplified);
            }
        } else {
            if (settings.diagnose) {
                setResult(k,"UNKNOWN", null);
            } else {
                sendUnknown();
            }
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

        VarDecl init = new VarDecl(INIT.str, NamedType.BOOL);

        aesolver.defineTVar(init, true);


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

        for (VarDecl vd : getRealizabilityOutputVarDecls()) {
            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
            if (constraint != null) {
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(-1)));
            }
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for universal part of the formula");
        }

        if (region !=null) {
            aesolver.assertSPart(region.getRefinedRegion());
        }


        aesolver.assertSPart(getUniversalInputVariablesAssertion(-1));
        aesolver.assertSPart(getUniversalOutputVariablesAssertion(-1));

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for existential part of the formula");
        }
        aesolver.assertTPart(getTransition(0, INIT), true);


        if (settings.scratch) {
            aesolver.scratch.println("; Constraints for existential part of the formula");
        }


        for (VarDecl vd : Util.getVarDecls(spec.node)) {
            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
            if (constraint != null) {
                aesolver.assertTPart(constraint.accept(new Lustre2Sexp(0)), true);
            }
        }
        aesolver.assertTPart(subset.getValidSubset(), true);
    }

    protected void createQueryVariables(AevalSolver aesolver, RefinedRegion region) {
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
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(0)));
            }
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for blocked regions - universal part of the formula");
        }

        if (region != null) {
            aesolver.assertSPart(region.getRefinedRegion());
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

        if (region !=null) {
            aesolver.assertTPart(new Symbol(convertOutputsToNextStep(region.getRefinedRegion().toString(), -1,
                    getOffsetVarDecls(-1, getRealizabilityOutputVarDecls()), false)), true);
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
            converted = converted.replaceAll("\\$~1", "\\$0");
        }
        return converted;
    }

    private void sendInconsistent(int k) {
        InconsistentMessage im = new InconsistentMessage(k + 1);
        director.incoming.add(im);
    }

    private void sendRealizable(int k) {
        RealizableMessage rm = new RealizableMessage(k);
        director.incoming.add(rm);
    }

    private void sendRealizable(int k, Model model) {
        RealizableMessage rm = new RealizableMessage(k, model);
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

    @Override
    protected void setResult(int k, String result, Model model) {
        this.result = result;
    }
}