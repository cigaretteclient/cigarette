package dev.cigarette.lib;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final int featureCount = 5;

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
        float[] feat = new float[featureCount];
        feat[0] = (float) x;
        feat[1] = (float) y;
        feat[2] = (float) z;
        feat[3] = yaw;
        feat[4] = pitch;
        features.add(feat);
        labels.add(label);
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
     * Train a small XGBoost model using the collected examples and save it to the configured path.
     * Clears the collected examples after training.
     */
    public void trainAndSaveModel(int numRounds) throws XGBoostError, IOException {
        if (features.isEmpty()) return;

        int nrow = features.size();
        int ncol = featureCount;
        float[] data = new float[nrow * ncol];
        float[] labelArr = new float[nrow];

        for (int i = 0; i < nrow; i++) {
            float[] row = features.get(i);
            System.arraycopy(row, 0, data, i * ncol, ncol);
            labelArr[i] = labels.get(i);
        }

        DMatrix dtrain = new DMatrix(data, nrow, ncol, Float.NaN);
        dtrain.setLabel(labelArr);

        Map<String, Object> params = new HashMap<>();
        params.put("eta", 0.1);
        params.put("max_depth", 3);
        params.put("objective", "binary:logistic");
        params.put("silent", 1);

        HashMap<String, DMatrix> watches = new HashMap<>();
        watches.put("train", dtrain);

        this.booster = XGBoost.train(dtrain, params, numRounds, watches, null, null);

        File out = new File(modelPath);
        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        this.booster.saveModel(modelPath);

        dtrain.dispose();

        features.clear();
        labels.clear();
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
        return features.size();
    }
}
