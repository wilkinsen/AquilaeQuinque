import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HospitalManagementTest {
    private Database db;
    private HospitalService service;

    @BeforeEach
    void setUp() {
        db = new Database();
        db.seed();
        service = new HospitalService(db);
    }

    @Test
    void testRoomRegistration() {
        Room room = service.registerRoom("K1-02", "1", 600000);
        assertEquals("Kelas 1", room.getRoomClass());
        assertFalse(room.isOccupied());
    }

    @Test
    void testRoomAvailabilityBeforeAdmission() {
        Room room = service.findAvailableRoom("Kelas 1");
        assertNotNull(room);
        assertFalse(room.isOccupied());
    }

    @Test
    void testAdmissionAssignsDoctor() throws Exception {
        Patient p = service.admitPatient("RM002", "Dina", 22, "VIP", "D001");
        assertEquals("VIP", p.getRoom().getRoomClass());
        assertEquals("dr. Andi Pratama", p.getDoctor().getName());
    }

    @Test
    void testRoomFullThrowsException() throws Exception {
        service.admitPatient("RM002", "Dina", 22, "VIP", "D001");
        assertThrows(RoomNotAvailableException.class,
                () -> service.admitPatient("RM003", "Eka", 23, "VIP", "D001"));
    }

    @Test
    void testNurseAssignment() throws Exception {
        Patient p = service.admitPatient("RM002", "Dina", 22, "VIP", "D001");
        service.assignNurse(p.getMedicalRecordNumber(), "N001", "Pagi");
        assertEquals(1, p.getNurseAssignments().size());
        assertEquals("Pagi", p.getNurseAssignments().get(0).getShift());
    }

    @Test
    void testDailyRecordIterator() throws Exception {
        service.addDailyRecord("RM001", new DailyRecord(LocalDate.now(), 120, 80, 36.7, 80, "Stabil"));
        service.addDailyRecord("RM001", new DailyRecord(LocalDate.now(), 118, 78, 36.6, 78, "Baik"));
        Patient p = db.findPatient("RM001");
        Iterator<DailyRecord> iterator = p.dailyRecordIterator();
        assertTrue(iterator.hasNext());
        assertEquals(2, p.getDailyRecords().size());
    }

    @Test
    void testDailyCostCalculation() throws Exception {
        service.addMedicalAction("RM001", "Visite dokter", 150000);
        service.addMedication("RM001", "Infus", 75000);
        assertEquals(825000, service.calculateDailyCost("RM001"), 0.001);
    }

    @Test
    void testLaboratoryRequestAndResult() throws Exception {
        service.addLabRequest("RM001", "Darah Lengkap");
        service.recordLatestLabResult("RM001", "Normal");
        LaboratoryRequest lab = db.findPatient("RM001").getLaboratoryRequests().get(0);
        assertEquals("SELESAI", lab.getStatus());
        assertEquals("Normal", lab.getResult());
    }

    @Test
    void testTransferRoom() throws Exception {
        Patient p = db.findPatient("RM001");
        service.transferRoom(p.getMedicalRecordNumber(), "Kelas 2");
        assertEquals("Kelas 2", p.getRoom().getRoomClass());
    }

    @Test
    void testDischargeGeneratesBillAndFreesRoom() throws Exception {
        Patient p = db.findPatient("RM001");
        String roomNumber = p.getRoom().getNumber();
        Bill bill = service.discharge(p.getMedicalRecordNumber());
        assertTrue(bill.getTotal() > 0);
        assertFalse(db.findRoom(roomNumber).isOccupied());
        assertEquals(1, service.getBills().size());
    }

    @Test
    void testInsuranceCalculation() throws Exception {
        Bill bill = service.discharge("RM001");
        InsuranceClaim claim = service.applyInsurance(bill, "Asuransi Sehat", 80);
        assertEquals(bill.getTotal() * 0.8, claim.getCoveredAmount(), 0.001);
        assertEquals(bill.getTotal() * 0.2, claim.getRemainingAmount(), 0.001);
    }
}
