package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.chunk.occlusion.RasterBudget;

/** How the raster test budget behaves in a benchmark; see {@link RasterBudget}. */
public enum BudgetMode {
    /** The controller adjusts the budget from measured yield, as in the game. */
    ADAPTIVE,
    /** Pinned at {@link RasterBudget#FLOOR}: the standing probe alone, which is the cost when yield is low. */
    FLOOR,
    /** Pinned unbounded: every far section is tested, the pre-budget behaviour. */
    MAX;

    public void apply(RasterBudget budget) {
        switch (this) {
            case ADAPTIVE -> budget.pin(0);
            case FLOOR -> budget.pin(RasterBudget.FLOOR);
            case MAX -> budget.pin(RasterBudget.UNBOUNDED);
        }
    }

    public static BudgetMode fromProperty(String key, BudgetMode fallback) {
        String value = System.getProperty(key);
        return value == null ? fallback : valueOf(value.trim().toUpperCase());
    }
}
