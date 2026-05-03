package com.whatxe.xlib.capability;

public record MenuPolicy(
        boolean canOpenAbilityMenu,
        boolean canOpenProgressionMenu,
        boolean canOpenInventoryScreen
) {
    public static final MenuPolicy FULL = new MenuPolicy(true, true, true);

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canOpenAbilityMenu = true;
        private boolean canOpenProgressionMenu = true;
        private boolean canOpenInventoryScreen = true;

        private Builder() {}

        public Builder canOpenAbilityMenu(boolean value) { this.canOpenAbilityMenu = value; return this; }
        public Builder canOpenProgressionMenu(boolean value) { this.canOpenProgressionMenu = value; return this; }
        public Builder canOpenInventoryScreen(boolean value) { this.canOpenInventoryScreen = value; return this; }

        public MenuPolicy build() {
            return new MenuPolicy(canOpenAbilityMenu, canOpenProgressionMenu, canOpenInventoryScreen);
        }
    }

    MenuPolicy mergeRestrictive(MenuPolicy other) {
        return new MenuPolicy(
                this.canOpenAbilityMenu && other.canOpenAbilityMenu,
                this.canOpenProgressionMenu && other.canOpenProgressionMenu,
                this.canOpenInventoryScreen && other.canOpenInventoryScreen
        );
    }
}
