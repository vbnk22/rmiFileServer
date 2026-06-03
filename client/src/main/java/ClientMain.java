import remote.AdminSerivce;
import remote.AuthService;
import remote.FileService;
import repository.UserRepository;
import repository.impl.UserRepositoryImpl;
import session.SessionManager;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class ClientMain {

    static AuthService authService;

    static FileService fileService;

    static AdminSerivce adminService;

    static {
        try {
            fileService = (FileService) Naming.lookup("//localhost:5555/FileService");
            authService = (AuthService) Naming.lookup("//localhost:5555/AuthService");
            adminService = (AdminSerivce) Naming.lookup("//localhost:5555/AdminService");
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }


    public ClientMain() {}

    public static void showLoginMenu() {
        try {
            boolean logged = false;
            Scanner sc = new Scanner(System.in);

            do {
                System.out.println("Enter login: ");
                String login = sc.nextLine();
                System.out.println("Enter password: ");
                String password = sc.nextLine();
                String token = authService.authenticate(login, password);

                if (token != null || !token.isEmpty()) {
                    System.out.println("poprawne");
                    SessionManager.setSessionId(token);
                    //TODO ogarnac przypisywanie roli do uzytkownika w sesji
                    logged = true;
                } else {
                    System.out.println("Invalid credentials");
                }
            } while (!logged);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        adminService.registerUser("admin", "admin");
        showLoginMenu();
    }
}
