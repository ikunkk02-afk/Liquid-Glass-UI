/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.ui;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class MappedSlider extends AbstractSliderButton {

    private final double min;
    private final double max;
    private final Consumer<Double> onChange;
    private final boolean integer;
    private final Component originalMessage;

    public static MappedSlider floatSlider(int x, int y, int width, int height, Component msg, double min, double max, double init, Consumer<Double> onChange) {
        return new MappedSlider(x, y, width, height, msg, min, max, init, onChange, false);
    }

    public static MappedSlider intSlider(int x, int y, int width, int height, Component msg, int min, int max, int init, Consumer<Integer> onChange) {
        return new MappedSlider(x, y, width, height, msg, min, max, init, d -> onChange.accept(d.intValue()), true);
    }

    private MappedSlider(int x, int y, int width, int height, Component message, double min, double max, double init, Consumer<Double> onChange, boolean integer) {
        super(x, y, width, height, message, 0);
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        this.integer = integer;
        this.originalMessage = message;
        this.value = Mth.clamp(inverseMap(init), 0.0, 1.0);
        updateMessage();
    }

    private double map(double v) {
        return min + v * (max - min);
    }

    private double inverseMap(double real) {
        if (max == min) return 0;
        return (real - min) / (max - min);
    }

    @Override
    protected void updateMessage() {
        double v = map(this.value);
        if (integer) v = Math.round(v);
        this.setMessage(Component.literal(originalMessage.getString() + ": " + format(v)));
    }

    private String format(double v) {
        if (integer) return Integer.toString((int) Math.round(v));
        return String.format("%.3f", v);
    }

    @Override
    protected void applyValue() {
        double v = map(this.value);
        if (integer) v = Math.round(v);
        onChange.accept(v);
    }
}
