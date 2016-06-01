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

    protected FileOutputStream SFileStream;
    protected FileOutputStream TFileStream;
    protected FileOutputStream GuardsFileStream;
    protected FileOutputStream SkolvarsFileStream;

    protected File SFile;
    protected File TFile;
    protected File GuardsFile;
    protected File SkolvarsFile;
    protected String check;
    protected PrintWriter scratch;

    public AevalSolver(String scratchBase, String check, PrintWriter scratch) {
        super(scratchBase, check);
        this.check = check;
        this.scratch = scratch;
        SFile = new File(scratchBase.split("\\.")[0] + "_" + check + "_s_part.smt2");
        TFile = new File(scratchBase.split("\\.")[0] + "_" + check + "_t_part.smt2");
        GuardsFile = new File(scratchBase.split("\\.")[0] + "_" + check + "_guards_vars.smt2");
        SkolvarsFile = new File(scratchBase.split("\\.")[0] + "_" + check + "_skol_vars.smt2");
        try {

            SFileStream = new FileOutputStream(SFile);
            TFileStream = new FileOutputStream(TFile);
            GuardsFileStream = new FileOutputStream(GuardsFile);
            SkolvarsFileStream = new FileOutputStream(SkolvarsFile);

            toSPart = new BufferedWriter(
                    new OutputStreamWriter(SFileStream, "utf-8"));
            toTPart = new BufferedWriter(
                    new OutputStreamWriter(TFileStream, "utf-8"));
            toGuards = new BufferedWriter(
                    new OutputStreamWriter(GuardsFileStream, "utf-8"));
            toSkolvars = new BufferedWriter(
                    new OutputStreamWriter(SkolvarsFileStream, "utf-8"));
        } catch (IOException ex) {
            throw new JKindException("Unable to open file", ex);
        }
    }

    public void deleteFiles() {
        try {
            SFileStream.close();
            SFile.delete();

            TFileStream.close();
            TFile.delete();

            GuardsFileStream.close();
            GuardsFile.delete();

            SkolvarsFileStream.close();
            SkolvarsFile.delete();
        } catch (IOException e) {
            throw new JKindException("Could not delete AE-VAL files");
        }
    }

    public void assertSPart(Sexp sexp) {
        sendSPart(new Cons("assert", sexp));
    }

    protected void sendSPart(Sexp sexp) {
        String str = Quoting.quoteSexp(sexp).toString();
        if(scratch != null) {
            scratch.println(str);
        }
        try {
            toSPart.append(str);
            toSPart.newLine();
            toSPart.flush();
        } catch (IOException e) {
            throw new JKindException("Unable to write to " + getName() + ", "
                    + "probably due to internal JKind error", e);
        }
    }

    protected void sendTPart(Sexp sexp) {
        String str = Quoting.quoteSexp(sexp).toString();
        if(scratch != null) {
            scratch.println(str);
        }
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
        String str = Quoting.quoteSexp(sexp).toString();
        if(scratch != null) {
            scratch.println(str);
        }
        sendGuards(new Cons("assert", sexp));
    }

    protected void sendGuards(Sexp sexp) {
        String str = Quoting.quoteSexp(sexp).toString();
        if(scratch != null) {
            scratch.println(str);
        }
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
        String str = Quoting.quoteSexp(sexp).toString();
        if(scratch != null) {
            scratch.println(str);
        }
        sendSkolvars(new Cons("assert", sexp));
    }

    protected void sendSkolvars(Sexp sexp) {
        String str = Quoting.quoteSexp(sexp).toString();
        if(scratch != null) {
            scratch.println(str);
        }
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

    public AevalResult synthesize(Sexp transition, Sexp properties) {
        AevalResult result;

        Sexp query = new Cons("assert", new Cons("and", transition, properties));

        sendGuards(new Cons("check-sat"));
        sendSkolvars(new Cons("check-sat"));
        sendTPart(query);
        callAeval(check);
        String status = readFromAeval();
        if (status.contains("Result: valid")) {
            String[] extracted = status.split("extracted skolem:");
            SkolemRelation skolem = new SkolemRelation(extracted[extracted.length - 1]);
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
                    break;
                } else if (line.contains("error \"") || line.contains("Error:")) {
                    throw new JKindException(getName()
                            + " error (see scratch file for details)");
                }
                else {
                    content.append(line);
                    content.append("\n");
                }
            }
            deleteFiles();
            return content.toString();
        } catch (RecognitionException e) {
            deleteFiles();
            throw new JKindException("Error parsing " + getName() + " output", e);
        } catch (IOException e) {
            deleteFiles();
            throw new JKindException("Unable to read from " + getName(), e);
        }
    }

    protected final Map<String, Type> varTypes = new HashMap<>();

}

