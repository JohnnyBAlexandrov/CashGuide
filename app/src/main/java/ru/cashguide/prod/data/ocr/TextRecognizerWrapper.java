package ru.cashguide.prod.data.ocr;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Обёртка над ML Kit Text Recognition. Преобразует изображение в распознанный текст.
 */
public final class TextRecognizerWrapper {

    public Single<String> recognize(@NonNull Context context, @NonNull Uri imageUri) {
        return Single.fromCallable(() -> {
            InputImage image = InputImage.fromFilePath(context, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            try {
                Text result = Tasks.await(recognizer.process(image));
                return result.getText();
            } finally {
                recognizer.close();
            }
        }).subscribeOn(Schedulers.io());
    }
}