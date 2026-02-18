package com.chat;

/**
 * Configuration class chứa các thiết lập của server
 */
public class ServerConfig {
    
    private final int port;
    private final String modelPath;
    private final int gpuLayers;
    private final float defaultTemperature;
    private final float minTemperature;
    private final float maxTemperature;
    
    private ServerConfig(Builder builder) {
        this.port = builder.port;
        this.modelPath = builder.modelPath;
        this.gpuLayers = builder.gpuLayers;
        this.defaultTemperature = builder.defaultTemperature;
        this.minTemperature = builder.minTemperature;
        this.maxTemperature = builder.maxTemperature;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getModelPath() {
        return modelPath;
    }
    
    public int getGpuLayers() {
        return gpuLayers;
    }
    
    public float getDefaultTemperature() {
        return defaultTemperature;
    }
    
    public float getMinTemperature() {
        return minTemperature;
    }
    
    public float getMaxTemperature() {
        return maxTemperature;
    }
    
    /**
     * Validate temperature value
     */
    public float validateTemperature(float temperature) {
        if (temperature < minTemperature || temperature > maxTemperature) {
            return defaultTemperature;
        }
        return temperature;
    }
    
    /**
     * Builder pattern for ServerConfig
     */
    public static class Builder {
        private int port = 8888;
        private String modelPath = "models/gemma-3-4b-it-Q4_0.gguf";
        private int gpuLayers = 43;
        private float defaultTemperature = 0.7f;
        private float minTemperature = 0.0f;
        private float maxTemperature = 2.0f;
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder modelPath(String modelPath) {
            this.modelPath = modelPath;
            return this;
        }
        
        public Builder gpuLayers(int gpuLayers) {
            this.gpuLayers = gpuLayers;
            return this;
        }
        
        public Builder defaultTemperature(float defaultTemperature) {
            this.defaultTemperature = defaultTemperature;
            return this;
        }
        
        public Builder temperatureRange(float min, float max) {
            this.minTemperature = min;
            this.maxTemperature = max;
            return this;
        }
        
        public ServerConfig build() {
            return new ServerConfig(this);
        }
    }
    
    /**
     * Tạo default config
     */
    public static ServerConfig defaultConfig() {
        return new Builder().build();
    }
    
    @Override
    public String toString() {
        return "ServerConfig{" +
                "port=" + port +
                ", modelPath='" + modelPath + '\'' +
                ", gpuLayers=" + gpuLayers +
                ", defaultTemperature=" + defaultTemperature +
                ", temperatureRange=[" + minTemperature + ", " + maxTemperature + "]" +
                '}';
    }
}
