package com.google.ar.sceneform.samples.augmentedimages;

public class Model {
    private String name;
    private float[] position;
    private float[] rotation;

    private float[] scale;

    private String url;


    public String getUrl() {
        return url;
    }

    public float[] getScale() {
        return scale;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Model(String name, float[] position, float[] rotation, float[] scale, String url) {
        this.name = name;
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
        this.url = url;
    }

    public Model() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float[] getPosition() {
        return position;
    }

    public void setPosition(float[] position) {
        this.position = position;
    }

    public float[] getRotation() {
        return rotation;
    }

    public void setRotation(float[] rotation) {
        this.rotation = rotation;
    }
}
