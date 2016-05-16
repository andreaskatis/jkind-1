package jkind.realizability.engines.messages;


import jkind.aeval.SkolemRelation;

import java.util.ArrayList;

public class ExtendImplementationMessage extends Message {
    public final int k;
    public final ArrayList<SkolemRelation> implementation;

    public ExtendImplementationMessage(int k, ArrayList<SkolemRelation> implementation) {
        this.k = k;
        this.implementation = implementation;
    }

    public SkolemRelation getExtendImplementation() {
        return this.implementation.get(0);
    }
}
