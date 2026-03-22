import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';

@Component({
  selector: 'app-agendador',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './agendador.html'
})
export class Agendador {

  pestanaActiva = 'listar';

  // RF1: Listar citas
  medicos: any[] = [];
  doctorIdBuscar = 0;
  fechaBuscar = '';
  citas: any[] = [];
  buscando = false;
  errorBuscar = '';

  // RF2: Crear cita
  nuevaCita = {
    // Datos del paciente
    identification: '',
    firstName: '',
    lastName: '',
    phone: '',
    gender: '',
    birthDate: '',
    email: '',
    // Datos de la cita
    doctorId: 0,
    startTime: ''
  };
  franjas: any[] = [];
  citaCreada = false;
  errorCita = '';
  cargandoCita = false;

  private apiUrl = 'http://localhost:8080/api/v1';

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.cargarMedicos();
    }
  }

  private headers(): HttpHeaders {
    const token = localStorage.getItem('token') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  cerrarSesion() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    this.router.navigate(['/']);
  }

  cargarMedicos() {
    this.http.get<any[]>(`${this.apiUrl}/doctors`,
      { headers: this.headers() }).subscribe({
      next: (data) => this.medicos = data,
      error: () => {}
    });
  }

  // RF1: Buscar citas de un médico en una fecha
  buscarCitas() {
    if (!this.doctorIdBuscar || !this.fechaBuscar) return;
    this.buscando = true;
    this.errorBuscar = '';
    this.citas = [];

    const params = new HttpParams()
      .set('doctorId', this.doctorIdBuscar)
      .set('date', this.fechaBuscar);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
      next: (data) => {
        this.citas = data;
        this.buscando = false;
      },
      error: () => {
        this.errorBuscar = 'Error al buscar citas';
        this.buscando = false;
      }
    });
  }

  // RF2: Cargar franjas al seleccionar médico y fecha
  cargarFranjas() {
    if (!this.nuevaCita.doctorId || !this.nuevaCita.startTime) return;
    const params = new HttpParams()
      .set('doctorId', this.nuevaCita.doctorId)
      .set('date', this.nuevaCita.startTime);

    this.http.get<any[]>(`${this.apiUrl}/appointments/slots`,
      { headers: this.headers(), params }).subscribe({
      next: (data) => this.franjas = data.filter(f => f.available),
      error: () => {}
    });
  }

  // RF2: Crear cita como agendador
  crearCita() {
    this.cargandoCita = true;
    this.errorCita = '';

    // Primero registrar paciente, luego crear cita
    const datosPaciente = {
      identification: this.nuevaCita.identification,
      firstName: this.nuevaCita.firstName,
      lastName: this.nuevaCita.lastName,
      phone: this.nuevaCita.phone,
      gender: this.nuevaCita.gender,
      birthDate: this.nuevaCita.birthDate || null,
      email: this.nuevaCita.email || null,
      username: this.nuevaCita.identification,
      password: this.nuevaCita.identification
    };

    this.http.post<any>(`${this.apiUrl}/patients/register`,
      datosPaciente, { headers: this.headers() }).subscribe({
      next: (paciente) => {
        const datosCita = {
          doctorId: this.nuevaCita.doctorId,
          patientId: paciente.id,
          date: this.nuevaCita.startTime,
          startTime: this.franjas[0]?.startTime,
          notes: 'Cita agendada por WhatsApp'
        };

        this.http.post(`${this.apiUrl}/appointments`,
          datosCita, { headers: this.headers() }).subscribe({
          next: () => {
            this.citaCreada = true;
            this.cargandoCita = false;
          },
          error: (err) => {
            this.errorCita = err.error?.message || 'Error al crear cita';
            this.cargandoCita = false;
          }
        });
      },
      error: (err) => {
        this.errorCita = err.error?.message || 'Error al registrar paciente';
        this.cargandoCita = false;
      }
    });
  }
}