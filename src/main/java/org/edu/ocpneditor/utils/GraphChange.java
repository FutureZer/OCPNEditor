package org.edu.ocpneditor.utils;

public interface GraphChange {

    public abstract void execute();

    public abstract void undo();
}
