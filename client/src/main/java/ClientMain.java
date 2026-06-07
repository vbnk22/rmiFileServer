import dto.FileInfoDTO;
import enums.UserRole;
import remote.AdminService;
import remote.AuthService;
import remote.FileService;
import session.SessionManager;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

public class ClientMain {

    private static AuthService authService;
    private static FileService fileService;
    private static AdminService adminService;

    private static final Scanner sc = new Scanner(System.in);

    static {
        try {
            fileService  = (FileService)  Naming.lookup("//localhost:5555/FileService");
            authService  = (AuthService)  Naming.lookup("//localhost:5555/AuthService");
            adminService = (AdminService) Naming.lookup("//localhost:5555/AdminService");
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            System.err.println("[BŁĄD] Nie można połączyć z serwerem: " + e.getMessage());
            System.err.println("Upewnij się, że serwer jest uruchomiony na porcie 5555.");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        showLoginMenu();
    }

    // -----------------------------------------------------------------------
    // Logowanie
    // -----------------------------------------------------------------------

    private static void showLoginMenu() {
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║     RMI File Server          ║");
        System.out.println("╚══════════════════════════════╝");

        while (true) {
            System.out.println("\n=== Logowanie ===");
            System.out.print("Login: ");
            String login = sc.nextLine().trim();
            System.out.print("Hasło: ");
            String password = sc.nextLine().trim();

            if (login.isEmpty() || password.isEmpty()) {
                System.out.println("[!] Login i hasło nie mogą być puste.");
                continue;
            }

            try {
                String token = authService.authenticate(login, password);
                if (token != null && !token.isEmpty()) {
                    UserRole role = authService.getRole(token);
                    SessionManager.setSessionId(token);
                    SessionManager.setRole(role);
                    System.out.println("[✓] Zalogowano jako " + login + " [" + role + "]");
                    showMainMenu();
                } else {
                    System.out.println("[!] Błędny login lub hasło. Spróbuj ponownie.");
                }
            } catch (RemoteException e) {
                System.err.println("[BŁĄD] Problem z połączeniem: " + e.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Menu główne
    // -----------------------------------------------------------------------

    private static void showMainMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== Menu ===");
            System.out.println("1. Lista plików");
            System.out.println("2. Szukaj pliku po nazwie");
            System.out.println("3. Pobierz plik");
            System.out.println("4. Wyślij plik");
            if (SessionManager.getRole() == UserRole.ADMIN) {
                System.out.println("5. Usuń plik");
                System.out.println("6. Dodaj użytkownika");
                System.out.println("7. Usuń użytkownika");
            }
            System.out.println("0. Wyloguj");
            System.out.print("Wybór: ");
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> listFiles();
                    case "2" -> searchFile();
                    case "3" -> downloadFile();
                    case "4" -> uploadFile();
                    case "5" -> { requireAdmin(); deleteFile(); }
                    case "6" -> { requireAdmin(); addUser(); }
                    case "7" -> { requireAdmin(); deleteUser(); }
                    case "0" -> {
                        SessionManager.clear();
                        System.out.println("[✓] Wylogowano.");
                        running = false;
                    }
                    default -> System.out.println("[!] Nieznana opcja: " + choice);
                }
            } catch (RemoteException e) {
                System.err.println("[BŁĄD SERWERA] " + e.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Operacje na plikach
    // -----------------------------------------------------------------------

    private static void listFiles() throws RemoteException {
        List<FileInfoDTO> files = fileService.listFiles();
        if (files.isEmpty()) {
            System.out.println("Brak plików na serwerze.");
            return;
        }
        System.out.println("\nPliki na serwerze (" + files.size() + "):");
        System.out.printf("  %-40s %12s%n", "Nazwa", "Rozmiar");
        System.out.println("  " + "-".repeat(54));
        for (FileInfoDTO f : files) {
            System.out.printf("  %-40s %10d B%n", f.getOriginalName(), f.getSize());
        }
    }

    private static void searchFile() throws RemoteException {
        System.out.print("Nazwa pliku do wyszukania: ");
        String filename = sc.nextLine().trim();
        if (filename.isEmpty()) {
            System.out.println("[!] Nazwa pliku nie może być pusta.");
            return;
        }
        FileInfoDTO result = fileService.findFileByName(filename);
        if (result == null) {
            System.out.println("[!] Plik \"" + filename + "\" nie istnieje na serwerze.");
        } else {
            System.out.println("\n[✓] Znaleziono plik:");
            System.out.printf("  Nazwa:   %s%n", result.getOriginalName());
            System.out.printf("  Rozmiar: %d B (%.2f MB)%n",
                    result.getSize(), result.getSize() / 1024.0 / 1024.0);
            System.out.printf("  ID:      %s%n", result.getId());
        }
    }

    private static void uploadFile() throws RemoteException {
        System.out.print("Ścieżka do pliku lokalnego: ");
        String pathStr = sc.nextLine().trim();
        if (pathStr.isEmpty()) {
            System.out.println("[!] Ścieżka nie może być pusta.");
            return;
        }
        Path localPath = Paths.get(pathStr);
        if (!Files.exists(localPath)) {
            System.out.println("[!] Plik nie istnieje: " + pathStr);
            return;
        }
        if (!Files.isRegularFile(localPath)) {
            System.out.println("[!] Podana ścieżka nie wskazuje na plik.");
            return;
        }
        try {
            long fileSize = Files.size(localPath);
            if (fileSize > 50L * 1024 * 1024) {
                System.out.printf("[!] Plik przekracza limit 50 MB (rozmiar: %.2f MB).%n",
                        fileSize / 1024.0 / 1024.0);
                return;
            }
            byte[] data = Files.readAllBytes(localPath);
            String filename = localPath.getFileName().toString();
            fileService.uploadFile(filename, data);
            System.out.println("[✓] Wysłano: " + filename + " (" + data.length + " B)");
        } catch (RemoteException e) {
            System.err.println("[BŁĄD] Nie udało się wysłać pliku: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[BŁĄD] Nie można odczytać pliku lokalnego: " + e.getMessage());
        }
    }

    private static void downloadFile() throws RemoteException {
        listFiles();
        System.out.print("Nazwa pliku do pobrania: ");
        String filename = sc.nextLine().trim();
        if (filename.isEmpty()) {
            System.out.println("[!] Nazwa pliku nie może być pusta.");
            return;
        }
        try {
            byte[] data = fileService.downloadFile(filename);
            Path dest = Paths.get(filename);
            Files.write(dest, data);
            System.out.println("[✓] Plik zapisany: " + dest.toAbsolutePath());
        } catch (RemoteException e) {
            System.err.println("[BŁĄD] " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[BŁĄD] Nie można zapisać pliku lokalnie: " + e.getMessage());
        }
    }

    private static void deleteFile() throws RemoteException {
        listFiles();
        System.out.print("Nazwa pliku do usunięcia: ");
        String filename = sc.nextLine().trim();
        if (filename.isEmpty()) {
            System.out.println("[!] Nazwa pliku nie może być pusta.");
            return;
        }
        System.out.print("Potwierdzenie usunięcia \"" + filename + "\" (tak/nie): ");
        String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("tak")) {
            System.out.println("Anulowano.");
            return;
        }
        try {
            boolean ok = fileService.deleteFile(SessionManager.getSessionId(), filename);
            System.out.println(ok ? "[✓] Plik usunięty." : "[!] Nie udało się usunąć.");
        } catch (RemoteException e) {
            System.err.println("[BŁĄD] " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Operacje admina na użytkownikach
    // -----------------------------------------------------------------------

    private static void addUser() throws RemoteException {
        System.out.print("Nowy login: ");
        String login = sc.nextLine().trim();
        if (login.isEmpty()) {
            System.out.println("[!] Login nie może być pusty.");
            return;
        }
        System.out.print("Hasło: ");
        String password = sc.nextLine().trim();
        if (password.isEmpty()) {
            System.out.println("[!] Hasło nie może być puste.");
            return;
        }
        try {
            boolean ok = adminService.registerUser(login, password);
            System.out.println(ok ? "[✓] Użytkownik dodany." : "[!] Nie udało się dodać.");
        } catch (RemoteException e) {
            System.err.println("[BŁĄD] " + e.getMessage());
        }
    }

    private static void deleteUser() throws RemoteException {
        System.out.print("Login użytkownika do usunięcia: ");
        String login = sc.nextLine().trim();
        if (login.isEmpty()) {
            System.out.println("[!] Login nie może być pusty.");
            return;
        }
        if (login.equals("admin")) {
            System.out.println("[!] Nie można usunąć konta administratora.");
            return;
        }
        try {
            adminService.deleteUser(SessionManager.getSessionId(), login);
            System.out.println("[✓] Użytkownik usunięty.");
        } catch (RemoteException e) {
            System.err.println("[BŁĄD] " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Pomocnicze
    // -----------------------------------------------------------------------

    private static void requireAdmin() {
        if (SessionManager.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Ta operacja wymaga roli ADMIN.");
        }
    }
}
