package me.diffusehyperion.inertiaanticheat.util;

public enum HashAlgorithm {
    MD5("MD5"),
    SHA1("SHA-1"),
    SHA256("SHA-256");

    private final String name;

    HashAlgorithm(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
