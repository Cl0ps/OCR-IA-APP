package app.ij.mlwithtensorflowlite;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import app.ij.mlwithtensorflowlite.network.ApiService;
import app.ij.mlwithtensorflowlite.network.RetrofitClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    // --- Constantes para la cámara ---
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    // --- Vistas de la UI ---
    private ImageView imageView;
    private TextView resultTextView;
    private Button captureButton, playButton; // Botón para reproducir el audio.

    // --- Variables de estado ---
    private Bitmap imageBitmap; // La imagen capturada.
    private Uri audioUri; // URI del archivo de audio guardado.
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Inicialización de vistas ---
        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.resultTextView);
        captureButton = findViewById(R.id.captureButton);
        playButton = findViewById(R.id.playButton);

        // --- Configuración de listeners ---
        captureButton.setOnClickListener(v -> abrirCamara());

        playButton.setOnClickListener(v -> {
            if (audioUri != null) {
                reproducirAudio(audioUri);
            } else {
                Toast.makeText(this, "No hay audio para reproducir", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Lógica para la cámara ---
    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
            // Una vez que tenemos la imagen, la enviamos al servidor.
            enviarImagenAlServidor(imageBitmap);
        }
    }

    // --- Lógica de Red (Retrofit) ---
    private void enviarImagenAlServidor(Bitmap bitmap) {
        // Convertimos el Bitmap a un archivo para poder enviarlo.
        File file = bitmapToFile(bitmap, "photo.jpg");
        if (file == null) {
            Toast.makeText(this, "Error al crear el archivo de imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creamos el cuerpo de la petición (RequestBody) para el archivo.
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Obtenemos el servicio de la API y hacemos la llamada.
        ApiService apiService = RetrofitClient.getService();
        Call<PredictionResponse> call = apiService.subirFoto(body);

        // La llamada se ejecuta en segundo plano.
        call.enqueue(new Callback<PredictionResponse>() {
            @Override
            public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PredictionResponse predictionResponse = response.body();
                    // Mostramos la predicción de texto.
                    resultTextView.setText(predictionResponse.getPrediction());

                    // Decodificamos el audio y lo guardamos.
                    guardarAudioDesdeBase64(predictionResponse.getAudio_base64());
                } else {
                    // Si algo falla, lo mostramos.
                    resultTextView.setText("Error en la respuesta del servidor");
                }
            }

            @Override
            public void onFailure(Call<PredictionResponse> call, Throwable t) {
                // Si la petición falla (ej. sin internet), lo mostramos.
                resultTextView.setText("Fallo en la conexión: " + t.getMessage());
                Log.e("MainActivity", "Error en Retrofit", t);
            }
        });
    }

    // --- Lógica de archivos ---
    private File bitmapToFile(Bitmap bitmap, String fileName) {
        File filesDir = getApplicationContext().getFilesDir();
        File imageFile = new File(filesDir, fileName);

        try (OutputStream os = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void guardarAudioDesdeBase64(String audioBase64) {
        try {
            // Decodificamos la cadena Base64 a bytes.
            byte[] audioBytes = Base64.decode(audioBase64, Base64.DEFAULT);

            // Guardamos el archivo en el almacenamiento compartido (directorio de Música).
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "audio_recibido.wav");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC);

            Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    os.write(audioBytes);
                    this.audioUri = uri; // Guardamos la URI para poder reproducirla después.
                    playButton.setVisibility(View.VISIBLE); // Hacemos visible el botón de play.
                    Toast.makeText(this, "Audio guardado en Música", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    throw new IOException("Error al escribir en el OutputStream", e);
                }
            } else {
                throw new IOException("No se pudo crear el archivo en MediaStore");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar el audio", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Lógica de reproducción de audio ---
    private void reproducirAudio(Uri audioUri) {
        // Si ya hay algo reproduciéndose, lo paramos.
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getApplicationContext(), audioUri);
            mediaPlayer.prepare(); // Puede tardar, idealmente sería asíncrono (prepareAsync).
            mediaPlayer.start();

            // Cuando termine, liberamos los recursos.
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al reproducir el audio", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Liberamos el MediaPlayer si la app se va a segundo plano.
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
