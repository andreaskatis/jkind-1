package jkind.realizability.engines.messages;


import jkind.aeval.SkolemRelation;

import java.util.ArrayList;

public class ExtendImplementationMessage extends Message {
    public final ArrayList<SkolemRelation> implementation;

    public ExtendImplementationMessage(ArrayList<SkolemRelation> implementation) {
        this.implementation = implementation;
    }

    public SkolemRelation getExtendImplementation() {
        return this.implementation.get(0);
    }
}
