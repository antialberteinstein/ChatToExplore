package model.bean;

public class UserFigure {
    private Long id;
    private String userName;
    private Long figureId;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getFigureId() {
        return figureId;
    }

    public void setFigureId(Long figureId) {
        this.figureId = figureId;
    }

    public UserFigure() {
    }

    public UserFigure(String userName, Long figureId) {
        this.userName = userName;
        this.figureId = figureId;
    }

    public UserFigure(User user, Figure figure) {
        this.userName = user.getUsername();
        this.figureId = figure.getId();
    }

    
    
}
