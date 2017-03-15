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

    protected FileOutputStream SFileStream;
    protected FileOutputStream TFileStream;

    protected File SFile;
    protected File TFile;
    protected String check;
    public PrintWriter scratch;

    public AevalSolver(String scratchBase, String check, PrintWriter scratch) {
        super(scratchBase, check);
        this.check = check;
        this.scratch = scratch;
        SFile = new File(scratchBase.split("\\.")[0] + "_" + check + "_s_part.smt2");
        TFile = new File(scratchBase.split("\\.")[0] + "_" + check + "_t_part.smt2");
        try {

            SFileStream = new FileOutputStream(SFile);
            TFileStream = new FileOutputStream(TFile);

            toSPart = new BufferedWriter(
                    new OutputStreamWriter(SFileStream, "utf-8"));
            toTPart = new BufferedWriter(
                    new OutputStreamWriter(TFileStream, "utf-8"));
        } catch (IOException ex) {
            throw new JKindException("Unable to open file", ex);
        }
    }

    public void deleteFiles() {
        //Does not delete all files in a consistent manner.
        try {
            SFileStream.close();
            SFile.delete();

            TFileStream.close();
            TFile.delete();
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
        //If I add a print to scratch, I get duplicates
        //due to the same thing being added in both S and T files
        String str = Quoting.quoteSexp(sexp).toString();
        try {
            toTPart.append(str);
            toTPart.newLine();
            toTPart.flush();
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

        if (scratch!=null) {
            scratch.println("; Assertion for existential part of the formula");
            scratch.println(Quoting.quoteSexp(query).toString());
        }
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
                    content.append(line);
                    content.append("\n");
                    throw new JKindException(getName()
                            + " error (see scratch file for details)");
                } else if (line.contains("(check-sat)")) {
                    continue;
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

