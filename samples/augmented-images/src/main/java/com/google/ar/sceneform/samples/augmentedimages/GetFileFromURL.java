package com.google.ar.sceneform.samples.augmentedimages;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GetFileFromURL {
    private ArrayList<Model> models;

    private String version;

    public void readJson() {
        CompletableFuture<ArrayList<Model>> completableFuture = CompletableFuture.supplyAsync(() -> buildModel());
        try {
            this.models = completableFuture.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<Model> buildModel() {
        ArrayList<Model> arrayList = new ArrayList<>();
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("", "")
                .build();
        Request request = new Request.Builder()
                .url("http://54.255.185.49/api/update")
                .post(requestBody)
                .build();

        try  {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String jsonData = response.body().string();
                JSONObject object = new JSONObject(jsonData);
                JSONObject data = object.getJSONObject("data");
                this.version = String.valueOf(object.getJSONObject("version"));
                JSONArray listModel = data.getJSONArray("appModels");
                JSONObject modelFiles = data.getJSONObject("files");
                for (int i = 0; i < listModel.length(); i++) {
                    JSONObject model = listModel.getJSONObject(i);
                    JSONArray position = model.getJSONArray("position");
                    JSONArray rotation = model.getJSONArray("rotation");
                    JSONArray scale = model.getJSONArray("scale");
                    String nameModel = model.getString("name");
                    String url = modelFiles.getJSONObject(nameModel).getString("downloadUrl");
                    arrayList.add(new Model(nameModel,
                            new float[]{(float) position.getDouble(0), (float) position.getDouble(1), (float) position.getDouble(2)},
                            new float[]{(float) rotation.getDouble(0), (float) rotation.getDouble(1), (float) rotation.getDouble(2), 0},
                            new float[]{(float) scale.getDouble(0), (float) scale.getDouble(1), (float) scale.getDouble(2)},
                            url));
//                            System.out.println("ABC1: " + this.models1.size());
//                            getUrl(fileCache, url, nameModel);
                }
//                        System.out.println(modelArray.size() + "9090");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return arrayList;
    }

    public ArrayList<Model> getModels() {
        return models;
    }

    public void getUrl(FileOutputStream fos, String url, String nameModel) {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> buildUrl(fos, url, nameModel));
        try {
            String complete = completableFuture.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String buildUrl(FileOutputStream fos, String url, String nameModel) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            System.out.println(url);
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                // Lấy đường dẫn đến thư mục muốn lưu file
                InputStream inputStream = response.body().byteStream();
//                File modelFile = new File(fileCache, nameModel);
//                FileOutputStream fileOutputStream = new FileOutputStream(modelFile);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                fos.close();
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getVersion() {
        return version;
    }
}
