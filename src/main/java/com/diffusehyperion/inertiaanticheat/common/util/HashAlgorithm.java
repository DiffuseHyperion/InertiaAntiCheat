package com.diffusehyperion.inertiaanticheat.common.util;

public enum HashAlgorithm {
    MD5("MD5", 32),
    SHA1("SHA-1", 40),
    SHA256("SHA-256", 64);

    private final String name;
    private final int length;

    HashAlgorithm(String name, int length) {
        this.name = name;
        this.length = length;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getLength() {
        return length;
    }
}
