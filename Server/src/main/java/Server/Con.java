package Server;

public class Con {
    public String nickname;
    public String response;
    public String request;
    public Boolean logout; //Flag che mi indica se l'utente vuole effettuare il logout
    public Integer lenght = 0;
    public Boolean challenge; //Flag che mi indica se l'utente sta già effettuando una sfida

    public Con(){
        challenge = false;
        logout = false;
    }
}
