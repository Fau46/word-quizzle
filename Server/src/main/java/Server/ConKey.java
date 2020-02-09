package Server;

public class ConKey {
    public String nickname;
    public String response;
    public String request;
    public Boolean logout; //Flag che mi indica se l'utente vuole effettuare il logout
    public Boolean challenge; //Flag che mi indica se l'utente sta già effettuando una sfida

    public ConKey(){
        challenge = false;
        logout = false;
    }
}
