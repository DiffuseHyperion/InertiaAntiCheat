package com.diffusehyperion.inertiaanticheat.util;

import java.util.List;

public class IndividualAnticheatDetails extends AnticheatDetails {

    private final List<String> blacklistedMods;
    private final List<String> whitelistedMods;

    public IndividualAnticheatDetails(boolean showInstalled, List<String> blacklistedMods, List<String> whitelistedMods) {
        super(showInstalled);
        this.blacklistedMods = blacklistedMods;
        this.whitelistedMods = whitelistedMods;
    }

    public List<String> getBlacklistedMods() {
        return blacklistedMods;
    }

    public List<String> getWhitelistedMods() {
        return whitelistedMods;
    }

    @Override
    public ValidationMethod getValidationMethod() {
        return ValidationMethod.INDIVIDUAL;
    }
}
