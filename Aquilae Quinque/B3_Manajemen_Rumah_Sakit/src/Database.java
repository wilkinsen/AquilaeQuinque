import java.util.ArrayList;
import java.util.List;

class Database {
    private final List<Room> rooms = new ArrayList<>();
    private final List<Patient> patients = new ArrayList<>(); // Pasien Rawat Inap Aktif
    private final List<Patient> patientHistory = new ArrayList<>(); // List History Pasien (Discharged)
    private final List<Doctor> doctors = new ArrayList<>();
    private final List<Nurse> nurses = new ArrayList<>();
    private final List<Bill> bills = new ArrayList<>();
    private final List<InsuranceClaim> claims = new ArrayList<>();

    public List<Room> getRooms() { return rooms; }
    public List<Patient> getPatients() { return patients; }
    public List<Patient> getPatientHistory() { return patientHistory; }
    public List<Doctor> getDoctors() { return doctors; }
    public List<Nurse> getNurses() { return nurses; }
    public List<Bill> getBills() { return bills; }
    public List<InsuranceClaim> getClaims() { return claims; }

    public Room findRoom(String number) {
        return rooms.stream().filter(r -> r.getNumber().equalsIgnoreCase(number)).findFirst().orElse(null);
    }
    public Patient findPatient(String rm) {
        Patient p = patients.stream().filter(x -> x.getMedicalRecordNumber().equalsIgnoreCase(rm)).findFirst().orElse(null);
        if (p == null) {
            p = patientHistory.stream().filter(x -> x.getMedicalRecordNumber().equalsIgnoreCase(rm)).findFirst().orElse(null);
        }
        return p;
    }
    public Doctor findDoctor(String id) {
        return doctors.stream().filter(d -> d.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
    public Nurse findNurse(String id) {
        return nurses.stream().filter(n -> n.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public void seed() {
        if (!rooms.isEmpty()) return;
        doctors.add(new Doctor("D001", "dr. Janice", "Penyakit Dalam"));
        doctors.add(new Doctor("D002", "dr. Sevany", "Bedah"));
        doctors.add(new Doctor("D003", "dr. Stanley", "Ortopedi"));
        doctors.add(new Doctor("D004", "dr. Steven", "Jantung"));
        doctors.add(new Doctor("D005", "dr. Wilkinsen", "Paru-Paru"));
        nurses.add(new Nurse("N001", "Suster Citra"));
        nurses.add(new Nurse("N002", "Suster Dina"));
        nurses.add(new Nurse("N003", "Suster Budi"));
        nurses.add(new Nurse("N004", "Suster Anto"));
        nurses.add(new Nurse("N005", "Suster Enta"));


        rooms.add(new VIPRoom("VIP-01", 1000000));
        rooms.add(new Class1Room("K1-01", 600000));
        rooms.add(new Class2Room("K2-01", 400000));
        rooms.add(new Class3Room("K3-01", 250000));
        rooms.add(new ICURoom("ICU-01", 1500000));

        Patient sample = new Patient("RM001", "Citra", 21);
        sample.admit(rooms.get(1), doctors.get(0));
        rooms.get(1).assignPatient(sample, doctors.get(0));
        sample.addNurseAssignment(new NurseAssignment(nurses.get(0), rooms.get(1), "Pagi", java.time.LocalDate.now()));
        patients.add(sample);
    }
}