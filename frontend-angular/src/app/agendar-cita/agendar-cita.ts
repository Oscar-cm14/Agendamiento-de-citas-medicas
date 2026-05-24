// ============================================================
// agendar-cita.ts  –  Panel del Paciente
// ============================================================


import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { AppointmentService } from '../services/appointment.service';
import { AuthService } from '../services/auth.service';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';

import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatNativeDateModule } from '@angular/material/core';
@Component({
  selector: 'app-agendar-cita',
  standalone: true,
  imports: [
    FormsModule, 
    CommonModule,
    MatDatepickerModule,
    MatInputModule,
    MatFormFieldModule,
    MatNativeDateModule
  ],
  templateUrl: './agendar-cita.html'
})
export class AgendarCita implements OnInit {

  medicos: any[] = [];
  franjas: any[] = [];
  especialidades: string[] = [];
  especialidadSeleccionada = '';
  medicosFiltrados: any[] = [];
  doctorId = 0;
  fecha: Date | null = null;
  fechaStr = '';
  franjaSeleccionada: any = null;
  patientId: number | null = null;
  citaCreada = false;
  error = '';
  cargando = false;
  cargandoFranjas = false;
  misCitas: any[] = [];
  cargandoCitas = false;
  errorCitas = '';
  pestanaActiva = 'agendar';

  // ── Nombre del paciente ──
  pacienteNombre = '';

  // ── Cancelación ──
  citaCancelando: any = null;
  motivoCancelacion = '';
  errorCancelacion = '';
  cancelando = false;

  // ── REGLAS DE NEGOCIO ──
  tieneCitaPendiente = false;
  tieneConsultaGeneralCompletada = false;

  // ── HORARIOS Y FESTIVOS ──
  doctorWorkingDays: string[] = [];
  minDate = new Date();
  festivosColombiaStr: string[] = [
    '2024-01-01', '2024-01-08', '2024-03-25', '2024-03-28', '2024-03-29', '2024-05-01', '2024-05-13', '2024-06-03', '2024-06-10', '2024-07-01', '2024-07-20', '2024-08-07', '2024-08-19', '2024-10-14', '2024-11-04', '2024-11-11', '2024-12-08', '2024-12-25',
    '2025-01-01', '2025-01-06', '2025-03-24', '2025-04-17', '2025-04-18', '2025-05-01', '2025-06-02', '2025-06-23', '2025-06-30', '2025-07-20', '2025-08-07', '2025-08-18', '2025-10-13', '2025-11-03', '2025-11-17', '2025-12-08', '2025-12-25',
    '2026-01-01', '2026-01-12', '2026-03-23', '2026-04-02', '2026-04-03', '2026-05-01', '2026-05-18', '2026-06-08', '2026-06-15', '2026-06-29', '2026-07-20', '2026-08-07', '2026-08-17', '2026-10-12', '2026-11-02', '2026-11-16', '2026-12-08', '2026-12-25'
  ];

  private apiUrl = 'http://localhost:8080/api/v1';

  constructor(
    private appointmentService: AppointmentService,
    private authService: AuthService,
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit() {
  if (isPlatformBrowser(this.platformId)) {
    // Intentar obtener patientId del localStorage
    this.patientId = this.authService.obtenerUserId();

    // Si no está en localStorage, buscarlo en el backend usando el username del JWT
    if (!this.patientId) {
      this.resolverPatientId();
    }

    this.cargarNombrePaciente();
    this.cargarMedicos();
  }
}

// NUEVO: resuelve el patientId desde el backend usando el username del token
resolverPatientId() {
  try {
    // Usar el username guardado al hacer login (es la cédula), NO el preferred_username del JWT
    // (Keycloak guarda el email como preferred_username, pero la BD busca por cédula)
    const username = localStorage.getItem('username') || '';
    if (!username) return;

    this.http.get<any>(
      `${this.apiUrl}/patients/by-username?username=${username}`,
      { headers: this.headers() }
    ).subscribe({
      next: (patient) => {
        if (patient?.id) {
          this.patientId = patient.id;
          localStorage.setItem('userId', String(patient.id));
          this.cdr.detectChanges();
        }
      },
      error: () => {} // silencioso, el error ya se muestra en confirmarCita()
    });
  } catch (e) {}
}

  // ── Carga el nombre del paciente desde el JWT de Keycloak ──
  cargarNombrePaciente() {
    try {
      const token = this.authService.obtenerToken() || '';
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const givenName  = payload.given_name  || '';
        const familyName = payload.family_name || '';
        if (givenName || familyName) {
          this.pacienteNombre = (givenName + ' ' + familyName).trim();
        } else {
          this.pacienteNombre = payload.name || payload.preferred_username || '';
        }
      }
    } catch (e) {
      this.pacienteNombre = '';
    }
  }

  private headers(): HttpHeaders {
    const token = this.authService.obtenerToken() || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  cargarMedicos() {
    this.appointmentService.listarMedicos().subscribe({
      next: (data) => {
        this.medicos = data || [];
        const permitidas = ['Consulta General', 'Terapia Neural', 'Quiropraxia', 'Fisioterapia'];
        const raw = this.medicos
          .map((m: any) => m.specialty)
          .filter((s: any) => s && s.trim() !== '');
        this.especialidades = [...new Set<string>(raw)]
          .filter(s => permitidas.includes(s))
          .sort();
      },
      error: () => this.error = 'Error al cargar médicos'
    });
  }

  onEspecialidadChange() {
    this.medicosFiltrados  = this.medicos.filter(m => m.specialty === this.especialidadSeleccionada);
    this.doctorId          = 0;
    this.franjas           = [];
    this.franjaSeleccionada = null;
    this.fecha             = null;
    this.fechaStr          = '';
    this.error             = '';
    this.doctorWorkingDays = [];

    if (this.medicosFiltrados.length === 1) {
      this.doctorId = this.medicosFiltrados[0].id;
      this.cargarHorarioMedico();
      this.cdr.detectChanges();
    }
  }

  onDoctorChange() {
    this.franjas = [];
    this.franjaSeleccionada = null;
    this.fecha = null;
    this.fechaStr = '';
    this.error = '';
    if (this.doctorId && this.doctorId > 0) {
      this.cargarHorarioMedico();
    } else {
      this.doctorWorkingDays = [];
    }
  }

  cargarHorarioMedico() {
    this.appointmentService.obtenerHorario(this.doctorId).subscribe({
      next: (horario) => {
        this.doctorWorkingDays = horario.workingDays || [];
      },
      error: () => this.doctorWorkingDays = []
    });
  }

  // Convertir fecha a string YYYY-MM-DD
  formatDateStr(d: Date): string {
    const year = d.getFullYear();
    const month = ('0' + (d.getMonth() + 1)).slice(-2);
    const day = ('0' + d.getDate()).slice(-2);
    return `${year}-${month}-${day}`;
  }

  // Filtro para deshabilitar fechas en el Datepicker
  dateFilter = (d: Date | null): boolean => {
    if (!d) return false;
    
    // 1. Bloquear sábados (6) y domingos (0)
    const day = d.getDay();
    if (day === 0 || day === 6) return false;

    // 2. Bloquear festivos
    const dateString = this.formatDateStr(d);
    if (this.festivosColombiaStr.includes(dateString)) return false;

    // 3. Bloquear días que el médico no trabaja
    if (this.doctorWorkingDays.length > 0) {
      const daysMap = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
      const dayName = daysMap[day];
      if (!this.doctorWorkingDays.includes(dayName)) {
        return false;
      }
    }

    return true;
  };

  // Clase CSS personalizada para los días del calendario
  dateClass = (d: Date): string => {
    const dateString = this.formatDateStr(d);
    return this.festivosColombiaStr.includes(dateString) ? 'holiday-date' : '';
  };

  cargarMisCitas() {
    if (!this.patientId) {
      this.errorCitas = 'No se pudo identificar al paciente.';
      return;
    }

    this.cargandoCitas = true;
    this.errorCitas = '';
    this.misCitas = [];

    const params = new HttpParams().set('patientId', this.patientId.toString());
    this.http.get<any[]>(`${this.apiUrl}/appointments`, { headers: this.headers(), params }).subscribe({
      next: (data) => {
        this.misCitas = data || [];
        this.tieneCitaPendiente = this.misCitas.some(c => c.status === 'SCHEDULED');
        this.tieneConsultaGeneralCompletada = this.misCitas.some(c => c.status === 'COMPLETED' && c.specialty === 'Consulta General');
        this.cargandoCitas = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargandoCitas = false;
        this.errorCitas = (err.status === 401 || err.status === 403)
          ? 'Sesión expirada.'
          : 'Error al cargar las citas.';
        this.cdr.detectChanges();
      }
    });
  }

  buscarFranjas() {
    if (!this.doctorId || !this.fecha) {
      this.franjas = [];
      this.franjaSeleccionada = null;
      return;
    }

    this.fechaStr = this.formatDateStr(this.fecha);

    this.franjas = [];
    this.franjaSeleccionada = null;
    this.error = '';
    this.cargandoFranjas = true;

    this.appointmentService.obtenerFranjas(this.doctorId, this.fechaStr).subscribe({
      next: (data) => {
        this.franjas = (data || []).filter((f: any) => f.available);
        this.cargandoFranjas = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargandoFranjas = false;
        this.error = err.status === 404
          ? 'El médico aún no tiene horario configurado.'
          : 'Error al cargar las franjas horarias.';
        this.cdr.detectChanges();
      }
    });
  }

  seleccionarFranja(franja: any) {
    if (franja && franja.available) {
      this.franjaSeleccionada = franja;
    }
  }

  cerrarSesion() {
    this.authService.cerrarSesion();
    this.router.navigate(['/login']);
  }

  confirmarCita() {
    if (!this.patientId) {
      this.error = 'No se ha detectado el ID del paciente.';
      return;
    }

    if (!this.doctorId || !this.fechaStr) {
      this.error = 'Seleccione médico y fecha.';
      return;
    }

    if (!this.franjaSeleccionada) {
      this.error = 'Seleccione una franja horaria.';
      return;
    }

    this.cargando = true;
    this.error = '';

    const datos = {
      doctorId: this.doctorId,
      patientId: this.patientId,
      date: this.fechaStr,
      startTime: this.franjaSeleccionada.startTime,
      notes: ''
    };

    this.appointmentService.crearCita(datos).subscribe({
      next: () => {
        this.cargando = false;
        this.citaCreada = true;
        this.cdr.detectChanges();
        this.cargarMisCitas();
        setTimeout(() => {
          this.limpiarFormulario();
          this.citaCreada = false;
        }, 2000);
      },
      error: (err) => {
        this.cargando = false;
        if (err.status === 409) {
          this.error = 'La franja ya fue tomada.';
        } else if (err.status === 400) {
          this.error = 'Datos inválidos.';
        } else if (err.status === 401 || err.status === 403) {
          this.error = 'Sesión expirada.';
        } else {
          this.error = 'Error al agendar cita.';
        }
      }
    });
  }

  abrirCancelacion(cita: any) {
    this.citaCancelando = cita;
    this.motivoCancelacion = '';
    this.errorCancelacion = '';
    this.cdr.detectChanges();
  }

  cerrarCancelacion() {
    this.citaCancelando = null;
    this.motivoCancelacion = '';
  }

  confirmarCancelacion() {
    if (!this.citaCancelando) {
      this.errorCancelacion = 'No hay cita seleccionada para cancelar.';
      return;
    }

    if (!this.motivoCancelacion.trim()) {
      this.errorCancelacion = 'Debe indicar el motivo de cancelación.';
      return;
    }

    this.cancelando = true;
    this.errorCancelacion = '';

    this.http.patch(`${this.apiUrl}/appointments/${this.citaCancelando.id}/cancel`,
      { reason: this.motivoCancelacion },
      { headers: this.headers() })
      .subscribe({
        next: () => {
          this.cancelando = false;
          this.citaCancelando = null;
          this.motivoCancelacion = '';
          this.cargarMisCitas();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.cancelando = false;
          this.errorCancelacion = err.error?.message || 'Error al cancelar la cita.';
          this.cdr.detectChanges();
        }
      });
  }

  etiquetaEstado(status: string): string {
    const map: any = {
      SCHEDULED: 'Programada',
      COMPLETED: 'Atendida',
      CANCELLED: 'Cancelada',
      NO_SHOW: 'No asistida'
    };
    return map[status] || status;
  }

  colorEstado(status: string): string {
    const map: any = {
      SCHEDULED: 'bg-primary',
      COMPLETED: 'bg-success',
      CANCELLED: 'bg-danger',
      NO_SHOW: 'bg-warning'
    };
    return map[status] || 'bg-secondary';
  }

  limpiarFormulario() {
    this.doctorId = 0;
    this.fecha = null;
    this.fechaStr = '';
    this.franjas = [];
    this.franjaSeleccionada = null;
    this.cargandoFranjas = false;
    this.error = '';
    this.citaCreada = false;
    this.especialidadSeleccionada = '';
    this.medicosFiltrados = [];
    this.doctorWorkingDays = [];
  }

  get especialidadesPermitidas() {
    if (!this.tieneConsultaGeneralCompletada) {
      return this.especialidades.filter(e => e === 'Consulta General');
    }
    return this.especialidades;
  }
}