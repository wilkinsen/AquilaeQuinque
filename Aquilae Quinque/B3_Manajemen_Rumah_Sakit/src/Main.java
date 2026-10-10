/** Entry point for B3 Sistem Manajemen Rumah Sakit. */
public class Main {
    public static void main(String[] args) {
        Database database = new Database();
        database.seed();
        new App(database).run();
    }
}
