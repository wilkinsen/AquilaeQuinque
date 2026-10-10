import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Models.java - Domain model B3 Sistem Manajemen Rumah Sakit (Rawat Inap).
 * No package is used so the project is easy to run in VS Code / terminal.
 */

abstract class Room {
    private final String number;
    private final double nightlyRate;
    private boolean occupied;
    private Patient patient;
    private Doctor assignedDoctor;

    protected Room(String number, double nightlyRate) {
        this.number = number;
        this.nightlyRate = nightlyRate;
    }

    public abstract String getRoomClass();

    public String getNumber() { return number; }
    public double getNightlyRate() { return nightlyRate; }
    public boolean isOccupied() { return occupied; }
    public Patient getPatient() { return patient; }
    public Doctor getAssignedDoctor() { return assignedDoctor; }

    public void assignPatient(Patient patient, Doctor doctor) {
        this.patient = patient;
        this.assignedDoctor = doctor;
        this.occupied = true;
    }

    public void discharge() {
        this.patient = null;
        this.assignedDoctor = null;
        this.occupied = false;
    }

    public String getDisplayInfo() {
        return String.format("%s | Kelas: %s | Rp %,.0f/malam | %s",
                number, getRoomClass(), nightlyRate, occupied ? "TERISI" : "KOSONG");
    }
}

class VIPRoom extends Room {
    public VIPRoom(String number, double rate) { super(number, rate); }
    @Override public String getRoomClass() { return "VIP"; }
}

class Class1Room extends Room {
    public Class1Room(String number, double rate) { super(number, rate); }
    @Override public String getRoomClass() { return "Kelas 1"; }
}

class Class2Room extends Room {
    public Class2Room(String number, double rate) { super(number, rate); }
    @Override public String getRoomClass() { return "Kelas 2"; }
}

class Class3Room extends Room {
    public Class3Room(String number, double rate) { super(number, rate); }
    @Override public String getRoomClass() { return "Kelas 3"; }
}

class ICURoom extends Room {
    public ICURoom(String number, double rate) { super(number, rate); }
    @Override public String getRoomClass() { return "ICU"; }
}

class Patient {
    private final String medicalRecordNumber;
    private final String name;
    private final int age;
    private Room room;
    private Doctor doctor;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private final List<DailyRecord> dailyRecords = new ArrayList<>();
    private final List<MedicalAction> medicalActions = new ArrayList<>();
    private final List<Medication> medications = new ArrayList<>();
    private final List<LaboratoryRequest> laboratoryRequests = new ArrayList<>();
    private final List<NurseAssignment> nurseAssignments = new ArrayList<>();

    public Patient(String medicalRecordNumber, String name, int age) {
        this.medicalRecordNumber = medicalRecordNumber;
        this.name = name;
        this.age = age;
    }

    public String getMedicalRecordNumber() { return medicalRecordNumber; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public Room getRoom() { return room; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getAdmissionDate() { return admissionDate; }
    public LocalDate getDischargeDate() { return dischargeDate; }
    public List<DailyRecord> getDailyRecords() { return dailyRecords; }
    public List<MedicalAction> getMedicalActions() { return medicalActions; }
    public List<Medication> getMedications() { return medications; }
    public List<LaboratoryRequest> getLaboratoryRequests() { return laboratoryRequests; }
    public List<NurseAssignment> getNurseAssignments() { return nurseAssignments; }

    public void admit(Room room, Doctor doctor) {
        this.room = room;
        this.doctor = doctor;
        this.admissionDate = LocalDate.now();
        this.dischargeDate = null;
    }

    public void discharge() { this.dischargeDate = LocalDate.now(); }

    public long getNights() {
        if (admissionDate == null) return 0;
        LocalDate end = dischargeDate == null ? LocalDate.now() : dischargeDate;
        long nights = ChronoUnit.DAYS.between(admissionDate, end);
        return Math.max(1, nights);
    }

    public void addDailyRecord(DailyRecord record) { dailyRecords.add(record); }
    public void addMedicalAction(MedicalAction action) { medicalActions.add(action); }
    public void addMedication(Medication medication) { medications.add(medication); }
    public void addLaboratoryRequest(LaboratoryRequest request) { laboratoryRequests.add(request); }
    public void addNurseAssignment(NurseAssignment assignment) { nurseAssignments.add(assignment); }

    /** Iterator Pattern: DailyRecord per patient. */
    public Iterator<DailyRecord> dailyRecordIterator() {
        return new DailyRecordIterator(dailyRecords);
    }

    public Iterable<DailyRecord> dailyRecordsIterable() {
        return () -> dailyRecordIterator();
    }
}

class Doctor {
    private final String id;
    private final String name;
    private final String specialty;

    public Doctor(String id, String name, String specialty) {
        this.id = id; this.name = name; this.specialty = specialty;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    @Override public String toString() { return id + " - " + name + " (" + specialty + ")"; }
}

class Nurse {
    private final String id;
    private final String name;

    public Nurse(String id, String name) { this.id = id; this.name = name; }
    public String getId() { return id; }
    public String getName() { return name; }
    @Override public String toString() { return id + " - " + name; }
}

class NurseAssignment {
    private final Nurse nurse;
    private final Room room;
    private final String shift;
    private final LocalDate date;

    public NurseAssignment(Nurse nurse, Room room, String shift, LocalDate date) {
        this.nurse = nurse; this.room = room; this.shift = shift; this.date = date;
    }
    public Nurse getNurse() { return nurse; }
    public Room getRoom() { return room; }
    public String getShift() { return shift; }
    public LocalDate getDate() { return date; }
}

class DailyRecord {
    private final LocalDate date;
    private final double systolic;
    private final double diastolic;
    private final double temperature;
    private final double heartRate;
    private final String notes;

    public DailyRecord(LocalDate date, double systolic, double diastolic,
                       double temperature, double heartRate, String notes) {
        this.date = date; this.systolic = systolic; this.diastolic = diastolic;
        this.temperature = temperature; this.heartRate = heartRate; this.notes = notes;
    }
    public LocalDate getDate() { return date; }
    public String getSummary() {
        return String.format("%s | TD %.0f/%.0f mmHg | Suhu %.1f C | Nadi %.0f bpm | %s",
                date, systolic, diastolic, temperature, heartRate, notes);
    }
}

class MedicalAction {
    private final String name;
    private final double cost;
    public MedicalAction(String name, double cost) { this.name = name; this.cost = cost; }
    public String getName() { return name; }
    public double getCost() { return cost; }
}

class Medication {
    private final String name;
    private final double dailyCost;
    public Medication(String name, double dailyCost) { this.name = name; this.dailyCost = dailyCost; }
    public String getName() { return name; }
    public double getDailyCost() { return dailyCost; }
}

class LaboratoryRequest {
    private final String testName;
    private String result;
    private String status;
    private final LocalDateTime requestedAt;

    public LaboratoryRequest(String testName) {
        this.testName = testName;
        this.status = "DIMINTA";
        this.requestedAt = LocalDateTime.now();
    }
    public void recordResult(String result) {
        this.result = result;
        this.status = "SELESAI";
    }
    public String getTestName() { return testName; }
    public String getResult() { return result; }
    public String getStatus() { return status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
}

class InsuranceClaim {
    private final String insuranceName;
    private final double coveragePercent;
    private double totalBill;
    private double coveredAmount;
    private double remainingAmount;

    public InsuranceClaim(String insuranceName, double coveragePercent) {
        this.insuranceName = insuranceName;
        this.coveragePercent = Math.max(0, Math.min(100, coveragePercent));
    }

    public void calculate(double billTotal) {
        totalBill = billTotal;
        coveredAmount = totalBill * coveragePercent / 100.0;
        remainingAmount = totalBill - coveredAmount;
    }
    public double getCoveredAmount() { return coveredAmount; }
    public double getRemainingAmount() { return remainingAmount; }
    public String getInsuranceName() { return insuranceName; }
    public double getCoveragePercent() { return coveragePercent; }
}

class BillItem {
    private final String category;
    private final String description;
    private final double amount;
    public BillItem(String category, String description, double amount) {
        this.category = category; this.description = description; this.amount = amount;
    }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
}

class Bill {
    private final Patient patient;
    private final List<BillItem> items;
    private final LocalDate issuedDate;
    private double insuranceCovered;
    private double remaining;

    public Bill(Patient patient, List<BillItem> items) {
        this.patient = patient;
        this.items = new ArrayList<>(items);
        this.issuedDate = LocalDate.now();
    }

    public double subtotal(String category) {
        return items.stream().filter(i -> i.getCategory().equalsIgnoreCase(category))
                .mapToDouble(BillItem::getAmount).sum();
    }
    public double getTotal() { return items.stream().mapToDouble(BillItem::getAmount).sum(); }
    public Patient getPatient() { return patient; }
    public List<BillItem> getItems() { return Collections.unmodifiableList(items); }
    public double getInsuranceCovered() { return insuranceCovered; }
    public double getRemaining() { return remaining; }
    public void applyInsurance(InsuranceClaim claim) {
        claim.calculate(getTotal());
        insuranceCovered = claim.getCoveredAmount();
        remaining = claim.getRemainingAmount();
    }
    public String formatted() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== TAGIHAN RAWAT INAP ==========\n");
        sb.append("Pasien : ").append(patient.getName()).append(" (RM ")
          .append(patient.getMedicalRecordNumber()).append(")\n");
        sb.append("Tanggal: ").append(issuedDate).append("\n");
        sb.append("----------------------------------------\n");
        String[] categories = {"Kamar", "Tindakan", "Obat/Infus", "Laboratorium"};
        for (String category : categories) {
            double subtotal = subtotal(category);
            if (subtotal > 0) {
                sb.append(String.format("%-18s Rp %,.0f%n", category, subtotal));
                for (BillItem item : items) {
                    if (item.getCategory().equalsIgnoreCase(category)) {
                        sb.append(String.format("  - %-28s Rp %,.0f%n", item.getDescription(), item.getAmount()));
                    }
                }
            }
        }
        sb.append("----------------------------------------\n");
        sb.append(String.format("TOTAL                  Rp %,.0f%n", getTotal()));
        if (insuranceCovered > 0) {
            sb.append(String.format("Asuransi               Rp %,.0f%n", insuranceCovered));
            sb.append(String.format("Sisa Dibayar           Rp %,.0f%n", remaining));
        }
        sb.append("========================================\n");
        return sb.toString();
    }
}

/** Iterator Pattern implementation required by B3 feature 14. */
class DailyRecordIterator implements Iterator<DailyRecord> {
    private final List<DailyRecord> records;
    private int index = 0;
    public DailyRecordIterator(List<DailyRecord> records) { this.records = records; }
    @Override public boolean hasNext() { return index < records.size(); }
    @Override public DailyRecord next() {
        if (!hasNext()) throw new NoSuchElementException();
        return records.get(index++);
    }
}

/** Observer-like notifier used to inform users when rooms become empty. */
class RoomNotificationService {
    private final List<String> logs = new ArrayList<>();
    public void notifyRoomAvailable(Room room) {
        String message = "[NOTIFIKASI] Kamar " + room.getNumber() + " (" + room.getRoomClass() + ") sekarang kosong.";
        logs.add(message);
        System.out.println(message);
    }
    public List<String> getLogs() { return Collections.unmodifiableList(logs); }
}

/**
 * Facade Pattern: one simple interface for complex billing calculations.
 * This class has one responsibility: composing the billing subsystems.
 */
class HospitalBillingFacade {
    public Bill generateFinalBill(Patient patient) {
        List<BillItem> items = new ArrayList<>();
        if (patient.getRoom() != null) {
            items.add(new BillItem("Kamar",
                    patient.getRoom().getRoomClass() + " " + patient.getRoom().getNumber()
                            + " x " + patient.getNights() + " malam",
                    patient.getRoom().getNightlyRate() * patient.getNights()));
        }
        for (MedicalAction action : patient.getMedicalActions()) {
            items.add(new BillItem("Tindakan", action.getName(), action.getCost()));
        }
        for (Medication medication : patient.getMedications()) {
            double amount = medication.getDailyCost() * patient.getNights();
            items.add(new BillItem("Obat/Infus", medication.getName(), amount));
        }
        for (LaboratoryRequest lab : patient.getLaboratoryRequests()) {
            // Simulated fixed laboratory charge per request.
            items.add(new BillItem("Laboratorium", lab.getTestName(), 100000));
        }
        return new Bill(patient, items);
    }
}

/** Core service layer used by the CLI. */
class HospitalService {
    private final Database db;
    private final HospitalBillingFacade billingFacade;
    private final RoomNotificationService notificationService;

    public HospitalService(Database db) {
        this.db = db;
        this.billingFacade = new HospitalBillingFacade();
        this.notificationService = new RoomNotificationService();
    }

    public Room registerRoom(String roomNumber, String roomClass, double rate) {
        if (db.findRoom(roomNumber) != null) throw new IllegalArgumentException("Nomor kamar sudah terdaftar.");
        Room room;
        switch (roomClass.toUpperCase()) {
            case "VIP" -> room = new VIPRoom(roomNumber, rate);
            case "1", "KELAS 1" -> room = new Class1Room(roomNumber, rate);
            case "2", "KELAS 2" -> room = new Class2Room(roomNumber, rate);
            case "3", "KELAS 3" -> room = new Class3Room(roomNumber, rate);
            case "ICU" -> room = new ICURoom(roomNumber, rate);
            default -> throw new IllegalArgumentException("Kelas kamar harus VIP/1/2/3/ICU.");
        }
        db.getRooms().add(room);
        return room;
    }

    public Room findAvailableRoom(String roomClass) {
        return db.getRooms().stream()
                .filter(r -> r.getRoomClass().equalsIgnoreCase(normalizeClass(roomClass)) && !r.isOccupied())
                .findFirst().orElse(null);
    }

    private String normalizeClass(String value) {
        String v = value.trim().toUpperCase();
        if (v.equals("1")) return "Kelas 1";
        if (v.equals("2")) return "Kelas 2";
        if (v.equals("3")) return "Kelas 3";
        return v.equals("VIP") ? "VIP" : v.equals("ICU") ? "ICU" : value;
    }

    public Patient admitPatient(String rm, String name, int age, String roomClass, String doctorId)
            throws RoomNotAvailableException {
        Room room = findAvailableRoom(roomClass);
        if (room == null) throw new RoomNotAvailableException("Tidak ada kamar " + roomClass + " yang tersedia.");
        Doctor doctor = db.findDoctor(doctorId);
        if (doctor == null) throw new IllegalArgumentException("Dokter tidak ditemukan.");
        Patient patient = new Patient(rm, name, age);
        patient.admit(room, doctor);
        room.assignPatient(patient, doctor);
        db.getPatients().add(patient);
        return patient;
    }

    public void assignNurse(String rm, String nurseId, String shift) throws PatientNotFoundException {
        Patient patient = db.findPatient(rm);
        Nurse nurse = db.findNurse(nurseId);
        if (patient == null) throw new PatientNotFoundException("Pasien tidak ditemukan.");
        if (nurse == null) throw new PatientNotFoundException("Perawat tidak ditemukan.");
        NurseAssignment assignment = new NurseAssignment(nurse, patient.getRoom(), shift, LocalDate.now());
        patient.addNurseAssignment(assignment);
    }

    public void addDailyRecord(String rm, DailyRecord record) throws PatientNotFoundException {
        Patient patient = requirePatient(rm);
        patient.addDailyRecord(record);
    }

    public void addMedicalAction(String rm, String name, double cost) throws PatientNotFoundException {
        requirePatient(rm).addMedicalAction(new MedicalAction(name, cost));
    }

    public void addMedication(String rm, String name, double dailyCost) throws PatientNotFoundException {
        requirePatient(rm).addMedication(new Medication(name, dailyCost));
    }

    public double calculateDailyCost(String rm) throws PatientNotFoundException {
        Patient patient = requirePatient(rm);
        double room = patient.getRoom().getNightlyRate();
        double actions = patient.getMedicalActions().stream().mapToDouble(MedicalAction::getCost).sum();
        double medication = patient.getMedications().stream().mapToDouble(Medication::getDailyCost).sum();
        return room + actions + medication;
    }

    public String dailySummary(String rm) throws PatientNotFoundException {
        Patient p = requirePatient(rm);
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- REKAP HARIAN PASIEN ---\n");
        sb.append("RM       : ").append(p.getMedicalRecordNumber()).append("\n");
        sb.append("Nama     : ").append(p.getName()).append("\n");
        sb.append("Kamar    : ").append(p.getRoom().getNumber()).append(" (" + p.getRoom().getRoomClass() + ")\n");
        sb.append("Dokter   : ").append(p.getDoctor().getName()).append("\n");
        sb.append(String.format("Estimasi biaya/hari: Rp %,.0f%n", calculateDailyCost(rm)));
        sb.append("DailyRecord:\n");
        for (DailyRecord record : p.dailyRecordsIterable()) sb.append("  ").append(record.getSummary()).append("\n");
        return sb.toString();
    }

public Bill discharge(String rm) throws PatientNotFoundException {
        Patient patient = requirePatient(rm);
        patient.discharge();
        Room room = patient.getRoom();
        Bill bill = billingFacade.generateFinalBill(patient);
        if (room != null) {
            room.discharge();
            notificationService.notifyRoomAvailable(room);
        }
        // Pindahkan dari list aktif ke history
        db.getPatients().remove(patient);
        if (!db.getPatientHistory().contains(patient)) {
            db.getPatientHistory().add(patient);
        }
        db.getBills().add(bill);
        return bill;
    }

    public InsuranceClaim applyInsurance(Bill bill, String insuranceName, double coveragePercent) {
        InsuranceClaim claim = new InsuranceClaim(insuranceName, coveragePercent);
        bill.applyInsurance(claim);
        db.getClaims().add(claim);
        return claim;
    }

    public void addLabRequest(String rm, String testName) throws PatientNotFoundException {
        requirePatient(rm).addLaboratoryRequest(new LaboratoryRequest(testName));
    }

    public void recordLatestLabResult(String rm, String result) throws PatientNotFoundException {
        Patient p = requirePatient(rm);
        if (p.getLaboratoryRequests().isEmpty()) throw new IllegalStateException("Belum ada permintaan laboratorium.");
        LaboratoryRequest lab = p.getLaboratoryRequests().get(p.getLaboratoryRequests().size() - 1);
        lab.recordResult(result);
    }

    public void transferRoom(String rm, String targetClass) throws PatientNotFoundException, RoomNotAvailableException {
        Patient patient = requirePatient(rm);
        Room target = findAvailableRoom(targetClass);
        if (target == null) throw new RoomNotAvailableException("Kamar tujuan " + targetClass + " tidak tersedia.");
        Room old = patient.getRoom();
        old.discharge();
        patient.admit(target, patient.getDoctor());
        target.assignPatient(patient, patient.getDoctor());
    }

    public List<Patient> getOccupants() { return db.getPatients(); }
    public List<Patient> getPatientHistory() { return db.getPatientHistory(); } // <-- TAMBAHKAN BARIS INI
    public List<Room> getRooms() { return db.getRooms(); }
    public List<Doctor> getDoctors() { return db.getDoctors(); }
    public List<Nurse> getNurses() { return db.getNurses(); }
    public List<Bill> getBills() { return db.getBills(); }
    public RoomNotificationService getNotificationService() { return notificationService; }

    private Patient requirePatient(String rm) throws PatientNotFoundException {
        Patient p = db.findPatient(rm);
        if (p == null) throw new PatientNotFoundException("Pasien dengan RM " + rm + " tidak ditemukan.");
        return p;
    }
}
