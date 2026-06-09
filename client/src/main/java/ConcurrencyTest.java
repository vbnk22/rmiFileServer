import dto.FileInfoDTO;
import remote.AuthService;
import remote.FileService;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ręczny test wielowątkowości dla RMI File Server.
 * Uruchom serwer, a następnie: java -cp target/classes;../common/target/classes ConcurrencyTest
 *
 * Testy:
 *   1 - Równoległy download tego samego pliku (10 wątków)
 *   2 - TOCTOU: download vs delete w tym samym czasie
 *   3 - Równoległy upload różnych plików (per-file lock)
 *   4 - Równoległy upload TEGO SAMEGO pliku (write lock kolejkuje)
 */
public class ConcurrencyTest {

    private static FileService fileService;
    private static AuthService authService;

    private static final String ANSI_GREEN  = "[32m";
    private static final String ANSI_RED    = "[31m";
    private static final String ANSI_YELLOW = "[33m";
    private static final String ANSI_RESET  = "[0m";

    public static void main(String[] args) throws Exception {
        System.out.println("=== RMI File Server — Test Współbieżności ===\n");

        try {
            fileService = (FileService) Naming.lookup("//localhost:5555/FileService");
            authService = (AuthService) Naming.lookup("//localhost:5555/AuthService");
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            System.err.println("[BŁĄD] Nie można połączyć z serwerem: " + e.getMessage());
            System.exit(1);
        }

        // Wgraj plik testowy przed testami
        byte[] sampleData = "Hello from concurrency test! ".repeat(1000).getBytes();
        try {
            fileService.uploadFile("__test_file.txt", sampleData);
            System.out.println("[Setup] Plik testowy __test_file.txt wgrany (" + sampleData.length + " B)\n");
        } catch (RemoteException e) {
            System.out.println("[Setup] Plik testowy już istnieje lub błąd: " + e.getMessage() + "\n");
        }

        test1_ParallelDownloads();
        test2_ParallelUploads_DifferentFiles();
        test3_ParallelUploads_SameFile();
        test4_TOCTOU_DownloadVsDelete();

        System.out.println("\n=== Wszystkie testy zakończone ===");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 1: 10 wątków pobiera ten sam plik jednocześnie
    // Oczekiwany wynik: wszystkie 10 kończy bez błędu, dane identyczne
    // ─────────────────────────────────────────────────────────────────────────
    static void test1_ParallelDownloads() throws InterruptedException {
        System.out.println("── Test 1: Równoległy download (10 wątków, ten sam plik) ──");

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);
        List<byte[]> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger(0);
        List<Long> times = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    startGun.await(); // wszystkie wątki czekają na strzał
                    long t0 = System.currentTimeMillis();
                    byte[] data = fileService.downloadFile("__test_file.txt");
                    times.add(System.currentTimeMillis() - t0);
                    results.add(data);
                    System.out.println("  Wątek " + id + " → pobrał " + data.length + " B");
                } catch (Exception e) {
                    errors.incrementAndGet();
                    System.out.println("  Wątek " + id + " → " + ANSI_RED + "BŁĄD: " + e.getMessage() + ANSI_RESET);
                }
            });
        }

        startGun.countDown(); // startujemy wszystkie naraz
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        boolean allSameSize = results.stream().mapToInt(b -> b.length).distinct().count() == 1;
        long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.println("  Pobrano: " + results.size() + "/" + threadCount);
        System.out.println("  Błędy: " + errors.get());
        System.out.println("  Dane identyczne: " + allSameSize);
        System.out.println("  Czas: min=" + minTime + "ms, max=" + maxTime + "ms");
        System.out.println(errors.get() == 0 && results.size() == threadCount
                ? ANSI_GREEN + "  [PASS]" + ANSI_RESET
                : ANSI_RED   + "  [FAIL]" + ANSI_RESET);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 2: 5 wątków uploaduje RÓŻNE pliki jednocześnie (per-file lock)
    // Oczekiwany wynik: wszystkie 5 kończy, lista ma o 5 więcej plików
    // Kluczowe: czas całkowity ~ czas 1 uploadu (nie 5x), bo różne locki
    // ─────────────────────────────────────────────────────────────────────────
    static void test2_ParallelUploads_DifferentFiles() throws InterruptedException, RemoteException {
        System.out.println("── Test 2: Równoległy upload różnych plików (per-file lock) ──");

        int threadCount = 5;
        byte[] data = ("dane testowe ").repeat(500).getBytes();
        List<FileInfoDTO> before = fileService.listFiles();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger errors  = new AtomicInteger(0);

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            final String name = "__parallel_" + i + ".txt";
            pool.submit(() -> {
                try {
                    startGun.await();
                    fileService.uploadFile(name, data);
                    success.incrementAndGet();
                    System.out.println("  Wgrany: " + name);
                } catch (Exception e) {
                    errors.incrementAndGet();
                    System.out.println("  " + ANSI_RED + "BŁĄD " + name + ": " + e.getMessage() + ANSI_RESET);
                }
            });
        }

        startGun.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - t0;

        List<FileInfoDTO> after = fileService.listFiles();
        int added = after.size() - before.size();

        System.out.println("  Wgrane: " + success.get() + "/" + threadCount);
        System.out.println("  Błędy: " + errors.get());
        System.out.println("  Nowych plików na serwerze: " + added);
        System.out.println("  Łączny czas: " + elapsed + "ms " +
                ANSI_YELLOW + "(gdyby globalny lock: ~" + threadCount + "x dłużej)" + ANSI_RESET);
        System.out.println(success.get() == threadCount && added == threadCount
                ? ANSI_GREEN + "  [PASS]" + ANSI_RESET
                : ANSI_RED   + "  [FAIL]" + ANSI_RESET);
        System.out.println();

        // Sprzątanie
        for (int i = 0; i < threadCount; i++) {
            String token = authService.authenticate("admin", "admin");
            fileService.deleteFile(token, "__parallel_" + i + ".txt");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 3: 5 wątków uploaduje TEGO SAMEGO pliku jednocześnie (write lock)
    // Oczekiwany wynik: żaden upload się nie posypie, plik istnieje na końcu
    // Write lock gwarantuje że bajty nie są mieszane
    // ─────────────────────────────────────────────────────────────────────────
    static void test3_ParallelUploads_SameFile() throws InterruptedException, RemoteException {
        System.out.println("── Test 3: Równoległy upload tego samego pliku (write lock kolejkuje) ──");

        int threadCount = 5;
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger errors  = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            final byte[] data = ("wersja_" + id + " ").repeat(200).getBytes();
            pool.submit(() -> {
                try {
                    startGun.await();
                    fileService.uploadFile("__same_file.txt", data);
                    success.incrementAndGet();
                    System.out.println("  Wątek " + id + " → upload zakończony");
                } catch (Exception e) {
                    errors.incrementAndGet();
                    System.out.println("  Wątek " + id + " → " + ANSI_RED + "BŁĄD: " + e.getMessage() + ANSI_RESET);
                }
            });
        }

        startGun.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        // Sprawdź że plik istnieje i jest pobieralny
        boolean canDownload = false;
        try {
            byte[] result = fileService.downloadFile("__same_file.txt");
            canDownload = result != null && result.length > 0;
        } catch (Exception ignored) {}

        System.out.println("  Zakończone uploady: " + success.get() + "/" + threadCount);
        System.out.println("  Błędy: " + errors.get());
        System.out.println("  Plik pobieralny po wszystkich uploadach: " + canDownload);
        System.out.println(errors.get() == 0 && canDownload
                ? ANSI_GREEN + "  [PASS]" + ANSI_RESET
                : ANSI_RED   + "  [FAIL]" + ANSI_RESET);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 4: TOCTOU — wątek A pobiera, wątek B usuwa ten sam plik jednocześnie
    // Oczekiwany wynik: żaden wątek nie dostaje NullPointerException ani
    // częściowych danych — albo A pobierze w całości, albo dostanie
    // czysty błąd "File not found"
    // ─────────────────────────────────────────────────────────────────────────
    static void test4_TOCTOU_DownloadVsDelete() throws InterruptedException, RemoteException {
        System.out.println("── Test 4: TOCTOU — download vs delete tego samego pliku ──");

        // Wgraj plik do usunięcia
        byte[] data = "toctou test content ".repeat(500).getBytes();
        fileService.uploadFile("__toctou.txt", data);
        String adminToken = authService.authenticate("admin", "admin");

        AtomicInteger downloadOk     = new AtomicInteger(0);
        AtomicInteger downloadClean  = new AtomicInteger(0); // "file not found" — też OK
        AtomicInteger downloadCorrupt= new AtomicInteger(0); // częściowe dane — ŹLE
        AtomicInteger deleteOk       = new AtomicInteger(0);

        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch done     = new CountDownLatch(2);

        // Wątek A — pobiera plik
        new Thread(() -> {
            try {
                startGun.await();
                byte[] result = fileService.downloadFile("__toctou.txt");
                if (result != null && result.length == data.length) {
                    downloadOk.incrementAndGet();
                    System.out.println("  Wątek-Download → pobrał kompletny plik (" + result.length + " B)");
                } else {
                    downloadCorrupt.incrementAndGet();
                    System.out.println("  Wątek-Download → " + ANSI_RED + "DANE USZKODZONE!" + ANSI_RESET);
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("not found") || msg.contains("File not found")) {
                    downloadClean.incrementAndGet();
                    System.out.println("  Wątek-Download → czysty błąd 'File not found' (plik już usunięty)");
                } else {
                    downloadCorrupt.incrementAndGet();
                    System.out.println("  Wątek-Download → " + ANSI_RED + "NIEOCZEKIWANY BŁĄD: " + msg + ANSI_RESET);
                }
            } finally {
                done.countDown();
            }
        }, "Download-Thread").start();

        // Wątek B — usuwa plik
        new Thread(() -> {
            try {
                startGun.await();
                fileService.deleteFile(adminToken, "__toctou.txt");
                deleteOk.incrementAndGet();
                System.out.println("  Wątek-Delete  → plik usunięty");
            } catch (Exception e) {
                System.out.println("  Wątek-Delete  → " + e.getMessage());
            } finally {
                done.countDown();
            }
        }, "Delete-Thread").start();

        startGun.countDown(); // strzał
        done.await(15, TimeUnit.SECONDS);

        boolean passed = downloadCorrupt.get() == 0
                && (downloadOk.get() + downloadClean.get()) == 1
                && deleteOk.get() == 1;

        System.out.println("  Wynik: download_ok=" + downloadOk.get()
                + ", download_clean_error=" + downloadClean.get()
                + ", download_corrupt=" + downloadCorrupt.get()
                + ", delete_ok=" + deleteOk.get());
        System.out.println(passed
                ? ANSI_GREEN + "  [PASS] Brak uszkodzonych danych — TOCTOU zabezpieczone" + ANSI_RESET
                : ANSI_RED   + "  [FAIL] Wykryto uszkodzone dane lub niespójny stan!" + ANSI_RESET);
        System.out.println();
    }
}