import java.time.LocalDate;
import java.util.Scanner;

/**
 * CLI application replacing the Streamlit app.py from the B1 example.
 * All B3 features are exposed through menu options.
 */
public class App {
    private final Scanner scanner = new Scanner(System.in);
    private final HospitalService service;

    public App(Database database) { this.service = new HospitalService(database); }

    public void run() {
        while (true) {
            printHeader();
            System.out.println("1. Dashboard & Data Master");
            System.out.println("2. Registrasi Kamar");
            System.out.println("3. Admisi Pasien / Check-in");
            System.out.println("4. Assign Perawat & Shift");
            System.out.println("5. Input Kondisi Harian Pasien");
            System.out.println("6. Input Tindakan Medis");
            System.out.println("7. Input Obat / Infus Harian");
            System.out.println("8. Rekap Harian & Iterator DailyRecord");
            System.out.println("9. Laboratorium");
            System.out.println("10. Transfer Pasien Antar Kamar");
            System.out.println("11. Discharge & Tagihan Final");
            System.out.println("12. Klaim Asuransi");
            System.out.println("13. Lihat Tagihan Tersimpan");
            System.out.println("0. Keluar");
            System.out.print("Pilih menu: ");
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> dashboard();
                    case "2" -> registerRoom();
                    case "3" -> admitPatient();
                    case "4" -> assignNurse();
                    case "5" -> dailyRecord();
                    case "6" -> medicalAction();
                    case "7" -> medication();
                    case "8" -> summary();
                    case "9" -> laboratory();
                    case "10" -> transfer();
                    case "11" -> discharge();
                    case "12" -> insurance();
                    case "13" -> showBills();
                    case "0" -> { System.out.println("Program selesai. Terima kasih!"); return; }
                    default -> System.out.println("Menu tidak tersedia.");
                }
            } catch (HospitalException | IllegalArgumentException | IllegalStateException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
            pause();
        }
    }

    private void printHeader() {
        System.out.println("\n============================================================");
        System.out.println("       SISTEM MANAJEMEN RUMAH SAKIT - B3 RAWAT INAP");
        System.out.println("============================================================");
    }

    private void dashboard() {
        System.out.println("\n--- DASHBOARD & DATA MASTER ---");
        System.out.println("Total kamar : " + service.getRooms().size());
        long occupied = service.getRooms().stream().filter(Room::isOccupied).count();
        System.out.println("Terisi      : " + occupied);
        System.out.println("Kosong      : " + (service.getRooms().size() - occupied));
        System.out.println("\nDAFTAR KAMAR:");
        service.getRooms().forEach(r -> System.out.println("- " + r.getDisplayInfo()));
        System.out.println("\nDOKTER:");
        service.getDoctors().forEach(d -> System.out.println("- " + d));
        System.out.println("\nPERAWAT:");
        service.getNurses().forEach(n -> System.out.println("- " + n));
        
        System.out.println("\nLIST PASIEN RAWAT INAP (AKTIF):");
        if (service.getOccupants().isEmpty()) {
            System.out.println("  (Tidak ada pasien aktif)");
        } else {
            service.getOccupants().forEach(p -> System.out.println(
                "- RM: " + p.getMedicalRecordNumber() + 
                " | Nama: " + p.getName() + 
                " | Kamar: " + (p.getRoom() != null ? p.getRoom().getNumber() : "-") + 
                " | Tgl Masuk: " + p.getAdmissionDate()
            ));
        }

        System.out.println("\nLIST HISTORY PASIEN (DISCHARGED):");
        if (service.getPatientHistory().isEmpty()) {
            System.out.println("  (Belum ada history pasien keluar)");
        } else {
            service.getPatientHistory().forEach(p -> System.out.println(
                "- RM: " + p.getMedicalRecordNumber() + 
                " | Nama: " + p.getName() + 
                " | Tgl Masuk: " + p.getAdmissionDate() + 
                " | Tgl Keluar: " + p.getDischargeDate()
            ));
        }
    }

    private void registerRoom() {
        System.out.println("\n--- REGISTRASI KAMAR ---");
        String number = ask("Nomor kamar: ");
        String roomClass = ask("Kelas (VIP/1/2/3/ICU): ");
        double rate = askDouble("Tarif per malam: ");
        Room room = service.registerRoom(number, roomClass, rate);
        System.out.println("Berhasil: " + room.getDisplayInfo());
    }

    private void admitPatient() throws RoomNotAvailableException {
        System.out.println("\n--- ADMISI PASIEN / CHECK-IN ---");
        String rm = ask("No. RM: ");
        String name = ask("Nama pasien: ");
        int age = askInt("Usia: ");
        String roomClass = ask("Kelas kamar yang diinginkan: ");
        String doctorId = ask("ID dokter (D001/D002): ");
        Patient patient = service.admitPatient(rm, name, age, roomClass, doctorId);
        System.out.println("Admisi berhasil. Pasien " + patient.getName() + " ditempatkan di kamar " + patient.getRoom().getNumber());
    }

    private void assignNurse() throws PatientNotFoundException {
        String rm = ask("No. RM: ");
        String nurse = ask("ID perawat (N001/N002): ");
        String shift = ask("Shift (Pagi/Siang/Malam): ");
        service.assignNurse(rm, nurse, shift);
        System.out.println("Perawat berhasil di-assign ke kamar pasien.");
    }

    private void dailyRecord() throws PatientNotFoundException {
        String rm = ask("No. RM: ");
        double systolic = askDouble("Sistolik: ");
        double diastolic = askDouble("Diastolik: ");
        double temp = askDouble("Suhu (C): ");
        double heartRate = askDouble("Detak jantung: ");
        String notes = ask("Catatan kondisi: ");
        service.addDailyRecord(rm, new DailyRecord(LocalDate.now(), systolic, diastolic, temp, heartRate, notes));
        System.out.println("DailyRecord tersimpan.");
    }

    private void medicalAction() throws PatientNotFoundException {
        String rm = ask("No. RM: ");
        String name = ask("Nama tindakan medis: ");
        double cost = askDouble("Biaya tindakan: ");
        service.addMedicalAction(rm, name, cost);
        System.out.println("Tindakan medis tercatat.");
    }

    private void medication() throws PatientNotFoundException {
        String rm = ask("No. RM: ");
        String name = ask("Nama obat/infus: ");
        double dailyCost = askDouble("Biaya:");
        service.addMedication(rm, name, dailyCost);
        System.out.println("Obat/infus harian tercatat.");
    }

    private void summary() throws PatientNotFoundException {
        String rm = ask("No. RM: ");
        System.out.println(service.dailySummary(rm));
    }

    private void laboratory() throws PatientNotFoundException {
        String rm = ask("No. RM: ");
        System.out.println("1. Buat permintaan laboratorium");
        System.out.println("2. Catat hasil laboratorium terbaru");
        String c = ask("Pilih: ");
        if (c.equals("1")) {
            String test = ask("Nama pemeriksaan: ");
            service.addLabRequest(rm, test);
            System.out.println("Permintaan laboratorium dibuat.");
        } else if (c.equals("2")) {
            String result = ask("Hasil pemeriksaan: ");
            service.recordLatestLabResult(rm, result);
            System.out.println("Hasil laboratorium dicatat.");
        }
    }

    private void transfer() throws PatientNotFoundException, RoomNotAvailableException {
        String rm = ask("No. RM: ");
        String target = ask("Kelas kamar tujuan: ");
        service.transferRoom(rm, target);
        System.out.println("Transfer pasien berhasil.");
    }

    private void discharge() throws PatientNotFoundException {
        String rm = ask("No. RM yang akan discharge: ");
        Bill bill = service.discharge(rm);
        System.out.println(bill.formatted());
    }

    private void insurance() throws PatientNotFoundException {
        if (service.getBills().isEmpty()) {
            System.out.println("Belum ada tagihan.");
            return;
        }
        Bill bill = service.getBills().get(service.getBills().size() - 1);
        String name = ask("Nama asuransi: ");
        double percent = askDouble("Persentase tanggungan (0-100): ");
        InsuranceClaim claim = service.applyInsurance(bill, name, percent);
        System.out.println(String.format("Asuransi menanggung Rp %,.0f; sisa bayar Rp %,.0f",
                claim.getCoveredAmount(), claim.getRemainingAmount()));
        System.out.println(bill.formatted());
    }

    private void showBills() {
        if (service.getBills().isEmpty()) {
            System.out.println("Belum ada tagihan tersimpan.");
            return;
        }
        service.getBills().forEach(b -> System.out.println(b.formatted()));
    }

    private String ask(String prompt) { System.out.print(prompt); return scanner.nextLine().trim(); }
    private int askInt(String prompt) { return Integer.parseInt(ask(prompt)); }
    private double askDouble(String prompt) { return Double.parseDouble(ask(prompt)); }
    private void pause() { System.out.println("\nTekan ENTER untuk kembali ke menu..."); scanner.nextLine(); }
}
