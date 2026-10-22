package dev.propulsionteam.propulsionsimulated.particles;

public final class ParticleScale {
    private double size = 1.0d;
    private double motion = 1.0d;
    private double owner = 1.0d;

    public static double valid(double scale) {
        return Double.isFinite(scale) && scale > 0.0d ? scale : 1.0d;
    }

    public void multiply(double factor) {
        size *= valid(factor);
    }

    public void initialize(double ownerScale, boolean pym) {
        owner = valid(ownerScale);
        if (pym) {
            motion = size;
        } else {
            size *= owner;
        }
    }

    public double resize(double ownerScale) {
        double next = valid(ownerScale);
        double ratio = next / owner;
        size *= ratio;
        motion *= ratio;
        owner = next;
        return ratio;
    }

    public float size() {
        return (float) size;
    }

    public double motion() {
        return motion;
    }

    public double owner() {
        return owner;
    }
}
