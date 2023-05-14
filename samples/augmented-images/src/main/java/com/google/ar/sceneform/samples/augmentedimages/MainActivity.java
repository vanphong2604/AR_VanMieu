package com.google.ar.sceneform.samples.augmentedimages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.FilamentInstance;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.FilamentEngineWrapper;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.gorisse.thomas.sceneform.Filament;
import com.gorisse.thomas.sceneform.FilamentKt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener {

    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private ArFragment arFragment;

    private Config config;

    private HashMap<String, Renderable> mapModel = new HashMap<>();
    private ArrayList<Node> nodeArrayList = new ArrayList<>();
    private AugmentedImageDatabase database;

    private Renderable model;

    private ProgressBar progressBar;

    private TextView updateStatus;

    private GetFileFromURL getFileFromURL = new GetFileFromURL();

    private Boolean check = true;

    private Boolean checkUpdate = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.progressBar = findViewById(R.id.progressBar);
        this.updateStatus = findViewById(R.id.textView);
        if(Sceneform.isSupported(this)) {
            // .glb models can be loaded at runtime when needed or when app starts
            // This method loads ModelRenderable when app starts
            CompletableFuture<Void> stringCompletableFuture = CompletableFuture.supplyAsync( () -> {
                checkUpdate();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                    }
                });
                return null;
            });
            stringCompletableFuture.thenAccept(result -> {
                getSupportFragmentManager().addFragmentOnAttachListener(this);
                if (savedInstanceState == null) {
                    if (Sceneform.isSupported(this)) {
                        getSupportFragmentManager().beginTransaction()
                                .add(R.id.arFragment, ArFragment.class, null)
                                .commit();
                    }
                }
                loadModels();
            });
        }
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        this.config = config;
        // Disable plane detection
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)

        database = new AugmentedImageDatabase(session);

        Bitmap qrImage = BitmapFactory.decodeResource(getResources(), R.drawable.qr);
        // Every image has to have its own unique String identifier
        database.addImage("qr", qrImage);

        config.setAugmentedImageDatabase(database);
        // Check for image detection
        arFragment.setOnAugmentedImageUpdateListener(this::onAugmentedImageTrackingUpdate);
    }

    public void checkUpdate() {
        this.getFileFromURL.readJson();
        File file = getFileStreamPath("version.txt");
        FileOutputStream fos = null;
        Integer currentVersion;
        if (file.mkdir()) {
            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();
            } catch (IOException e) {
                // handle exception
            }
            currentVersion = Integer.valueOf(text.toString());
        } else {
            currentVersion = 0;
        }
        System.out.println("ABC1: " + currentVersion);
        Integer newVersion = Integer.valueOf(this.getFileFromURL.getVersion());
        if (newVersion <=  currentVersion) {
           return;
        }
        ArrayList<Model> modelArrayList = this.getFileFromURL.getModels();
        for (int i = 0; i < modelArrayList.size(); i++) {
            System.out.println("9999999999999999");
            String name = modelArrayList.get(i).getName();
            String url = modelArrayList.get(i).getUrl();
            file = getFileStreamPath(name);
            if (!file.exists()) {
                try {
                    fos = openFileOutput(name, Context.MODE_PRIVATE);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                this.getFileFromURL.getUrl(fos, url, name);
            }
        }
        try {
            fos = openFileOutput("version.txt", Context.MODE_PRIVATE);
            fos.write(newVersion);
            fos.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        this.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        futures.forEach(future -> {
            if (!future.isDone())
                future.cancel(true);
        });
    }

    public void loadModels() {
        ArrayList<Model> modelArrayList = this.getFileFromURL.getModels();
        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
        Set<String> set = new HashSet<>();
        for (int i = 0; i < modelArrayList.size(); i++) {
                String name = modelArrayList.get(i).getName();
                File file = getFileStreamPath(name);
                if (!set.contains(name)) {
                    ModelRenderable.builder()
                            .setSource(this, Uri.parse(file.getAbsolutePath()))
                            .setIsFilamentGltf(true)
    //                .setAsyncLoadEnabled(true)
                            .build()
                            .thenAccept(model -> {
                                MainActivity activity = weakActivity.get();
                                if (activity != null) {
                                    activity.mapModel.put(name, model);
                                }
                            })
                            .exceptionally(throwable -> {
                                Toast.makeText(
                                        this, "Unable to load model", Toast.LENGTH_LONG).show();
                                return null;
                            });
                    set.add(name);
                }
        }
    }

    public void createNode(AnchorNode anchorNode, float[] position, float[] rotation, float[] scale, String name) {
        Node node = new Node();
        node.setParent(anchorNode);
        node.setRenderable(this.mapModel.get(name)).animate(true).start();
        node.setLocalPosition(new Vector3(position[0], position[1], position[2]));
        Quaternion ro = Quaternion.axisAngle(new Vector3(rotation[0],rotation[1], rotation[2]), 1);
        node.setLocalRotation(ro);
        node.setLocalScale(new Vector3(scale[0], scale[1], scale[2]));
    }

    public void onAugmentedImageTrackingUpdate(AugmentedImage augmentedImage) {
        // If there are both images already detected, for better CPU usage we do not need scan for them
        if (!check) {
            return;
        }
        if (augmentedImage.getTrackingState() == TrackingState.TRACKING
                && augmentedImage.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {

            if (check) {
                this.config.setAugmentedImageDatabase(null);
                ArrayList<Model> listModel = this.getFileFromURL.getModels();
                check = false;

                Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(new Pose(new float[]{0,0,-5},new float[]{0,0,0,0}));
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());
//                anchorNode.setRenderable(this.model).animate(true).start();
//                anchorNode.setRenderable(this.mapModel.get(listModel.get(15).getName())).animate(true).start();

                System.out.println("ABC: " + listModel.size());
                for (int i = 0; i < listModel.size(); i++) {
                    this.createNode(anchorNode, listModel.get(i).getPosition(),
                            listModel.get(i).getRotation(),
                            listModel.get(i).getScale(),
                            listModel.get(i).getName());
                }
            }

        }
    }
}