package model.bean;

public class Figure {
    private Long id;
    private String name;
    private String shortInfo;
    private String image_url;
    private int born;
    private int died;
    private String hometown;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortInfo() {
        return shortInfo;
    }

    public void setShortInfo(String shortInfo) {
        this.shortInfo = shortInfo;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public int getBorn() {
        return born;
    }

    public void setBorn(int born) {
        this.born = born;
    }

    public int getDied() {
        return died;
    }

    public void setDied(int died) {
        this.died = died;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public Figure() {
    }

    public Figure(String name, String shortInfo, String image_url, int born, int died, String hometown) {
        this.name = name;
        this.shortInfo = shortInfo;
        this.image_url = image_url;
        this.born = born;
        this.died = died;
        this.hometown = hometown;
    }


}
