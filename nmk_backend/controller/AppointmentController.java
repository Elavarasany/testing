
package nmk_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

import nmk_backend.entity.Appointment;
import nmk_backend.repository.AppointmentRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import nmk_backend.service.AdminActionAuthorizationService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AdminActionAuthorizationService authorizationService;

    private final AppointmentRepository appointmentRepository;

    public AppointmentController(
            AppointmentRepository appointmentRepository,
            AdminActionAuthorizationService authorizationService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.authorizationService = authorizationService;
    }
    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<?> clearAllAppointments(
            @RequestHeader(
                    value = "X-Admin-Action-Authorization",
                    required = false
            ) String authorizationToken,
            Authentication authentication
    ) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(
                        Map.of("message", "Admin login required")
                );
            }

            authorizationService.consumeAuthorization(
                    authentication.getName(),
                    authorizationToken,
                    "CLEAR_APPOINTMENTS"
            );

            long deletedCount = appointmentRepository.count();

            appointmentRepository.deleteAll();

            return ResponseEntity.ok(Map.of(
                    "message", "All appointments deleted successfully",
                    "deletedCount", deletedCount
            ));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(403).body(
                    Map.of("message", exception.getMessage())
            );
        }
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

        Set<LocalTime> allowedSlots = Set.of(
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                LocalTime.of(15, 0)
        );

        if (!allowedSlots.contains(time)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Choose one of the available slots: 9 AM, 12 PM, or 3 PM."
                    ));
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
