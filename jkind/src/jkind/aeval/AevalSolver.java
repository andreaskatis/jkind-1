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

    public void assertTPart(Sexp sexp, boolean scr) {
        sendTPart(new Cons("assert", sexp), scr);
    }

    public void assertTPartasString(String str, boolean scr) {
        sendTPartasString("(assert "+str+")", scr);
    }

    protected void sendTPart(Sexp sexp, boolean scr) {
        String str = Quoting.quoteSexp(sexp).toString();
        if(scratch != null && scr) {
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

    public void sendTPartasString(String str, boolean scr) {
        if(scratch != null && scr) {
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

    public void sendBlockedRegionSPart(String str) {
        if(scratch !=null) {
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

    public void sendBlockedRegionTPart(String str) {
        if(scratch !=null) {
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

    public void sendSubsetTPart(String str) {
        if(scratch !=null) {
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
    public void defineTVar(VarDecl decl, boolean scr) {
        varTypes.put(decl.id, decl.type);
        sendTPart(new Cons("declare-fun", new Symbol(decl.id), new Symbol("()"), type(decl.type)), scr);
    }


    public void defineSVar(Relation relation) {
        sendSPart(new Cons("define-fun", new Symbol(relation.getName()), inputs(relation.getInputs()),
                type(NamedType.BOOL), relation.getBody()));
    }

    public void defineTVar(Relation relation, boolean scr) {
        sendTPart(new Cons("define-fun", new Symbol(relation.getName()), inputs(relation.getInputs()),
                type(NamedType.BOOL), relation.getBody()), scr);
    }

    private Sexp inputs(List<VarDecl> inputs) {
        List<Sexp> args = new ArrayList<>();
        for (VarDecl vd : inputs) {
            args.add(new Cons(vd.id, type(vd.type)));
        }
        return new Cons(args);
    }

    public AevalResult realizabilityQuery(Sexp transition, Sexp properties, boolean generateSkolem) {
        AevalResult result;

        Sexp query = new Cons("assert", new Cons("and", transition, properties));
//        Sexp query = new Cons("assert", new Cons("and", transition, new Cons ("not", properties)));

        if (scratch!=null) {
            scratch.println("; Assertion for Transition Relation - existential part of the formula");
        }
        sendTPart(query, true);
        callAeval(check, generateSkolem);
        String status = readFromAeval();
        if (status.contains("Result: valid")) {
            if (status.contains("WARNING: Skolem can be arbitrary\n")) {
                result = new UnknownResult();
            } else {
                String[] extracted = status.split("Sanity check: .\\n");
//                String[] extracted = status.split("extracted skolem:");
                SkolemFunction skolem = new SkolemFunction(extracted[extracted.length - 1]);
                result = new ValidResult(skolem);
            }
        } else if (status.contains("Result: invalid")){
            if (status.contains("WARNING: Trivial valid subset (equal to False) due to 0 iterations")) {
                result = new InvalidResult(new ValidSubset("Empty"));
            } else {
                String[] extracted = status.split("assert");
                //the code below added defines in the subset
//                String[] extracted = status.split("valid subset:");
                ValidSubset subset = new ValidSubset(extracted[extracted.length - 1]);
                result = new InvalidResult(subset);
            }
        } else {
            result = new UnknownResult();
        }
        return result;
    }

    public AevalResult refinementQuery() {
        AevalResult result;
        callAeval(check, false);
        String status = readFromAeval();
        if (status.contains("Result: valid")) {
            String[] extracted = status.split("extracted skolem:");
            SkolemFunction skolem = new SkolemFunction("(declare-fun"+extracted[extracted.length - 1]);
            result = new ValidResult(skolem);
        } else if (status.contains("Result: invalid")){
//            if (status.contains("WARNING: Trivial valid subset (equal to False) due to 0 iterations")) {
//                result = new UnknownResult();
//            } else {
                if (status.contains("WARNING: Trivial valid subset (equal to False) due to 0 iterations")) {
                    result = new InvalidResult(new ValidSubset("Empty"));
                } else {
                    String[] extracted = status.split("assert");
                    ValidSubset subset = new ValidSubset(extracted[extracted.length - 1]);
                      //the variable defines were part of the subset returned by the code below
//                    String[] extracted = status.split("valid subset:");
//                    ValidSubset subset = new ValidSubset(extracted[extracted.length - 1]);
                    result = new InvalidResult(subset);
                }
            //}
        } else {
            result = new UnknownResult();
        }
        return result;
    }

    protected String readFromAeval() {
        try {
            String line;
            StringBuilder content = new StringBuilder();
            boolean result = false;
            while (true) {
                line = fromAeval.readLine();


                if (line == null) {
                    break;
                } else if ((line.contains("error \"") || line.contains("Error:") ||
                        line.contains("WARNING: ")) && !result) {
                    if(scratch != null) {
                        scratch.println(";" + getName() + ": " + line);
                    }
                    while ((line = fromAeval.readLine()) != null) {
                        if(scratch != null) {
                            scratch.println(";" + getName() + ": " + line);
                        }
                        if (isCheckSat(line)) {
                            break;
                        }
                    }
                    throw new JKindException(getName()
                            + " error (see " + SFile + " file for details)");
                } else if (line.contains("(check-sat)") || line.startsWith(".subst:") || line.startsWith("subst:") || line.startsWith(".model:")
                || line.startsWith("model:") || line.startsWith("compiling skolem")) {
                    continue;
                } else if (line.startsWith("E.v.:")) {
                    result = true;
                    content.append(line);
                    content.append("\n");
                    if(scratch != null) {
                        scratch.println(";" + getName() + ": " + line);
                    }
                } else if (result) {
                    content.append(line);
                    content.append("\n");
                    if(scratch != null) {
                        scratch.println(";" + getName() + ": " + line);
                    }
                } else if (scratch != null) {
                    scratch.println(";" + getName() + ": " + line);
                }
            }
            //deleteFiles();
            return content.toString();
        } catch (RecognitionException e) {
            deleteFiles();
            throw new JKindException("Error parsing " + getName() + " output", e);
        } catch (IOException e) {
            deleteFiles();
            throw new JKindException("Unable to read from " + getName() + " file :" + check, e);
        }
    }

    protected final Map<String, Type> varTypes = new HashMap<>();

    protected boolean isCheckSat(String line) {
        return line.contains(CHECKSAT);
    }

}

