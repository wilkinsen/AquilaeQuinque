import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Jembatan antara front end web (folder /web) dan kode Java yang sudah ada.
 * Semua logika bisnis tetap berada di HospitalService / Models.java;
 * kelas ini hanya menerjemahkan request HTTP (JSON) ke pemanggilan service.
 * Hanya memakai JDK (com.sun.net.httpserver), tanpa library tambahan.
 *
 * Jalankan: java -cp out WebMain [port]   lalu buka http://localhost:8080
 */
public class WebMain {
    private final Database db;
    private final HospitalService service;
    private final Path webRoot;
    private final Object lock = new Object();

    private static class NotFound extends RuntimeException {
        NotFound(String m) { super(m); }
    }

    public WebMain(Database db, Path webRoot) {
        this.db = db;
        this.service = new HospitalService(db);
        this.webRoot = webRoot;
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Database database = new Database();
        database.seed();
        Path web = findWebRoot();
        WebMain app = new WebMain(database, web);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", app::route);
        server.start();
        System.out.println("============================================================");
        System.out.println("  Sistem Manajemen Rumah Sakit - B3 Rawat Inap (Web)");
        System.out.println("  Buka di browser : http://localhost:" + port);
        System.out.println("  Folder tampilan : " + web.toAbsolutePath());
        System.out.println("  Tekan Ctrl+C untuk berhenti.");
        System.out.println("============================================================");
    }

    private static Path findWebRoot() {
        List<String> candidates = new ArrayList<>();
        String prop = System.getProperty("web.dir");
        if (prop != null) candidates.add(prop);
        candidates.addAll(List.of("web", "../web", "B3_Manajemen_Rumah_Sakit/web"));
        for (String c : candidates) {
            Path p = Paths.get(c);
            if (Files.exists(p.resolve("index.html"))) return p;
        }
        throw new IllegalStateException("Folder 'web' (berisi index.html) tidak ditemukan. "
                + "Jalankan dari folder project atau pakai -Dweb.dir=<path>.");
    }

    // ------------------------------------------------------------------ routing

    private void route(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                handleApi(ex, path);
            } else {
                serveStatic(ex, path);
            }
        } catch (Throwable t) {
            try { send(ex, 500, "text/plain; charset=utf-8", "Server error: " + t.getMessage()); }
            catch (IOException ignored) { }
        } finally {
            ex.close();
        }
    }

    private void handleApi(HttpExchange ex, String path) throws IOException {
        int status = 200;
        Object result;
        try {
            Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());
            Map<String, String> body = new HashMap<>();
            if (!ex.getRequestMethod().equals("GET")) {
                body = parseJson(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            }
            synchronized (lock) {
                result = api(ex.getRequestMethod(), path, query, body);
            }
        } catch (NotFound e) {
            status = 404; result = mapOf("error", e.getMessage());
        } catch (PatientNotFoundException e) {
            status = 404; result = mapOf("error", e.getMessage());
        } catch (HospitalException | IllegalArgumentException | IllegalStateException e) {
            status = 400; result = mapOf("error", e.getMessage());
        } catch (Exception e) {
            status = 500; result = mapOf("error", "Kesalahan server: " + e.getMessage());
        }
        send(ex, status, "application/json; charset=utf-8", toJson(result));
    }

    private Object api(String method, String path, Map<String, String> q, Map<String, String> b) throws Exception {
        switch (method + " " + path) {
            case "GET /api/overview":
                return overview();
            case "GET /api/patient":
                return patientDetail(anyPatient(q.get("rm")));
            case "GET /api/bills":
                return billsJson();

            // 2. Registrasi kamar
            case "POST /api/rooms": {
                Room r = service.registerRoom(str(b, "number", "Nomor kamar"),
                        str(b, "roomClass", "Kelas kamar"), positive(num(b, "rate", "Tarif per malam"), "Tarif per malam"));
                return mapOf("message", "Kamar berhasil didaftarkan: " + r.getDisplayInfo(), "room", roomJson(r));
            }
            // 3. Admisi pasien
            case "POST /api/admit": {
                String rm = str(b, "rm", "No. RM");
                if (db.findPatient(rm) != null) throw new IllegalArgumentException("No. RM " + rm + " sudah terdaftar.");
                int age = (int) num(b, "age", "Usia");
                if (age < 0 || age > 150) throw new IllegalArgumentException("Usia tidak valid.");
                Patient p = service.admitPatient(rm, str(b, "name", "Nama pasien"), age,
                        str(b, "roomClass", "Kelas kamar"), str(b, "doctorId", "Dokter"));
                return mapOf("message", "Admisi berhasil. " + p.getName() + " ditempatkan di kamar " + p.getRoom().getNumber() + ".",
                        "patient", patientBrief(p));
            }
            // 4. Assign perawat & shift
            case "POST /api/nurse": {
                Patient p = activePatient(str(b, "rm", "Pasien"));
                service.assignNurse(p.getMedicalRecordNumber(), str(b, "nurseId", "Perawat"), str(b, "shift", "Shift"));
                return mapOf("message", "Perawat berhasil di-assign ke kamar pasien.");
            }
            // 5. Kondisi harian
            case "POST /api/daily": {
                Patient p = activePatient(str(b, "rm", "Pasien"));
                service.addDailyRecord(p.getMedicalRecordNumber(), new DailyRecord(java.time.LocalDate.now(),
                        num(b, "systolic", "Sistolik"), num(b, "diastolic", "Diastolik"),
                        num(b, "temperature", "Suhu"), num(b, "heartRate", "Detak jantung"), opt(b, "notes")));
                return mapOf("message", "Kondisi harian tersimpan.");
            }
            // 6. Tindakan medis
            case "POST /api/action": {
                Patient p = activePatient(str(b, "rm", "Pasien"));
                service.addMedicalAction(p.getMedicalRecordNumber(), str(b, "name", "Nama tindakan"),
                        positive(num(b, "cost", "Biaya tindakan"), "Biaya tindakan"));
                return mapOf("message", "Tindakan medis tercatat.");
            }
            // 7. Obat / infus
            case "POST /api/medication": {
                Patient p = activePatient(str(b, "rm", "Pasien"));
                service.addMedication(p.getMedicalRecordNumber(), str(b, "name", "Nama obat/infus"),
                        positive(num(b, "cost", "Biaya harian"), "Biaya harian"));
                return mapOf("message", "Obat/infus harian tercatat.");
            }
            // 9. Laboratorium
            case "POST /api/lab/request": {
                Patient p = activePatient(str(b, "rm", "Pasien"));
                service.addLabRequest(p.getMedicalRecordNumber(), str(b, "test", "Nama pemeriksaan"));
                return mapOf("message", "Permintaan laboratorium dibuat.");
            }
            case "POST /api/lab/result": {
                Patient p = activePatient(str(b, "rm", "Pasien"));
                service.recordLatestLabResult(p.getMedicalRecordNumber(), str(b, "result", "Hasil pemeriksaan"));
                return mapOf("message", "Hasil laboratorium dicatat.");
            }
            // 10. Transfer antar kamar
            case "POST /api/transfer": {
                Patient p = activePatient(str(b, "rm", "Pasien"));
                service.transferRoom(p.getMedicalRecordNumber(), str(b, "targetClass", "Kelas tujuan"));
                return mapOf("message", "Transfer berhasil. Pasien kini di kamar " + p.getRoom().getNumber()
                        + " (" + p.getRoom().getRoomClass() + ").", "patient", patientBrief(p));
            }
            // 11. Discharge & tagihan final
            case "POST /api/discharge": {
                Patient p = activePatient(str(b, "rm", "Pasien"));
                Bill bill = service.discharge(p.getMedicalRecordNumber());
                return mapOf("message", "Pasien dipulangkan dan tagihan final dibuat.",
                        "bill", billJson(bill, service.getBills().size() - 1));
            }
            // 12. Klaim asuransi
            case "POST /api/insurance": {
                int idx = (int) num(b, "billIndex", "Tagihan");
                if (idx < 0 || idx >= service.getBills().size()) throw new IllegalArgumentException("Tagihan tidak ditemukan.");
                double percent = num(b, "percent", "Persentase tanggungan");
                if (percent < 0 || percent > 100) throw new IllegalArgumentException("Persentase harus 0 sampai 100.");
                Bill bill = service.getBills().get(idx);
                InsuranceClaim c = service.applyInsurance(bill, str(b, "name", "Nama asuransi"), percent);
                return mapOf("message", String.format(Locale.US, "Asuransi menanggung Rp %,.0f; sisa bayar Rp %,.0f",
                        c.getCoveredAmount(), c.getRemainingAmount()), "bill", billJson(bill, idx));
            }
            default:
                throw new NotFound("Endpoint tidak ditemukan: " + path);
        }
    }

    // ------------------------------------------------------------------ guards

    private Patient anyPatient(String rm) throws PatientNotFoundException {
        Patient p = rm == null ? null : db.findPatient(rm);
        if (p == null) throw new PatientNotFoundException("Pasien dengan RM " + rm + " tidak ditemukan.");
        return p;
    }

    /** Hanya pasien yang masih dirawat yang boleh diubah (mencegah data kamar rusak setelah discharge). */
    private Patient activePatient(String rm) throws PatientNotFoundException {
        for (Patient p : service.getOccupants()) {
            if (p.getMedicalRecordNumber().equalsIgnoreCase(rm)) return p;
        }
        anyPatient(rm);
        throw new IllegalStateException("Pasien dengan RM " + rm + " sudah pulang dan tidak bisa diubah lagi.");
    }

    // ------------------------------------------------------------------ JSON builders

    private Map<String, Object> overview() {
        long occupied = service.getRooms().stream().filter(Room::isOccupied).count();
        List<Object> rooms = new ArrayList<>();
        service.getRooms().forEach(r -> rooms.add(roomJson(r)));
        List<Object> doctors = new ArrayList<>();
        service.getDoctors().forEach(d -> doctors.add(mapOf("id", d.getId(), "name", d.getName(), "specialty", d.getSpecialty())));
        List<Object> nurses = new ArrayList<>();
        service.getNurses().forEach(n -> nurses.add(mapOf("id", n.getId(), "name", n.getName())));
        List<Object> patients = new ArrayList<>();
        service.getOccupants().forEach(p -> patients.add(patientBrief(p)));
        List<Object> history = new ArrayList<>();
        service.getPatientHistory().forEach(p -> history.add(patientBrief(p)));
        return mapOf(
                "stats", mapOf("totalRooms", service.getRooms().size(), "occupied", occupied,
                        "empty", service.getRooms().size() - occupied, "activePatients", service.getOccupants().size(),
                        "history", service.getPatientHistory().size(), "bills", service.getBills().size()),
                "rooms", rooms, "doctors", doctors, "nurses", nurses,
                "patients", patients, "history", history,
                "notifications", new ArrayList<>(service.getNotificationService().getLogs()),
                "classes", List.of("VIP", "Kelas 1", "Kelas 2", "Kelas 3", "ICU"));
    }

    private Map<String, Object> roomJson(Room r) {
        Patient p = r.getPatient();
        return mapOf("number", r.getNumber(), "roomClass", r.getRoomClass(), "rate", r.getNightlyRate(),
                "occupied", r.isOccupied(), "info", r.getDisplayInfo(),
                "patientName", p == null ? null : p.getName(),
                "patientRm", p == null ? null : p.getMedicalRecordNumber(),
                "doctor", r.getAssignedDoctor() == null ? null : r.getAssignedDoctor().getName());
    }

    private Map<String, Object> patientBrief(Patient p) {
        Room r = p.getRoom();
        return mapOf("rm", p.getMedicalRecordNumber(), "name", p.getName(), "age", p.getAge(),
                "room", r == null ? null : r.getNumber(), "roomClass", r == null ? null : r.getRoomClass(),
                "doctor", p.getDoctor() == null ? null : p.getDoctor().getName(),
                "admissionDate", String.valueOf(p.getAdmissionDate()),
                "dischargeDate", p.getDischargeDate() == null ? null : p.getDischargeDate().toString(),
                "nights", p.getNights());
    }

    private Map<String, Object> patientDetail(Patient p) throws PatientNotFoundException {
        Map<String, Object> m = patientBrief(p);
        boolean active = service.getOccupants().contains(p);
        List<Object> records = new ArrayList<>();
        // Iterator Pattern (DailyRecordIterator) dipakai untuk menelusuri catatan harian.
        Iterator<DailyRecord> it = p.dailyRecordIterator();
        while (it.hasNext()) records.add(it.next().getSummary());
        List<Object> actions = new ArrayList<>();
        p.getMedicalActions().forEach(a -> actions.add(mapOf("name", a.getName(), "cost", a.getCost())));
        List<Object> meds = new ArrayList<>();
        p.getMedications().forEach(a -> meds.add(mapOf("name", a.getName(), "dailyCost", a.getDailyCost())));
        List<Object> labs = new ArrayList<>();
        p.getLaboratoryRequests().forEach(l -> labs.add(mapOf("test", l.getTestName(), "status", l.getStatus(),
                "result", l.getResult(), "requestedAt", l.getRequestedAt().toString())));
        List<Object> nurses = new ArrayList<>();
        p.getNurseAssignments().forEach(n -> nurses.add(mapOf("id", n.getNurse().getId(), "name", n.getNurse().getName(),
                "room", n.getRoom() == null ? null : n.getRoom().getNumber(), "shift", n.getShift(),
                "date", n.getDate().toString())));
        m.put("active", active);
        m.put("records", records);
        m.put("actions", actions);
        m.put("medications", meds);
        m.put("labs", labs);
        m.put("nurses", nurses);
        m.put("dailyCost", active ? service.calculateDailyCost(p.getMedicalRecordNumber()) : null);
        return m;
    }

    private Map<String, Object> billJson(Bill bill, int index) {
        List<Object> items = new ArrayList<>();
        bill.getItems().forEach(i -> items.add(mapOf("category", i.getCategory(),
                "description", i.getDescription(), "amount", i.getAmount())));
        Patient p = bill.getPatient();
        return mapOf("index", index, "rm", p.getMedicalRecordNumber(), "patient", p.getName(),
                "date", p.getDischargeDate() == null ? null : p.getDischargeDate().toString(),
                "items", items, "total", bill.getTotal(),
                "insuranceCovered", bill.getInsuranceCovered(), "remaining", bill.getRemaining(),
                "text", bill.formatted());
    }

    private Map<String, Object> billsJson() {
        List<Object> bills = new ArrayList<>();
        for (int i = 0; i < service.getBills().size(); i++) bills.add(billJson(service.getBills().get(i), i));
        List<Object> claims = new ArrayList<>();
        db.getClaims().forEach(c -> claims.add(mapOf("name", c.getInsuranceName(), "percent", c.getCoveragePercent(),
                "covered", c.getCoveredAmount(), "remaining", c.getRemainingAmount())));
        return mapOf("bills", bills, "claims", claims);
    }

    // ------------------------------------------------------------------ input helpers

    private static String str(Map<String, String> b, String key, String label) {
        String v = b.get(key);
        if (v == null || v.trim().isEmpty() || v.equals("null")) throw new IllegalArgumentException(label + " wajib diisi.");
        return v.trim();
    }

    private static String opt(Map<String, String> b, String key) {
        String v = b.get(key);
        return v == null || v.equals("null") ? "" : v.trim();
    }

    private static double num(Map<String, String> b, String key, String label) {
        String v = str(b, key, label);
        try {
            double d = Double.parseDouble(v);
            if (Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException();
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " harus berupa angka.");
        }
    }

    private static double positive(double v, String label) {
        if (v < 0) throw new IllegalArgumentException(label + " tidak boleh negatif.");
        return v;
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0) m.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    // ------------------------------------------------------------------ tiny JSON (flat objects in, any out)

    private static Map<String, String> parseJson(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null) return m;
        int i = s.indexOf('{');
        if (i < 0) return m;
        i++;
        int n = s.length();
        int[] pos = new int[1];
        while (i < n) {
            while (i < n && s.charAt(i) != '"' && s.charAt(i) != '}') i++;
            if (i >= n || s.charAt(i) == '}') break;
            pos[0] = i;
            String key = readString(s, pos);
            i = pos[0];
            while (i < n && s.charAt(i) != ':') i++;
            i++;
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            String val;
            if (i < n && s.charAt(i) == '"') {
                pos[0] = i;
                val = readString(s, pos);
                i = pos[0];
            } else {
                int st = i;
                while (i < n && s.charAt(i) != ',' && s.charAt(i) != '}') i++;
                val = s.substring(st, Math.min(i, n)).trim();
            }
            m.put(key, val);
        }
        return m;
    }

    private static String readString(String s, int[] pos) {
        StringBuilder sb = new StringBuilder();
        int i = pos[0] + 1;
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') break;
            if (c == '\\' && i < s.length()) {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        pos[0] = i;
        return sb.toString();
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static String toJson(Object o) {
        StringBuilder sb = new StringBuilder();
        write(sb, o);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object o) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof String s) {
            quote(sb, s);
        } else if (o instanceof Boolean || o instanceof Integer || o instanceof Long) {
            sb.append(o);
        } else if (o instanceof Number n) {
            double d = n.doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e15) sb.append((long) d); else sb.append(d);
        } else if (o instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                quote(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object x : it) {
                if (!first) sb.append(',');
                first = false;
                write(sb, x);
            }
            sb.append(']');
        } else {
            quote(sb, String.valueOf(o));
        }
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------ static files

    private void serveStatic(HttpExchange ex, String path) throws IOException {
        if (path.equals("/")) path = "/index.html";
        Path file = webRoot.resolve(path.substring(1)).normalize();
        if (!file.startsWith(webRoot.normalize()) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain; charset=utf-8", "Halaman tidak ditemukan.");
            return;
        }
        String name = file.getFileName().toString().toLowerCase();
        String type = name.endsWith(".html") ? "text/html; charset=utf-8"
                : name.endsWith(".css") ? "text/css; charset=utf-8"
                : name.endsWith(".js") ? "text/javascript; charset=utf-8"
                : name.endsWith(".svg") ? "image/svg+xml"
                : name.endsWith(".png") ? "image/png"
                : name.endsWith(".ico") ? "image/x-icon" : "application/octet-stream";
        send(ex, 200, type, Files.readAllBytes(file));
    }

    private void send(HttpExchange ex, int status, String type, String body) throws IOException {
        send(ex, status, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
}
