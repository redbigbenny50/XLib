package com.whatxe.xlib.capability;

public record ContainerPolicy(
        boolean canOpenContainers,
        boolean canOpenChests,
        boolean canOpenFurnaces,
        boolean canOpenShulkerBoxes
) {
    public static final ContainerPolicy FULL = new ContainerPolicy(true, true, true, true);

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canOpenContainers = true;
        private boolean canOpenChests = true;
        private boolean canOpenFurnaces = true;
        private boolean canOpenShulkerBoxes = true;

        private Builder() {}

        public Builder canOpenContainers(boolean value) { this.canOpenContainers = value; return this; }
        public Builder canOpenChests(boolean value) { this.canOpenChests = value; return this; }
        public Builder canOpenFurnaces(boolean value) { this.canOpenFurnaces = value; return this; }
        public Builder canOpenShulkerBoxes(boolean value) { this.canOpenShulkerBoxes = value; return this; }

        public ContainerPolicy build() {
            return new ContainerPolicy(canOpenContainers, canOpenChests, canOpenFurnaces, canOpenShulkerBoxes);
        }
    }

    ContainerPolicy mergeRestrictive(ContainerPolicy other) {
        return new ContainerPolicy(
                this.canOpenContainers && other.canOpenContainers,
                this.canOpenChests && other.canOpenChests,
                this.canOpenFurnaces && other.canOpenFurnaces,
                this.canOpenShulkerBoxes && other.canOpenShulkerBoxes
        );
    }
}
