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

@Component({
  selector: 'app-agendar-cita',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './agendar-cita.html'
})
export class AgendarCita implements OnInit {

  medicos: any[] = [];
  franjas: any[] = [];
  especialidades: string[] = [];
  especialidadSeleccionada = '';
  medicosFiltrados: any[] = [];
  doctorId = 0;
  fecha = '';
  franjaSeleccionada: any = null;
  patientId: number | null = null;
  citaCreada = false;
  error = '';
  cargando = false;
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
        const raw = this.medicos
          .map((m: any) => m.specialty)
          .filter((s: any) => s && s.trim() !== '');
        this.especialidades = [...new Set<string>(raw)].sort();
      },
      error: () => this.error = 'Error al cargar médicos'
    });
  }

  onEspecialidadChange() {
    this.medicosFiltrados  = this.medicos.filter(m => m.specialty === this.especialidadSeleccionada);
    this.doctorId          = 0;
    this.franjas           = [];
    this.franjaSeleccionada = null;
    this.fecha             = '';
    this.error             = '';

    if (this.medicosFiltrados.length === 1) {
      this.doctorId = this.medicosFiltrados[0].id;
      if (this.fecha) {
        this.buscarFranjas();
      }
      this.cdr.detectChanges();
    }
  }

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

    this.franjas = [];
    this.franjaSeleccionada = null;
    this.error = '';

    this.appointmentService.obtenerFranjas(this.doctorId, this.fecha).subscribe({
      next: (data) => {
        this.franjas = (data || []).filter((f: any) => f.available);
        this.cdr.detectChanges();
      },
      error: (err) => {
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

    if (!this.doctorId || !this.fecha) {
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
      date: this.fecha,
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
      COMPLETED: 'Completada',
      CANCELLED: 'Cancelada'
    };
    return map[status] || status;
  }

  limpiarFormulario() {
    this.doctorId = 0;
    this.fecha = '';
    this.franjas = [];
    this.franjaSeleccionada = null;
    this.error = '';
    this.citaCreada = false;
    this.especialidadSeleccionada = '';
    this.medicosFiltrados = [];
  }
}