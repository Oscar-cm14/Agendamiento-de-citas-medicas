// ============================================================
// agendador.ts  –  Panel del Agendador
// ============================================================

import { Component, Inject, PLATFORM_ID, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-agendador',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './agendador.html'
})
export class Agendador implements OnInit {

  // Pestaña activa ('listar' | 'crear')
  pestanaActiva = 'listar';

  // Lista completa de médicos
  medicos: any[] = [];

  // Especialidades únicas construidas desde medicos[]
  especialidades: string[] = [];

  // ── Variables para la pestaña LISTAR ──
  especialidadBuscar = '';          // especialidad elegida en el filtro de búsqueda
  medicosFiltradosBuscar: any[] = []; // médicos de esa especialidad (para el select)
  doctorIdBuscar = 0;
  fechaBuscar = '';
  citas: any[] = [];
  buscando = false;
  errorBuscar = '';

  // ── Variables para la pestaña CREAR ──
  nuevaCita = {
    identification: '',
    firstName: '',
    lastName: '',
    phone: '',
    gender: '',
    birthDate: '',
    email: '',
    doctorId: 0,
    fecha: '',
    startTime: ''
  };

  // Especialidad elegida al crear una nueva cita
  especialidadNuevaCita = '';

  // Médicos filtrados para la nueva cita
  medicosFiltradosNueva: any[] = [];

  franjas: any[] = [];
  citaCreada = false;
  errorCita = '';
  cargandoCita = false;

  // Estado del autocompletado por cédula
  buscandoPaciente = false;
  pacienteEncontrado = false;
  pacienteId: number | null = null;

  private apiUrl = 'http://localhost:8080/api/v1';

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.cargarMedicos();
    }
  }

  // Construye cabecera con el JWT del agendador autenticado
  private headers(): HttpHeaders {
    const token = localStorage.getItem('token') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  cerrarSesion() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    this.router.navigate(['/']);
  }

  // ── Trae todos los médicos y construye especialidades ──
  cargarMedicos() {
    this.http.get<any[]>(`${this.apiUrl}/doctors`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          this.medicos = data;

          // CORRECCIÓN: filtrar nulos antes de construir el Set
          const raw = data
            .map((m: any) => m.specialty)
            .filter((s: any) => s && s.trim() !== '');
          this.especialidades = [...new Set<string>(raw)].sort();
        },
        error: () => { }
      });
  }

  // ── Cambio de especialidad en la pestaña LISTAR ──
  onEspecialidadBuscarChange() {
    // Filtrar médicos por especialidad elegida
    this.medicosFiltradosBuscar = this.medicos.filter(
      m => m.specialty === this.especialidadBuscar
    );
    // Resetear médico y resultados anteriores
    this.doctorIdBuscar = 0;
    this.citas = [];
  }

  // ── Cambio de especialidad en la pestaña CREAR ──
  onEspecialidadNuevaCitaChange() {
    // Filtrar médicos por especialidad elegida
    this.medicosFiltradosNueva = this.medicos.filter(
      m => m.specialty === this.especialidadNuevaCita
    );
    // Resetear médico, franjas y hora
    this.nuevaCita.doctorId   = 0;
    this.franjas              = [];
    this.nuevaCita.startTime  = '';
    this.nuevaCita.fecha      = '';

    // Auto-seleccionar si solo hay un médico en esa especialidad
    if (this.medicosFiltradosNueva.length === 1) {
      this.nuevaCita.doctorId = this.medicosFiltradosNueva[0].id;
    }
  }

  // ── RF1: Buscar citas de un médico en una fecha ──
  buscarCitas() {
    if (!this.doctorIdBuscar || !this.fechaBuscar) return;
    this.buscando    = true;
    this.errorBuscar = '';
    this.citas       = [];

    const params = new HttpParams()
      .set('doctorId', this.doctorIdBuscar)
      .set('date',     this.fechaBuscar);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.citas    = data;
          this.buscando = false;
        },
        error: () => {
          this.errorBuscar = 'Error al buscar citas';
          this.buscando    = false;
        }
      });
  }

  // ── Busca el paciente por cédula al salir del campo (blur) ──
  buscarPacientePorCedula() {
    const cedula = this.nuevaCita.identification.trim();
    if (!cedula) return;

    this.buscandoPaciente  = true;
    this.pacienteEncontrado = false;
    this.pacienteId        = null;

    const params = new HttpParams().set('identification', cedula);

    this.http.get<any>(`${this.apiUrl}/patients/by-identification`,
      { headers: this.headers(), params }).subscribe({
        next: (paciente) => {
          // Paciente existente: autocompletar todos los campos
          this.nuevaCita.firstName = paciente.firstName;
          this.nuevaCita.lastName  = paciente.lastName;
          this.nuevaCita.phone     = paciente.phone;
          this.nuevaCita.email     = paciente.email     || '';
          this.nuevaCita.gender    = paciente.gender    || '';
          this.nuevaCita.birthDate = paciente.birthDate || '';
          this.pacienteId          = paciente.id;
          this.pacienteEncontrado  = true;
          this.buscandoPaciente    = false;
        },
        error: () => {
          // Paciente nuevo: limpiar campos para ingreso manual
          this.nuevaCita.firstName = '';
          this.nuevaCita.lastName  = '';
          this.nuevaCita.phone     = '';
          this.nuevaCita.email     = '';
          this.nuevaCita.gender    = '';
          this.nuevaCita.birthDate = '';
          this.pacienteId          = null;
          this.pacienteEncontrado  = false;
          this.buscandoPaciente    = false;
        }
      });
  }

  // ── RF2: Carga las franjas disponibles del médico + fecha ──
  cargarFranjas() {
    if (!this.nuevaCita.doctorId || !this.nuevaCita.fecha) return;
    this.franjas             = [];
    this.nuevaCita.startTime = '';

    const params = new HttpParams()
      .set('doctorId', this.nuevaCita.doctorId)
      .set('date',     this.nuevaCita.fecha);

    this.http.get<any[]>(`${this.apiUrl}/appointments/slots`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => this.franjas = data.filter(f => f.available),
        error: () => { }
      });
  }

  // ── RF2: Punto de entrada para crear la cita ──
  crearCita() {
    this.cargandoCita = true;
    this.errorCita    = '';

    // Si el paciente ya existe, ir directo a agendar
    if (this.pacienteId) {
      this.agendarCitaConPaciente(this.pacienteId);
      return;
    }

    // Paciente nuevo: registrarlo primero
    const datosPaciente = {
      identification: this.nuevaCita.identification,
      firstName:      this.nuevaCita.firstName,
      lastName:       this.nuevaCita.lastName,
      phone:          this.nuevaCita.phone,
      gender:         this.nuevaCita.gender,
      birthDate:      this.nuevaCita.birthDate || null,
      email:          this.nuevaCita.email     || null,
      username:       this.nuevaCita.identification, // usuario = cédula
      password:       this.nuevaCita.identification  // contraseña inicial = cédula
    };

    this.http.post<any>(`${this.apiUrl}/patients/register`,
      datosPaciente, { headers: this.headers() }).subscribe({
        next: (paciente) => this.agendarCitaConPaciente(paciente.id),
        error: (err) => {
          this.errorCita    = err.error?.message || 'Error al registrar paciente';
          this.cargandoCita = false;
        }
      });
  }

  // ── Crea la cita una vez que se tiene el patientId ──
  private agendarCitaConPaciente(patientId: number) {
    const datosCita = {
      doctorId:  this.nuevaCita.doctorId,
      patientId: patientId,
      date:      this.nuevaCita.fecha,
      startTime: this.nuevaCita.startTime,
      notes:     'Cita agendada por WhatsApp'
    };

    this.http.post(`${this.apiUrl}/appointments`,
      datosCita, { headers: this.headers() }).subscribe({
        next: () => {
          this.cargandoCita = false;
          this.citaCreada   = true;
          this.cdr.detectChanges();
          this.cargarFranjas(); // refrescar franjas para quitar la ocupada
        },
        error: (err) => {
          this.errorCita    = err.error?.message || 'Error al crear cita';
          this.cargandoCita = false;
        }
      });
  }

  // ── Limpia el formulario para crear otra cita ──
  reiniciarFormulario() {
    this.citaCreada          = false;
    this.pacienteEncontrado  = false;
    this.pacienteId          = null;
    this.especialidadNuevaCita = '';
    this.medicosFiltradosNueva = [];
    this.nuevaCita = {
      identification: '',
      firstName:  '',
      lastName:   '',
      phone:      '',
      gender:     '',
      birthDate:  '',
      email:      '',
      doctorId:   0,
      fecha:      '',
      startTime:  ''
    };
    this.franjas = [];
  }
}