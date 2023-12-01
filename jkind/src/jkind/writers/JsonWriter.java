package jkind.writers;

import jkind.engines.ivcs.AllIVCs;

import jkind.lustre.Expr;
import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.results.Counterexample;
import jkind.results.Signal;
import jkind.util.Util;
import jkind.results.layout.Layout;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class JsonWriter extends Writer{

    private final PrintWriter out;
    private final Map<String, Type> types;
    private final Layout layout;

    public JsonWriter(String filename, Map<String, Type> types, Layout layout, boolean useStdout)
            throws FileNotFoundException {
        if (useStdout) {
            this.out = new PrintWriter(System.out, true);
        } else {
            this.out = new PrintWriter(new FileOutputStream(filename));
        }
        this.types = types;
        this.layout = layout;
    }

    @Override
    public void begin() {
        out.println("{");
    }

    @Override
    public void end() {
        out.println("}");
        out.close();
    }

    @Override
    public void writeValid(List<String> props, String source, int k, double proofTime, double runtime,
                           List<Expr> invariants, Set<String> ivc, List<AllIVCs> allIvcs, boolean mivcTimedOut) {
        for (String prop : props) {
            writeValid(source, k, runtime);
        }
    }


    public void writeValid(String source, int k, double runtime) {
        out.println("    \"Runtime\": {");
        out.println("        \"unit\": \"sec\",");
        out.println("        \"value\": \"" + runtime + "\"");
        out.println("    },");
        out.println("    \"Answer\": {");
        out.println("        \"source\": \"" + source + "\",");
        out.println("        \"text\": \"realizable\"");
        out.println("    },");
        out.println("    \"K\": \"" + k + "\"");
        out.flush();
    }


    public void writeValid(Counterexample cex, String source, int k, double runtime) {
        out.println("    \"Runtime\": {");
        out.println("        \"unit\": \"sec\",");
        out.println("        \"value\": \"" + runtime + "\"");
        out.println("    },");
        out.println("    \"Answer\": {");
        out.println("        \"source\": \"" + source + "\",");
        out.println("        \"text\": \"realizable\"");
        out.println("    },");
        out.println("    \"K\": \"" + k + "\",");
        writeCounterexample(cex);
        out.flush();
    }



    private String escape(Expr invariant) {
        return invariant.toString().replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public void writeInvalid(String prop, String source, Counterexample cex,
                             List<String> conflicts, double runtime) {
        out.println("    \"Runtime\": {");
        out.println("        \"unit\": \"sec\",");
        out.println("        \"value\": \"" + runtime + "\"");
        out.println("    },");
        out.println("    \"Answer\": {");
        out.println("        \"source\": \"" + source + "\",");
        out.println("        \"text\": \"unrealizable\"");
        out.println("    },");
        out.println("    \"K\": \"" + cex.getLength() + "\",");
        if (!conflicts.isEmpty()) {
            out.println("    \"props\": \"" + conflicts.get(0) + "\",");
        }
        writeCounterexample(cex);
        writeConflicts(conflicts);
        out.flush();
    }

    public void writeInvalid(String prop, String source, List<Counterexample> counterexamples,
                             List<String> conflicts, List<List<String>> diagnoses, double runtime) {
        out.println("    \"Runtime\": {");
        out.println("        \"unit\": \"sec\",");
        out.println("        \"value\": \"" + runtime + "\"");
        out.println("    },");
        out.println("    \"Answer\": {");
        out.println("        \"source\": \"" + source + "\",");
        out.println("        \"text\": \"unrealizable\"");
        out.println("    },");
        out.println("    \"Counterexamples\": [");
        for (Counterexample cex : counterexamples) {
            out.println("    {");
            out.println("        \"K\": \"" + cex.getLength() + "\",");
            out.println("        \"props\": \"" + conflicts.get(counterexamples.indexOf(cex)) + "\",");
            writeCounterexample(cex);
            if(counterexamples.indexOf(cex) < counterexamples.size()-1) {
                out.println("        },");
            } else {
                out.println("        }");
            }
        }
        out.println("    ],");
        writeConflicts(conflicts);
        writeDiagnoses(diagnoses);
        out.flush();
    }

    public void writeInvalid(String prop, String source, List<Counterexample> counterexamples,
                             List<String> conflicts, List<List<String>> diagnoses, double runtime,
                             HashMap<String, Node> dependencies) {
        out.println("    \"Runtime\": {");
        out.println("        \"unit\": \"sec\",");
        out.println("        \"value\": \"" + runtime + "\"");
        out.println("    },");
        out.println("    \"Answer\": {");
        out.println("        \"source\": \"" + source + "\",");
        out.println("        \"text\": \"unrealizable\"");
        out.println("    },");
        out.println("    \"Counterexamples\": [");
        for (Counterexample cex : counterexamples) {
            out.println("    {");
            out.println("        \"K\": \"" + cex.getLength() + "\",");
            out.println("        \"props\": \"" + conflicts.get(counterexamples.indexOf(cex)) + "\",");
            writeCounterexample(cex);
            if(counterexamples.indexOf(cex) < counterexamples.size()-1) {
                out.println("    },");
            } else {
                out.println("    }");
            }
        }
        out.println("    ],");
        writeConflicts(conflicts);
        writeDiagnoses(diagnoses);
        writeDependencies(dependencies);
        out.flush();
    }

    private void writeDependencies(HashMap<String, Node> dependencies) {
        if (dependencies.isEmpty()) {
            return;
        }
        out.println("    \"Dependencies\": {");
        Iterator it = dependencies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Node node = (Node) entry.getValue();
            List<String> inputs = node.inputs.stream().map(in -> in.id).collect(Collectors.toList());
            List<String> outputs = new ArrayList<>(inputs);
            inputs.retainAll(node.realizabilityInputs);
            outputs.removeAll(node.realizabilityInputs);
            List<String> locals = node.locals.stream().map(loc -> loc.id).collect(Collectors.toList());
            String print = "         \""+entry.getKey() + "\": {\"inputs\": [" + addQuotes(inputs) +
                    "] , \"locals\": [" + addQuotes(locals) + "] , \"outputs\": [" + addQuotes(outputs) + "]}";
            if (it.hasNext()) {
                print += (",");
            }
            out.println(print);
        }
        out.println("    }");
    }

    private String addQuotes(List<String> strings) {
        String result = strings.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(", "));
        return result;
    };

    private void writeDiagnoses(List<List<String>> diagnoses) {
        if (diagnoses.isEmpty()) {
            return;
        }

        out.println("    \"Diagnoses\": [");
        for (List<String> diagnosis : diagnoses) {
            String print = "        {\"Diagnosis\": \"" + diagnosis + "\"}";
            if (diagnoses.indexOf(diagnosis) != diagnoses.size() - 1) {
                out.println(print + ",");
            } else {
                out.println(print);
            }
        }
        out.println("    ],");
    }

    private void writeConflicts(List<String> conflicts) {
        if (conflicts.isEmpty()) {
            return;
        }

        out.println("    \"Conflicts\": [");
        for (String conflict : conflicts) {
            String print = "        {\"Conflict\": \"" + conflict + "\"}";
            if (conflicts.indexOf(conflict) != conflicts.size() - 1) {
                out.println(print + ",");
            } else {
                out.println(print);
            }
        }
        out.println("    ],");
    }

    private void writeUnknown(String prop, int trueFor, Counterexample cex, double runtime) {
        out.flush();
        out.println("    \"Runtime\": {");
        out.println("        \"unit\": \"sec\",");
        out.println("        \"value\": \"" + runtime + "\"");
        out.println("    },");
        out.println("    \"Answer\": {");
        out.println("        \"text\": \"unknown\"");
        out.println("    },");
        out.println("    \"K\": \"" + trueFor + "\"");
        out.flush();
    }

    private void writeCounterexample(Counterexample cex) {
        out.println("        \"Counterexample\": [");
//        for (Signal<Value> signal : cex.getSignals()) {
//            out.print("            {");
//            writeSignal(cex.getLength(), signal);
//            if (cex.getSignals().indexOf(signal) == cex.getSignals().size() - 1) {
//                out.println("}");
//            } else {
//                out.println("},");
//            }
//        }
        List<Signal<Value>> orderedSignals = cex.getCategorySignals(layout, "Realizability Inputs");
        orderedSignals.addAll(cex.getCategorySignals(layout, "Realizability Outputs"));
        orderedSignals.addAll(cex.getCategorySignals(layout, "Node Outputs"));
        orderedSignals.addAll(cex.getCategorySignals(layout, "Node Locals"));

        for (Signal<Value> signal : orderedSignals) {
            out.print("            {");
            writeSignal(cex.getLength(), signal);
            if (orderedSignals.indexOf(signal) == orderedSignals.size() - 1) {
                out.println("}");
            } else {
                out.println("},");
            }
        }
        out.println("        ]");
    }

    private void writeSignal(int k, Signal<Value> signal) {
        String name = signal.getName();
        Type type = types.get(name);
//        String elements = "";
//        out.println("            \"Signal\": ");
//        out.println("              {");
        out.print("\"name\": \"" + name + "\",");
        out.print("\"type\": \"" + type + "\",");
        for (int i = 0; i < k; i++) {
            Value value = signal.getValue(i);
            if (value != null) {
                String element;
                if (value instanceof RealValue) {
                    RealValue rv = (RealValue) value;
                    String text = rv.value.toTruncatedDecimal(3, "");
                    element = text;
                } else {
                    element = formatValue(value);
                }
                if (k != 1 && i != k - 1) {
                    element += ",";
                }
                out.print("\"Step " + i + "\": "+ element);
            }
        }
//        out.println("              }");
    }

    /**
     * pkind prints booleans as 0/1. We do the same for compatibility, but we
     * should eventually switch to true/false
     */
    private String formatValue(Value value) {
        if (value instanceof BooleanValue) {
            BooleanValue bv = (BooleanValue) value;
            return bv.value ? "true" : "false";
        } else {
            return value.toString();
        }
    }

    @Override
    public void writeUnknown(List<String> props, int trueFor,
                             Map<String, Counterexample> inductiveCounterexamples, double runtime) {
        for (String prop : props) {
            writeUnknown(prop, trueFor, inductiveCounterexamples.get(prop), runtime);
        }
    }

    @Override
    public void writeBaseStep(List<String> props, int k) {
        out.println("  <Progress source=\"bmc\" trueFor=\"" + k + "\">");
        for (String prop : props) {
            out.println("    <PropertyProgress name=\"" + prop + "\"/>");
        }
        out.println("  </Progress>");
        out.flush();
    }

    @Override
    public void writeInconsistent(String prop, String source, int k, double runtime) {
        out.println("    \"Runtime\": {");
        out.println("        \"unit\": \"sec\",");
        out.println("        \"value\": \"" + runtime + "\"");
        out.println("    },");
        out.println("    \"Answer\": {");
        out.println("        \"source\": \"" + source + "\",");
        out.println("        \"text\": \"inconsistent\"");
        out.println("    },");
        out.println("    \"K\": \"" + k + "\"");
        out.flush();
    }

}
