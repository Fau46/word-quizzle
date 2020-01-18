package User;

public class User {
    private String nickname;
    private String password;

    public User(String nick, String pwd){
        this.nickname = nick;
        this.password = pwd;
    }

    public String getNickname() {
        return nickname;
    }
}
