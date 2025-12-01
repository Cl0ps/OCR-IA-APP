package app.ij.mlwithtensorflowlite.network;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // URL base del servidor. Si cambia, hay que actualizarla.
    private static final String BASE_URL = "http://192.168.1.37:8000/";

    private static ApiService apiService = null;

    public static ApiService getService() {
        if (apiService == null) {
            // Para depuración: un interceptor que muestra en Logcat los detalles de la petición.
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Se necesita un cliente OkHttp para ajustar los tiempos de espera.
            // Es importante para archivos grandes o respuestas lentas del servidor.
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS) // Tiempo para conectar.
                    .readTimeout(120, TimeUnit.SECONDS)    // Tiempo para leer la respuesta.
                    .writeTimeout(120, TimeUnit.SECONDS)   // Tiempo para escribir la petición (subir archivo).
                    .addInterceptor(loggingInterceptor)
                    .build();

            // Construimos Retrofit.
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // Usamos el cliente personalizado.
                    .addConverterFactory(GsonConverterFactory.create()) // Gson para convertir JSON.
                    .build();

            // Creamos la instancia del servicio.
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}
