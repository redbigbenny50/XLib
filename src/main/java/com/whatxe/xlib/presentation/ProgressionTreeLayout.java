package com.whatxe.xlib.presentation;

public record ProgressionTreeLayout(
        int nodeSize,
        int columnSpacing,
        int rowSpacing,
        int labelWidth,
        int labelLineHeight,
        int maxLabelLines,
        int labelGap
) {
    public ProgressionTreeLayout {
        requirePositive(nodeSize, "nodeSize");
        requirePositive(columnSpacing, "columnSpacing");
        requirePositive(rowSpacing, "rowSpacing");
        requirePositive(labelWidth, "labelWidth");
        requirePositive(labelLineHeight, "labelLineHeight");
        requirePositive(maxLabelLines, "maxLabelLines");
        requirePositive(labelGap, "labelGap");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ProgressionTreeLayout defaultLayout() {
        return builder().build();
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    public static final class Builder {
        private int nodeSize = 34;
        private int columnSpacing = 132;
        private int rowSpacing = 84;
        private int labelWidth = 96;
        private int labelLineHeight = 10;
        private int maxLabelLines = 3;
        private int labelGap = 6;

        private Builder() {}

        public Builder nodeSize(int nodeSize) {
            this.nodeSize = nodeSize;
            return this;
        }

        public Builder columnSpacing(int columnSpacing) {
            this.columnSpacing = columnSpacing;
            return this;
        }

        public Builder rowSpacing(int rowSpacing) {
            this.rowSpacing = rowSpacing;
            return this;
        }

        public Builder labelWidth(int labelWidth) {
            this.labelWidth = labelWidth;
            return this;
        }

        public Builder labelLineHeight(int labelLineHeight) {
            this.labelLineHeight = labelLineHeight;
            return this;
        }

        public Builder maxLabelLines(int maxLabelLines) {
            this.maxLabelLines = maxLabelLines;
            return this;
        }

        public Builder labelGap(int labelGap) {
            this.labelGap = labelGap;
            return this;
        }

        public ProgressionTreeLayout build() {
            return new ProgressionTreeLayout(
                    this.nodeSize,
                    this.columnSpacing,
                    this.rowSpacing,
                    this.labelWidth,
                    this.labelLineHeight,
                    this.maxLabelLines,
                    this.labelGap
            );
        }
    }
}
