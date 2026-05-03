package com.whatxe.xlib.capability;

public record ContainerPolicy(
        boolean canOpenContainers,
        boolean canOpenChests,
        boolean canOpenFurnaces,
        boolean canOpenBrewingStands,
        boolean canOpenShulkerBoxes,
        boolean canInsertIntoFurnace,
        boolean canExtractFromFurnace,
        boolean canTakeFurnaceOutput,
        boolean canInsertIntoBrewing,
        boolean canExtractFromBrewing,
        boolean canTakeBrewingOutput,
        boolean canTakeCraftingOutput,
        boolean canInsertIntoAnvil,
        boolean canTakeAnvilOutput
) {
    public static final ContainerPolicy FULL = new ContainerPolicy(
            true, true, true, true, true,
            true, true, true,
            true, true, true,
            true,
            true, true
    );

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canOpenContainers = true;
        private boolean canOpenChests = true;
        private boolean canOpenFurnaces = true;
        private boolean canOpenBrewingStands = true;
        private boolean canOpenShulkerBoxes = true;
        private boolean canInsertIntoFurnace = true;
        private boolean canExtractFromFurnace = true;
        private boolean canTakeFurnaceOutput = true;
        private boolean canInsertIntoBrewing = true;
        private boolean canExtractFromBrewing = true;
        private boolean canTakeBrewingOutput = true;
        private boolean canTakeCraftingOutput = true;
        private boolean canInsertIntoAnvil = true;
        private boolean canTakeAnvilOutput = true;

        private Builder() {}

        public Builder canOpenContainers(boolean value) { this.canOpenContainers = value; return this; }
        public Builder canOpenChests(boolean value) { this.canOpenChests = value; return this; }
        public Builder canOpenFurnaces(boolean value) { this.canOpenFurnaces = value; return this; }
        public Builder canOpenBrewingStands(boolean value) { this.canOpenBrewingStands = value; return this; }
        public Builder canOpenShulkerBoxes(boolean value) { this.canOpenShulkerBoxes = value; return this; }
        public Builder canInsertIntoFurnace(boolean value) { this.canInsertIntoFurnace = value; return this; }
        public Builder canExtractFromFurnace(boolean value) { this.canExtractFromFurnace = value; return this; }
        public Builder canTakeFurnaceOutput(boolean value) { this.canTakeFurnaceOutput = value; return this; }
        public Builder canInsertIntoBrewing(boolean value) { this.canInsertIntoBrewing = value; return this; }
        public Builder canExtractFromBrewing(boolean value) { this.canExtractFromBrewing = value; return this; }
        public Builder canTakeBrewingOutput(boolean value) { this.canTakeBrewingOutput = value; return this; }
        public Builder canTakeCraftingOutput(boolean value) { this.canTakeCraftingOutput = value; return this; }
        public Builder canInsertIntoAnvil(boolean value) { this.canInsertIntoAnvil = value; return this; }
        public Builder canTakeAnvilOutput(boolean value) { this.canTakeAnvilOutput = value; return this; }

        public ContainerPolicy build() {
            return new ContainerPolicy(
                    canOpenContainers,
                    canOpenChests,
                    canOpenFurnaces,
                    canOpenBrewingStands,
                    canOpenShulkerBoxes,
                    canInsertIntoFurnace,
                    canExtractFromFurnace,
                    canTakeFurnaceOutput,
                    canInsertIntoBrewing,
                    canExtractFromBrewing,
                    canTakeBrewingOutput,
                    canTakeCraftingOutput,
                    canInsertIntoAnvil,
                    canTakeAnvilOutput
            );
        }
    }

    ContainerPolicy mergeRestrictive(ContainerPolicy other) {
        return new ContainerPolicy(
                this.canOpenContainers && other.canOpenContainers,
                this.canOpenChests && other.canOpenChests,
                this.canOpenFurnaces && other.canOpenFurnaces,
                this.canOpenBrewingStands && other.canOpenBrewingStands,
                this.canOpenShulkerBoxes && other.canOpenShulkerBoxes,
                this.canInsertIntoFurnace && other.canInsertIntoFurnace,
                this.canExtractFromFurnace && other.canExtractFromFurnace,
                this.canTakeFurnaceOutput && other.canTakeFurnaceOutput,
                this.canInsertIntoBrewing && other.canInsertIntoBrewing,
                this.canExtractFromBrewing && other.canExtractFromBrewing,
                this.canTakeBrewingOutput && other.canTakeBrewingOutput,
                this.canTakeCraftingOutput && other.canTakeCraftingOutput,
                this.canInsertIntoAnvil && other.canInsertIntoAnvil,
                this.canTakeAnvilOutput && other.canTakeAnvilOutput
        );
    }
}
