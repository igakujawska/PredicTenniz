package dev.courtvision;

public record Player(String id, String name, String country, double hardElo, double clayElo, double grassElo) {
    public double rating(Surface surface) {
        return switch (surface) {
            case HARD -> hardElo;
            case CLAY -> clayElo;
            case GRASS -> grassElo;
        };
    }
}
