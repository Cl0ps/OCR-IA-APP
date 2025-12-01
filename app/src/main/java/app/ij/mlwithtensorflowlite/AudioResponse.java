package app.ij.mlwithtensorflowlite;

import com.google.gson.annotations.SerializedName;

public class AudioResponse {

    // El nombre "Texto" debe coincidir EXACTAMENTE con la clave en el JSON.
    // Si la clave en el JSON fuera "texto_resultado", usarías @SerializedName("texto_resultado")
    @SerializedName("Texto")
    private String texto;

    // Lo mismo para el audio en base64
    @SerializedName("AudioBase64") // <-- Asegúrate que este nombre coincida con tu JSON real
    private String audioBase64;

    // Getters para acceder a los datos
    public String getTexto() {
        return texto;
    }

    public String getAudioBase64() {
        return audioBase64;
    }
}
