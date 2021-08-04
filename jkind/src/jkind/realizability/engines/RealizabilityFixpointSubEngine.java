package jkind.realizability.engines;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.aeval.*;
import jkind.analysis.LinearChecker;
import jkind.engines.StopException;
import jkind.lustre.*;

import jkind.realizability.engines.fixpoint.RefinedRegion;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.solvers.z3.Z3Solver;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Specification;
import jkind.util.SexpUtil;
import jkind.util.StreamIndex;
import jkind.util.Util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class RealizabilityFixpointSubEngine extends RealizabilityFixpointEngine implements Runnable {
    private PrintWriter aevalscratch;
    private Specification spec, factorSpec;
    private Z3Solver solver;
    private RefinedRegion currentFixpoint;
    private RefinedRegion region, blockedRegion;
    private AevalSolver aesolver;
    protected final String name;
    private ValidSubset vsubset;
    private boolean skolemize, maximalsubset;
    private String precondition;
    private List<VarDecl> factorOutputs, uncomputedOutputs, uncomputedfactorOutputs;
    private String skolem;


    public RealizabilityFixpointSubEngine(Specification spec, Specification factorSpec, JRealizabilitySettings settings,
                                          RealizabilityDirector director, boolean skolemize, RefinedRegion region, String precondition, List<VarDecl> factorOutputs, List<VarDecl> uncomputedOutputs) {
        super(factorSpec, settings, director);
        this.name = super.name;
        this.aevalscratch = getaevalScratch(factorSpec, name);
        this.spec = spec;
        this.factorSpec = factorSpec;
        this.skolemize = skolemize;
        this.maximalsubset = maximalsubset;
        this.currentFixpoint = region;
        this.region = region;
        this.precondition = precondition;
        this.skolem = skolem;
        this.factorOutputs = factorOutputs;
        this.uncomputedOutputs = uncomputedOutputs;
        this.uncomputedfactorOutputs = uncomputedOutputs;
    }

    @Override
    public void run() {
        try {
            solver = new Z3Solver(settings.filename + "." + name + factorSpec.node.properties.get(0) + factorSpec.node.properties.get(factorSpec.node.properties.size()-1) + "_" + factorOutputs.get(0) + factorOutputs.get(factorOutputs.size()-1), LinearChecker.isLinear(factorSpec.node));
            solver.define(new VarDecl(INIT.str, NamedType.BOOL));
            List<VarDecl> dummyoutvars = getOffsetVarDecls(0, getRealizabilityOutputVarDecls(spec));
            List<VarDecl> preinvars = getOffsetVarDecls(-1, getRealizabilityInputVarDecls(spec));
            List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls(spec));
            List<String> locIds = new ArrayList<>();

            for (VarDecl vd : dummyoutvars) {
                solver.define(vd);
            }

            for (VarDecl in : preinvars) {
                solver.define(in);
            }

            List<VarDecl> preoutvars = getOffsetVarDecls(-1, getRealizabilityOutputVarDecls(spec));

            for (VarDecl vd : preoutvars) {
                solver.define(vd);
            }

            for (VarDecl in : currinvars) {
                solver.define(in);
            }

            List<String> uncomputedVars = new ArrayList<>();
            for (VarDecl vd : getRealizabilityOutputVarDecls(factorSpec)) {
//            for (VarDecl vd : factorOutputs) {
                uncomputedVars.add(vd.id);
            }
//            uncomputedOutputs.removeIf(vd -> uncomputedVars.contains(vd.id));
//            uncomputedOutputs.removeIf(vd -> !uncomputedVars.contains(vd.id));

            uncomputedfactorOutputs.removeIf(vd -> !uncomputedVars.contains(vd.id));
            System.out.println("Uncomputed outputs in this factor: " + uncomputedfactorOutputs);
//            for (VarDecl loc : factorSpec.node.locals) {
//                locIds.add(loc.id);
//            }
//            uncomputedOutputs.removeIf(vd -> !locIds.contains(vd.id));
//            System.out.println("Uncomputed local outputs: " + uncomputedOutputs);
//            factorOutputs.addAll(uncomputedOutputs);


            for (int k = 0; k < settings.n; k++) {
                solver.comment("K = " + (k + 1));
                //checkConsistency(k);
//                if (k == 0 && spec.node.properties.size() > 1) {
//                    splitandcheckRealizable(k);
//                }
                checkRealizable(k);
            }
        } catch (StopException se) {
        }
    }

    private void checkRealizable(int k) {

        System.out.println(factorSpec.node.properties + " " + factorOutputs.get(0) + " " +
                factorOutputs.get(factorOutputs.size()-1) + ",frame =" + k + " " + skolemize);
            aesolver = new AevalSolver(settings.filename, name + factorSpec.node.properties.get(0) + factorSpec.node.properties.get(factorSpec.node.properties.size()-1) + k + factorOutputs.get(0).id + factorOutputs.get(factorOutputs.size()-1).id + skolemize, aevalscratch);

            createQueryVariables(aesolver, region, k, skolemize);

            AevalResult aeresult;
            aeresult = aesolver.realizabilityQuery(getAevalInductiveTransition(0), StreamIndex.conjoinEncodings(factorSpec.node.properties, 2), settings.synthesis, settings.nondet, settings.compact, settings.allinclusive);
//        aeresult = aesolver.realizabilityQuery(getAevalInductiveTransition(0), StreamIndex.conjoinEncodings(factorSpec.node.properties, 2), false);
            if (aeresult instanceof ValidResult) {
                    System.out.println(factorSpec.node.properties + " : valid");
                    precondition = getFixpointRegion();
                    skolem = ((ValidResult) aeresult).getSkolem();
//                SplitFramesExtractor splitExtractor = new SplitFramesExtractor(spec, factorSpec, this, k, region, factorOutputs, uncomputedfactorOutputs);
//                try {
//                    Thread splittingFrames = new Thread(splitExtractor);
//                    splittingFrames.start();
//                    splittingFrames.join();
//                    region = new RefinedRegion(splitExtractor.getRegion());
//                    precondition = splitExtractor.getremainderRegion();
//                } catch (InterruptedException ie) {
//                }
                throw new StopException();
            } else if (aeresult instanceof InvalidResult) {
                    System.out.println(factorSpec.node.properties + " : invalid");
                String result = ((InvalidResult) aeresult).getValidSubset();
                if (result.equals("Empty")) {
                    region = new RefinedRegion(result);
                    throw new StopException();
                } else {
                    String negatedsimplified = "(assert (and " + solver.simplify("(assert (not" + result + ")", null, null) + " true))";
//                    String negatedsimplified = "(assert (not" + result + ")";
                    ValidSubset negatedsubset = new ValidSubset(negatedsimplified);

                    try {
                        String simplifiedRegions;
                        RegionExtractor negatedsubsetExtractor = new RegionExtractor(spec, factorSpec, this, k,
                                region, negatedsubset, true, false, factorOutputs, uncomputedOutputs, skolemize, precondition, blockedRegion);

                        Thread negatedsubsetThread = new Thread(negatedsubsetExtractor);
                        negatedsubsetThread.start();
                        negatedsubsetThread.join();
                        AevalResult negatedsubsetResult = negatedsubsetExtractor.getResult();

                        if (negatedsubsetResult instanceof ValidResult) {
                            System.out.println(factorSpec.node.properties + " REFINEMENT IS VALID");
                            region = new RefinedRegion("Empty");
                            throw new StopException();
                        } else {
                            RefinedRegion negatedsubsetRegion = new RefinedRegion(negatedsubsetExtractor.getRegion());
                            if (negatedsubsetRegion.getRefinedRegion().equals("Empty")) {
                                maximalsubset = true;
                                System.out.println("Refinement gave empty subset");
                                if (!region.getRefinedRegion().equals("true")) {
                                    simplifiedRegions = solver.simplify(region.getRefinedRegion(), null, "(assert " + result);
//                                    simplifiedRegions = region.getRefinedRegion() + "\n(assert "+ result;
                                } else {
                                    simplifiedRegions = solver.simplify(null, null, "(assert " + result);
//                                    simplifiedRegions = "(assert " + result;
                                }
                                //april 23 uncommented below
                                sendUnrealizable(k);
                            } else {
                                if (region != null && !region.getRefinedRegion().equals("true")) {

                                    if (negatedsubsetRegion.getRefinedRegion() != null) {
                                        simplifiedRegions = solver.simplify(region.getRefinedRegion(), null, negatedsubsetRegion.getRefinedRegion());
//                                        simplifiedRegions = region.getRefinedRegion() + "\n" + negatedsubsetRegion.getRefinedRegion();
                                    } else {
                                        throw new StopException();
                                    }
                                } else {
                                    if (negatedsubsetRegion.getRefinedRegion() != null) {
                                        simplifiedRegions = solver.simplify(null, null, negatedsubsetRegion.getRefinedRegion());
//                                        simplifiedRegions = negatedsubsetRegion.getRefinedRegion();
                                    } else {
                                        throw new StopException();
                                    }
                                }
                                if (blockedRegion != null && !blockedRegion.getRefinedRegion().equals("true")) {
                                    if (negatedsubsetRegion.getRefinedRegion() != null) {
                                        blockedRegion = new RefinedRegion("(assert (and " + solver.simplify(blockedRegion.getRefinedRegion(), null, negatedsubsetRegion.getRefinedRegion()) + "true))");
//                                        blockedRegion = new RefinedRegion(blockedRegion.getRefinedRegion() + "\n" + negatedsubsetRegion.getRefinedRegion());
                                    } else {
                                        throw new StopException();
                                    }
                                } else {
                                    if (negatedsubsetRegion.getRefinedRegion() != null) {
                                        blockedRegion = new RefinedRegion(negatedsubsetRegion.getRefinedRegion());
                                    } else {
                                        throw new StopException();
                                    }
                                }
                            }
                            if (!simplifiedRegions.equals("  false\n" + "  ")) {
                                region = new RefinedRegion("(assert (and\n" + simplifiedRegions + " true))");
                                System.out.println("Simplification complete.");
                            } else {
                                region = null;
                                throw new StopException();
                            }
                        }

                    } catch (InterruptedException ie) {
                    }
                }
            } else if (aeresult instanceof UnknownResult) {
                    throw new StopException();
            }
    }

    protected void createQueryVariables(AevalSolver aesolver, RefinedRegion region, int k, boolean skolemize) {
        if (settings.scratch) {
            aesolver.scratch.println("; Transition relation");
        }

        aesolver.defineSVar(factorSpec.getFixpointTransitionRelation());
//        aesolver.defineTVar(factorSpec.getFixpointTransitionRelation(), false);
        aesolver.defineTVar(factorSpec.getRefinementFixpointTransitionRelation(), false);


        if (settings.scratch) {
            aesolver.scratch.println("; Universally quantified variables");
        }

        aesolver.defineSVar(new VarDecl(INIT.str, NamedType.BOOL));
        aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL), false);

        List<VarDecl> realOutputs = getRealizabilityOutputVarDecls(spec);
//        List<VarDecl> realOutputs = getRealizabilityOutputVarDecls(factorSpec);

        if (skolemize) {
            List<String> factorIds = new ArrayList<>();
            for (VarDecl factorOutput : factorOutputs) {
                factorIds.add(factorOutput.id);
            }

            realOutputs.removeIf(vd -> factorIds.contains(vd.id));
        }

//        realOutputs.addAll(uncomputedOutputs);
        List<VarDecl> dummyoutvars = getOffsetVarDecls(0, realOutputs);

        List<VarDecl> preinvars = getOffsetVarDecls(-1, getRealizabilityInputVarDecls(spec));
        List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls(spec));
//
//        List<VarDecl> preinvars = getOffsetVarDecls(-1, getRealizabilityInputVarDecls(factorSpec));
//        List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls(factorSpec));

        List<VarDecl> offsetoutvars = getOffsetVarDecls(2, realOutputs);


        for (VarDecl vd : dummyoutvars) {
            aesolver.defineSVar(vd);
            if (skolemize) {
                aesolver.defineTVar(vd, false);
            }
        }

        for (VarDecl in : preinvars) {
            aesolver.defineSVar(in);
            aesolver.defineTVar(in, false);
        }

        List<VarDecl> preoutvars = getOffsetVarDecls(-1, getRealizabilityOutputVarDecls(spec));
//        List<VarDecl> preoutvars = getOffsetVarDecls(-1, getRealizabilityOutputVarDecls(factorSpec));

//        preoutvars.addAll(getOffsetVarDecls(-1, uncomputedOutputs));

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
            if (skolemize) {
//                aesolver.defineSVar(out);
//                aesolver.defineTVar(out, false);
            } else {
                aesolver.defineTVar(out, true);
            }
        }

        if (skolemize) {
            for (VarDecl factorOutput : factorOutputs) {
                StreamIndex dummysi = new StreamIndex(factorOutput.id, 0);
                aesolver.defineSVar(new VarDecl(dummysi.getEncoded().str, factorOutput.type));

                StreamIndex si = new StreamIndex(factorOutput.id, 2);
                aesolver.defineTVar(new VarDecl(si.getEncoded().str, factorOutput.type), true);
            }

//            for (VarDecl factorLocal : factorSpec.node.locals) {
//                StreamIndex factorLocalIndex = new StreamIndex(factorLocal.id, 2);
//                aesolver.defineTVar(new VarDecl(factorLocalIndex.getEncoded().str, factorLocal.type), true);
//            }

            for (String prop : factorSpec.node.properties) {
                StreamIndex propsi = new StreamIndex(prop, 2);
                aesolver.defineTVar(new VarDecl(propsi.getEncoded().str, NamedType.BOOL), true);
            }

        }

        if (settings.scratch) {
            aesolver.scratch.println("; Assertion for T - universal part");
        }

        aesolver.assertSPart(getTransition(0, INIT));

        if (settings.scratch) {
            aesolver.scratch.println("; Constraints for universal part of the formula");
        }


//        for (VarDecl vd : Util.getVarDecls(spec.node)) {
//            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
//            if (constraint != null) {
//                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(-1)));
//                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(0)));
//            }
//        }
//
        for (VarDecl vd : Util.getVarDecls(factorSpec.node)) {
            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
            if (constraint != null) {
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(-1)));
                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(0)));
            }
        }
//
//        for (VarDecl vd : getRealizabilityInputVarDecls(factorSpec)) {
//            Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
//            if (constraint != null) {
//                aesolver.assertSPart(constraint.accept(new Lustre2Sexp(0)));
//            }
//        }

        if(skolemize) {
//            aesolver.assertSPart(getUniversalOutputVariablesAssertionSkolemize(-1));
            aesolver.assertSPart(getUniversalOutputVariablesAssertionSkolemize(0));
        }
        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for blocked regions - universal part of the formula");
        }

        if (region != null && !region.getRefinedRegion().equals("true")) {
            aesolver.sendBlockedRegionSPart(region.getRefinedRegion());
        }

        if (region != null && !region.getRefinedRegion().equals("true")) {
//            && !maximalsubset) {
//            aesolver.sendBlockedRegionSPart(convertOutputsToNextStep(region.getRefinedRegion(), -1,
//                    preoutvars, true));
        }

//        if (currentFixpoint != null && !currentFixpoint.getRefinedRegion().equals("true")) {
//                aesolver.sendBlockedRegionSPart(convertOutputsToNextStep(currentFixpoint.getRefinedRegion(), -1,
//                        preoutvars, true));
//            }

        if (settings.scratch) {
            aesolver.scratch.println("; Constraints for existential part of the formula");
        }

        if (skolemize) {
//            for (VarDecl vd : Util.getVarDecls(spec.node)) {
//            for (VarDecl vd : Util.getVarDecls(factorSpec.node)) {
//                Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
//                if (constraint != null) {
//                    aesolver.assertTPart(constraint.accept(new Lustre2Sexp(-1)), true);
//                }
//            }
//            for (VarDecl currin : getRealizabilityInputVarDecls(spec)) {
//            for (VarDecl currin : getRealizabilityInputVarDecls(factorSpec)) {
//                Expr constraint = LustreUtil.typeConstraint(currin.id, currin.type);
//                if (constraint != null) {
//                    aesolver.assertTPart(constraint.accept(new Lustre2Sexp(0)), true);
//                }
//            }
//
//            for (VarDecl realout : realOutputs) {
//                Expr constraint = LustreUtil.typeConstraint(realout.id, realout.type);
//                if (constraint != null) {
//                    aesolver.assertTPart(constraint.accept(new Lustre2Sexp(0)), true);
//                }
//            }
            for (VarDecl factorOutput : factorOutputs) {
                Expr constraint = LustreUtil.typeConstraint(factorOutput.id, factorOutput.type);
                if (constraint != null) {
                    aesolver.assertTPart(constraint.accept(new Lustre2Sexp(2)), true);
                }
            }
        } else {
            for (VarDecl vd : getRealizabilityOutputVarDecls(factorSpec)) {
                Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
                if (constraint != null) {
                    aesolver.assertTPart(constraint.accept(new Lustre2Sexp(2)), true);
                }
            }
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for blocked regions - existential part of the formula");
        }

        if (region !=null && !region.getRefinedRegion().equals("true") && !region.getRefinedRegion().equals("Empty")) {
//            if (!maximalsubset) {
                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getRefinedRegion(), -1,
                        getOffsetVarDecls(-1, getRealizabilityOutputVarDecls(spec)), false));
//            } else {
//                aesolver.sendBlockedRegionTPart(region.getRefinedRegion());
//            }
//            if (skolemize && k > 0) {
//                if (currentFixpoint !=null && !currentFixpoint.getRefinedRegion().equals("true") && !currentFixpoint.getRefinedRegion().equals("Empty")) {
//                    aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(currentFixpoint.getRefinedRegion(), -1,
//                            getOffsetVarDecls(-1, getRealizabilityOutputVarDecls(spec)), false));
//                }
//            }
        }

//        if (precondition !=null && !precondition.equals("true") && !precondition.equals("Empty")) {
//            if (skolemize) {
//                // April 22 : commented following line out
//                aesolver.sendBlockedRegionSPart(precondition);
////
//                String modifiedprecondition = precondition;
//                for (VarDecl factorOutput : factorOutputs) {
//                    modifiedprecondition = modifiedprecondition.replaceAll(factorOutput.id + "\\$0", factorOutput.id + "\\$2");
//                }
//                aesolver.sendBlockedRegionTPart(modifiedprecondition);
//            } else {
//                aesolver.sendBlockedRegionSPart(precondition);
//                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(precondition, 0, getOffsetVarDecls(0, getRealizabilityOutputVarDecls(spec)), false));
//            }
//        }
//        aesolver.assertSPart(StreamIndex.conjoinEncodings(factorSpec.node.properties, 0));
    }

    protected List<VarDecl> getOffsetVarDecls(int k) {
        return getOffsetVarDecls(k, Util.getVarDecls(factorSpec.node));
    }

    protected List<VarDecl> getOffsetVarDecls(int k, List<VarDecl> varDecls) {
        List<VarDecl> result = new ArrayList<>();
        for (VarDecl vd : varDecls) {
            StreamIndex si = new StreamIndex(vd.id, k);
            result.add(new VarDecl(si.getEncoded().str, vd.type));
        }
        return result;
    }

    protected List<VarDecl> getFactoredOffsetVarDecls(int k, List<VarDecl> varDecls) {
        List<VarDecl> result = new ArrayList<>();
        List<String> factorIds = new ArrayList<>();
        List<String> locIds = new ArrayList<>();
        for (VarDecl factorOutput : factorOutputs) {
            factorIds.add(factorOutput.id);
        }
        for (VarDecl loc : factorSpec.node.locals) {
            locIds.add(loc.id);
        }

        for (VarDecl vd : varDecls) {
            if (factorIds.contains(vd.id) || factorSpec.node.properties.contains(vd.id)) {
//                    || locIds.contains(vd.id)) {
                StreamIndex si = new StreamIndex(vd.id, k);
                result.add(new VarDecl(si.getEncoded().str, vd.type));
            } else {
                StreamIndex si = new StreamIndex(vd.id, 0);
                result.add(new VarDecl(si.getEncoded().str, vd.type));
            }
        }
        return result;
    }

    public List<VarDecl> getRealizabilityOutputVarDecls(Specification spec) {
        List<String> realizabilityInputs = spec.node.realizabilityInputs;
        List<VarDecl> all = Util.getVarDecls(spec.node);

        all.removeIf(vd -> realizabilityInputs.contains(vd.id));
        return all;
    }

    public List<VarDecl> getRealizabilityInputVarDecls(Specification spec) {
        List<String> realizabilityInputs = spec.node.realizabilityInputs;
        List<VarDecl> all = Util.getVarDecls(spec.node);
        all.removeIf(vd -> !realizabilityInputs.contains(vd.id));
        return all;
    }

    protected Sexp getUniversalOutputVariablesAssertionSkolemize(int k){
        List<Sexp> conjuncts = new ArrayList<>();
        List<Sexp> equatities = new ArrayList<>();

        List<VarDecl> realOutputs = getRealizabilityOutputVarDecls(spec);
//        List<VarDecl> realOutputs = getRealizabilityOutputVarDecls(factorSpec);
        realOutputs.addAll(uncomputedOutputs);

        List<String> factorIds = new ArrayList<>();
        for (VarDecl factorOutput : factorOutputs) {
            factorIds.add(factorOutput.id);
        }
        realOutputs.removeIf(vd -> factorIds.contains(vd.id));
        conjuncts.addAll(getSymbols(getOffsetVarDecls(k, realOutputs)));
        for (Sexp c : conjuncts) {
            equatities.add(new Cons("=", c, c));
        }
        return SexpUtil.conjoin(equatities);
    }

    protected Sexp getUniversalInputVariablesAssertion(int k){
        List<Sexp> conjuncts = new ArrayList<>();
        List<Sexp> equalities = new ArrayList<>();
        conjuncts.addAll(getSymbols(getOffsetVarDecls(k, getRealizabilityInputVarDecls(spec))));
        for (Sexp c : conjuncts) {
            equalities.add(new Cons("=", c, c));
        }
        return SexpUtil.conjoin(equalities);
    }


    protected Sexp getTransition(int k, Sexp init) {
        List<Sexp> args = new ArrayList<>();
        args.add(init);
        args.addAll(getSymbols(getOffsetVarDecls(k - 1)));
        args.addAll(getSymbols(getOffsetVarDecls(k)));
        return new Cons(factorSpec.getTransitionRelation().getName(), args);
    }

    protected Sexp getTransition(int k, boolean init) {
        return getTransition(k, Sexp.fromBoolean(init));
    }

    protected Sexp getAevalTransition(int k, Sexp init) {
        List<Sexp> args = new ArrayList<>();
        args.add(init);
        args.addAll(getSymbols(getOffsetVarDecls(k - 1)));
        args.addAll(getSymbols(getOffsetVarDecls(k,
                getRealizabilityInputVarDecls(factorSpec))));

        if (skolemize) {
            args.addAll(getSymbols(getFactoredOffsetVarDecls(k + 2,
                    getRealizabilityOutputVarDecls(factorSpec))));
        } else {
            args.addAll(getSymbols(getOffsetVarDecls(k + 2,
                    getRealizabilityOutputVarDecls(factorSpec))));
        }
        return new Cons(factorSpec.getTransitionRelation().getName(), args);
    }

    protected Sexp getAevalInductiveTransition(int k) {
        if (k == 0) {
            return getAevalTransition(0, INIT);
        } else {
            return getAevalTransition(k, false);
        }
    }

    protected Sexp getAevalTransition(int k, boolean init) {
        return getAevalTransition(k, Sexp.fromBoolean(init));
    }


    protected Sexp getAssertions() {
        Sexp assertions = Lustre2Sexp.getConjunctedAssertions(factorSpec.node);
        return assertions;
    }

    protected Sexp getNextStepAssertions() {
        Sexp assertions = Lustre2Sexp.getNextStepConjunctedAssertions(factorSpec.node);
        return assertions;
    }

    public String getFixpointRegion() {
        if (region != null) {
            return region.getRefinedRegion();
        } else
            return null;
    }

    public String getPrecondition() {
        if (precondition != null) {
            return precondition;
        } else
            return null;
    }

    public String getSkolem() {
        if (skolem != null) {
            return skolem;
        } else
            return null;
    }

    public boolean getMaximalSubsetFlag() {
        return maximalsubset;
    }

    private PrintWriter getaevalScratch(Specification spec, String name) {
        if (settings.scratch) {
            String filename = settings.filename + ".aeval" + "." + name + spec.node.properties.get(0) + spec.node.properties.get(spec.node.properties.size()-1) + ".smt2";
            try {
                return new PrintWriter(new FileOutputStream(filename), true);
            } catch (FileNotFoundException e) {
                throw new JKindException("Unable to open scratch file: " + filename, e);
            }
        } else {
            return null;
        }
    }

    protected String convertOutputsToNextStep(String region, int k, List<VarDecl> offsetVarDecls, boolean lhs) {
        String converted = region;
        String newvarid;
        if (lhs) {
//            if(skolemize) {
//                for (VarDecl factor : factorOutputs) {
//                    if(converted.contains(factor.id + "$0")) {
//                    }
//                    converted = converted.replaceAll(factor.id + "\\$~1", factor.id + "\\$0");
//                }
//            } else {



//            for (VarDecl var : offsetVarDecls) {
//                if (converted.contains(var.id)) {
//                    newvarid = var.id.replaceAll("\\$~1", "\\$0");
//                    converted = converted.replace(var.id, newvarid);
//                }
//            }

            converted = converted.replaceAll("\\$~1", "\\$0");



//            }
        } else {
            if (skolemize) {
//                if (maximalsubset) {
//                    for (VarDecl factor : factorOutputs) {
////                    if (converted.contains(factor.id + "$0")) {
//                        converted = converted.replaceAll(factor.id + "\\$0", factor.id + "\\$2");
////                    } else {
////                        converted = converted.replaceAll(factor.id + "\\$~1", factor.id + "\\$2");
////                    }
//                    }
//                    for (String prop : factorSpec.node.properties) {
//                        converted = converted.replaceAll(prop + "\\$0", prop + "\\$2");
//                    }
////                    for (VarDecl var : offsetVarDecls) {
////                        if (converted.contains(var.id)) {
////                            newvarid = var.id.replaceAll("\\$~1", "\\$0");
////                            converted = converted.replace(var.id, newvarid);
////                        }
////                    }
//                } else {
                    for (VarDecl factor : factorOutputs) {
//                        converted = converted.replaceAll(factor.id + "\\$~1", factor.id + "\\$2");
//
                        converted = converted.replaceAll(factor.id + "\\$0", factor.id + "\\$2");
                    }
//                converted = converted.replaceAll("\\$~1", "\\$0");
                    for (String prop : factorSpec.node.properties) {
                        converted = converted.replaceAll(prop + "\\$0", prop + "\\$2");
                    }
//                    for (VarDecl var : offsetVarDecls) {
//                        if (converted.contains(var.id)) {
//                            newvarid = var.id.replaceAll("\\$~1", "\\$0");
//                            converted = converted.replace(var.id, newvarid);
//                        }
//                    }
//                }
            } else {
                for (VarDecl off : offsetVarDecls) {
                    String varid = off.id;
                    if (converted.contains(varid)) {
                        if (k == -1) {
                            newvarid = varid.replaceAll("\\$~1", "\\$2");
                        } else {
                            newvarid = varid.replaceAll("\\$" + k, "\\$2");
                        }
                        converted = converted.replace(varid, newvarid);
                    }
                }
            }

        }
        return converted;
    }
}