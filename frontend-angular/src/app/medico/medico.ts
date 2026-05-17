// ============================================================
// medico.ts  –  Panel del Médico / Terapista
// ============================================================

import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-medico',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './medico.html',
  styleUrl: './medico.css'
})
export class Medico implements OnInit {

  // Pestaña activa: 'hoy' | 'buscar' | 'agendar' | 'reagendar'
  pestanaActiva = 'hoy';

  // Info del médico autenticado
  medicoNombre       = '';
  medicoEspecialidad = '';
  medicoId: number | null = null;

  // ── Pestaña: Citas de hoy ──────────────────────────────────
  citasHoy: any[] = [];
  cargandoHoy     = false;
  errorHoy        = '';
  fechaHoy        = '';

  // ── Pestaña: Buscar citas por fecha ───────────────────────
  fechaBuscar        = '';
  citasBuscar: any[] = [];
  buscando           = false;
  errorBuscar        = '';
  exportando         = false;
  mensajeExport      = '';

  // ── Cancelar cita (modal igual al paciente) ───────────────
  citaCancelando: any = null;
  motivoCancelacion = '';
  errorCancelacion = '';
  cancelando = false;
  mensajeCancelacion = '';

  // ── Pestaña: Agendar nueva cita ───────────────────────────
  todosMedicos: any[]    = [];
  especialidades: string[] = [];

  nuevaCita = {
    identification : '',
    firstName      : '',
    lastName       : '',
    phone          : '',
    gender         : '',
    birthDate      : '',
    email          : '',
    doctorId       : 0,
    fecha          : '',
    startTime      : ''
  };
  especialidadNueva      = '';
  medicosFiltradosNueva: any[] = [];
  franjasNueva: any[]    = [];
  citaCreada             = false;
  errorCita              = '';
  cargandoCita           = false;
  buscandoPaciente       = false;
  pacienteEncontrado     = false;
  pacienteId: number | null = null;

  // ── Pestaña: Reagendar cita ────────────────────────────────
  reagBuscarFecha        = '';
  reagCitas: any[]       = [];
  reagBuscando           = false;
  reagError              = '';
  reagCitaSeleccionada: any = null;
  reagNuevaFecha         = '';
  reagFranjas: any[]     = [];
  reagNuevaHora          = '';
  reagCargandoFranjas    = false;
  reagGuardando          = false;
  reagExito              = '';
  reagErrorGuardar       = '';

  private apiUrl = 'http://localhost:8080/api/v1';

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.fechaHoy = new Date().toISOString().split('T')[0];
      this.cargarDatosMedico();
      this.cargarTodosMedicos();
    }
  }

  private headers(): HttpHeaders {
    const token = localStorage.getItem('token') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  // ── Carga datos del médico autenticado ─────────────────────
  cargarDatosMedico() {
    // Usa /doctors/me — el backend lee el username del JWT de Keycloak
    // No se necesita userId en localStorage
    this.http.get<any>(`${this.apiUrl}/doctors/me`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          this.medicoId           = data.id;
          this.medicoNombre       = data.fullName
            || `${data.firstName} ${data.lastName}`;
          this.medicoEspecialidad = data.specialty || '';
          localStorage.setItem('nombreUsuario', this.medicoNombre);
          this.nuevaCita.doctorId = data.id;
          this.cdr.detectChanges();
          this.cargarCitasHoy();
        },
        error: () => {
          this.medicoNombre = localStorage.getItem('nombreUsuario') || '';
          this.cargarCitasHoy();
        }
      });
  }

  // ── Carga todos los médicos (para el selector de agendar) ──
  cargarTodosMedicos() {
    this.http.get<any[]>(`${this.apiUrl}/doctors`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          this.todosMedicos = data;
          const raw = data
            .map((m: any) => m.specialty)
            .filter((s: any) => s && s.trim() !== '');
          this.especialidades = [...new Set<string>(raw)].sort();
          this.cdr.detectChanges();
        },
        error: () => { }
      });
  }

  // ══════════════════════════════════════════════════════════
  //  PESTAÑA: CITAS DE HOY
  // ══════════════════════════════════════════════════════════

  cargarCitasHoy() {
    if (!this.medicoId) return;
    this.cargandoHoy = true;
    this.errorHoy    = '';
    this.citasHoy    = [];

    const params = new HttpParams()
      .set('doctorId', this.medicoId)
      .set('date',     this.fechaHoy);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.citasHoy    = data || [];
          this.cargandoHoy = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorHoy    = 'Error al cargar las citas de hoy.';
          this.cargandoHoy = false;
          this.cdr.detectChanges();
        }
      });
  }

  // ── Cancelar cita (modal) ──────────────────────────────────
  abrirCancelacion(cita: any) {
    this.citaCancelando = cita;
    this.motivoCancelacion = '';
    this.errorCancelacion = '';
    this.cdr.detectChanges();
  }

  cerrarCancelacion() {
    this.citaCancelando = null;
    this.motivoCancelacion = '';
    this.errorCancelacion = '';
  }

  confirmarCancelacion() {
    if (!this.citaCancelando) return;
    if (!this.motivoCancelacion.trim()) {
      this.errorCancelacion = 'Debe indicar el motivo de cancelación.';
      return;
    }

    this.cancelando = true;
    this.errorCancelacion = '';

    this.http.patch(
      `${this.apiUrl}/appointments/${this.citaCancelando.id}/cancel`,
      { reason: this.motivoCancelacion },
      { headers: this.headers() }
    ).subscribe({
      next: () => {
        this.cancelando = false;
        this.mensajeCancelacion = '✅ Cita cancelada exitosamente.';
        this.citaCancelando = null;
        this.motivoCancelacion = '';
        this.cdr.detectChanges();
        this.cargarCitasHoy();
        if (this.fechaBuscar) this.buscarCitas();
        setTimeout(() => { this.mensajeCancelacion = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.cancelando = false;
        this.errorCancelacion = err.error?.message || 'Error al cancelar la cita.';
        this.cdr.detectChanges();
      }
    });
  }

  // ══════════════════════════════════════════════════════════
  //  PESTAÑA: BUSCAR POR FECHA
  // ══════════════════════════════════════════════════════════

  buscarCitas() {
    if (!this.medicoId || !this.fechaBuscar) return;
    this.buscando      = true;
    this.errorBuscar   = '';
    this.citasBuscar   = [];
    this.mensajeExport = '';

    const params = new HttpParams()
      .set('doctorId', this.medicoId)
      .set('date',     this.fechaBuscar);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.citasBuscar = data || [];
          this.buscando    = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorBuscar = 'Error al buscar citas.';
          this.buscando    = false;
          this.cdr.detectChanges();
        }
      });
  }

  exportarCsv() {
    if (!this.medicoId || !this.fechaBuscar) return;
    this.exportando    = true;
    this.mensajeExport = '';
    this.cdr.detectChanges();

    const params = new HttpParams()
      .set('doctorId', this.medicoId)
      .set('date',     this.fechaBuscar);

    this.http.get(`${this.apiUrl}/appointments/export`,
      { headers: this.headers(), params, responseType: 'blob' }).subscribe({
        next: (blob: Blob) => {
          const url  = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href  = url;
          link.download = `citas_${this.fechaBuscar}.csv`;
          link.click();
          window.URL.revokeObjectURL(url);
          this.exportando    = false;
          this.mensajeExport = '✅ CSV descargado exitosamente';
          this.cdr.detectChanges();
          setTimeout(() => { this.mensajeExport = ''; this.cdr.detectChanges(); }, 4000);
        },
        error: () => {
          this.exportando    = false;
          this.mensajeExport = '❌ Error al exportar el CSV';
          this.cdr.detectChanges();
        }
      });
  }

  // ══════════════════════════════════════════════════════════
  //  PESTAÑA: AGENDAR NUEVA CITA
  // ══════════════════════════════════════════════════════════

  onEspecialidadNuevaCitaChange() {
    this.medicosFiltradosNueva = this.todosMedicos.filter(
      m => m.specialty === this.especialidadNueva
    );
    this.nuevaCita.doctorId  = 0;
    this.franjasNueva        = [];
    this.nuevaCita.startTime = '';
    this.nuevaCita.fecha     = '';

    if (this.medicosFiltradosNueva.length === 1) {
      this.nuevaCita.doctorId = this.medicosFiltradosNueva[0].id;
    }
    this.cdr.detectChanges();
  }

  buscarPacientePorCedula() {
    const cedula = this.nuevaCita.identification.trim();
    if (!cedula) return;
    this.buscandoPaciente   = true;
    this.pacienteEncontrado = false;
    this.pacienteId         = null;
    this.cdr.detectChanges();

    const params = new HttpParams().set('identification', cedula);
    this.http.get<any>(`${this.apiUrl}/patients/by-identification`,
      { headers: this.headers(), params }).subscribe({
        next: (p) => {
          this.nuevaCita.firstName = p.firstName  || '';
          this.nuevaCita.lastName  = p.lastName   || '';
          this.nuevaCita.phone     = p.phone      || '';
          this.nuevaCita.email     = p.email      || '';
          this.nuevaCita.gender    = p.gender     || '';
          this.nuevaCita.birthDate = p.birthDate  || '';
          this.pacienteId          = p.id;
          this.pacienteEncontrado  = true;
          this.buscandoPaciente    = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.nuevaCita.firstName = '';
          this.nuevaCita.lastName  = '';
          this.nuevaCita.phone     = '';
          this.nuevaCita.email     = '';
          this.nuevaCita.gender    = '';
          this.nuevaCita.birthDate = '';
          this.pacienteId          = null;
          this.pacienteEncontrado  = false;
          this.buscandoPaciente    = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarFranjasNueva() {
    if (!this.nuevaCita.doctorId || !this.nuevaCita.fecha) return;
    this.franjasNueva        = [];
    this.nuevaCita.startTime = '';

    const params = new HttpParams()
      .set('doctorId', this.nuevaCita.doctorId)
      .set('date',     this.nuevaCita.fecha);

    this.http.get<any[]>(`${this.apiUrl}/appointments/slots`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.franjasNueva = data.filter(f => f.available);
          this.cdr.detectChanges();
        },
        error: () => { }
      });
  }

  crearCita() {
    this.cargandoCita = true;
    this.errorCita    = '';

    if (this.pacienteId) {
      this.agendarCitaConPaciente(this.pacienteId);
      return;
    }

    const datosPaciente = {
      identification : this.nuevaCita.identification,
      firstName      : this.nuevaCita.firstName,
      lastName       : this.nuevaCita.lastName,
      phone          : this.nuevaCita.phone,
      gender         : this.nuevaCita.gender,
      birthDate      : this.nuevaCita.birthDate || null,
      email          : this.nuevaCita.email     || null,
      username       : this.nuevaCita.identification,
      password       : this.nuevaCita.identification
    };

    this.http.post<any>(`${this.apiUrl}/patients/register`,
      datosPaciente, { headers: this.headers() }).subscribe({
        next: (p) => this.agendarCitaConPaciente(p.id),
        error: (err) => {
          this.errorCita    = err.error?.message || 'Error al registrar paciente';
          this.cargandoCita = false;
          this.cdr.detectChanges();
        }
      });
  }

  private agendarCitaConPaciente(patientId: number) {
    const datosCita = {
      doctorId  : this.nuevaCita.doctorId,
      patientId : patientId,
      date      : this.nuevaCita.fecha,
      startTime : this.nuevaCita.startTime,
      notes     : 'Cita agendada por el médico'
    };

    this.http.post(`${this.apiUrl}/appointments`,
      datosCita, { headers: this.headers() }).subscribe({
        next: () => {
          this.cargandoCita = false;
          this.citaCreada   = true;
          this.cdr.detectChanges();
          this.cargarCitasHoy();
          this.cargarFranjasNueva();
        },
        error: (err) => {
          this.errorCita    = err.error?.message || 'Error al crear cita';
          this.cargandoCita = false;
          this.cdr.detectChanges();
        }
      });
  }

  reiniciarFormulario() {
    this.citaCreada         = false;
    this.pacienteEncontrado = false;
    this.pacienteId         = null;
    this.especialidadNueva  = '';
    this.medicosFiltradosNueva = [];
    this.nuevaCita = {
      identification : '',
      firstName      : '',
      lastName       : '',
      phone          : '',
      gender         : '',
      birthDate      : '',
      email          : '',
      doctorId       : this.medicoId || 0,
      fecha          : '',
      startTime      : ''
    };
    this.franjasNueva = [];
    this.cdr.detectChanges();
  }

  // ══════════════════════════════════════════════════════════
  //  PESTAÑA: REAGENDAR CITA
  // ══════════════════════════════════════════════════════════

  reagBuscarCitas() {
    if (!this.medicoId || !this.reagBuscarFecha) return;
    this.reagBuscando = true;
    this.reagError    = '';
    this.reagCitas    = [];
    this.reagCitaSeleccionada = null;
    this.reagNuevaFecha       = '';
    this.reagFranjas          = [];
    this.reagNuevaHora        = '';

    const params = new HttpParams()
      .set('doctorId', this.medicoId)
      .set('date',     this.reagBuscarFecha);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.reagCitas    = (data || []).filter(c => c.status === 'SCHEDULED');
          this.reagBuscando = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.reagError    = 'Error al buscar citas.';
          this.reagBuscando = false;
          this.cdr.detectChanges();
        }
      });
  }

  seleccionarCitaReagendar(cita: any) {
    this.reagCitaSeleccionada = cita;
    this.reagNuevaFecha       = '';
    this.reagFranjas          = [];
    this.reagNuevaHora        = '';
    this.reagExito            = '';
    this.reagErrorGuardar     = '';
    this.cdr.detectChanges();
  }

  cargarFranjasReagendar() {
    if (!this.reagCitaSeleccionada || !this.reagNuevaFecha) return;
    this.reagCargandoFranjas = true;
    this.reagFranjas         = [];
    this.reagNuevaHora       = '';

    const params = new HttpParams()
      .set('doctorId', this.reagCitaSeleccionada.doctorId)
      .set('date',     this.reagNuevaFecha);

    this.http.get<any[]>(`${this.apiUrl}/appointments/slots`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.reagFranjas         = data.filter(f => f.available);
          this.reagCargandoFranjas = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.reagCargandoFranjas = false;
          this.cdr.detectChanges();
        }
      });
  }

  confirmarReagendar() {
    if (!this.reagCitaSeleccionada || !this.reagNuevaFecha || !this.reagNuevaHora) return;
    this.reagGuardando    = true;
    this.reagExito        = '';
    this.reagErrorGuardar = '';

    const payload = {
      newDate: this.reagNuevaFecha,
      newStartTime: this.reagNuevaHora,
      reason: this.reagCitaSeleccionada.notes || 'Reagendamiento solicitado por el médico'
    };

    this.http.put(`${this.apiUrl}/appointments/${this.reagCitaSeleccionada.id}/reschedule`,
      payload, { headers: this.headers() }).subscribe({
        next: () => {
          this.reagGuardando    = false;
          this.reagExito        = `✅ Cita de ${this.reagCitaSeleccionada.patientName} reagendada para el ${this.reagNuevaFecha} a las ${this.reagNuevaHora}`;
          this.reagCitaSeleccionada = null;
          this.reagFranjas          = [];
          this.reagNuevaHora        = '';
          this.reagBuscarCitas();
          this.cargarCitasHoy();
          this.cdr.detectChanges();
          setTimeout(() => { this.reagExito = ''; this.cdr.detectChanges(); }, 5000);
        },
        error: (err) => {
          this.reagGuardando    = false;
          this.reagErrorGuardar = err.error?.message || 'Error al reagendar la cita';
          this.cdr.detectChanges();
        }
      });
  }

  // ── Helpers ────────────────────────────────────────────────
  etiquetaEstado(status: string): string {
    return { SCHEDULED: 'Programada', COMPLETED: 'Completada', CANCELLED: 'Cancelada' }
      [status] || status;
  }

  colorEstado(status: string): string {
    return { SCHEDULED: 'bg-success', COMPLETED: 'bg-secondary', CANCELLED: 'bg-danger' }
      [status] || 'bg-secondary';
  }

  cerrarSesion() {
    localStorage.clear();
    this.router.navigate(['/login']);
  }
}