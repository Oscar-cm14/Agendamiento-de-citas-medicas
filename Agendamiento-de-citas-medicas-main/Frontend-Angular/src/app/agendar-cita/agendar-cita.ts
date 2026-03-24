import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { AppointmentService } from '../services/appointment.service';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

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
  patientId: number | null = null;
  citaCreada = false;
  error = '';
  cargando = false;

  constructor(
    private appointmentService: AppointmentService,
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.patientId = this.authService.obtenerUserId();
      this.cargarMedicos();
    }
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
      next: (data) => this.franjas = data.filter((f: any) => f.available),
      error: () => this.error = 'Error al cargar franjas'
    });
  }

  seleccionarFranja(franja: any) {
    if (!franja.available) return;
    this.franjaSeleccionada = franja;
  }

  cerrarSesion() {
    this.authService.cerrarSesion();
    this.router.navigate(['/login']);
  }

  confirmarCita() {
    if (!this.patientId) {
      this.error = 'No se ha detectado el ID del paciente. Por favor, cierre sesión e inicie de nuevo. Si el problema persiste, asegúrese de haber reiniciado el Backend.';
      return;
    }
    if (!this.franjaSeleccionada) {
      this.error = 'Por favor, seleccione una franja horaria.';
      return;
    }
    
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
        console.error('Error al agendar cita:', err);
        let msg = 'Error desconocido al agendar la cita.';
        if (err.status === 400 || err.status === 404 || err.status === 409) {
           msg = err.error?.message || err.error || 'Revise los datos ingresados (' + err.status + ')';
        } else if (err.status === 401 || err.status === 403) {
           msg = 'Sesión expirada o acceso denegado. Por favor inicie sesión nuevamente.';
        } else if (err.status === 0) {
           msg = 'No se pudo contactar con el servidor. Verifique si está encendido.';
        } else if (err.error?.message) {
           msg = err.error.message;
        }
        this.error = msg;
        this.cargando = false;
      }
    });
  }
}