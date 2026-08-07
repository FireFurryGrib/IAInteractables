package me.FireKillGrib.iAInteractables.fluids;

public class FluidMath {
    public static final int LN_PER_BUCKET = 81;
    public static final int MB_PER_BUCKET = 1000;

    // Конвертация для GUI (без потери внутренней точности)
    public static int lnToMb(int ln) {
        return (int) (((long) ln * MB_PER_BUCKET) / LN_PER_BUCKET);
    }

    public static int mbToLn(int mb) {
        return (int) (((long) mb * LN_PER_BUCKET) / MB_PER_BUCKET);
    }
}