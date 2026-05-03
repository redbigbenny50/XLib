package com.whatxe.xlib.ability;

/**
 * Controls the semantic effect of a {@link RestrictedRecipeRule} when it is the highest-priority
 * match for a recipe.
 *
 * <p>{@link #RESTRICT} is the legacy default: the rule marks the recipe as restricted, requiring
 * an explicit permission grant before the player can craft it.
 *
 * <p>{@link #EXEMPT} inverts the effect: a high-priority EXEMPT rule shields the recipe from any
 * lower-priority RESTRICT rules that would otherwise match it. This enables "restrict all except X"
 * patterns — place a blanket RESTRICT rule at low priority and EXEMPT rules at higher priority for
 * the specific recipes that should remain freely craftable.
 */
public enum RecipeRuleMode {
    /** Default — this rule restricts the recipe (existing behavior). */
    RESTRICT,
    /** This rule exempts the recipe from lower-priority restrictions. */
    EXEMPT
}
