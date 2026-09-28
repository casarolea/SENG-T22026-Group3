package org.yourcompany.smartclinic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import io.javalin.Javalin;

public class SmartClinic {

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        }).start(7070);

        app.get("/", ctx -> {
            ctx.result("SmartClinic server is working!");
        });

        app.get("/tasks", ctx -> {
            Connection connection = SQLConnection.getConnection();
            Statement statement = connection.createStatement();

            String sql = "SELECT task.*, " +
                    "staff.first_name AS staff_first_name, " +
                    "staff.last_name AS staff_last_name, " +
                    "patient.first_name AS patient_first_name, " +
                    "patient.last_name AS patient_last_name " +
                    "FROM task " +
                    "LEFT JOIN staff ON task.staff_id = staff.staff_id " +
                    "LEFT JOIN patient ON task.patient_id = patient.patient_id";

            ResultSet result = statement.executeQuery(sql);

            String tasks = "[";

            while (result.next()) {
                tasks += "{";
                tasks += "\"task_id\":" + result.getInt("task_id") + ",";
                tasks += "\"patient_id\":" + result.getInt("patient_id") + ",";
                tasks += "\"staff_id\":" + result.getInt("staff_id") + ",";
                tasks += "\"staff_name\":\"" +
                        result.getString("staff_first_name") + " " +
                        result.getString("staff_last_name") + "\",";
                tasks += "\"patient_name\":\"" +
                        result.getString("patient_first_name") + " " +
                        result.getString("patient_last_name") + "\",";
                tasks += "\"task_description\":\"" + result.getString("task_description") + "\",";
                tasks += "\"status\":\"" + result.getString("status") + "\",";
                tasks += "\"due_date\":\"" + result.getDate("due_date") + "\"";
                tasks += "},";
            }

            if (tasks.endsWith(",")) {
                tasks = tasks.substring(0, tasks.length() - 1);
            }

            tasks += "]";

            ctx.contentType("application/json");
            ctx.result(tasks);

            result.close();
            statement.close();
            connection.close();
        });

        app.put("/tasks/{id}/done", ctx -> {
            int taskId = Integer.parseInt(ctx.pathParam("id"));

            Connection connection = SQLConnection.getConnection();
            Statement statement = connection.createStatement();

            String sql = "UPDATE task SET status = 'Completed' WHERE task_id = " + taskId;

            statement.executeUpdate(sql);

            ctx.result("Task marked as completed!");

            statement.close();
            connection.close();
        });

        app.delete("/tasks/{id}", ctx -> {
            int taskId = Integer.parseInt(ctx.pathParam("id"));

            Connection connection = SQLConnection.getConnection();
            Statement statement = connection.createStatement();

            String sql = "DELETE FROM task WHERE task_id = " + taskId;

            statement.executeUpdate(sql);

            ctx.result("Task deleted!");

            statement.close();
            connection.close();
        });

        app.post("/tasks", ctx -> {
            int patientId = Integer.parseInt(ctx.formParam("patient_id"));
            String taskDescription = ctx.formParam("task_description");
            String dueDate = ctx.formParam("due_date");

            Connection connection = SQLConnection.getConnection();

            String sql = "INSERT INTO task " +
                    "(patient_id, staff_id, task_description, status, due_date) " +
                    "VALUES (?, 1, ?, 'Pending', ?)";

            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setInt(1, patientId);
            statement.setString(2, taskDescription);
            statement.setString(3, dueDate);

            statement.executeUpdate();

            ctx.result("Task added successfully!");

            statement.close();
            connection.close();
        });

        app.get("/staff", ctx -> {
            Connection connection = SQLConnection.getConnection();
            Statement statement = connection.createStatement();

            ResultSet result = statement.executeQuery("SELECT * FROM staff");

            String json = "[";
            boolean first = true;

            while (result.next()) {
                if (!first) {
                    json += ",";
                }

                json += "{";
                json += "\"staff_id\":" + result.getInt("staff_id") + ",";
                json += "\"first_name\":\"" + result.getString("first_name") + "\",";
                json += "\"last_name\":\"" + result.getString("last_name") + "\",";
                json += "\"role\":\"" + result.getString("role") + "\"";
                json += "}";

                first = false;
            }

            json += "]";

            ctx.contentType("application/json");
            ctx.result(json);

            result.close();
            statement.close();
            connection.close();
        });

        app.put("/tasks/{taskId}/assign/{staffId}", ctx -> {
            int taskId = Integer.parseInt(ctx.pathParam("taskId"));
            int staffId = Integer.parseInt(ctx.pathParam("staffId"));

            Connection connection = SQLConnection.getConnection();

            String sql = "UPDATE task SET staff_id = ? WHERE task_id = ?";

            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setInt(1, staffId);
            statement.setInt(2, taskId);

            statement.executeUpdate();

            ctx.result("Task assigned successfully!");

            statement.close();
            connection.close();
        });

        app.get("/doctors", ctx -> {
            Connection connection = SQLConnection.getConnection();
            Statement statement = connection.createStatement();

            ResultSet result = statement.executeQuery(
                    "SELECT doctor_id, first_name, last_name FROM doctor"
            );

            String doctors = "[";

            while (result.next()) {
                doctors += "{";
                doctors += "\"doctor_id\":" + result.getInt("doctor_id") + ",";
                doctors += "\"first_name\":\"" + result.getString("first_name") + "\",";
                doctors += "\"last_name\":\"" + result.getString("last_name") + "\"";
                doctors += "},";
            }

            if (doctors.endsWith(",")) {
                doctors = doctors.substring(0, doctors.length() - 1);
            }

            doctors += "]";

            ctx.contentType("application/json");
            ctx.result(doctors);

            result.close();
            statement.close();
            connection.close();
        });

        app.post("/appointments", ctx -> {
            int patientId = Integer.parseInt(ctx.formParam("patient_id"));
            int doctorId = Integer.parseInt(ctx.formParam("doctor_id"));

            String appointmentDate = ctx.formParam("appointment_date");
            String startTime = ctx.formParam("start_time");
            String endTime = ctx.formParam("end_time");
            String reason = ctx.formParam("reason");

            Connection connection = SQLConnection.getConnection();

            String sql = "INSERT INTO appointment " +
                    "(patient_id, doctor_id, appointment_date, start_time, end_time, status, reason) " +
                    "VALUES (?, ?, ?, ?, ?, 'Scheduled', ?)";

            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setInt(1, patientId);
            statement.setInt(2, doctorId);
            statement.setString(3, appointmentDate);
            statement.setString(4, startTime);
            statement.setString(5, endTime);
            statement.setString(6, reason);

            statement.executeUpdate();

            ctx.result("Appointment booked successfully!");

            statement.close();
            connection.close();
        });

        app.get("/appointments", ctx -> {
            Connection connection = SQLConnection.getConnection();

            String sql = "SELECT appointment.appointment_id, " +
                    "appointment.patient_id, " +
                    "appointment.doctor_id, " +
                    "appointment.appointment_date, " +
                    "appointment.start_time, " +
                    "appointment.end_time, " +
                    "appointment.status, " +
                    "patient.first_name, " +
                    "patient.last_name " +
                    "FROM appointment " +
                    "JOIN patient ON appointment.patient_id = patient.patient_id " +
                    "WHERE appointment.status != 'Cancelled' " +
                    "ORDER BY appointment.appointment_date, appointment.start_time";

            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet result = statement.executeQuery();

            String appointments = "[";

            while (result.next()) {
                appointments += "{";
                appointments += "\"appointment_id\":" + result.getInt("appointment_id") + ",";
                appointments += "\"patient_id\":" + result.getInt("patient_id") + ",";
                appointments += "\"doctor_id\":" + result.getInt("doctor_id") + ",";
                appointments += "\"patient_name\":\"" +
                        result.getString("first_name") + " " +
                        result.getString("last_name") + "\",";
                appointments += "\"appointment_date\":\"" + result.getDate("appointment_date") + "\",";
                appointments += "\"start_time\":\"" + result.getTime("start_time") + "\",";
                appointments += "\"end_time\":\"" + result.getTime("end_time") + "\",";
                appointments += "\"status\":\"" + result.getString("status") + "\"";
                appointments += "},";
            }

            if (appointments.endsWith(",")) {
                appointments = appointments.substring(0, appointments.length() - 1);
            }

            appointments += "]";

            ctx.contentType("application/json");
            ctx.result(appointments);

            result.close();
            statement.close();
            connection.close();
        });

        app.post("/appointments/{id}/checkin", ctx -> {
            int appointmentId = Integer.parseInt(ctx.pathParam("id"));

            Connection connection = SQLConnection.getConnection();

            String findSql =
                    "SELECT patient_id FROM appointment WHERE appointment_id = ?";

            PreparedStatement findStatement =
                    connection.prepareStatement(findSql);

            findStatement.setInt(1, appointmentId);

            ResultSet result = findStatement.executeQuery();

            if (!result.next()) {
                ctx.status(404);
                ctx.result("Appointment not found.");

                result.close();
                findStatement.close();
                connection.close();
                return;
            }

            int patientId = result.getInt("patient_id");

            String insertSql =
                    "INSERT INTO queue " +
                    "(patient_id, appointment_id, queue_number, check_in_time, status, priority) " +
                    "VALUES (?, ?, ?, CURRENT_TIME, 'Waiting', 'Normal')";

            PreparedStatement insertStatement =
                    connection.prepareStatement(insertSql);

            insertStatement.setInt(1, patientId);
            insertStatement.setInt(2, appointmentId);
            insertStatement.setInt(3, appointmentId);

            insertStatement.executeUpdate();

            ctx.result("Patient checked in successfully.");

            result.close();
            findStatement.close();
            insertStatement.close();
            connection.close();
        });

        app.put("/appointments/{id}/cancel", ctx -> {
            int appointmentId = Integer.parseInt(ctx.pathParam("id"));

            Connection connection = SQLConnection.getConnection();

            String sql = "UPDATE appointment SET status = 'Cancelled' WHERE appointment_id = ?";

            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setInt(1, appointmentId);
            statement.executeUpdate();

            ctx.result("Appointment cancelled successfully!");

            statement.close();
            connection.close();
        });

        app.put("/queue/{id}/start", ctx -> {
            int queueId = Integer.parseInt(ctx.pathParam("id"));

            Connection connection = SQLConnection.getConnection();

            String sql = "UPDATE queue SET status = 'In Progress' WHERE queue_id = ?";

            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setInt(1, queueId);

            int rows = statement.executeUpdate();

            if (rows > 0) {
                ctx.result("Patient consultation started.");
            } else {
                ctx.status(404);
                ctx.result("Queue record not found.");
            }

            statement.close();
            connection.close();
        });

        app.put("/queue/{id}/complete", ctx -> {
            int queueId = Integer.parseInt(ctx.pathParam("id"));

            Connection connection = SQLConnection.getConnection();

            String sql = "UPDATE queue SET status = 'Completed' WHERE queue_id = ?";

            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setInt(1, queueId);

            int rows = statement.executeUpdate();

            if (rows > 0) {
                ctx.result("Patient consultation completed.");
            } else {
                ctx.status(404);
                ctx.result("Queue record not found.");
            }

            statement.close();
            connection.close();
        });

        app.get("/queue", ctx -> {
            Connection connection = SQLConnection.getConnection();

            String sql = "SELECT q.queue_id, q.patient_id, q.appointment_id, " +
                    "q.queue_number, q.check_in_time, q.status, q.priority, " +
                    "p.first_name, p.last_name, " +
                    "a.start_time, a.end_time " +
                    "FROM queue q " +
                    "JOIN patient p ON q.patient_id = p.patient_id " +
                    "JOIN appointment a ON q.appointment_id = a.appointment_id " +
                    "ORDER BY q.queue_number";

            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet result = statement.executeQuery();

            String queue = "[";

            while (result.next()) {
                queue += "{";
                queue += "\"queue_id\":" + result.getInt("queue_id") + ",";
                queue += "\"patient_id\":" + result.getInt("patient_id") + ",";
                queue += "\"appointment_id\":" + result.getInt("appointment_id") + ",";
                queue += "\"queue_number\":" + result.getInt("queue_number") + ",";
                queue += "\"patient_name\":\"" +
                        result.getString("first_name") + " " +
                        result.getString("last_name") + "\",";
                queue += "\"start_time\":\"" + result.getTime("start_time") + "\",";
                queue += "\"end_time\":\"" + result.getTime("end_time") + "\",";
                queue += "\"status\":\"" + result.getString("status") + "\",";
                queue += "\"priority\":\"" + result.getString("priority") + "\"";
                queue += "},";
            }

            if (queue.endsWith(",")) {
                queue = queue.substring(0, queue.length() - 1);
            }

            queue += "]";

            ctx.contentType("application/json");
            ctx.result(queue);

            result.close();
            statement.close();
            connection.close();
        });

        app.get("/appointments/patient/{patientId}", ctx -> {
            int patientId = Integer.parseInt(ctx.pathParam("patientId"));

            Connection connection = SQLConnection.getConnection();

            String sql =
                    "SELECT appointment.*, doctor.first_name, doctor.last_name " +
                    "FROM appointment " +
                    "JOIN doctor ON appointment.doctor_id = doctor.doctor_id " +
                    "WHERE appointment.patient_id = ? " +
                    "ORDER BY appointment.appointment_date, appointment.start_time";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, patientId);

            ResultSet result = statement.executeQuery();

            String appointments = "[";

            while (result.next()) {
                appointments += "{";
                appointments += "\"appointment_id\":" + result.getInt("appointment_id") + ",";
                appointments += "\"appointment_date\":\"" + result.getDate("appointment_date") + "\",";
                appointments += "\"start_time\":\"" + result.getTime("start_time") + "\",";
                appointments += "\"end_time\":\"" + result.getTime("end_time") + "\",";
                appointments += "\"status\":\"" + result.getString("status") + "\",";
                appointments += "\"doctor_name\":\"Dr. " +
                        result.getString("first_name") + " " +
                        result.getString("last_name") + "\"";
                appointments += "},";
            }

            if (appointments.endsWith(",")) {
                appointments = appointments.substring(0, appointments.length() - 1);
            }

            appointments += "]";

            ctx.contentType("application/json");
            ctx.result(appointments);

            result.close();
            statement.close();
            connection.close();
        });

        app.delete("/appointments/{id}", ctx -> {
            int appointmentId = Integer.parseInt(ctx.pathParam("id"));

            Connection connection = SQLConnection.getConnection();

            String sql = "DELETE FROM appointment WHERE appointment_id = ?";

            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setInt(1, appointmentId);

            int rows = statement.executeUpdate();

            if (rows > 0) {
                ctx.result("Appointment cancelled successfully!");
            } else {
                ctx.status(404);
                ctx.result("Appointment not found.");
            }

            statement.close();
            connection.close();
        });
    }
}