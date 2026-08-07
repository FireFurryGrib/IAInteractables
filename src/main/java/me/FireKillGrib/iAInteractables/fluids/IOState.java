package me.FireKillGrib.iAInteractables.fluids;

public enum IOState {
    NONE, INPUT, OUTPUT;

    public IOState next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}