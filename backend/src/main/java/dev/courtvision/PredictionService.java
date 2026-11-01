package dev.courtvision;

import org.springframework.stereotype.Service;

@Service
public class PredictionService {
    public static final String MODEL_VERSION = "surface-elo-demo-v1";

    public double probability(Player first, Player second, Surface surface) {
        return 1.0 / (1.0 + Math.pow(10, (second.rating(surface) - first.rating(surface)) / 400.0));
    }
}
