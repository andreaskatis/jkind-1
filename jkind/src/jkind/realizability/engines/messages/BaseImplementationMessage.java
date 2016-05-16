package jkind.realizability.engines.messages;

import jkind.aeval.SkolemRelation;

import java.util.ArrayList;

public class BaseImplementationMessage extends Message {
    public final int k;
    public final ArrayList<SkolemRelation> implementation;

    public BaseImplementationMessage(int k, ArrayList<SkolemRelation> implementation) {
        this.k = k;
        this.implementation = implementation;
    }

    public ArrayList<SkolemRelation> getBaseImplementation() {
        return this.implementation;
    }
}
