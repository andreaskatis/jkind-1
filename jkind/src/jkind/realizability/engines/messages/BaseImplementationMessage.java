package jkind.realizability.engines.messages;

import jkind.aeval.SkolemRelation;

import java.util.ArrayList;

public class BaseImplementationMessage extends Message {
    public final ArrayList<SkolemRelation> implementation;

    public BaseImplementationMessage(ArrayList<SkolemRelation> implementation) {
        this.implementation = implementation;
    }

    public ArrayList<SkolemRelation> getBaseImplementation() {
        return this.implementation;
    }
}
