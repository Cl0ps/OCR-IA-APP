package app.ij.mlwithtensorflowlite;

import com.google.gson.annotations.SerializedName;

// Clase para mapear la respuesta JSON del servidor.
// Los nombres de los campos deben coincidir con las claves del JSON.
public class PredictionResponse {

    // @SerializedName se usa si el nombre de la variable en Java
    private String prediction;
    private String audio_base64;

    // --- Getters ---
    public String getPrediction() {
        return prediction;
    }

    public String getAudio_base64() {
        return audio_base64;
    }

    // --- Setters (pueden ser útiles para pruebas) ---
    public void setPrediction(String prediction) {
        this.prediction = prediction;
    }

    public void setAudio_base64(String audio_base64) {
        this.audio_base64 = audio_base64;
    }
}
