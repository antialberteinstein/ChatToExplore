package com.chat;

import java.io.DataOutputStream;
import java.io.IOException;

/* FigureDTO - DTO for sending/receiving Figure over network
   Note: removed sentBy / receivedFrom helpers as requested.
*/
public class FigureDTO {
    private Long id;
    private String name;
    private String shortInfo;
    private String imageUrl;
    private Integer born;
    private Integer died;
    private String hometown;

    public boolean isEmpty = true;

    public FigureDTO() {}

    public FigureDTO(Long id, String name, String shortInfo, String imageUrl,
                     Integer born, Integer died, String hometown) {
        this.id = id;
        this.name = name;
        this.shortInfo = shortInfo;
        this.imageUrl = imageUrl;
        this.born = born;
        this.died = died;
        this.hometown = hometown;
    }

    public void sendBy(DataOutputStream out) throws IOException {
        out.writeLong(id != null ? id : -1L);
        out.writeUTF(name != null ? name : "");
        out.writeUTF(shortInfo != null ? shortInfo : "");
        out.writeUTF(imageUrl != null ? imageUrl : "");
        out.writeInt(born != null ? born : -1);
        out.writeInt(died != null ? died : -1);
        out.writeUTF(hometown != null ? hometown : "");
        out.writeBoolean(isEmpty);
    }

    public static FigureDTO receiveFrom(java.io.DataInputStream in) throws IOException {
        Long id = in.readLong();
        if (id == -1L) id = null;
        String name = in.readUTF();
        if (name.isEmpty()) name = null;
        String shortInfo = in.readUTF();
        if (shortInfo.isEmpty()) shortInfo = null;
        String imageUrl = in.readUTF();
        if (imageUrl.isEmpty()) imageUrl = null;
        int bornVal = in.readInt();
        Integer born = (bornVal == -1) ? null : bornVal;
        int diedVal = in.readInt();
        Integer died = (diedVal == -1) ? null : diedVal;
        String hometown = in.readUTF();
        if (hometown.isEmpty()) hometown = null;
        boolean isEmpty = in.readBoolean();

        FigureDTO dto = new FigureDTO(id, name, shortInfo, imageUrl, born, died, hometown);
        dto.isEmpty = isEmpty;
        return dto;
    }

    // --- Getters / Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortInfo() { return shortInfo; }
    public void setShortInfo(String shortInfo) { this.shortInfo = shortInfo; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getBorn() { return born; }
    public void setBorn(Integer born) { this.born = born; }

    public Integer getDied() { return died; }
    public void setDied(Integer died) { this.died = died; }

    public String getHometown() { return hometown; }
    public void setHometown(String hometown) { this.hometown = hometown; }
}