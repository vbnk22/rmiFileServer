import repository.impl.FileRepositoryImpl;
import repository.impl.UserRepositoryImpl;
import service.AdminServiceImpl;
import service.AuthServiceImpl;
import service.FileServiceImpl;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;

public class ServerMain {
    public static void main(String[] args)
            throws Exception {

        LocateRegistry.createRegistry(5555);

        UserRepositoryImpl userRepository = new UserRepositoryImpl();
        FileRepositoryImpl fileRepository = new FileRepositoryImpl();
        AuthServiceImpl authService = new AuthServiceImpl(userRepository);

        Naming.rebind(
                "//localhost:5555/AuthService",
                authService);

        Naming.rebind(
                "//localhost:5555/FileService",
                new FileServiceImpl(fileRepository, authService, userRepository));

        Naming.rebind(
                "//localhost:5555/AdminService",
                new AdminServiceImpl(userRepository, authService));

        System.out.println("Server started");
    }
}
