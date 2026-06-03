import service.AdminServiceImpl;
import service.AuthServiceImpl;
import service.FileServiceImpl;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class ServerMain {
    public static void main(String[] args)
            throws Exception {

        LocateRegistry.createRegistry(1099);

        Naming.rebind(
                "AuthService",
                new AuthServiceImpl());

        Naming.rebind(
                "FileService",
                new FileServiceImpl());

        Naming.rebind(
                "AdminService",
                new AdminServiceImpl());

        System.out.println("Server started");
    }
}
