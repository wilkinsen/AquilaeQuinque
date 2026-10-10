/** Custom exceptions for B3 Hospital Management System. */
class HospitalException extends Exception {
    public HospitalException(String message) { super(message); }
}

class RoomNotAvailableException extends HospitalException {
    public RoomNotAvailableException(String message) { super(message); }
}

class PatientNotFoundException extends HospitalException {
    public PatientNotFoundException(String message) { super(message); }
}

class InvalidTransferException extends HospitalException {
    public InvalidTransferException(String message) { super(message); }
}
