package com.remoterecognition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Pair;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class GetImage implements Runnable {

    protected Socket clientSocket;
    String json = null;

    private final Lock lock = new ReentrantLock();
    private final Condition complete = lock.newCondition();

    public GetImage(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        try {
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();

            StringBuilder s = new StringBuilder();

            for (int i = 0; i<15; i++ ){
                int c = input.read();
                if ((char)c == ' '){
                    break;
                }
                s.append((char) c);
            }
            int length = Integer.decode(s.toString());

            byte[] bits = new byte[length];

            for (int i = 0; i < length; i++) {
                bits[i] = (byte) input.read();
            }

            Log.d("REC", String.valueOf(length));

            Bitmap imageBitmap = BitmapFactory.decodeByteArray(bits, 0, length);
            if (imageBitmap == null){
                output.write("err".getBytes());
            } else {
                runTextRecognition(imageBitmap);

                lock.lock();
                try {
                    while (json == null) {
                        complete.await();
                    }

                    output.write(json.getBytes());
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                finally {
                    lock.unlock();
                }
            }

//            Log.e("I", "fin");
//            output.write(("RES").getBytes());
//            Log.e("I", "res");
//
            output.close();
            input.close();
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }


    private void runTextRecognition(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        recognizer.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                processTextRecognitionResult(texts);
                            }
                        });
//                .addOnFailureListener(
//                        new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                // Task failed with an exception
//                                e.printStackTrace();
//                            }
//                        });
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) {
        ArrayList<Pair<String, String>> results = new ArrayList<>();

        lock.lock();
        try {

            Log.d("REC", "EL_TEXT" + texts.getText());
            List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
            if (blocks.size() == 0) {
                Log.d("REC", "No text found");
                json = "[]";
                complete.signal();
                return;
            }
            for (FirebaseVisionText.TextBlock block : blocks) {
                List<FirebaseVisionText.Line> lines = block.getLines();
                for (FirebaseVisionText.Line line : lines) {
                    List<FirebaseVisionText.Element> elements = line.getElements();
                    for (FirebaseVisionText.Element el : elements) {
                        Log.d("REC", "EL_BBOX" + el.getBoundingBox().flattenToString());
                        results.add(new Pair<>(el.getText(), el.getBoundingBox().flattenToString()));

                    }
                }
            }

            json = new Gson().toJson(results);
            Log.d("REC", "JSON: " + json);
            complete.signal();
        }
        finally {
            lock.unlock();
        }

    }


}