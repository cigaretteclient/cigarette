package dev.cigarette.lib;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Lightweight helper for collecting simple position+rotation training examples
 * and training/saving a small XGBoost model.
 */
public class XGBoostModelHelper {
    private final String modelPath;
    private Booster booster;

    private final List<float[]> features = new ArrayList<>();
    private final List<Float> labels = new ArrayList<>();
    private final List<Long> featureHashes = new ArrayList<>();
    private final Set<Long> dedup = new HashSet<>();
    private int positives = 0, negatives = 0;

    private final int featureCount = 5;

    private volatile boolean trainingInProgress = false;
    private long lastTrainMillis = 0L;

    private static final int DEFAULT_MIN_EXAMPLES_TO_TRAIN = 60;
    private static final int MAX_BUFFER = 256;
    private static final long TRAIN_COOLDOWN_MS = 3000L;

    public XGBoostModelHelper(String modelPath) {
        this.modelPath = modelPath;
        try {
            loadModelIfExists();
        } catch (Exception ignored) {
        }
    }

    public String getModelPath() {
        return modelPath;
    }

    /**
     * Static helper to prepare a model helper instance.
     *
     * Supported "spec" formats:
     *  - "github:<repo>" -> downloads content from repo
     *  - "url:<url>" -> treats as direct file URL
     *  - etc -> local path
     */
    public static <T extends XGBoostModelHelper> T prepareModel(String filename, String sourceSpec, Function<String, T> factory) {
        String destPath = FabricLoader.getInstance().getConfigDir().resolve(filename).toString();
        try {
            File dest = new File(destPath);
            if (!dest.exists()) {
                if (sourceSpec != null && !sourceSpec.isEmpty()) {
                    if (sourceSpec.startsWith("github:")) {
                        String repoPath = sourceSpec.split("github:")[1];
                        String url = "https://raw.githubusercontent.com/" + repoPath + "/refs/heads/main/" + filename;
                        HttpL.downloadUrlToFile(url, destPath);
                    } else if (sourceSpec.startsWith("url:")) {
                        String raw = sourceSpec.substring("url:".length());
                        String url = raw.endsWith("/") ? raw + filename : raw;
                        HttpL.downloadUrlToFile(url, destPath);
                    } else {
                        File provided = new File(sourceSpec);
                        if (provided.isDirectory()) {
                            File candidate = new File(provided, filename);
                            if (candidate.exists()) {
                                File parent = dest.getParentFile();
                                if (parent != null && !parent.exists()) parent.mkdirs();
                                java.nio.file.Files.copy(candidate.toPath(), dest.toPath());
                            }
                        } else if (provided.isFile()) {
                            File parent = dest.getParentFile();
                            if (parent != null && !parent.exists()) parent.mkdirs();
                            java.nio.file.Files.copy(provided.toPath(), dest.toPath());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return factory.apply(destPath);
    }

    /**
     * Prepare a model helper instance with the default implementation.
     */
    public static XGBoostModelHelper prepareModel(String filename, String sourceSpec) {
        return prepareModel(filename, sourceSpec, XGBoostModelHelper::new);
    }

    /**
     * Load model from disk if the file exists.
     */
    public void loadModelIfExists() throws XGBoostError, IOException {
        File f = new File(modelPath);
        if (f.exists()) {
            this.booster = XGBoost.loadModel(modelPath);
        }
    }

    /**
     * Add a training example by coordinates and rotation.
     */
    public void addExampleFromPosition(double x, double y, double z, float yaw, float pitch, float label) {
        long h = hashExample(x, y, z, yaw, pitch, label);
        synchronized (this) {
            float[] feat = new float[featureCount];
            feat[0] = (float) x;
            feat[1] = (float) y;
            feat[2] = (float) z;
            feat[3] = yaw;
            feat[4] = pitch;

            features.add(feat);
            labels.add(label);
            featureHashes.add(h);
            dedup.add(h);
            if (label >= 0.5f) positives++; else negatives++;

            trimBufferIfNeeded();
        }
    }

    /**
     * Convenience: add an example from a PersistentPlayer.
     */
    public void addExampleFromPersistentPlayer(dev.cigarette.agent.MurderMysteryAgent.PersistentPlayer p, float label) {
        if (p == null || p.playerEntity == null) return;

        double x = p.playerEntity.getX();
        double y = p.playerEntity.getY();
        double z = p.playerEntity.getZ();
        float yaw = p.playerEntity.getYaw();
        float pitch = p.playerEntity.getPitch();
        addExampleFromPosition(x, y, z, yaw, pitch, label);
    }

    /**
     * Train in the background if we have enough buffered examples and we're not already training,
     * with cooldown to avoid spamming heavy computations on the main thread.
     */
    public void trainAsyncIfNeeded() {
        trainAsyncIfNeeded(DEFAULT_MIN_EXAMPLES_TO_TRAIN, 120);
    }

    public void trainAsyncIfNeeded(int minExamples, int numRounds) {
        final float[][] snapFeatures;
        final float[] snapLabels;
        synchronized (this) {
            long now = System.currentTimeMillis();
            if (trainingInProgress) return;
            if (features.size() < Math.max(1, minExamples)) return;
            if (positives == 0 || negatives == 0) return;
            if (now - lastTrainMillis < TRAIN_COOLDOWN_MS) return;

            int nrow = features.size();
            int ncol = featureCount;
            snapFeatures = new float[nrow][ncol];
            snapLabels = new float[nrow];
            for (int i = 0; i < nrow; i++) {
                float[] row = features.get(i);
                System.arraycopy(row, 0, snapFeatures[i], 0, ncol);
                snapLabels[i] = labels.get(i);
            }
            features.clear();
            labels.clear();
            dedup.clear();
            featureHashes.clear();
            positives = negatives = 0;
            trainingInProgress = true;
            lastTrainMillis = now;
        }

        new Thread(() -> {
            try {
                booster(numRounds, snapFeatures, snapLabels);
            } catch (Throwable ignored) {
            } finally {
                trainingInProgress = false;
            }
        }, "XGBoost-Train-Thread").start();
    }

    private void booster(int numRounds, float[][] snapFeatures, float[] snapLabels) throws XGBoostError {
        Booster trained = trainBoosterFromSnapshot(snapFeatures, snapLabels, numRounds);
        synchronized (this) {
            if (this.booster != null) {
                try { this.booster.dispose(); } catch (Exception ignored) {}
            }
            this.booster = trained;
            File out = new File(modelPath);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            this.booster.saveModel(modelPath);
        }
    }

    private Booster trainBoosterFromSnapshot(float[][] snapFeatures, float[] snapLabels, int numRounds) throws XGBoostError {
        if (snapFeatures == null || snapFeatures.length == 0) return this.booster;
        int nrow = snapFeatures.length;
        int ncol = featureCount;
        float[] data = new float[nrow * ncol];
        for (int i = 0; i < nrow; i++) {
            System.arraycopy(snapFeatures[i], 0, data, i * ncol, ncol);
        }

        DMatrix dtrain = new DMatrix(data, nrow, ncol, Float.NaN);
        dtrain.setLabel(snapLabels);

        Map<String, Object> params = new HashMap<>();
        params.put("eta", 0.1);
        params.put("max_depth", 3);
        params.put("objective", "binary:logistic");
        params.put("silent", 1);

        HashMap<String, DMatrix> watches = new HashMap<>();
        watches.put("train", dtrain);

        Booster result = XGBoost.train(dtrain, params, numRounds, watches, null, null);
        dtrain.dispose();
        return result;
    }

    /**
     * Prefer trainAsyncIfNeeded().
     */
    public void trainAndSaveModel(int numRounds) throws XGBoostError, IOException {
        float[][] snapF;
        float[] snapL;
        synchronized (this) {
            if (features.isEmpty()) return;
            int nrow = features.size();
            int ncol = featureCount;
            snapF = new float[nrow][ncol];
            snapL = new float[nrow];
            for (int i = 0; i < nrow; i++) {
                float[] row = features.get(i);
                System.arraycopy(row, 0, snapF[i], 0, ncol);
                snapL[i] = labels.get(i);
            }
            features.clear();
            labels.clear();
            dedup.clear();
            featureHashes.clear();
            positives = negatives = 0;
        }

        booster(numRounds, snapF, snapL);
    }

    public boolean hasModel() {
        return this.booster != null;
    }

    public float predict(double x, double y, double z, float yaw, float pitch) throws XGBoostError {
        if (booster == null) throw new IllegalStateException("No model loaded or trained");
        float[] row = new float[]{(float) x, (float) y, (float) z, yaw, pitch};
        DMatrix dmat = new DMatrix(row, 1, featureCount, Float.NaN);
        float[][] out = booster.predict(dmat);
        dmat.dispose();
        if (out.length == 0 || out[0].length == 0) return Float.NaN;
        return out[0][0];
    }

    public void dispose() {
        if (this.booster != null) {
            try {
                this.booster.dispose();
            } catch (Exception ignored) {
            }
            this.booster = null;
        }
    }

    public int getBufferedExampleCount() {
        synchronized (this) {
            return features.size();
        }
    }

    private void trimBufferIfNeeded() {
        while (features.size() > MAX_BUFFER) {
            float removedLabel = labels.remove(0);
            float[] removedFeat = features.remove(0);
            Long removedHash = featureHashes.remove(0);
            if (removedHash != null) dedup.remove(removedHash);
            if (removedLabel >= 0.5f) positives--; else negatives--;
        }
    }

    private static long hashExample(double x, double y, double z, float yaw, float pitch, float label) {
        int xi = Math.round((float) (x * 100f));
        int yi = Math.round((float) (y * 100f));
        int zi = Math.round((float) (z * 100f));
        int yiw = Math.round(yaw * 10f);
        int piw = Math.round(pitch * 10f);
        int li = label >= 0.5f ? 1 : 0;
        long h = 1469598103934665603L;
        h ^= xi; h *= 1099511628211L;
        h ^= yi; h *= 1099511628211L;
        h ^= zi; h *= 1099511628211L;
        h ^= yiw; h *= 1099511628211L;
        h ^= piw; h *= 1099511628211L;
        h ^= li; h *= 1099511628211L;
        return h;
    }
}
