import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AppointmentService } from '../services/appointment.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-agendar-cita',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './agendar-cita.html'
})
export class AgendarCita implements OnInit {

  medicos: any[] = [];
  franjas: any[] = [];
  doctorId = 0;
  fecha = '';
  franjaSeleccionada: any = null;
  patientId = 1; // TODO: obtener del token JWT
  citaCreada = false;
  error = '';
  cargando = false;

  constructor(
    private appointmentService: AppointmentService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.cargarMedicos();
  }

  cargarMedicos() {
    this.appointmentService.listarMedicos().subscribe({
      next: (data) => this.medicos = data,
      error: () => this.error = 'Error al cargar médicos'
    });
  }

  buscarFranjas() {
    if (!this.doctorId || !this.fecha) return;
    this.franjas = [];
    this.appointmentService.obtenerFranjas(this.doctorId, this.fecha).subscribe({
      next: (data) => this.franjas = data,
      error: () => this.error = 'Error al cargar franjas'
    });
  }

  seleccionarFranja(franja: any) {
    if (!franja.available) return;
    this.franjaSeleccionada = franja;
  }

  confirmarCita() {
    if (!this.franjaSeleccionada) return;
    this.cargando = true;
    this.error = '';

    const datos = {
      doctorId: this.doctorId,
      patientId: this.patientId,
      date: this.fecha,
      startTime: this.franjaSeleccionada.startTime,
      notes: ''
    };

    this.appointmentService.crearCita(datos).subscribe({
      next: () => {
        this.citaCreada = true;
        this.cargando = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al agendar la cita';
        this.cargando = false;
      }
    });
  }
}