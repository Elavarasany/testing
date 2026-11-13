
package nmk_backend.controller;

import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

import nmk_backend.entity.Appointment;
import nmk_backend.repository.AppointmentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;

    public AppointmentController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }
    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<?> clearAllAppointments() {

        long deletedCount = appointmentRepository.count();

        appointmentRepository.deleteAll();

        return ResponseEntity.ok(Map.of(
                "message", "All appointments deleted successfully",
                "deletedCount", deletedCount
        ));
    }

    // Customer: Book an appointment
    @PostMapping
    public ResponseEntity<?> bookAppointment(
            @RequestBody Appointment appointment) {

        if (appointment.getCustomerName() == null ||
                appointment.getCustomerName().isBlank() ||
                appointment.getPhone() == null ||
                appointment.getPhone().isBlank() ||
                appointment.getAppointmentDate() == null ||
                appointment.getAppointmentTime() == null) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Please provide all required details."));
        }

        if (appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Appointment date cannot be in the past."));
        }

        LocalTime time = appointment.getAppointmentTime();

        if (time.isBefore(LocalTime.of(9, 0)) ||
                !time.isBefore(LocalTime.of(18, 0))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Appointments are available between 9 AM and 6 PM."));
        }

        boolean alreadyBooked =
                appointmentRepository.existsByAppointmentDateAndAppointmentTimeAndStatusNot(
                        appointment.getAppointmentDate(),
                        time,
                        "CANCELLED"
                );

        if (alreadyBooked) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "This appointment slot is already booked."));
        }

        appointment.setId(null);
        appointment.setStatus("PENDING");
        appointment.setCreatedAt(null);

        Appointment saved = appointmentRepository.save(appointment);
        System.out.println("SAVED APPOINTMENT ID: " + saved.getId());
        System.out.println("SAVED CUSTOMER: " + saved.getCustomerName());
        System.out.println("SAVED DATE: " + saved.getAppointmentDate());
        System.out.println("SAVED TIME: " + saved.getAppointmentTime());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Admin: View all appointments
    @GetMapping
    public List<Appointment> getAllAppointments() {
        List<Appointment> appointments =
                appointmentRepository.findAllByOrderByAppointmentDateAscAppointmentTimeAsc();

        System.out.println("TOTAL APPOINTMENTS: " + appointments.size());

        return appointments;
    }

    // Admin: Update appointment status
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        Appointment appointment = appointmentRepository.findById(id)
                .orElse(null);

        if (appointment == null) {
            return ResponseEntity.notFound().build();
        }

        String status = request.get("status");

        Set<String> allowedStatuses = Set.of(
                "PENDING",
                "CONFIRMED",
                "COMPLETED",
                "CANCELLED"
        );

        if (status == null || !allowedStatuses.contains(status)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid appointment status."));
        }

        appointment.setStatus(status);

        return ResponseEntity.ok(appointmentRepository.save(appointment));
    }
}
