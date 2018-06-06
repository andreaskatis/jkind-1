package jkind.realizability.engines;

        import jkind.JKindException;
        import jkind.JRealizabilitySettings;
        import jkind.aeval.*;
        import jkind.lustre.VarDecl;
        import jkind.realizability.engines.fixpoint.RefinedRegion;
        import jkind.sexp.Cons;
        import jkind.sexp.Sexp;
        import jkind.sexp.Symbol;
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

public class SplitFramesExtractor implements Runnable {
    private AevalSolver aesolver;
    private AevalResult result;
    private JRealizabilitySettings settings;
    private int k;
    private String name;
    private PrintWriter aevalscratch;
    private Specification spec, factorSpec;
    private RefinedRegion region, preRegion, remainderRegion;
    private RealizabilityFixpointEngine engine;
    private List<VarDecl> factorOutputs;


    public SplitFramesExtractor(Specification spec, Specification factorSpec, RealizabilityFixpointEngine engine, int k, RefinedRegion region, List<VarDecl> factorOutputs) {
        this.settings = engine.settings;
        this.k = k;
        this.name = engine.name;
        this.aevalscratch = getaevalScratch(spec, name);
        this.spec = spec;
        this.factorSpec = factorSpec;
        this.region = region;
        this.engine = engine;
        this.factorOutputs = factorOutputs;
    }

    public void run() {
        System.out.println(factorSpec.node.properties + ", frame splitting = " + k);
        aesolver = new AevalSolver(settings.filename, name + "split" + factorSpec.node.properties.get(0) + factorSpec.node.properties.get(factorSpec.node.properties.size()-1) + "_"+ factorOutputs.get(0).id + factorOutputs.get(factorOutputs.size()-1).id + "_" + k, aevalscratch);
        aevalscratch.println(";Frame splitting = " + k);
        createSubQueryVariablesAndAssertions(aesolver, k, true);
        result = aesolver.refinementQuery();
        if (result instanceof ValidResult) {
            preRegion = new RefinedRegion("true");
        } else if (result instanceof InvalidResult) {
            System.out.println(factorSpec.node.properties + "splitting=" + k + ", invalid result,");
            String subset = ((InvalidResult) result).getValidSubset();
            if (subset.equals("Empty")) {
                preRegion = new RefinedRegion("Empty");
            } else {
                preRegion = new RefinedRegion("(assert (not" + (subset + ")"));
            }
        } else {
            engine.sendUnknown();
        }

        System.out.println(factorSpec.node.properties + ", frame splitting, remainder = " + k);
        aesolver = new AevalSolver(settings.filename, name + "splitremainder" + factorSpec.node.properties.get(0) + factorSpec.node.properties.get(factorSpec.node.properties.size()-1) + "_"+ factorOutputs.get(0).id + factorOutputs.get(factorOutputs.size()-1).id + "_" + k, aevalscratch);
        aevalscratch.println(";Frame splitting, remainder = " + k);
        createSubQueryVariablesAndAssertions(aesolver, k, false);
        result = aesolver.refinementQuery();
        if (result instanceof ValidResult) {
            remainderRegion = new RefinedRegion("true");
        } else if (result instanceof InvalidResult) {
            System.out.println(factorSpec.node.properties + "splitting=" + k + ", invalid result,");
            String subset = ((InvalidResult) result).getValidSubset();
            if (subset.equals("Empty")) {
                remainderRegion = new RefinedRegion("Empty");
            } else {
                remainderRegion = new RefinedRegion("(assert (not" + (subset + ")"));
            }
        } else {
            engine.sendUnknown();
        }
    }



    protected void createSubQueryVariablesAndAssertions(AevalSolver aesolver, int k, boolean prestates) {

//        aesolver.defineTVar(factorSpec.getFixpointTransitionRelation(), true);

        if (settings.scratch) {
            aesolver.scratch.println("; Universally quantified variables");
        }

        List<VarDecl> preoutvars = getOffsetVarDecls(-1, getRealizabilityOutputVarDecls(spec));

        for (VarDecl out : preoutvars) {
            if (prestates) {
                aesolver.defineSVar(out);
            }
            aesolver.defineTVar(out, false);
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Existentially quantified variables");
        }

        List<VarDecl> realOutputs = getRealizabilityOutputVarDecls(spec);

        List<String> factorIds = new ArrayList<>();
        for (VarDecl factorOutput : factorOutputs) {
            factorIds.add(factorOutput.id);
        }
        realOutputs.removeIf(vd -> factorIds.contains(vd.id));
        realOutputs.removeIf(vd -> factorSpec.node.properties.contains(vd.id));
        List<VarDecl> curroutvars = getOffsetVarDecls(0, realOutputs);

        for (VarDecl out : curroutvars) {
            if (!prestates) {
                aesolver.defineSVar(out);
                aesolver.defineTVar(out, true);
            } else {
                aesolver.defineTVar(out, true);
            }
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for universal part of the formula");
        }

        aesolver.assertSPart(getUniversalOutputVariablesAssertion(prestates));
        aesolver.assertTPart(getUniversalOutputVariablesAssertion(!prestates), true);

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for existential part of the formula");
        }

        if (region != null && !region.getRefinedRegion().equals("true") && !region.getRefinedRegion().equals("Empty")) {
//            aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getRefinedRegion(), preoutvars, true));
            aesolver.sendBlockedRegionTPart(region.getRefinedRegion());
        }
    }


    private String convertOutputsToAbstract(String region, List<VarDecl> preoutvars, boolean lhs) {
        String converted = region;
        if (lhs) {
            for (VarDecl var : preoutvars) {
                String varid = var.id;
                if (converted.contains(varid)) {
                    String newvarid = varid + "abs";
                    converted = converted.replace(varid, newvarid);
                }
            }
        } else {
            for (VarDecl var : preoutvars) {
                String varid = var.id;
                if (converted.contains(varid)) {
                    String newvarid = varid.replaceAll("\\$~1", "\\$2") + "abs";
                    converted = converted.replace(varid, newvarid);
                }
            }
        }
        return converted;
    }

    private String convertOutputsToPreStep(String str) {
        String converted = str;
        converted = converted.replaceAll("\\$0", "\\$~1");
        return converted;
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


    protected Sexp getFactoredUniversalOutputVariablesAssertion(int k){
        List<Sexp> conjuncts = new ArrayList<>();
        List<Sexp> equatities = new ArrayList<>();
        List<VarDecl> outputs = getRealizabilityOutputVarDecls(spec);
        List<String> factorIds = new ArrayList<>();
        List<String> locIds = new ArrayList<>();
        for (VarDecl fvd : factorOutputs) {
            factorIds.add(fvd.id);
        }
        for (VarDecl loc : factorSpec.node.locals) {
            locIds.add(loc.id);
        }

        outputs.removeIf(vd -> factorIds.contains(vd.id));
        // are locals existentially quantified?
        outputs.removeIf(vd -> factorSpec.node.properties.contains(vd.id));
//        outputs.removeIf(vd -> locIds.contains(vd.id));

        conjuncts.addAll(getSymbols(getOffsetVarDecls(k, outputs)));
        for (Sexp c : conjuncts) {
            equatities.add(new Cons("=", c, c));
        }
        return SexpUtil.conjoin(equatities);
    }

    protected Sexp getUniversalOutputVariablesAssertion(boolean prestates){
        List<Sexp> conjuncts = new ArrayList<>();
        List<Sexp> equatities = new ArrayList<>();
        List<VarDecl> realOutputs = getRealizabilityOutputVarDecls(spec);

        if (prestates) {
            conjuncts.addAll(getSymbols(getOffsetVarDecls(-1, realOutputs)));
            for (Sexp c : conjuncts) {
                equatities.add(new Cons("=", c, c));
            }
        } else {
            List<String> factorIds = new ArrayList<>();
            for (VarDecl factorOutput : factorOutputs) {
                factorIds.add(factorOutput.id);
            }
            realOutputs.removeIf(vd -> factorIds.contains(vd.id));
            realOutputs.removeIf(vd -> factorSpec.node.properties.contains(vd.id));
            List<VarDecl> curroutvars = getOffsetVarDecls(0, realOutputs);
            conjuncts.addAll(getSymbols(curroutvars));
            for (Sexp c : conjuncts) {
                equatities.add(new Cons("=", c, c));
            }
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


    protected Sexp getAssertions() {
        Sexp assertions = Lustre2Sexp.getConjunctedAssertions(factorSpec.node);
        return assertions;
    }

    protected Sexp getNextStepAssertions() {
        Sexp assertions = Lustre2Sexp.getNextStepConjunctedAssertions(factorSpec.node);
        return assertions;
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
    protected List<VarDecl> getAbstractOffsetVarDecls(int k) {
        return getAbstractOffsetVarDecls(k, Util.getVarDecls(factorSpec.node));
    }

    protected List<VarDecl> getAbstractOffsetVarDecls(int k, List<VarDecl> varDecls) {
        List<VarDecl> result = new ArrayList<>();
        for (VarDecl vd : varDecls) {
            StreamIndex si = new StreamIndex(vd.id, k);
            result.add(new VarDecl(si.getEncoded().str + "abs", vd.type));
        }
        return result;
    }


    private Sexp getAbstractTransition(int i, Symbol init) {
        List<Sexp> args = new ArrayList<>();
        args.add(init);
        args.addAll(getSymbols(getAbstractOffsetVarDecls(k - 1)));
        args.addAll(getSymbols(getAbstractOffsetVarDecls(k)));
        return new Cons(factorSpec.getTransitionRelation().getName(), args);
    }

    protected Sexp getTransition(int k, Sexp init) {
        List<Sexp> args = new ArrayList<>();
        args.add(init);
        args.addAll(getSymbols(getOffsetVarDecls(k - 1)));
        args.addAll(getSymbols(getOffsetVarDecls(k)));
        return new Cons(factorSpec.getTransitionRelation().getName(), args);
    }

    protected static final Symbol INIT = Lustre2Sexp.INIT;

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

    protected List<Sexp> getSymbols(List<VarDecl> varDecls) {
        List<Sexp> result = new ArrayList<>();
        for (VarDecl vd : varDecls) {
            result.add(new Symbol(vd.id));
        }
        return result;
    }

    public String getRegion() {
        if (preRegion != null) {
            return preRegion.getRefinedRegion();
        } else {
            return null;
        }
    }

    public String getremainderRegion() {
        if (remainderRegion != null) {
            return remainderRegion.getRefinedRegion();
        } else {
            return null;
        }
    }

    public AevalResult getResult() {
        return result;
    }
}