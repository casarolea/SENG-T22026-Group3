let staffList = [];

fetch("http://localhost:7070/staff")
    .then(response => response.json())
    .then(staff => {
        staffList = staff;
    })
    .catch(error => {
        console.log("Error loading staff:", error);
    });

function markDone(taskId) {
    fetch("http://localhost:7070/tasks/" + taskId + "/done", {
        method: "PUT"
    })
    .then(response => response.text())
    .then(message => {
        alert(message);
        location.reload();
    })
    .catch(error => {
        console.log("Error:", error);
    });
}

function assignTask(person, taskId, staffId) {
    fetch("http://localhost:7070/tasks/" + taskId + "/assign/" + staffId, {
        method: "PUT"
    })
    .then(response => response.text())
    .then(message => {
        const dropdown = person.closest(".dropdown");
        const button = dropdown.querySelector(".assign-btn");

        button.textContent = person.textContent.trim();
        alert(message);
    })
    .catch(error => {
        console.log("Error:", error);
    });
}

function deleteTask(taskId) {
    fetch("http://localhost:7070/tasks/" + taskId, {
        method: "DELETE"
    })
    .then(response => response.text())
    .then(message => {
        alert(message);
        location.reload();
    })
    .catch(error => {
        console.log("Error:", error);
    });
}

function addTask() {
    const patientID = document.getElementById("patientID").value;
    const taskDescription = document.getElementById("taskDescription").value;
    const dueDate = document.getElementById("dueDate").value;

    if (patientID === "" || taskDescription === "") {
        alert("Please complete all fields.");
        return;
    }

    const formData = new URLSearchParams();

    formData.append("patient_id", patientID);
    formData.append("task_description", taskDescription);
    formData.append("due_date", dueDate);

    fetch("http://localhost:7070/tasks", {
        method: "POST",
        body: formData
    })
    .then(response => response.text())
    .then(message => {
        alert(message);
        location.reload();
    })
    .catch(error => {
        console.log("Error:", error);
    });
}

fetch("http://localhost:7070/tasks")
    .then(response => response.json())
    .then(tasks => {
        const taskList = document.getElementById("taskList");

        tasks.forEach(task => {
            const taskCard = document.createElement("div");

            taskCard.className = "card mb-2 border-left-danger";

            taskCard.innerHTML = `
                <div class="card-body d-flex justify-content-between align-items-start">
                    <div>
                        <strong>${task.patient_name}</strong>

                        <p class="small mb-0" style="font-size: 10px;">
                            Patient ID: ${task.patient_id}
                        </p>

                        <p class="text-muted small mb-1 mt-1">
                            Status: ${task.status}
                        </p>

                        <p class="small mb-0">
                            <strong>Task: ${task.task_description}</strong>
                        </p>

                        <p class="small mb-1 mt-1">
                            Due Date: ${task.due_date}
                        </p>

                        <div class="dropdown">
                            <button class="btn btn-primary btn-sm dropdown-toggle assign-btn"
                                    type="button"
                                    data-toggle="dropdown">
                                Assign Task
                            </button>

                            <div class="dropdown-menu">
                                ${staffList.map(staff => `
                                    <button class="dropdown-item"
                                            type="button"
                                            onclick="assignTask(this, ${task.task_id}, ${staff.staff_id})">
                                        ${staff.first_name} ${staff.last_name}
                                    </button>
                                `).join("")}
                            </div>
                        </div>
                    </div>

                    <div class="task-buttons">
                        <button class="btn btn-sm btn-outline-success"
                                onclick="markDone(${task.task_id})">
                            Mark done
                        </button>

                        <button class="btn btn-sm btn-outline-danger"
                                onclick="deleteTask(${task.task_id})">
                            Delete
                        </button>
                    </div>
                </div>
            `;

            taskList.appendChild(taskCard);
        });
    })
    .catch(error => {
        console.log("Error:", error);
    });

function cancelAppointment(appointmentId) {
    if (!confirm("Are you sure you want to cancel this appointment?")) {
        return;
    }

    fetch("http://localhost:7070/appointments/" + appointmentId, {
        method: "DELETE"
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Could not cancel appointment.");
        }

        return response.text();
    })
    .then(message => {
        alert(message);
        location.reload();
    })
    .catch(error => {
        console.log("Error:", error);
        alert("Could not cancel appointment.");
    });
}

function checkInAppointment(appointmentId) {
    fetch("http://localhost:7070/appointments/" + appointmentId + "/checkin", {
        method: "POST"
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Could not check in patient.");
        }

        return response.text();
    })
    .then(message => {
        alert(message);
        location.reload();
    })
    .catch(error => {
        console.log("Error:", error);
        alert("Could not check in patient.");
    });
}

function loadPatientAppointments(patientId) {
    fetch("http://localhost:7070/appointments/patient/" + patientId)
        .then(response => response.json())
        .then(appointments => {
            const container = document.getElementById("patientAppointments");

            container.innerHTML = "";

            if (appointments.length === 0) {
                container.innerHTML = "<p>No upcoming appointment yet.</p>";
                return;
            }

            appointments.forEach(appointment => {
                const appointmentDiv = document.createElement("div");

                appointmentDiv.innerHTML = `
                    <p>
                        ${appointment.appointment_date},
                        ${appointment.start_time.substring(0, 5)}–
                        ${appointment.end_time.substring(0, 5)}
                    </p>

                    <button class="btn btn-action-green"
                            onclick="checkInAppointment(${appointment.appointment_id})">
                        Check In
                    </button>

                    <button class="btn btn-action-muted"
                            onclick="cancelAppointment(${appointment.appointment_id})">
                        Cancel
                    </button>
                `;

                container.appendChild(appointmentDiv);
            });
        })
        .catch(error => {
            console.log("Error loading appointments:", error);
        });
}