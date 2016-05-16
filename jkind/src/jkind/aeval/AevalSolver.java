package jkind.aeval;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.JKindException;
import jkind.lustre.NamedType;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.solvers.smtlib2.Quoting;
import jkind.translation.Relation;

import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.util.Util;
import org.antlr.v4.runtime.RecognitionException;


public class AevalSolver extends AevalProcess{
    protected BufferedWriter toSPart;
    protected BufferedWriter toTPart;
    protected BufferedWriter toGuards;
    protected BufferedWriter toSkolvars;

    public AevalSolver(String scratchBase) {
        super(scratchBase);
        try {
            toSPart = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(scratchBase.split("\\.")[0] + "_s_part.smt2"), "utf-8"));
            toTPart = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(scratchBase.split("\\.")[0] + "_t_part.smt2"), "utf-8"));
            toGuards = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(scratchBase.split("\\.")[0] + "_guards_vars.smt2"), "utf-8"));
            toSkolvars = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(scratchBase.split("\\.")[0] + "_skol_vars.smt2"), "utf-8"));
        } catch (IOException ex) {
            throw new JKindException("Unable to open file", ex);
        }
    }

    public void assertSPart(Sexp sexp) {
        sendSPart(new Cons("assert", sexp));
    }

    protected void sendSPart(Sexp sexp) {
        String str = Quoting.quoteSexp(sexp).toString();
        scratch(str);
        try {
            toSPart.append(str);
            toSPart.newLine();
            toSPart.flush();
        } catch (IOException e) {
            throw new JKindException("Unable to write to " + getName() + ", "
                    + "probably due to internal JKind error", e);
        }
    }

    public void assertTPart(Sexp sexp) {
        sendTPart(new Cons("assert", sexp));
    }

    protected void sendTPart(Sexp sexp) {
        String str = Quoting.quoteSexp(sexp).toString();
        scratch(str);
        try {
            toTPart.append(str);
            toTPart.newLine();
            toTPart.flush();
        } catch (IOException e) {
            throw new JKindException("Unable to write to " + getName() + ", "
                    + "probably due to internal JKind error", e);
        }
    }

    public void assertGuards(Sexp sexp) {
        sendGuards(new Cons("assert", sexp));
    }

    protected void sendGuards(Sexp sexp) {
        String str = Quoting.quoteSexp(sexp).toString();
        scratch(str);
        try {
            toGuards.append(str);
            toGuards.newLine();
            toGuards.flush();
        } catch (IOException e) {
            throw new JKindException("Unable to write to " + getName() + ", "
                    + "probably due to internal JKind error", e);
        }
    }

    public void assertSkolvars(Sexp sexp) {
        sendSkolvars(new Cons("assert", sexp));
    }

    protected void sendSkolvars(Sexp sexp) {
        String str = Quoting.quoteSexp(sexp).toString();
        scratch(str);
        try {
            toSkolvars.append(str);
            toSkolvars.newLine();
            toSkolvars.flush();
        } catch (IOException e) {
            throw new JKindException("Unable to write to " + getName() + ", "
                    + "probably due to internal JKind error", e);
        }
    }

    public Symbol type(Type type) {
        return new Symbol(capitalize(Util.getName(type)));
    }

    private String capitalize(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public void defineSVar(VarDecl decl) {
        varTypes.put(decl.id, decl.type);
        sendSPart(new Cons("declare-fun", new Symbol(decl.id), new Symbol("()"), type(decl.type)));

    }
    public void defineTVar(VarDecl decl) {
        varTypes.put(decl.id, decl.type);
        sendTPart(new Cons("declare-fun", new Symbol(decl.id), new Symbol("()"), type(decl.type)));
    }

    public void defineGuardVar(VarDecl decl) {
        varTypes.put(decl.id, decl.type);
        sendGuards(new Cons("declare-fun", new Symbol(decl.id), new Symbol("()"), type(decl.type)));
    }

    public void defineSkolVar(VarDecl decl) {
        varTypes.put(decl.id, decl.type);
        sendSkolvars(new Cons("declare-fun", new Symbol(decl.id), new Symbol("()"), type(decl.type)));
    }

    public void defineSVar(Relation relation) {
        sendSPart(new Cons("define-fun", new Symbol(relation.getName()), inputs(relation.getInputs()),
                type(NamedType.BOOL), relation.getBody()));
    }

    public void defineTVar(Relation relation) {
        sendTPart(new Cons("define-fun", new Symbol(relation.getName()), inputs(relation.getInputs()),
                type(NamedType.BOOL), relation.getBody()));
    }

    private Sexp inputs(List<VarDecl> inputs) {
        List<Sexp> args = new ArrayList<>();
        for (VarDecl vd : inputs) {
            args.add(new Cons(vd.id, type(vd.type)));
        }
        return new Cons(args);
    }

    public AevalResult synthesize(Sexp outputs, Sexp transition, Sexp properties) {
        AevalResult result;

        Sexp query = new Cons("assert", new Cons("and", transition, new Cons ("not", properties)));
//        if (outputs != null) {
//            //these outputs HAVE to be renamed!
//            query = new Cons("forall", outputs, query);
//        }
        sendGuards(new Cons("check-sat"));
        sendSkolvars(new Cons("check-sat"));
        sendTPart(query);
        callAeval();
        String status = readFromAeval();
        if (status.contains("Result: valid")) {
            String[] extracted = status.split("\n");
            SkolemRelation skolem = new SkolemRelation(extracted[extracted.length-1]);
            result = new ValidResult(skolem);
        } else {
            //probably parse valid subset model for pdr refinement here.
            result = new InvalidResult();
        }

        return result;
    }

    protected String readFromAeval() {
        try {
            String line;
            StringBuilder content = new StringBuilder();
            while (true) {
                line = fromAeval.readLine();
                if (line == null) {
                    throw new JKindException(getName() + " terminated unexpectedly");
                } else if (line.contains("error \"") || line.contains("Error:")) {
                    // Flush the output since errors span multiple lines
                    while ((line = fromAeval.readLine()) != null) {
                        comment(getName() + ": " + line);
                    }
                    throw new JKindException(getName()
                            + " error (see scratch file for details)");
                } else if (line.contains("extracted skolem: ") ||
                        line.contains("valid subset of S: ")) {

                    content.append(line);
                    break;
                } else {
                    content.append(line);
                    content.append("\n");
                }
            }

            return content.toString();
        } catch (RecognitionException e) {
            throw new JKindException("Error parsing " + getName() + " output", e);
        } catch (IOException e) {
            throw new JKindException("Unable to read from " + getName(), e);
        }
    }

    public void aecomment(String str) {
        scratch("; " + str);
    }

    protected final Map<String, Type> varTypes = new HashMap<>();

}

