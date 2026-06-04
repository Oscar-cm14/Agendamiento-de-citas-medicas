// ============================================================
// medico.ts  –  Panel del Médico / Terapista (Versión Integrada)
// ============================================================

// ============================================================
// medico.ts  –  Panel del Médico (Versión Corregida)
// ============================================================

import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';
import { environment } from '../../environments/environment';

import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatNativeDateModule } from '@angular/material/core';

@Component({
  selector: 'app-medico',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink, MatDatepickerModule, MatInputModule, MatFormFieldModule, MatNativeDateModule],
  templateUrl: './medico.html',
  styleUrl: './medico.css'
})
export class Medico implements OnInit {

  pestanaActiva = 'agenda';

  // Info del médico autenticado
  medicoNombre = '';
  medicoEspecialidad = '';
  medicoSkills = '';
  medicoId: number | null = null;

  // ── Pestaña: Agenda ──
  fechaAgenda = '';
  citasAgenda: any[] = [];
  cargandoAgenda = false;
  errorAgenda = '';
  mensajeAccion = '';
  procesandoCitaId: number | null = null;

  // ── Pestaña: Buscar citas por fecha ───────────────────────
  fechaBuscarObj: Date | null = null;
  fechaBuscar = '';
  citasBuscar: any[] = [];
  buscando = false;
  errorBuscar = '';
  exportando = false;
  mensajeExport = '';

  // ── Cancelar cita (modal) ──
  citaCancelando: any = null;
  motivoCancelacion = '';
  errorCancelacion = '';
  cancelando = false;
  mensajeCancelacion = '';

  // ── Pestaña: Agendar nueva cita ──
  todosMedicos: any[] = [];
  especialidades: string[] = [];
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
    startTime: '',
    prioritaria: false,
    motivoPrioridad: ''
  };
  nuevaCitaFechaObj: Date | null = null;
  nuevaCitaNacimientoObj: Date | null = null;
  especialidadNueva = '';
  medicosFiltradosNueva: any[] = [];
  franjasNueva: any[] = [];
  citaCreada = false;
  errorCita = '';
  cargandoCita = false;
  buscandoPaciente = false;
  pacienteEncontrado = false;
  pacienteId: number | null = null;

  // ── Pestaña: Reagendar cita ────────────────────────────────
  reagBuscarFechaObj: Date | null = null;
  reagBuscarFecha = '';
  reagCitas: any[] = [];
  reagBuscando = false;
  reagError = '';
  reagCitaSeleccionada: any = null;
  reagNuevaFechaObj: Date | null = null;
  reagNuevaFecha = '';
  reagFranjas: any[] = [];
  reagNuevaHora = '';
  reagCargandoFranjas = false;
  reagGuardando = false;
  reagExito = '';
  reagErrorGuardar = '';

  // ── HORARIOS Y FESTIVOS ──
  doctorWorkingDays: string[] = [];
  minDate = new Date();
  festivosColombiaStr: string[] = [
    '2024-01-01', '2024-01-08', '2024-03-25', '2024-03-28', '2024-03-29', '2024-05-01', '2024-05-13', '2024-06-03', '2024-06-10', '2024-07-01', '2024-07-20', '2024-08-07', '2024-08-19', '2024-10-14', '2024-11-04', '2024-11-11', '2024-12-08', '2024-12-25',
    '2025-01-01', '2025-01-06', '2025-03-24', '2025-04-17', '2025-04-18', '2025-05-01', '2025-06-02', '2025-06-23', '2025-06-30', '2025-07-20', '2025-08-07', '2025-08-18', '2025-10-13', '2025-11-03', '2025-11-17', '2025-12-08', '2025-12-25',
    '2026-01-01', '2026-01-12', '2026-03-23', '2026-04-02', '2026-04-03', '2026-05-01', '2026-05-18', '2026-06-08', '2026-06-15', '2026-06-29', '2026-07-20', '2026-08-07', '2026-08-17', '2026-10-12', '2026-11-02', '2026-11-16', '2026-12-08', '2026-12-25'
  ];

  // ── Pestaña: Prioritarias ──
  priorFechaDesde = '';
  priorFechaHasta = '';
  priorCitas: any[] = [];
  priorBuscando = false;
  priorError = '';
  priorMensaje = '';
  priorProcesandoId: number | null = null;
  priorCitaEditar: any = null;
  priorNuevoNivel: 'HIGH' | 'MEDIUM' | 'LOW' | '' = '';

  // ── Pestaña: Perfil ──
  perfilDatos: any = null;
  perfilBackup: any = null;
  perfilEditando = false;
  perfilGuardando = false;
  perfilCargando = false;
  perfilError = '';
  perfilMensaje = '';
  perfilNuevoPassword = '';
  perfilConfirmPassword = '';

  get perfilPasswordMismatch(): boolean {
    return !!this.perfilNuevoPassword &&
      this.perfilNuevoPassword !== this.perfilConfirmPassword;
  }

  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.fechaAgenda = new Date().toISOString().split('T')[0];
      this.cargarDatosMedico();
      this.cargarTodosMedicos();
    }
  }

  private headers(): HttpHeaders {
    const token = localStorage.getItem('token') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  cargarDatosMedico() {
    this.http.get<any>(`${this.apiUrl}/doctors/me`, { headers: this.headers() }).subscribe({
      next: (data) => {
        this.medicoId = data.id;
        this.medicoNombre = data.fullName || `${data.firstName} ${data.lastName}`;
        this.medicoEspecialidad = data.specialty || '';
        this.medicoSkills = data.skills || '';
        localStorage.setItem('nombreUsuario', this.medicoNombre);
        localStorage.setItem('userId', String(data.id));
        this.nuevaCita.doctorId = data.id;
        this.cdr.detectChanges();
        this.cargarAgenda();
      },
      error: () => {
        this.medicoNombre = localStorage.getItem('nombreUsuario') || '';
        const idGuardado = localStorage.getItem('userId');
        if (idGuardado) this.medicoId = +idGuardado;
        if (this.medicoId) this.nuevaCita.doctorId = this.medicoId;
        this.cargarAgenda();
      }
    });
  }

  cargarTodosMedicos() {
    this.http.get<any[]>(`${this.apiUrl}/doctors`, { headers: this.headers() }).subscribe({
      next: (data) => {
        this.todosMedicos = data;
        const raw = data.map((m: any) => m.specialty).filter((s: any) => s && s.trim() !== '');
        this.especialidades = [...new Set<string>(raw)].sort();
        this.cdr.detectChanges();
      },
      error: () => { }
    });
  }

  // ══════════════════════════════════════════════════════════
  //  PESTAÑA: AGENDA
  // ══════════════════════════════════════════════════════════

  cargarAgenda() {
    if (!this.medicoId || !this.fechaAgenda) return;
    this.cargandoAgenda = true;
    this.errorAgenda = '';
    this.citasAgenda = [];

    const params = new HttpParams()
      .set('doctorId', this.medicoId)
      .set('date', this.fechaAgenda);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.citasAgenda = (data || []).sort((a, b) => {
            if (a.priority && !b.priority) return -1;
            if (!a.priority && b.priority) return 1;
            const orden: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };
            const diffUrg = (orden[a.urgencyLevel] ?? 3) - (orden[b.urgencyLevel] ?? 3);
            if (diffUrg !== 0) return diffUrg;
            return (a.startTime || '').localeCompare(b.startTime || '');
          });
          this.cargandoAgenda = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorAgenda = 'Error al cargar la agenda.';
          this.cargandoAgenda = false;
          this.cdr.detectChanges();
        }
      });
  }

  marcarAtendida(citaId: number) {
    if (!confirm('¿Confirma que esta cita fue atendida?')) return;
    this.procesandoCitaId = citaId;
    this.mensajeAccion = '';
    this.cdr.detectChanges();

    this.http.patch(
      `${this.apiUrl}/appointments/${citaId}/complete`,
      {},
      { headers: this.headers() }
    ).subscribe({
      next: () => {
        this.procesandoCitaId = null;
        this.mensajeAccion = '✅ Cita marcada como atendida.';
        this.cdr.detectChanges();
        this.cargarAgenda();
        if (this.fechaBuscar) this.buscarCitas();
        setTimeout(() => { this.mensajeAccion = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.procesandoCitaId = null;
        this.mensajeAccion = '❌ ' + (err.error?.message || 'Error al actualizar la cita.');
        this.cdr.detectChanges();
        setTimeout(() => { this.mensajeAccion = ''; this.cdr.detectChanges(); }, 5000);
      }
    });
  }

  etiquetaEstado(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'Programada';
      case 'COMPLETED': return 'Atendida';
      case 'CANCELLED': return 'Cancelada';
      case 'NO_SHOW': return 'No asistió';
      default: return status;
    }
  }

  cambiarEstadoCita(citaId: number, event: any, contexto: 'agenda' | 'buscar' | 'prioritaria' = 'agenda') {
    const nuevoEstado = event.target.value;
    if (!confirm('¿Confirma cambiar el estado de la cita a ' + this.etiquetaEstado(nuevoEstado) + '?')) {
      if (contexto === 'agenda') this.cargarAgenda();
      else if (contexto === 'buscar') this.buscarCitas();
      else if (contexto === 'prioritaria') this.buscarCitasPrioritarias();
      return;
    }
    
    this.http.patch(
      `${this.apiUrl}/appointments/${citaId}/status`,
      { status: nuevoEstado },
      { headers: this.headers() }
    ).subscribe({
      next: () => {
        this.mensajeAccion = '✅ Estado actualizado correctamente.';
        this.cdr.detectChanges();
        if (contexto === 'agenda') this.cargarAgenda();
        else if (contexto === 'buscar') this.buscarCitas();
        else if (contexto === 'prioritaria') this.buscarCitasPrioritarias();
        setTimeout(() => { this.mensajeAccion = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.mensajeAccion = '❌ ' + (err.error?.message || 'Error al actualizar el estado.');
        this.cdr.detectChanges();
        if (contexto === 'agenda') this.cargarAgenda();
        else if (contexto === 'buscar') this.buscarCitas();
        else if (contexto === 'prioritaria') this.buscarCitasPrioritarias();
        setTimeout(() => { this.mensajeAccion = ''; this.cdr.detectChanges(); }, 5000);
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
        this.cargarAgenda();
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

  formatDateStr(d: Date): string {
    if (!d) return '';
    const year = d.getFullYear();
    const month = ('0' + (d.getMonth() + 1)).slice(-2);
    const day = ('0' + d.getDate()).slice(-2);
    return `${year}-${month}-${day}`;
  }

  dateFilter = (d: Date | null): boolean => {
    if (!d) return false;
    const day = d.getDay();
    if (day === 0 || day === 6) return false;
    const dateString = this.formatDateStr(d);
    if (this.festivosColombiaStr.includes(dateString)) return false;
    if (this.doctorWorkingDays.length > 0) {
      const daysMap = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
      if (!this.doctorWorkingDays.includes(daysMap[day])) return false;
    }
    return true;
  };

  dateClass = (d: Date): string => {
    const dateString = this.formatDateStr(d);
    return this.festivosColombiaStr.includes(dateString) ? 'holiday-date' : '';
  };

  onFechaBuscarChange() {
    this.fechaBuscar = this.fechaBuscarObj ? this.formatDateStr(this.fechaBuscarObj) : '';
  }

  onNuevaCitaNacimientoChange() {
    this.nuevaCita.birthDate = this.nuevaCitaNacimientoObj ? this.formatDateStr(this.nuevaCitaNacimientoObj) : '';
  }

  onNuevaCitaFechaChange() {
    this.nuevaCita.fecha = this.nuevaCitaFechaObj ? this.formatDateStr(this.nuevaCitaFechaObj) : '';
    if (this.nuevaCita.fecha) this.cargarFranjasNueva();
  }

  onReagBuscarFechaChange() {
    this.reagBuscarFecha = this.reagBuscarFechaObj ? this.formatDateStr(this.reagBuscarFechaObj) : '';
  }

  onReagNuevaFechaChange() {
    this.reagNuevaFecha = this.reagNuevaFechaObj ? this.formatDateStr(this.reagNuevaFechaObj) : '';
    if (this.reagNuevaFecha) this.cargarFranjasReagendar();
  }

  cargarHorarioMedico(doctorId: number) {
    if (!doctorId) {
      this.doctorWorkingDays = [];
      return;
    }
    this.http.get<any>(`${this.apiUrl}/doctors/schedules/${doctorId}`, { headers: this.headers() })
      .subscribe({
        next: (horario) => {
          this.doctorWorkingDays = horario.workingDays || [];
        },
        error: () => this.doctorWorkingDays = []
      });
  }

  buscarCitas() {
    if (!this.medicoId || !this.fechaBuscar) return;
    this.buscando = true;
    this.errorBuscar = '';
    this.citasBuscar = [];
    this.mensajeExport = '';

    const params = new HttpParams()
      .set('doctorId', this.medicoId)
      .set('date', this.fechaBuscar);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.citasBuscar = data || [];
          this.buscando = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorBuscar = 'Error al buscar citas.';
          this.buscando = false;
          this.cdr.detectChanges();
        }
      });
  }

  exportarCsv() {
    if (!this.medicoId || !this.fechaBuscar) return;
    this.exportando = true;
    this.mensajeExport = '';
    this.cdr.detectChanges();

    const params = new HttpParams()
      .set('doctorId', this.medicoId)
      .set('date', this.fechaBuscar);

    this.http.get(`${this.apiUrl}/appointments/export`,
      { headers: this.headers(), params, responseType: 'blob' }).subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `citas_${this.fechaBuscar}.csv`;
          link.click();
          window.URL.revokeObjectURL(url);
          this.exportando = false;
          this.mensajeExport = '✅ CSV descargado exitosamente';
          this.cdr.detectChanges();
          setTimeout(() => { this.mensajeExport = ''; this.cdr.detectChanges(); }, 4000);
        },
        error: () => {
          this.exportando = false;
          this.mensajeExport = '❌ Error al exportar el CSV';
          this.cdr.detectChanges();
        }
      });
  }

  // ══════════════════════════════════════════════════════════
  //  PESTAÑA: AGENDAR NUEVA CITA
  // ══════════════════════════════════════════════════════════

  onEspecialidadNuevaCitaChange() {
    this.medicosFiltradosNueva = this.todosMedicos.filter(m => m.specialty === this.especialidadNueva);
    this.nuevaCita.doctorId = 0;
    this.franjasNueva = [];
    this.nuevaCitaFechaObj = null;
    this.nuevaCita.fecha = '';
    this.nuevaCita.startTime = '';

    if (this.medicosFiltradosNueva.length === 1) {
      this.nuevaCita.doctorId = this.medicosFiltradosNueva[0].id;
      this.cargarHorarioMedico(this.nuevaCita.doctorId);
    } else if (this.medicosFiltradosNueva.length === 0 && this.medicoId) {
      this.nuevaCita.doctorId = this.medicoId;
      this.cargarHorarioMedico(this.nuevaCita.doctorId);
    }
    this.cdr.detectChanges();
  }

  onDoctorChangeNuevaCita() {
    this.nuevaCitaFechaObj = null;
    this.nuevaCita.fecha = '';
    this.franjasNueva = [];
    this.nuevaCita.startTime = '';
    this.cargarHorarioMedico(this.nuevaCita.doctorId);
  }

  buscarPacientePorCedula() {
    const cedula = this.nuevaCita.identification.trim();
    if (!cedula) return;
    this.buscandoPaciente = true;
    this.pacienteEncontrado = false;
    this.pacienteId = null;
    this.cdr.detectChanges();

    const params = new HttpParams().set('identification', cedula);
    this.http.get<any>(`${this.apiUrl}/patients/by-identification`,
      { headers: this.headers(), params }).subscribe({
        next: (paciente) => {
          this.nuevaCita.firstName = paciente.firstName || '';
          this.nuevaCita.lastName = paciente.lastName || '';
          this.nuevaCita.phone = paciente.phone || '';
          this.nuevaCita.email = paciente.email || '';
          this.nuevaCita.gender = paciente.gender ? paciente.gender.toUpperCase() : '';
          this.nuevaCita.birthDate = paciente.birthDate || '';
          this.nuevaCitaNacimientoObj = paciente.birthDate ? new Date(paciente.birthDate + 'T00:00:00') : null;
          this.pacienteId = paciente.id;
          this.pacienteEncontrado = true;
          this.buscandoPaciente = false;
          this.cdr.detectChanges();
          if (this.nuevaCita.fecha) this.cargarFranjasNueva();
        },
        error: () => {
          this.nuevaCita.firstName = '';
          this.nuevaCita.lastName = '';
          this.nuevaCita.phone = '';
          this.nuevaCita.email = '';
          this.nuevaCita.gender = '';
          this.nuevaCita.birthDate = '';
          this.nuevaCitaNacimientoObj = null;
          this.pacienteId = null;
          this.pacienteEncontrado = false;
          this.buscandoPaciente = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarFranjasNueva() {
    const docId = this.nuevaCita.doctorId || this.medicoId;
    if (!docId || !this.nuevaCita.fecha) {
      this.franjasNueva = [];
      return;
    }
    this.franjasNueva = [];
    this.nuevaCita.startTime = '';
    this.cdr.detectChanges();

    const params = new HttpParams()
      .set('doctorId', docId)
      .set('date', this.nuevaCita.fecha);

    this.http.get<any[]>(`${this.apiUrl}/appointments/slots`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          if (data && data.length > 0) {
            if (typeof data[0] === 'string') {
              // backend retorna strings simples
              this.franjasNueva = data.map(slot => ({ time: slot, available: true }));
            } else {
              // backend retorna AvailableSlotResponse {startTime, endTime, available}
              this.franjasNueva = data
                .filter(f => f.available !== false)
                .map(f => ({ time: f.startTime, available: true }));
            }
          } else {
            this.franjasNueva = [];
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.franjasNueva = [];
          this.cdr.detectChanges();
        }
      });
  }

  crearCita() {
    this.cargandoCita = true;
    this.errorCita = '';

    if (this.pacienteId) {
      this.agendarCitaConPaciente(this.pacienteId);
      return;
    }

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
        next: (p) => this.agendarCitaConPaciente(p.id),
        error: (err) => {
          this.errorCita = err.error?.message || 'Error al registrar paciente';
          this.cargandoCita = false;
          this.cdr.detectChanges();
        }
      });
  }

  private agendarCitaConPaciente(patientId: number) {
    const docId = this.nuevaCita.doctorId || this.medicoId || 0;
    const datosCita: any = {
      doctorId: docId,
      patientId: patientId,
      date: this.nuevaCita.fecha,
      startTime: this.nuevaCita.startTime,
      notes: 'Cita agendada por el médico',
      priority: this.nuevaCita.prioritaria,
      priorityReason: this.nuevaCita.prioritaria ? this.nuevaCita.motivoPrioridad : ''
    };

    this.http.post(`${this.apiUrl}/appointments`,
      datosCita, { headers: this.headers() }).subscribe({
        next: () => {
          this.cargandoCita = false;
          this.citaCreada = true;
          this.cdr.detectChanges();
          this.cargarAgenda();
          this.cargarFranjasNueva();
        },
        error: (err) => {
          this.errorCita = err.error?.message || 'Error al crear cita';
          this.cargandoCita = false;
          this.cdr.detectChanges();
        }
      });
  }

  reiniciarFormulario() {
    this.citaCreada = false;
    this.pacienteEncontrado = false;
    this.pacienteId = null;
    this.especialidadNueva = '';
    this.medicosFiltradosNueva = [];
    this.nuevaCita = {
      identification: '',
      firstName: '',
      lastName: '',
      phone: '',
      gender: '',
      birthDate: '',
      email: '',
      doctorId: this.medicoId || 0,
      fecha: '',
      startTime: '',
      prioritaria: false,
      motivoPrioridad: ''
    };
    this.nuevaCitaFechaObj = null;
    this.nuevaCitaNacimientoObj = null;
    this.franjasNueva = [];
    this.cdr.detectChanges();
  }

  // ══════════════════════════════════════════════════════════
  //  PESTAÑA: REAGENDAR CITA
  // ══════════════════════════════════════════════════════════

  reagBuscarCitas() {
    if (!this.medicoId || !this.reagBuscarFecha) return;
    this.reagBuscando = true;
    this.reagError = '';
    this.reagCitas = [];
    this.reagCitaSeleccionada = null;
    this.reagNuevaFecha = '';
    this.reagFranjas = [];
    this.reagNuevaHora = '';

    const params = new HttpParams()
      .set('doctorId', this.medicoId)
      .set('date', this.reagBuscarFecha);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.reagCitas = (data || []).filter(c => c.status === 'SCHEDULED');
          this.reagBuscando = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.reagError = 'Error al buscar citas.';
          this.reagBuscando = false;
          this.cdr.detectChanges();
        }
      });
  }

  seleccionarCitaReagendar(cita: any) {
    this.reagCitaSeleccionada = cita;
    this.reagNuevaFechaObj = null;
    this.reagNuevaFecha = '';
    this.reagFranjas = [];
    this.reagNuevaHora = '';
    this.reagExito = '';
    this.reagErrorGuardar = '';
    // Load doctor's schedule to filter the calendar correctly for the new date
    this.cargarHorarioMedico(cita.doctorId || this.medicoId);
    this.cdr.detectChanges();
  }

  cargarFranjasReagendar() {
    if (!this.reagCitaSeleccionada || !this.reagNuevaFecha) return;
    this.reagCargandoFranjas = true;
    this.reagFranjas = [];
    this.reagNuevaHora = '';

    const params = new HttpParams()
      .set('doctorId', this.reagCitaSeleccionada.doctorId)
      .set('date', this.reagNuevaFecha);

    this.http.get<any[]>(`${this.apiUrl}/appointments/slots`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          // Normalizar siempre a {time, available} para que el template use f.time
          if (data && data.length > 0) {
            if (typeof data[0] === 'string') {
              this.reagFranjas = data.map(s => ({ time: s, available: true }));
            } else {
              this.reagFranjas = (data || [])
                .filter(f => f.available)
                .map(f => ({ time: f.startTime, available: true }));
            }
          } else {
            this.reagFranjas = [];
          }
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
    this.reagGuardando = true;
    this.reagExito = '';
    this.reagErrorGuardar = '';

    const payload = {
      newDate: this.reagNuevaFecha,
      newStartTime: this.reagNuevaHora,
      reason: this.reagCitaSeleccionada.notes || 'Reagendamiento solicitado por el médico'
    };

    this.http.put(`${this.apiUrl}/appointments/${this.reagCitaSeleccionada.id}/reschedule`,
      payload, { headers: this.headers() }).subscribe({
        next: () => {
          this.reagGuardando = false;
          this.reagExito = `✅ Cita de ${this.reagCitaSeleccionada.patientName} reagendada para el ${this.reagNuevaFecha} a las ${this.reagNuevaHora}`;
          this.reagCitaSeleccionada = null;
          this.reagNuevaFechaObj = null;
          this.reagFranjas = [];
          this.reagNuevaHora = '';
          this.reagBuscarCitas();
          this.cargarAgenda();
          this.cdr.detectChanges();
          setTimeout(() => { this.reagExito = ''; this.cdr.detectChanges(); }, 5000);
        },
        error: (err) => {
          this.reagGuardando = false;
          this.reagErrorGuardar = err.error?.message || 'Error al reagendar la cita';
          this.cdr.detectChanges();
        }
      });
  }

  // ══════════════════════════════════════════════════════════
  //  PESTAÑA: PRIORITARIAS
  // ══════════════════════════════════════════════════════════

  buscarCitasPrioritarias() {
    if (!this.priorFechaDesde) {
      this.priorError = 'Debe ingresar al menos la fecha de inicio.';
      return;
    }
    this.priorBuscando = true;
    this.priorError = '';
    this.priorCitas = [];
    this.priorMensaje = '';
    this.cdr.detectChanges();

    let params = new HttpParams()
      .set('dateFrom', this.priorFechaDesde)
      .set('dateTo', this.priorFechaHasta || this.priorFechaDesde);

    if (this.medicoId) {
      params = params.set('doctorId', this.medicoId);
    }

    this.http.get<any[]>(`${this.apiUrl}/appointments/priority`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.priorCitas = data || [];
          this.priorBuscando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.priorError = err.error?.message || 'Error al buscar citas prioritarias.';
          this.priorBuscando = false;
          this.cdr.detectChanges();
        }
      });
  }

  abrirEditarUrgencia(cita: any) {
    this.priorCitaEditar = cita;
    this.priorNuevoNivel = cita.urgencyLevel || 'MEDIUM';
    this.cdr.detectChanges();
  }

  cancelarEditarUrgencia() {
    this.priorCitaEditar = null;
    this.priorNuevoNivel = '';
    this.cdr.detectChanges();
  }

  guardarNivelUrgencia() {
    if (!this.priorCitaEditar || !this.priorNuevoNivel) return;
    this.priorProcesandoId = this.priorCitaEditar.id;
    this.priorMensaje = '';
    this.cdr.detectChanges();

    const payload = {
      priority: true,
      urgencyLevel: this.priorNuevoNivel,
      priorityReason: this.priorCitaEditar.priorityReason || ''
    };

    this.http.patch(
      `${this.apiUrl}/appointments/${this.priorCitaEditar.id}/priority`,
      payload, { headers: this.headers() }
    ).subscribe({
      next: () => {
        this.priorProcesandoId = null;
        this.priorMensaje = `✅ Nivel de urgencia actualizado a "${this.textoUrgencia(this.priorNuevoNivel)}".`;
        const idx = this.priorCitas.findIndex(c => c.id === this.priorCitaEditar.id);
        if (idx >= 0) this.priorCitas[idx].urgencyLevel = this.priorNuevoNivel;
        this.cancelarEditarUrgencia();
        this.cdr.detectChanges();
        setTimeout(() => { this.priorMensaje = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.priorProcesandoId = null;
        this.priorMensaje = '❌ ' + (err.error?.message || 'Error al actualizar el nivel de urgencia.');
        this.cdr.detectChanges();
        setTimeout(() => { this.priorMensaje = ''; this.cdr.detectChanges(); }, 5000);
      }
    });
  }

  contarPorNivel(nivel: string): number {
    return this.priorCitas.filter(c => c.urgencyLevel === nivel).length;
  }

  // ══════════════════════════════════════════════════════════
  //  PESTAÑA: PERFIL — usa /doctors/me
  // ══════════════════════════════════════════════════════════

  cargarPerfil() {
    if (this.perfilDatos) return;
    this.perfilCargando = true;
    this.perfilError = '';
    this.cdr.detectChanges();

    this.http.get<any>(`${this.apiUrl}/doctors/me`, { headers: this.headers() }).subscribe({
      next: (data) => {
        this.perfilDatos = { ...data };
        this.perfilBackup = { ...data };
        this.perfilCargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.perfilError = err.error?.message || 'Error al cargar los datos del perfil.';
        this.perfilCargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  guardarPerfil() {
    if (this.perfilPasswordMismatch) return;
    this.perfilGuardando = true;
    this.perfilMensaje = '';
    this.cdr.detectChanges();

    const payload: any = {
      firstName: this.perfilDatos.firstName,
      lastName: this.perfilDatos.lastName,
      email: this.perfilDatos.email,
      phone: this.perfilDatos.phone
    };
    if (this.perfilNuevoPassword.trim()) {
      payload.password = this.perfilNuevoPassword.trim();
    }

    // Usa PUT /doctors/me para que el backend identifique al médico por su token
    this.http.put(`${this.apiUrl}/doctors/me`, payload, { headers: this.headers() }).subscribe({
      next: () => {
        this.perfilGuardando = false;
        this.perfilEditando = false;
        this.perfilMensaje = '✅ Perfil actualizado correctamente.';
        this.medicoNombre = `${this.perfilDatos.firstName} ${this.perfilDatos.lastName}`;
        localStorage.setItem('nombreUsuario', this.medicoNombre);
        this.perfilNuevoPassword = '';
        this.perfilConfirmPassword = '';
        this.perfilBackup = { ...this.perfilDatos };
        this.cdr.detectChanges();
        setTimeout(() => { this.perfilMensaje = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.perfilGuardando = false;
        this.perfilMensaje = '❌ ' + (err.error?.message || 'Error al guardar los cambios.');
        this.cdr.detectChanges();
        setTimeout(() => { this.perfilMensaje = ''; this.cdr.detectChanges(); }, 5000);
      }
    });
  }

  cancelarEditarPerfil() {
    this.perfilDatos = { ...this.perfilBackup };
    this.perfilEditando = false;
    this.perfilNuevoPassword = '';
    this.perfilConfirmPassword = '';
    this.perfilMensaje = '';
    this.cdr.detectChanges();
  }

  // ══════════════════════════════════════════════════════════
  //  UTILIDADES
  // ══════════════════════════════════════════════════════════

  badgeUrgencia(nivel: string): string {
    switch (nivel) {
      case 'HIGH': return 'bg-danger';
      case 'MEDIUM': return 'bg-warning text-dark';
      case 'LOW': return 'bg-info text-dark';
      default: return 'bg-secondary';
    }
  }

  textoUrgencia(nivel: string): string {
    const mapa: Record<string, string> = { HIGH: 'Alta', MEDIUM: 'Media', LOW: 'Baja' };
    return mapa[nivel] || nivel || '—';
  }

  get citasPrioritariasHoy(): number {
    return this.citasAgenda.filter(c => c.priority).length;
  }

  cerrarSesion() {
    localStorage.clear();
    this.router.navigate(['/login']);
  }
}