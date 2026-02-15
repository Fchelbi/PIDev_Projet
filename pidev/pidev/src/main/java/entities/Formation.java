package entities;

import java.util.Objects;

public class Formation {
    private int id;
    private String title;
    private String description;
    private String videoUrl;
    private String category;

    public Formation() {}

    public Formation(int id, String title, String description, String videoUrl, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.category = category;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return "Formation{id=" + id + ", title='" + title + "', category='" + category + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Formation f)) return false;
        return id == f.id && Objects.equals(title, f.title);
    }

    @Override
    public int hashCode() { return Objects.hash(id, title); }
}