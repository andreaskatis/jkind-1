package jkind.realizability.engines;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import jkind.JKindException;
import jkind.JRealizabilitySettings;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.builders.NodeBuilder;
import jkind.realizability.engines.messages.RealizableMessage;
import jkind.realizability.engines.messages.UnknownMessage;
import jkind.realizability.engines.messages.UnrealizableMessage;
import jkind.slicing.LustreSlicer;
import jkind.solvers.Model;
import jkind.translation.Specification;

import java.util.*;
import java.util.stream.Collectors;



public class RealizabilityDiagnosisEngine extends RealizabilityEngine {
    List<RealizabilityEngine> engines = new ArrayList<>();
    List<Thread> threads = new ArrayList<>();
    HashMap<String, String> realizableMap = new HashMap<>();
    HSNode root = new HSNode(null, null);
    List<HSNode> labeled = new ArrayList<>();
    List<HSNode> unlabeled = new ArrayList<>();
    HashMap<String, List<String>> minconflicts = new HashMap<>();
    List<List<String>> diagnoses = new ArrayList<>();
    HashMap<List<String>, Model> counterexamples = new HashMap<>();
    HashMap<List<String>, Integer> cexLengths = new HashMap<>();
    int counter = 0;
    int count = 0;
    public RealizabilityDiagnosisEngine(Specification spec, JRealizabilitySettings settings,
                                        RealizabilityDirector director) {
        super("diagnosis", spec, settings, director);
    }

    @Override
    public void main() {
        labelRootNode();
        while (!unlabeled.isEmpty()) {
            String labels = "";
            for (HSNode u : labeled) {
                labels+= "{" + u.getLabel() + " " + u.getHittingSet() + "}";
            }
            HSNode hsNode = reuseLabelorCloseNode(unlabeled.get(0));
            if (hsNode.getLabel().isEmpty()) {
                List<String> hittingSet = hsNode.getHittingSet();
                if (!labelNode(hsNode)) {
                    List<String> properties = new ArrayList<>(spec.node.properties);
                    properties.removeAll(hittingSet);
                    Node node = new NodeBuilder(spec.node).clearProperties().addProperties(properties).build();
                    Node slicedNode = LustreSlicer.slice(node, spec.dependencyMap);
                    Program slicedProgram = new Program(slicedNode);
                    // Specification slicedSpec = new Specification(slicedNode);
                    Specification slicedSpec = new Specification(slicedProgram);
                    if (realizableMap.containsKey(properties.toString())) {
                        if (realizableMap.get(properties.toString()).equals("REALIZABLE")) {
                            List<String> label = new ArrayList<>();
                            label.add("done");
                            hsNode.setLabel(label);
                            labeled.add(hsNode);
                            unlabeled.remove(0);
                            continue;
                        }
                    }

                    //check if remaining spec is realizable. If the answer is "yes", we have found
                    //all minimal conflicts
//                    JRealizabilitySettings tSettings = new JRealizabilitySettings();
//                    tSettings.fixpoint = this.settings.fixpoint;
//                    tSettings.diagnose = this.settings.diagnose;
//
//                    registerPartitionProcess(tSettings, slicedSpec);
                    registerPartitionProcess(this.settings, slicedSpec);

                    HashMap<List<String>, String> localMap = runThreadsAndGatherResults();
                    String result = localMap.get(slicedSpec.node.properties);
                    if (result.equals("REALIZABLE") || result.equals("UNKNOWN")) {
                        List<String> label = new ArrayList<>();
                        label.add("done");
                        hsNode.setLabel(label);
                        labeled.add(hsNode);
                        unlabeled.remove(0);
                        continue;
                    }
                    List<List<String>> conflicts = deltaDebug(slicedSpec, 2);
                    if (conflicts.isEmpty()) {
                        unlabeled.remove(0);
                        List<String> label = new ArrayList<>();
                        label.add("done");
                        hsNode.setLabel(label);
                        labeled.add(hsNode);
                    } else {
                        addUniqueConflicts(conflicts);
                        labelNode(hsNode);
                        System.out.println("Assigned " + hsNode.getLabel());
                    }
                }
            } else if (hsNode.getLabel().equals("[closed]")){
                unlabeled.remove(0);
            } else {
                labeled.add(hsNode);
                unlabeled.remove(0);
                unlabeled.addAll(hsNode.getChildren());
            }
        }
        for (HSNode l : labeled) {
            if (l.getParent() != null) {
                System.out.println(l.getParent().getLabel() + "<---- " + l.getParentEdge() + " ---- " + l.getLabel() + " ----> " +
                        l.getChildren().stream().map(lbl -> lbl.getLabel()).collect(Collectors.toList()));
            } else {
                System.out.println("Root <---- " + l.getLabel() + " ----> " +
                        l.getChildren().stream().map(lbl -> lbl.getLabel()).collect(Collectors.toList()));
            }
        }
        if (minconflicts.isEmpty()) {
            sendResult(minconflicts.size(), "REALIZABLE", null, null);
        } else {
            if (settings.fixpoint) {
                //run kinduction on minimal conflicts to grab cexs
                for (Map.Entry confl : minconflicts.entrySet()) {
                    List<String> conflList = (List<String>) confl.getValue();
                    Node conflNode = new NodeBuilder(spec.node).clearProperties().addProperties(conflList).build();
                    Node slicedNode = LustreSlicer.slice(conflNode, spec.dependencyMap);
                    Program slicedProgram = new Program(slicedNode);
                    Specification slicedSpec = new Specification(slicedProgram);
                    JRealizabilitySettings newSettings = new JRealizabilitySettings();
                    newSettings.diagnose = true;
                    registerPartitionProcess(newSettings, slicedSpec);
                }
                runThreadsAndGatherResults();
                computeDiagnoses();
                sendResult(minconflicts.size(), "UNREALIZABLE", counterexamples, cexLengths);

            } else {
                //check sanity of kinduction results with fixpoint
                if (sanityCheck(minconflicts)) {
                    computeDiagnoses();
                    sendResult(minconflicts.size(), "UNREALIZABLE", counterexamples, cexLengths);
                } else {
                    sendResult(minconflicts.size(), "REALIZABLE", null, null);
                }

            }
        }
        System.out.println(count);
        System.out.println(realizableMap);

    }

    private boolean sanityCheck(HashMap<String, List<String>> conflicts) {
        for (Map.Entry confl : conflicts.entrySet()) {
            List<String> conflList = (List<String>) confl.getValue();
            Node conflNode = new NodeBuilder(spec.node).clearProperties().addProperties(conflList).build();
            Node slicedNode = LustreSlicer.slice(conflNode, spec.dependencyMap);
            Program slicedProgram = new Program(slicedNode);
            Specification slicedSpec = new Specification(slicedProgram);
            JRealizabilitySettings newSettings = new JRealizabilitySettings();
            newSettings.filename = settings.filename;
            newSettings.diagnose = true;
            newSettings.fixpoint = true;
            registerPartitionProcess(newSettings, slicedSpec);
        }
        HashMap<List<String>, String> localMap = runThreadsAndGatherResults();

        if(!localMap.containsValue("REALIZABLE")) {
            return true;
        } else {
            throw new JKindException("Unsound results for minimal conflicts");
        }
    }

    private void computeDiagnoses() {
        List<HSNode> leaves = labeled.stream().
                filter(node -> node.getLabel().get(0).equals("done")).collect(Collectors.toList());
        for (HSNode leaf : leaves) {
            diagnoses.add(leaf.getHittingSet());
        }
    }

    private void addUniqueConflicts(List<List<String>> conflicts) {
        for (List<String> conflict : conflicts) {
            String conflID = conflict.toString();
            if (!minconflicts.containsKey(conflID)) {
                minconflicts.put(conflID, conflict);
            }
        }
    }

    private HSNode reuseLabelorCloseNode(HSNode hsNode) {
        List<String> hittingSet = hsNode.getHittingSet();
        System.out.println(hsNode.getLabel() + " has HS " + hsNode.getHittingSet() + " and parent " + hsNode.getParent().getLabel());
        for (HSNode labeledNode : labeled) {
            List<String> label = labeledNode.getLabel();
            if (!label.toString().equals("[done]") && !label.toString().equals("[closed]")) {
                List<String> tempSet = new ArrayList<>(hittingSet);
                tempSet.retainAll(label);

                if (tempSet.isEmpty()) {
                    hsNode.setLabel(label);
                    System.out.println("Label after reuseOrClose " + hsNode.getLabel());
                    return hsNode;
                }
            }

            List<String> labeledHittingSet = labeledNode.getHittingSet();
            //If node n is marked as done and n' is such that H(n) subsetOf H(n'), close n'
            if (label.toString().equals("[done]") && hittingSet.containsAll(labeledHittingSet)) {
                System.out.println("H( " + labeledHittingSet + " ) c= H ( " + hittingSet + " )");
                List<String> closedLabel = new ArrayList<>();
                closedLabel.add("closed");
                hsNode.setLabel(closedLabel);
                System.out.println("Label after reuseOrClose " + hsNode.getLabel());
                return hsNode;
            }
            //If n was generated and n' is such that H(n) = H(n'), close n'
            if (labeledHittingSet.containsAll(hittingSet)){
                System.out.println("H( " + labeledHittingSet + " ) = H ( " + hittingSet + " )");
                List<String> closedLabel = new ArrayList<>();
                closedLabel.add("closed");
                hsNode.setLabel(closedLabel);
                System.out.println("Label after reuseOrClose " + hsNode.getLabel());
                return hsNode;
            }
        }
        System.out.println("Label after reuseOrClose " + hsNode.getLabel());
        return hsNode;
    }

    private boolean labelNode(HSNode hsNode) {
        boolean labeled = false;
        List<String> hittingSet = hsNode.getHittingSet();
        for (Map.Entry<String, List<String>> conflict : minconflicts.entrySet()) {
            List<String> confList = conflict.getValue();
            List<String> tempSet = new ArrayList<>(hittingSet);
            tempSet.retainAll(confList);
            if (tempSet.isEmpty()) {
                hsNode.setLabel(confList);
                unlabeled.remove(0);
                unlabeled.addAll(hsNode.getChildren());
                this.labeled.add(hsNode);
                labeled = true;
                break;
            }
        }
        assert(!hsNode.getLabel().isEmpty());
        return labeled;
    }

    private void labelRootNode() {
        List<List<String>> conflicts = deltaDebug(spec, 2);
        if (!conflicts.isEmpty()) {
            root.setLabel(conflicts.get(0));
            unlabeled.addAll(root.children);
            addUniqueConflicts(conflicts);
            labeled.add(root);
        } else {
            registerPartitionProcess(this.settings, spec);
//            JRealizabilitySettings tSettings = new JRealizabilitySettings();
//            tSettings.fixpoint = this.settings.fixpoint;
//            tSettings.diagnose = this.settings.diagnose;
//            registerPartitionProcess(tSettings, spec);

            runThreadsAndGatherResults();
            String res = new String();
            for (RealizabilityEngine engine : engines) {
                res = engine.getResult();
                if (res.equals("REALIZABLE")) {
                    return;
                }
            }
            root.setLabel(spec.node.properties);
            labeled.add(root);
            conflicts.add(spec.node.properties);
            addUniqueConflicts(conflicts);
            return;
        }
    }

    private List<List<String>> deltaDebug(Specification spec, int granularity) {
        System.out.println("DD instance on : " + spec.node.properties + " Granularity : " + granularity);
        HashMap<List<String>, String> partitionMap = new HashMap<>(), complementsMap = new HashMap<>();
        List<List<String>> minConflicts = new ArrayList<>();

        Partition partitions = new Partition(spec.node.properties, granularity);
//        Partition partitions = Partition.inParts(spec.node.properties, granularity);
        List<Node> complements = new ArrayList<>();
        boolean conflictExists = false;

        for (int i = 0; i < partitions.size(); i++) {

            List<String> partition = partitions.get(i);
            String partID = partition.toString();
            if (!realizableMap.containsKey(partID)) {
                Node partNode = new NodeBuilder(spec.node).clearProperties().addProperties(partition).build();
                Node slicedPart = LustreSlicer.slice(partNode, spec.dependencyMap);
                Program slicedProgram = new Program(slicedPart);
                Specification slicedSpec = new Specification(slicedProgram);
//                JRealizabilitySettings tSettings = new JRealizabilitySettings();
//                tSettings.fixpoint = this.settings.fixpoint;
//                tSettings.diagnose = this.settings.diagnose;
//                registerPartitionProcess(tSettings, slicedSpec);

                registerPartitionProcess(this.settings, slicedSpec);
            } else if (realizableMap.get(partID).equals("UNREALIZABLE") && !minconflicts.toString().contains(partID)) {
                conflictExists = true;
                partitionMap.put(partition, "UNREALIZABLE");
            } else if (realizableMap.get(partID).equals("REALIZABLE")) {
                count++;
            }
            if (granularity != 2) {
                Node partComplementNode = complementNode(partitions, i);
                complements.add(partComplementNode);
            }
        }
        if (partitionMap.isEmpty()) {
            partitionMap = runThreadsAndGatherResults();
        }
        for (Map.Entry<List<String>, String> localEntry : partitionMap.entrySet()) {
            String entryID = localEntry.getKey().toString();
            if (!realizableMap.containsKey(entryID)) {
                realizableMap.put(entryID, localEntry.getValue());
                System.out.println(entryID + " : " + localEntry.getValue());
                if (localEntry.getValue().equals("UNREALIZABLE")) {
                    conflictExists = true;
                } else if (localEntry.getValue().equals("REALIZABLE")) {
                    Set<String> s = ImmutableSet.copyOf(localEntry.getKey());
                    Set<Set<String>> pwrSet = Sets.powerSet(s);
                    List<List<String>> pSet = new ArrayList<>();
                    for (Set<String> st : pwrSet) {
                        List<String> tempList = new ArrayList<>();
                        if (!st.isEmpty()) {
                            tempList.addAll(st);
                        }
                        realizableMap.put(tempList.toString(), "REALIZABLE");
                    }
                }
            }
        }

        for (Node complNode : complements) {
                Program complProgram = new Program(complNode);
                Specification complSpec = new Specification(complProgram);
                if(!realizableMap.containsKey(complSpec.node.properties.toString())) {
                    registerPartitionProcess(this.settings, complSpec);
                } else if (realizableMap.get(complSpec.node.properties.toString()).equals("UNREALIZABLE") &&
                        !minconflicts.toString().contains(complSpec.node.properties.toString())) {
                    conflictExists = true;
                    complementsMap.put(complSpec.node.properties, "UNREALIZABLE");
                } else if (realizableMap.get(complSpec.node.properties.toString()).equals("REALIZABLE")) {
                    count++;
                }
                if(minconflicts.containsKey(complSpec.node.properties.toString())) {
                    minConflicts.add(complSpec.node.properties);
                }

        }
        if (complementsMap.isEmpty()) {
            complementsMap = runThreadsAndGatherResults();
        }
        for (Map.Entry<List<String>, String> localEntry : complementsMap.entrySet()) {
            String entryID = localEntry.getKey().toString();
            if (!realizableMap.containsKey(entryID)) {
                realizableMap.put(entryID, localEntry.getValue());
                System.out.println(entryID + " : " + localEntry.getValue());
                if (localEntry.getValue().equals("UNREALIZABLE")) {
                    conflictExists = true;
                } else if (localEntry.getValue().equals("REALIZABLE")) {
                    Set<String> s = ImmutableSet.copyOf(localEntry.getKey());
                    Set<Set<String>> pwrSet = Sets.powerSet(s);
                    for (Set<String> st : pwrSet) {
                        List<String> tempList = new ArrayList<>();
                        if (!st.isEmpty()) {
                            tempList.addAll(st);
                        }
                        realizableMap.put(tempList.toString(), "REALIZABLE");
                    }
                }
            }
        }

        assert(partitionMap.containsValue("UNREALIZABLE") || partitionMap.containsValue("REALIZABLE") ||
                partitionMap.containsValue("UNKNOWN"));
        if (partitionMap.containsValue("UNREALIZABLE")) {
            System.out.println("Some partition unrealizable. Continue with these partitions");
            Map<List<String>, String> unrealMap = partitionMap.entrySet().stream().
                    filter(entry -> entry.getValue().equals("UNREALIZABLE")).
                    collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

//            Map.Entry<List<String>, String> entry = unrealMap.entrySet().iterator().next();
            for (Map.Entry<List<String>, String> entry : unrealMap.entrySet()) {
                if (entry.getKey().size() > 1) {
                    Node partitionNode = new NodeBuilder(this.spec.node).clearProperties().addProperties(entry.getKey()).build();
                    Node slicedPartitionNode = LustreSlicer.slice(partitionNode, this.spec.dependencyMap);
                    Program slicedPartitionProgram = new Program(slicedPartitionNode);
                    Specification partitionSpec = new Specification(slicedPartitionProgram);
                    if (minconflicts.containsKey(partitionSpec.node.properties.toString())) {
                        minConflicts.add(partitionSpec.node.properties);
                    } else {
                        List<List<String>> conflicts = deltaDebug(partitionSpec, 2);
                        minConflicts.addAll(conflicts);
                        addUniqueConflicts(conflicts);
                    }
                } else {
                    minConflicts.add(entry.getKey());
                }
            }
        }
        if (complementsMap.containsValue("UNREALIZABLE") && granularity != 2) {
            System.out.println("A complement is unrealizable. Continue with the complements.");
             Map<List<String>, String> unrealMap = complementsMap.entrySet().stream().
                     filter(entry -> entry.getValue().equals("UNREALIZABLE")).
                     collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

//            Map.Entry<List<String>, String> entry = unrealMap.entrySet().iterator().next();

            for (Map.Entry<List<String>, String> entry : unrealMap.entrySet()) {
                    Node partitionNode = new NodeBuilder(spec.node).clearProperties().addProperties(entry.getKey()).build();
                    Node slicedPartitionNode = LustreSlicer.slice(partitionNode, spec.dependencyMap);
                    Program slicedPartitionProgram = new Program(slicedPartitionNode);
                    Specification partitionSpec = new Specification(slicedPartitionProgram);
                    if (minconflicts.containsKey(partitionSpec.node.properties.toString())) {
                        minConflicts.add(partitionSpec.node.properties);
                    } else {
                        List<List<String>> complMinConfls = deltaDebug(partitionSpec, Math.max(granularity - 1, 2));
                        minConflicts.addAll(complMinConfls);
                        addUniqueConflicts(complMinConfls);
                    }
            }
        }
        if (minConflicts.isEmpty() && granularity < spec.node.properties.size()){
            System.out.println("No partition unrealizable. Increase granularity " + spec.node.properties);
            List<List<String>> conflicts = deltaDebug(spec, Math.min(spec.node.properties.size(), 2*granularity));
            minConflicts.addAll(conflicts);
            addUniqueConflicts(conflicts);
        }
        if (minConflicts.isEmpty() && !conflictExists){
            System.out.println(spec.node.properties + " should be minimal conflict");
            List<List<String>> conflicts = new ArrayList<>();
            conflicts.add(spec.node.properties);
            minConflicts.addAll(conflicts);
            addUniqueConflicts(conflicts);
        }
        return minConflicts;
    }

//    private void linearDebug() {
//
//    }

    private HashMap<List<String>, String> runThreadsAndGatherResults() {
        HashMap<List<String>, String> localMap = new HashMap<>();
        try {
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException ie ) {
            throw new JKindException("Thread was interrupted");
        }

        for (RealizabilityEngine engine : engines) {
            String result = engine.getResult();
            int cexLength = engine.getCexLength();
            Model model = engine.getModel();
            if (!result.equals("NONE")) {
                List<String> props = engine.spec.node.properties;
                localMap.put(props, result);
                if (model != null) {
                    counterexamples.put(props, model);
                    cexLengths.put(props, cexLength);
                }
            } else {
                continue;
            }
        }
        threads.clear();
        engines.clear();
        return localMap;
    }

    private Node complementNode(Partition partitions, int index) {
        List<String> complement = partitions.getComplement(index);
        Node complNode = new NodeBuilder(spec.node).clearProperties().addProperties(complement).build();
        Node slicedComplNode = LustreSlicer.slice(complNode, spec.dependencyMap);
        return slicedComplNode;
    }

    private void registerPartitionProcess(JRealizabilitySettings settings, Specification slicedSpec) {
        System.out.println("New engine : " + slicedSpec.node.properties);
//        JRealizabilitySettings tSettings = new JRealizabilitySettings();
//        tSettings.fixpoint = settings.fixpoint;
//        RealizabilityDirector newDir = new RealizabilityDirector(tSettings, slicedSpec);

        if (settings.fixpoint) {
            String engineName = "";
            for (String prop : slicedSpec.node.properties) {
                engineName+= prop;
            }
            RealizabilityFixpointEngine fixpointEngine = new RealizabilityFixpointEngine(slicedSpec, settings, director, engineName);
            engines.add(fixpointEngine);
            threads.add(new Thread(fixpointEngine, fixpointEngine.getName()));
        } else {
            RealizabilityBaseEngine baseEngine = new RealizabilityBaseEngine(slicedSpec, settings, director);
            engines.add(baseEngine);
            counter++;
            threads.add(new Thread(baseEngine, baseEngine.getName()));

            RealizabilityExtendEngine extendEngine = new RealizabilityExtendEngine(slicedSpec, settings, director);
            baseEngine.setExtendEngine(extendEngine);
            extendEngine.setBaseEngine(baseEngine);

            engines.add(extendEngine);
            threads.add(new Thread(extendEngine, extendEngine.getName()));
        }
    }

    protected void setResult(int k, String result, Model model) {
        this.result = result;
        this.model = model;
    }

    protected void sendResult(int k, String result, HashMap<List<String>, Model> counterexamples,
                              HashMap<List<String>, Integer> cexLengths) {
        if (result.equals("REALIZABLE")) {
            RealizableMessage rm = new RealizableMessage(k);
            director.incoming.add(rm);
        } else if (result.equals("UNREALIZABLE")) {
            HashMap<String, Node> dependencies = calculateDependencies();
            HashMap<List<String>, Model> minCounterexamples = new HashMap<>();
            List<String> conflicts = new ArrayList<>();
            for (String conflict : minconflicts.keySet()) {
                conflicts.add(conflict);
                for (Map.Entry<List<String>, Model> cex : counterexamples.entrySet()) {
                    if (conflict.equals(cex.getKey().toString())) {
                        minCounterexamples.put(cex.getKey(), cex.getValue());
                    }
                }
            }
            conflicts.sort(Comparator.comparing( String::toString));
            UnrealizableMessage um = new UnrealizableMessage(k, conflicts, diagnoses, minCounterexamples, cexLengths, dependencies);
            director.incoming.add(um);
        } else {
            UnknownMessage um = new UnknownMessage();
            director.incoming.add(um);
        }
    }

    private HashMap<String, Node> calculateDependencies() {
        HashMap<String, Node> dependencies = new HashMap<>();
        for (String prop : this.spec.node.properties) {
            Node node = new NodeBuilder(spec.node).clearProperties().addProperty(prop).build();
            Node slicedNode = LustreSlicer.slice(node, spec.dependencyMap);
            dependencies.put(prop, slicedNode);
        }
        return dependencies;
    }

}
