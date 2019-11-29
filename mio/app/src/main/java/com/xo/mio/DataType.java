package com.xo.mio;

public class DataType {

    public final static String TYPE_ID = "id";
    public final static String TYPE_CATEGORY = "category";
    public final static String TYPE_DATA = "data";
    public final static String TYPE_NOTE = "note";
    public final static String TYPE_DATE = "date";


    private int id;
    private String category;
    private int data; //jin e
    private String note;
    private String date;

    public DataType() {
    }

    public DataType(int id, String category, int data, String note, String date) {
        this.id = id;
        this.category = category;
        this.data = data;
        this.note = note;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Data_Type{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", data=" + data +
                ", note='" + note + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
