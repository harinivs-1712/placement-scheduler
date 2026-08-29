package placement_scheduler.controller;

public class DatasetRequest {

    private int students;
    private int companies;
    private int rooms;
    private int days;

    public DatasetRequest() {
    }

    public int getStudents() {
        return students;
    }

    public void setStudents(int students) {
        this.students = students;
    }

    public int getCompanies() {
        return companies;
    }

    public void setCompanies(int companies) {
        this.companies = companies;
    }

    public int getRooms() {
        return rooms;
    }

    public void setRooms(int rooms) {
        this.rooms = rooms;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }
}