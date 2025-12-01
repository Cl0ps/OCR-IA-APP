package app.ij.mlwithtensorflowlite.network;

import app.ij.mlwithtensorflowlite.PredictionResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    // Define el endpoint para la subida de la imagen.
    // Se usa @POST con la URL del servicio de predicción.
    @POST("https://cleanliest-crenately-leena.ngrok-free.dev/predict2/")
    Call<PredictionResponse> subirFoto(@Part MultipartBody.Part file);
}
