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
    private Specification spec;
    private Z3Solver solver;
    private RefinedRegion region;
    private AevalSolver aesolver;
    protected final String name;
    private ValidSubset vsubset;
    private boolean skolemize;
    private String precondition;
    private List<VarDecl> factorOutputs;
    private String skolem;


    public RealizabilityFixpointSubEngine(Specification spec, JRealizabilitySettings settings, RealizabilityDirector director, boolean skolemize, RefinedRegion region, String precondition, List<VarDecl> factorOutputs) {
        super(spec, settings, director);
        this.name = super.name;
        this.aevalscratch = getaevalScratch(spec, name);
        this.spec = spec;
        this.skolemize = skolemize;
        this.region = region;
        this.precondition = precondition;
        this.skolem = skolem;
        this.factorOutputs = factorOutputs;
    }

    @Override
    public void run() {
        try {
            solver = new Z3Solver(settings.filename + "." + name + spec.node.properties.get(0) + spec.node.properties.get(spec.node.properties.size()-1) + "_" + factorOutputs.get(0) + factorOutputs.get(factorOutputs.size()-1), LinearChecker.isLinear(spec.node));
            solver.define(new VarDecl(INIT.str, NamedType.BOOL));
            List<VarDecl> dummyoutvars = getOffsetVarDecls(0, getRealizabilityOutputVarDecls());
            List<VarDecl> preinvars = getOffsetVarDecls(-1, getRealizabilityInputVarDecls());
            List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls());

            for (VarDecl vd : dummyoutvars) {
                solver.define(vd);
            }

            for (VarDecl in : preinvars) {
                solver.define(in);
            }

            List<VarDecl> preoutvars = getOffsetVarDecls(-1, getRealizabilityOutputVarDecls());

            for (VarDecl vd : preoutvars) {
                solver.define(vd);
            }

            for (VarDecl in : currinvars) {
                solver.define(in);
            }

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
        System.out.println(spec.node.properties + " " + factorOutputs.get(0) +
                factorOutputs.get(factorOutputs.size()-1) + ",frame =" + k + " " + skolemize);
            aesolver = new AevalSolver(settings.filename, name + spec.node.properties.get(0) + spec.node.properties.get(spec.node.properties.size()-1) + k + factorOutputs.get(0).id + factorOutputs.get(factorOutputs.size()-1).id + skolemize, aevalscratch);
//            aevalscratch.aecomment("; Frame = " + (k) + " Properties : " + spec.node.properties);

            //if skolemize, only one output is existentially quantified
            createQueryVariables(aesolver, region, k, skolemize);

            AevalResult aeresult;
//            if (skolemize) {
//                aeresult = aesolver.realizabilityQuery(getAevalInductiveTransition(0),
//                        StreamIndex.conjoinEncodings(spec.node.properties, 0), skolemize);
//            } else {
                aeresult = aesolver.realizabilityQuery(getAevalInductiveTransition(0),
                        StreamIndex.conjoinEncodings(spec.node.properties, 2), false);
//                        StreamIndex.conjoinEncodings(spec.node.properties, 2), skolemize);
//            }
//        System.out.println(region.getRefinedRegion());
//        System.out.println(precondition);
            if (aeresult instanceof ValidResult) {
//                if (skolemize) {
                    System.out.println(spec.node.properties + " : valid");
//                    precondition = getFixpointRegion();
//                    skolem = ((ValidResult) aeresult).getSkolem();
//                    throw new StopException();
//                } else {
//                    System.out.println(spec.node.properties + " : valid");
//                    try {
//                        RealizabilityFixpointSubEngine skolemEngine = new RealizabilityFixpointSubEngine(spec, settings, director, true, region, precondition, factorOutputs);
//                        Thread extractorThread = new Thread(skolemEngine);
//                        extractorThread.start();
//                        extractorThread.join();
//                        precondition = skolemEngine.getFixpointRegion();
//                        skolem = skolemEngine.getSkolem();
//                    } catch (InterruptedException ie) {
//
//                    }
//                }
                throw new StopException();
            } else if (aeresult instanceof InvalidResult) {
                    System.out.println(spec.node.properties + " : invalid");
                String result = ((InvalidResult) aeresult).getValidSubset();
                if (result.equals("Empty")) {
//                    sendUnrealizable(k);
                    region = new RefinedRegion("Empty");
                    throw new StopException();
                } else {
                    String negatedsimplified = "(assert (and " + solver.simplify("(assert (not" + result + ")", null, null) + " true))";
                    ValidSubset negatedsubset = new ValidSubset(negatedsimplified);

                    try {
                        String simplifiedRegions;
                        RegionExtractor negatedsubsetExtractor = new RegionExtractor(spec, this, k,
                                region, negatedsubset, true, false, factorOutputs, skolemize, precondition);

                        Thread negatedsubsetThread = new Thread(negatedsubsetExtractor);
                        negatedsubsetThread.start();
                        negatedsubsetThread.join();
                        AevalResult negatedsubsetResult = negatedsubsetExtractor.getResult();

                        if (negatedsubsetResult instanceof ValidResult) {
                            System.out.println(spec.node.properties + " REFINEMENT IS VALID");
                            region = new RefinedRegion("Empty");
                            throw new StopException();
                        } else {
                            RefinedRegion negatedsubsetRegion = new RefinedRegion(negatedsubsetExtractor.getRegion());
                            if (region != null && !region.getRefinedRegion().equals("true")) {

                                if (negatedsubsetRegion.getRefinedRegion() != null) {
                                    simplifiedRegions = solver.simplify(region.getRefinedRegion(), null, negatedsubsetRegion.getRefinedRegion());
                                } else {
                                    throw new StopException();
                                }
                            } else {
                                if (negatedsubsetRegion.getRefinedRegion() != null) {
                                    simplifiedRegions = solver.simplify(null, null, negatedsubsetRegion.getRefinedRegion());
                                } else {
                                    throw new StopException();
                                }
                            }
                            if (!simplifiedRegions.equals("  false\n" + "  ")) {
                                region = new RefinedRegion("(assert (and\n" + simplifiedRegions + " true))");
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

        aesolver.defineSVar(spec.getFixpointTransitionRelation());
        aesolver.defineTVar(spec.getFixpointTransitionRelation(), false);


        if (settings.scratch) {
            aesolver.scratch.println("; Universally quantified variables");
        }

        aesolver.defineSVar(new VarDecl(INIT.str, NamedType.BOOL));
        aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL), false);

        List<VarDecl> realOutputs = getRealizabilityOutputVarDecls();

        if (skolemize) {
            List<String> factorIds = new ArrayList<>();
            for (VarDecl factorOutput : factorOutputs) {
                factorIds.add(factorOutput.id);
            }
            realOutputs.removeIf(vd -> factorIds.contains(vd.id));

            List<String> locIds = new ArrayList<>();
            for (VarDecl loc : spec.node.locals) {
                locIds.add(loc.id);
            }
            realOutputs.removeIf(vd -> locIds.contains(vd.id));
        }

        List<VarDecl> dummyoutvars = getOffsetVarDecls(0, realOutputs);
        List<VarDecl> preinvars = getOffsetVarDecls(-1, getRealizabilityInputVarDecls());
        List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls());
        List<VarDecl> offsetoutvars = getOffsetVarDecls(2, realOutputs);


        for (VarDecl vd : dummyoutvars) {
            aesolver.defineSVar(vd);
            if (skolemize) {
                aesolver.defineTVar(vd, false);
            }
        }

        if (skolemize) {
            for (VarDecl loc : getOffsetVarDecls(0, spec.node.locals)) {
                aesolver.defineSVar(loc);
            }
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

            for (VarDecl loc : getOffsetVarDecls(2, spec.node.locals)) {
                aesolver.defineTVar(loc, true);
            }

//            for (String prop : spec.node.properties) {
//                StreamIndex propsi = new StreamIndex(prop, 2);
//                aesolver.defineTVar(new VarDecl(propsi.getEncoded().str, NamedType.BOOL), true);
//            }

        }


        if (settings.scratch) {
            aesolver.scratch.println("; Assertion for T - universal part");
        }

        aesolver.assertSPart(getTransition(0, INIT));

        if (settings.scratch) {
            aesolver.scratch.println("; Constraints for universal part of the formula");
        }

        aesolver.assertSPart(getNextStepAssertions());
//        aesolver.assertSPart(StreamIndex.conjoinEncodings(spec.node.properties, -1));
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

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for blocked regions - universal part of the formula");
        }

        if (region != null && !region.getRefinedRegion().equals("true")) {
            aesolver.sendBlockedRegionSPart(region.getRefinedRegion());
        }

//        if (k == 0 || !skolemize) {
            if (region != null && !region.getRefinedRegion().equals("true")) {
                aesolver.sendBlockedRegionSPart(convertOutputsToNextStep(region.getRefinedRegion(), -1,
                        preoutvars, true));
            }
//        }

        if (settings.scratch) {
            aesolver.scratch.println("; Constraints for existential part of the formula");
        }

        if (skolemize) {
            for (VarDecl factorOutput : factorOutputs) {
                Expr constraint = LustreUtil.typeConstraint(factorOutput.id, factorOutput.type);
                if (constraint != null) {
                    aesolver.assertTPart(constraint.accept(new Lustre2Sexp(2)), true);
                }
            }
        } else {
            for (VarDecl vd : getRealizabilityOutputVarDecls()) {
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
            if (skolemize) {
                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getRefinedRegion(), -1,
                        getFactoredOffsetVarDecls(-1, getRealizabilityOutputVarDecls()), false));
            } else {
                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getRefinedRegion(), -1,
                        getOffsetVarDecls(-1, getRealizabilityOutputVarDecls()), false));
            }
        }

        if (precondition !=null && !precondition.equals("true") && !precondition.equals("Empty")) {
            if (skolemize) {
//                aesolver.sendBlockedRegionSPart(precondition);
//
                String modifiedprecondition = precondition;
                for (VarDecl factorOutput : factorOutputs) {
                    modifiedprecondition = modifiedprecondition.replaceAll(factorOutput.id + "\\$0", factorOutput.id + "\\$2");
                }
                for (VarDecl loc : spec.node.locals) {
                    modifiedprecondition = modifiedprecondition.replaceAll(loc.id + "\\$0", loc.id + "\\$2");
                }
                aesolver.sendBlockedRegionTPart(modifiedprecondition);
            } else {
                aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(precondition, 0, getOffsetVarDecls(0, getRealizabilityOutputVarDecls()), false));
//                aesolver.sendBlockedRegionTPart(precondition);
            }
        }

//        if (skolem !=null && !skolem.equals("true")) {
//            skolem = skolem.replaceAll("\\$2", "\\$0");
//            String[] extracted = skolem.split("assert");
////                String[] extracted = status.split("extracted skolem:");
//            aesolver.sendBlockedRegionTPart("(assert " + extracted[extracted.length - 1]);
//        }
    }

    protected List<VarDecl> getOffsetVarDecls(int k) {
        return getOffsetVarDecls(k, Util.getVarDecls(spec.node));
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
        for (VarDecl loc : spec.node.locals) {
            locIds.add(loc.id);
        }

        for (VarDecl vd : varDecls) {
            if (factorIds.contains(vd.id) || (locIds.contains(vd.id))) {
                StreamIndex si = new StreamIndex(vd.id, k);
                result.add(new VarDecl(si.getEncoded().str, vd.type));
            } else {
                StreamIndex si = new StreamIndex(vd.id, 0);
                result.add(new VarDecl(si.getEncoded().str, vd.type));
            }
        }
        return result;
    }

    public List<VarDecl> getRealizabilityOutputVarDecls() {
        List<String> realizabilityInputs = spec.node.realizabilityInputs;
        List<VarDecl> all = Util.getVarDecls(spec.node);

        all.removeIf(vd -> realizabilityInputs.contains(vd.id));
        return all;
    }

    public List<VarDecl> getRealizabilityInputVarDecls() {
        List<String> realizabilityInputs = spec.node.realizabilityInputs;
        List<VarDecl> all = Util.getVarDecls(spec.node);
        all.removeIf(vd -> !realizabilityInputs.contains(vd.id));
        return all;
    }

    protected Sexp getUniversalOutputVariablesAssertion(int k){
        List<Sexp> conjuncts = new ArrayList<>();
        List<Sexp> equatities = new ArrayList<>();
        conjuncts.addAll(getSymbols(getOffsetVarDecls(k, getRealizabilityOutputVarDecls())));
        for (Sexp c : conjuncts) {
            equatities.add(new Cons("=", c, c));
        }
        return SexpUtil.conjoin(equatities);
    }

    protected Sexp getUniversalInputVariablesAssertion(int k){
        List<Sexp> conjuncts = new ArrayList<>();
        List<Sexp> equalities = new ArrayList<>();
        conjuncts.addAll(getSymbols(getOffsetVarDecls(k, getRealizabilityInputVarDecls())));
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
        return new Cons(spec.getTransitionRelation().getName(), args);
    }

    protected Sexp getTransition(int k, boolean init) {
        return getTransition(k, Sexp.fromBoolean(init));
    }

    protected Sexp getAevalTransition(int k, Sexp init) {
        List<Sexp> args = new ArrayList<>();
        args.add(init);
        args.addAll(getSymbols(getOffsetVarDecls(k - 1)));
        args.addAll(getSymbols(getOffsetVarDecls(k,
                getRealizabilityInputVarDecls())));

        if (skolemize) {
            args.addAll(getSymbols(getFactoredOffsetVarDecls(k + 2,
                    getRealizabilityOutputVarDecls())));
        } else {
            args.addAll(getSymbols(getOffsetVarDecls(k + 2,
                    getRealizabilityOutputVarDecls())));
        }
        return new Cons(spec.getTransitionRelation().getName(), args);
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
        Sexp assertions = Lustre2Sexp.getConjunctedAssertions(spec.node);
        return assertions;
    }

    protected Sexp getNextStepAssertions() {
        Sexp assertions = Lustre2Sexp.getNextStepConjunctedAssertions(spec.node);
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
            if (skolemize) {
                for (VarDecl factor : factorOutputs) {
                    converted = converted.replaceAll(factor.id + "\\$~1", factor.id + "\\$0");
                }
            } else {
                converted = converted.replaceAll("\\$~1", "\\$0");
            }
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
}



// COMMENTED OUT FOR COMPILATION OF FACTORED SYNTHESIS

//    private void splitandcheckRealizable(int k) {
//        try {
//            List<String> propsublist2 = new ArrayList<>();
//            List<String> propsublist1 = new ArrayList<>();
//            propsublist2.addAll(spec.node.properties);
//            propsublist1.addAll(propsublist2.subList(0, spec.node.properties.size() / 2));
//            propsublist2.removeIf(prop -> propsublist1.contains(prop));
//
//            Node left = new NodeBuilder(spec.node).clearProperties().addProperties(propsublist1).build();
//            Node newleft = LustreSlicer.slice(left, spec.dependencyMap);
//            Specification leftspec = new Specification(newleft);
//
//            Node right = new NodeBuilder(spec.node).clearProperties().addProperties(propsublist2).build();
//            Node newright = LustreSlicer.slice(right, spec.dependencyMap);
//            Specification rightspec = new Specification(newright);
//
//            RealizabilityFixpointSubEngine leftEngine = new RealizabilityFixpointSubEngine(leftspec, settings, director);
//            Thread leftThread = new Thread(leftEngine);
//            RealizabilityFixpointSubEngine rightEngine = new RealizabilityFixpointSubEngine(rightspec, settings, director);
//            Thread rightThread = new Thread(rightEngine);
//            leftThread.start();
//            rightThread.start();
//            leftThread.join();
//            rightThread.join();
//            String leftFixpointRegion = leftEngine.getFixpointRegion();
//            String rightFixpointRegion = rightEngine.getFixpointRegion();
//
////            if (leftFixpointRegion != null && rightFixpointRegion != null) {
////                region = new RefinedRegion(solver.simplify(null, leftFixpointRegion, rightFixpointRegion));
////            } else if (leftFixpointRegion == null && rightFixpointRegion != null) {
////                region = new RefinedRegion(solver.simplify(null, null, rightFixpointRegion));
////            } else if (leftFixpointRegion != null && rightFixpointRegion == null) {
////                region = new RefinedRegion(solver.simplify(null, leftFixpointRegion, null));
//
//
//                //before modification of region structure
//            if (leftFixpointRegion != null && rightFixpointRegion != null) {
//                region = new RefinedRegion("(assert (and\n" +
//                        solver.simplify(null, leftFixpointRegion, rightFixpointRegion) + " true))");
//            } else if (leftFixpointRegion == null && rightFixpointRegion != null) {
//                region = new RefinedRegion("(assert (and\n" +
//                        solver.simplify(null, null, rightFixpointRegion) + " true))");
//            } else if (leftFixpointRegion != null && rightFixpointRegion == null) {
//                region = new RefinedRegion("(assert (and\n" +
//                        solver.simplify(null, leftFixpointRegion, null) + " true))");
//
////            System.out.println(spec.node.properties + " region of subfixpoints: " + region.getRefinedRegion());
//
////            } else {
////                //both subproblems are valid for F(s) = true
////                //what happens in this case?
////                //Do we care to explore the subproblem of two such subproblems?
////
////                //1)This is definitely not true for "unrealizable" subproblems, as the
////                // bigger scope from the next upper level can introduce constraints that
////                //lead to realizability
////
////                //2) What happens if both subproblems are valid? If we ignore a check
////                //   on their conjunction, we miss constraints that lead to the realizability
////                //   of these two, but these constraints may over constraint the bigger subproblem...
////                //   The previous sentence shouldnt be true though, as the constraints are a greatest fixpoint
////                throw new StopException();
//            }
//
//            if (region != null && region.getRefinedRegion().equals("(assert (and\n  false\n" +
//                    "  "+ " true))")) {
//                region = null;
////                sendUnrealizable(k);
//            }
//
//
//        } catch (InterruptedException ie) {
//        }
//    }

