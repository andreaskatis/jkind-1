package jkind.realizability.engines;

import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.aeval.*;
import jkind.engines.StopException;
import jkind.lustre.Expr;
import jkind.lustre.LustreUtil;
import jkind.lustre.NamedType;
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

public class RegionExtractor implements Runnable {
    private AevalSolver aesolver;
    private AevalResult result;
    private JRealizabilitySettings settings;
    private ValidSubset subset;
    private int k;
    private String name;
    private PrintWriter aevalscratch;
    private Specification spec;
    private boolean negate;
    private boolean subrefine;
    private RefinedRegion region;
    private RealizabilityFixpointEngine engine;
    private boolean skolemize;
    private List<VarDecl> factorOutputs;
    private String precondition;


    public RegionExtractor(Specification spec, RealizabilityFixpointEngine engine, int k, RefinedRegion region, ValidSubset subset, boolean negate, boolean subrefine, List<VarDecl> factorOutputs, boolean skolemize, String precondition) {
        this.settings = engine.settings;
        this.subset = subset;
        this.k = k;
        this.name = engine.name;
        this.aevalscratch = getaevalScratch(spec, name);
        this.spec = spec;
        this.negate = negate;
        this.region = region;
        this.engine = engine;
        this.subrefine = subrefine;
        this.skolemize = skolemize;
        this.factorOutputs = factorOutputs;
        this.precondition = precondition;
    }

    public RegionExtractor(Specification spec, RealizabilityFixpointSubEngine engine, int k, RefinedRegion region, ValidSubset subset, boolean negate, boolean subrefine) {
        this.settings = engine.settings;
        this.subset = subset;
        this.k = k;
        this.name = engine.name;
        this.aevalscratch = getaevalScratch(spec, name);
        this.spec = spec;
        this.negate = negate;
        this.region = region;
        this.engine = engine;
        this.subrefine = subrefine;

    }

    public void run() {
//        System.out.println(spec.node.properties + ", refinement = " + k);
        aesolver = new AevalSolver(settings.filename, name + "subset" + spec.node.properties.get(0) + spec.node.properties.get(spec.node.properties.size()-1) + "_"+ factorOutputs.get(0).id + factorOutputs.get(factorOutputs.size()-1).id + "_" + skolemize + k, aevalscratch);
        aevalscratch.println(";Refinement = " + k);
        createSubQueryVariablesAndAssertions(aesolver, subset, k);
        result = aesolver.refinementQuery();
        if (result instanceof ValidResult) {
        } else if (result instanceof InvalidResult) {
            System.out.println(spec.node.properties + "refinement=" + k + ", invalid result," + negate + subrefine);
            String subset = ((InvalidResult) result).getValidSubset();
            if (subset.equals("Empty")) {
                region = null;
            } else {
                region = new RefinedRegion("(assert (not" + (subset + ")"));
            }
        } else {
            engine.sendUnknown();
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
                aesolver.defineSVar(in);

                aesolver.defineTVar(in, false);
        }

        for (VarDecl out : preoutvars) {
            aesolver.defineSVar(out);
                aesolver.defineTVar(out, false);
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Existentially quantified variables");
        }

        aesolver.defineTVar(new VarDecl(INIT.str, NamedType.BOOL), true);

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

        List<VarDecl> currinvars = getOffsetVarDecls(0, getRealizabilityInputVarDecls());
        List<VarDecl> curroutvars = getOffsetVarDecls(0, realOutputs);

        for (VarDecl in : currinvars) {
            aesolver.defineTVar(in, true);
        }

        for (VarDecl out : curroutvars) {
            if (skolemize) {
                aesolver.defineSVar(out);
                aesolver.defineTVar(out, false);
            } else {
                aesolver.defineTVar(out, true);
            }
        }

        if (skolemize) {
            for (VarDecl factorOutput : factorOutputs) {
                StreamIndex si = new StreamIndex(factorOutput.id, 0);
                aesolver.defineTVar(new VarDecl(si.getEncoded().str, factorOutput.type), true);
            }
            for (VarDecl loc : getOffsetVarDecls(0, spec.node.locals)) {
                aesolver.defineTVar(loc, true);
            }
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
            if (region != null && !region.getRefinedRegion().equals("true")) {
                aesolver.sendBlockedRegionSPart(region.getRefinedRegion());
            }
        aesolver.assertSPart(getUniversalInputVariablesAssertion(-1));
        aesolver.assertSPart(getUniversalOutputVariablesAssertion(-1));
        if (skolemize) {
            aesolver.assertSPart(getFactoredUniversalOutputVariablesAssertion(0));
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Assertions for existential part of the formula");
        }

        if(negate) {
            aesolver.assertTPart(getTransition(0, INIT), true);
        }
        if (!negate) {
            aesolver.assertTPart(new Cons("not", StreamIndex.conjoinEncodings(spec.node.properties, 0)), true);
        }
        if(negate) {
            aesolver.assertTPart(getNextStepAssertions(), true);
        }

        if (settings.scratch) {
            aesolver.scratch.println("; Constraints for existential part of the formula");
        }
        if (negate) {
            for (VarDecl vd : Util.getVarDecls(spec.node)) {
                Expr constraint = LustreUtil.typeConstraint(vd.id, vd.type);
                if (constraint != null) {
                    aesolver.assertTPart(constraint.accept(new Lustre2Sexp(0)), true);
                }
            }
        }

        if (subrefine) {
            aesolver.assertTPart(new Cons("not", StreamIndex.conjoinEncodings(spec.node.properties, 0)), true);
        } else {
            aesolver.sendSubsetTPart(subset.getValidSubset());
        }

        if (region != null && !region.getRefinedRegion().equals("true") && !region.getRefinedRegion().equals("Empty")) {
            aesolver.sendBlockedRegionTPart(convertOutputsToNextStep(region.getRefinedRegion(), preoutvars, true));
        }

//        if (precondition != null && !precondition.equals("true") && !precondition.equals("Empty")) {
////            aesolver.sendBlockedRegionSPart(precondition);
//            aesolver.sendBlockedRegionTPart(precondition);
//        }
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
        List<VarDecl> outputs = getRealizabilityOutputVarDecls();
        List<String> factorIds = new ArrayList<>();
        List<String> locIds = new ArrayList<>();
        for (VarDecl fvd : factorOutputs) {
            factorIds.add(fvd.id);
        }
        for (VarDecl loc : spec.node.locals) {
            locIds.add(loc.id);
        }

        outputs.removeIf(vd -> factorIds.contains(vd.id));
        outputs.removeIf(vd -> locIds.contains(vd.id));

        conjuncts.addAll(getSymbols(getOffsetVarDecls(k, outputs)));
        for (Sexp c : conjuncts) {
            equatities.add(new Cons("=", c, c));
        }
        return SexpUtil.conjoin(equatities);
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


    protected Sexp getAssertions() {
        Sexp assertions = Lustre2Sexp.getConjunctedAssertions(spec.node);
        return assertions;
    }

    protected Sexp getNextStepAssertions() {
        Sexp assertions = Lustre2Sexp.getNextStepConjunctedAssertions(spec.node);
        return assertions;
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
    protected List<VarDecl> getAbstractOffsetVarDecls(int k) {
        return getAbstractOffsetVarDecls(k, Util.getVarDecls(spec.node));
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
        return new Cons(spec.getTransitionRelation().getName(), args);
    }

    protected Sexp getTransition(int k, Sexp init) {
        List<Sexp> args = new ArrayList<>();
        args.add(init);
        args.addAll(getSymbols(getOffsetVarDecls(k - 1)));
        args.addAll(getSymbols(getOffsetVarDecls(k)));
        return new Cons(spec.getTransitionRelation().getName(), args);
    }

    protected static final Symbol INIT = Lustre2Sexp.INIT;

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

    protected List<Sexp> getSymbols(List<VarDecl> varDecls) {
        List<Sexp> result = new ArrayList<>();
        for (VarDecl vd : varDecls) {
            result.add(new Symbol(vd.id));
        }
        return result;
    }

    public String getRegion() {
        if (region != null) {
            return region.getRefinedRegion();
        } else {
            return null;
        }
    }

    public AevalResult getResult() {
        return result;
    }
}