package jkind.realizability.engines;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HSNode {
    HSNode parent;
    List<String> label = new ArrayList<>();
    List<String> hittingSet = new ArrayList<>();
    String parentEdge;
    List<HSNode> children = new ArrayList<>();

    HSNode(HSNode parent, String parentEdge) {
        this.parent = parent;
        this.parentEdge = parentEdge;
        this.hittingSet = computeHittingSet();
    }

    private List<String> computeHittingSet() {
        if (parent != null) {
            HSNode parent = this.parent;
            hittingSet.add(parentEdge);
            while (parent.getParent() != null) {
                hittingSet.add(parent.getParentEdge());
                parent = parent.getParent();
            }
        }
        if (!hittingSet.isEmpty()) {
            hittingSet.sort(Comparator.comparing(String::toString));
        }
        return hittingSet;
    }

    public List<String> getHittingSet() {
        return hittingSet;
    }

    public String getParentEdge() {
        return this.parentEdge;
    }

    public HSNode getParent() {
        return this.parent;
    }

    public void setLabel(List<String> label) {
        this.label = label;
        if (!label.get(0).equals("done") && !label.get(0).equals("closed")) {
            generateChildren(this);
        }
    }

    private void generateChildren(HSNode hsNode) {
        for (String prop : hsNode.label) {
            HSNode child = new HSNode(hsNode, prop);
            hsNode.setChild(child);
        }
    }

    public List<String> getLabel() {
        return this.label;
    }

    public void setChild(HSNode hsNode) {
        this.children.add(hsNode);
    }

    public List<HSNode> getChildren() {
        return this.children;
    }
}
