import dto.UserDTO;
import enums.UserRole;
import repository.impl.FileRepositoryImpl;
import repository.impl.UserRepositoryImpl;
import service.AdminServiceImpl;
import service.AuthServiceImpl;
import service.FileServiceImpl;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class ServerMain {
    public static void main(String[] args) throws Exception {

        LocateRegistry.createRegistry(5555);

        UserRepositoryImpl userRepository = new UserRepositoryImpl();
        FileRepositoryImpl fileRepository = new FileRepositoryImpl();
        AuthServiceImpl authService = new AuthServiceImpl(userRepository);


        try {
            userRepository.save(new UserDTO("admin", "admin", UserRole.ADMIN));
            System.out.println("[Server] Konto admina utworzone (login: admin, hasło: admin)");
        } catch (IllegalArgumentException e) {
            System.out.println("[Server] Konto admina już istnieje (login: admin, hasło: admin)");
        }

        Naming.rebind("//localhost:5555/AuthService", authService);
        Naming.rebind("//localhost:5555/FileService",
                new FileServiceImpl(fileRepository, authService, userRepository)); // przekazuje interfejsy
        Naming.rebind("//localhost:5555/AdminService",
                new AdminServiceImpl(userRepository, authService));

        System.out.println("[Server] Serwer uruchomiony na porcie 5555");
    }
}
