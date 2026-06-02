// ============================================================
// agendador.ts  –  Panel del Agendador (Versión Integrada)
// ============================================================

import { Component, Inject, PLATFORM_ID, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-agendador',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './agendador.html'
})
export class Agendador implements OnInit {

  pestanaActiva = 'listar';
  medicos: any[] = [];
  especialidades: string[] = [];

  agendadorNombre = '';

  // ── Variables pestaña LISTAR ──
  especialidadBuscar = '';
  medicosFiltradosBuscar: any[] = [];
  doctorIdBuscar = 0;
  fechaBuscar = '';
  citas: any[] = [];
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

  // ── Variables pestaña REAGENDAR ──
  reagEspecialidad = '';
  reagMedicosFiltrados: any[] = [];
  reagDoctorId = 0;
  reagBuscarFecha = '';
  reagCitas: any[] = [];
  reagBuscando = false;
  reagError = '';
  reagCitaSeleccionada: any = null;
  reagNuevaFecha = '';
  reagFranjas: any[] = [];
  reagNuevaHora = '';
  reagCargandoFranjas = false;
  reagGuardando = false;
  reagExito = '';
  reagErrorGuardar = '';

  // ── Variables pestaña CREAR ──
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
  especialidadNuevaCita = '';
  medicosFiltradosNueva: any[] = [];
  franjas: any[] = [];
  citaCreada = false;
  errorCita = '';
  cargandoCita = false;
  buscandoPaciente = false;
  pacienteEncontrado = false;
  pacienteId: number | null = null;

  // ── Variables pestaña PRIORITARIAS ──
  priorEspecialidad = '';
  priorMedicosFiltrados: any[] = [];
  priorDoctorId = 0;
  priorFechaDesde = '';
  priorFechaHasta = '';
  priorCitas: any[] = [];
  priorBuscando = false;
  priorError = '';
  priorMensaje = '';
  priorProcesandoId: number | null = null;

  // ── Variables pestaña PERFIL ──
  agendadorId = 0;
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
      this.cargarNombreAgendador();
      this.cargarMedicos();
    }
  }

  private headers(): HttpHeaders {
    const token = localStorage.getItem('token') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  cargarNombreAgendador() {
    const idGuardado = localStorage.getItem('userId');
    if (idGuardado) this.agendadorId = +idGuardado;

    try {
      const token = localStorage.getItem('token') || '';
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const givenName = payload.given_name || '';
        const familyName = payload.family_name || '';
        if (givenName || familyName) {
          this.agendadorNombre = (givenName + ' ' + familyName).trim();
        } else {
          this.agendadorNombre = payload.name || payload.preferred_username || '';
        }
        if (!this.agendadorId) {
          this.agendadorId = payload.userId || payload.schedulerId || 0;
        }
        if (this.agendadorNombre) {
          localStorage.setItem('nombreUsuario', this.agendadorNombre);
        }
      }
    } catch (e) {
      this.agendadorNombre = localStorage.getItem('nombreUsuario') || '';
    }
    this.cdr.detectChanges();
  }

  cerrarSesion() {
    localStorage.clear();
    this.router.navigate(['/']);
  }

  cargarMedicos() {
    this.http.get<any[]>(`${this.apiUrl}/doctors`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          this.medicos = data;
          const raw = data.map((m: any) => m.specialty).filter((s: any) => s && s.trim() !== '');
          this.especialidades = [...new Set<string>(raw)].sort();
          this.cdr.detectChanges();
        },
        error: () => { }
      });
  }

  onEspecialidadBuscarChange() {
    this.medicosFiltradosBuscar = this.medicos.filter(m => m.specialty === this.especialidadBuscar);
    this.doctorIdBuscar = 0;
    this.citas = [];
    this.mensajeExport = '';
  }

  onEspecialidadNuevaCitaChange() {
    this.medicosFiltradosNueva = this.medicos.filter(m => m.specialty === this.especialidadNuevaCita);
    this.nuevaCita.doctorId = 0;
    this.franjas = [];
    this.nuevaCita.startTime = '';
    this.nuevaCita.fecha = '';
    if (this.medicosFiltradosNueva.length === 1) {
      this.nuevaCita.doctorId = this.medicosFiltradosNueva[0].id;
    }
    this.cdr.detectChanges();
  }

  buscarCitas() {
    if (!this.doctorIdBuscar || !this.fechaBuscar) return;
    this.buscando = true;
    this.errorBuscar = '';
    this.citas = [];
    this.mensajeExport = '';

    const params = new HttpParams()
      .set('doctorId', this.doctorIdBuscar)
      .set('date', this.fechaBuscar);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.citas = data;
          this.buscando = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorBuscar = 'Error al buscar citas';
          this.buscando = false;
          this.cdr.detectChanges();
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
        if (this.doctorIdBuscar && this.fechaBuscar) this.buscarCitas();
        setTimeout(() => { this.mensajeCancelacion = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.cancelando = false;
        this.errorCancelacion = err.error?.message || 'Error al cancelar la cita.';
        this.cdr.detectChanges();
      }
    });
  }

  exportarCsv() {
    if (!this.doctorIdBuscar || !this.fechaBuscar) return;
    this.exportando = true;
    this.mensajeExport = '';
    this.cdr.detectChanges();

    const params = new HttpParams()
      .set('doctorId', this.doctorIdBuscar)
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
          this.nuevaCita.firstName  = paciente.firstName  || '';
          this.nuevaCita.lastName   = paciente.lastName   || '';
          this.nuevaCita.phone      = paciente.phone      || '';
          this.nuevaCita.email      = paciente.email      || '';
          this.nuevaCita.gender     = paciente.gender     || '';
          this.nuevaCita.birthDate  = paciente.birthDate  || '';
          this.pacienteId           = paciente.id;
          this.pacienteEncontrado   = true;
          this.buscandoPaciente     = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.nuevaCita.firstName  = '';
          this.nuevaCita.lastName   = '';
          this.nuevaCita.phone      = '';
          this.nuevaCita.email      = '';
          this.nuevaCita.gender     = '';
          this.nuevaCita.birthDate  = '';
          this.pacienteId           = null;
          this.pacienteEncontrado   = false;
          this.buscandoPaciente     = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarFranjas() {
    if (!this.nuevaCita.doctorId || !this.nuevaCita.fecha) return;
    this.franjas = [];
    this.nuevaCita.startTime = '';

    const params = new HttpParams()
      .set('doctorId', this.nuevaCita.doctorId)
      .set('date', this.nuevaCita.fecha);

    this.http.get<any[]>(`${this.apiUrl}/appointments/slots`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.franjas = (data || []).filter(f => f.available);
          this.cdr.detectChanges();
        },
        error: () => { }
      });
  }

  // ── CORRECCIÓN: contraseña temporal = cédula ──
  // El paciente debe registrarse en /registro para establecer su propia contraseña.
  crearCita() {
    this.cargandoCita = true;
    this.errorCita = '';

    if (this.pacienteId) {
      this.agendarCitaConPaciente(this.pacienteId);
      return;
    }

    const datosPaciente = {
      identification: this.nuevaCita.identification,
      firstName:      this.nuevaCita.firstName,
      lastName:       this.nuevaCita.lastName,
      phone:          this.nuevaCita.phone,
      gender:         this.nuevaCita.gender,
      birthDate:      this.nuevaCita.birthDate || null,
      email:          this.nuevaCita.email     || null,
      username:       this.nuevaCita.identification,
      password:       this.nuevaCita.identification  // contraseña temporal = cédula
    };

    this.http.post<any>(`${this.apiUrl}/patients/register`,
      datosPaciente, { headers: this.headers() }).subscribe({
        next: (paciente) => this.agendarCitaConPaciente(paciente.id),
        error: (err) => {
          this.errorCita   = err.error?.message || 'Error al registrar paciente';
          this.cargandoCita = false;
          this.cdr.detectChanges();
        }
      });
  }

  private agendarCitaConPaciente(patientId: number) {
    const datosCita: any = {
      doctorId:       this.nuevaCita.doctorId,
      patientId:      patientId,
      date:           this.nuevaCita.fecha,
      startTime:      this.nuevaCita.startTime,
      notes:          'Cita agendada por WhatsApp',
      priority:       this.nuevaCita.prioritaria,
      priorityReason: this.nuevaCita.prioritaria ? this.nuevaCita.motivoPrioridad : ''
    };

    this.http.post(`${this.apiUrl}/appointments`,
      datosCita, { headers: this.headers() }).subscribe({
        next: () => {
          this.cargandoCita = false;
          this.citaCreada   = true;
          this.cdr.detectChanges();
          this.cargarFranjas();
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
    this.especialidadNuevaCita   = '';
    this.medicosFiltradosNueva   = [];
    this.nuevaCita = {
      identification: '',
      firstName:      '',
      lastName:       '',
      phone:          '',
      gender:         '',
      birthDate:      '',
      email:          '',
      doctorId:       0,
      fecha:          '',
      startTime:      '',
      prioritaria:    false,
      motivoPrioridad: ''
    };
    this.franjas = [];
    this.cdr.detectChanges();
  }

  onReagEspecialidadChange() {
    this.reagMedicosFiltrados  = this.medicos.filter(m => m.specialty === this.reagEspecialidad);
    this.reagDoctorId          = 0;
    this.reagCitas             = [];
    this.reagCitaSeleccionada  = null;
  }

  reagBuscarCitas() {
    if (!this.reagDoctorId || !this.reagBuscarFecha) return;
    this.reagBuscando         = true;
    this.reagError            = '';
    this.reagCitas            = [];
    this.reagCitaSeleccionada = null;

    const params = new HttpParams()
      .set('doctorId', this.reagDoctorId)
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
          this.reagFranjas         = (data || []).filter(f => f.available);
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
      newDate:      this.reagNuevaFecha,
      newStartTime: this.reagNuevaHora,
      reason:       this.reagCitaSeleccionada.notes || 'Reagendamiento solicitado'
    };

    this.http.put(`${this.apiUrl}/appointments/${this.reagCitaSeleccionada.id}/reschedule`,
      payload, { headers: this.headers() }).subscribe({
        next: () => {
          this.reagGuardando        = false;
          this.reagExito            = `✅ Cita de ${this.reagCitaSeleccionada.patientName} reagendada para el ${this.reagNuevaFecha} a las ${this.reagNuevaHora}`;
          this.reagCitaSeleccionada = null;
          this.reagFranjas          = [];
          this.reagNuevaHora        = '';
          this.reagBuscarCitas();
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

  onPriorEspecialidadChange() {
    this.priorMedicosFiltrados = this.medicos.filter(m => m.specialty === this.priorEspecialidad);
    this.priorDoctorId         = 0;
    this.priorCitas            = [];
    this.priorError            = '';
    this.cdr.detectChanges();
  }

  buscarCitasPrioritarias() {
    if (!this.priorFechaDesde) {
      this.priorError = 'Debe ingresar al menos la fecha de inicio.';
      return;
    }
    this.priorBuscando = true;
    this.priorError    = '';
    this.priorCitas    = [];
    this.priorMensaje  = '';
    this.cdr.detectChanges();

    let params = new HttpParams()
      .set('dateFrom', this.priorFechaDesde)
      .set('dateTo',   this.priorFechaHasta || this.priorFechaDesde);

    if (this.priorDoctorId && this.priorDoctorId > 0) {
      params = params.set('doctorId', this.priorDoctorId);
    }

    this.http.get<any[]>(`${this.apiUrl}/appointments/priority`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.priorCitas    = data || [];
          this.priorBuscando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.priorError    = err.error?.message || 'Error al buscar citas prioritarias.';
          this.priorBuscando = false;
          this.cdr.detectChanges();
        }
      });
  }

  desmarcarPrioritaria(cita: any) {
    if (!confirm(`¿Desea quitar la prioridad de la cita de ${cita.patientName}?`)) return;
    this.priorProcesandoId = cita.id;
    this.priorMensaje      = '';
    this.cdr.detectChanges();

    const payload = { priority: false, priorityReason: '', urgencyLevel: null };

    this.http.patch(
      `${this.apiUrl}/appointments/${cita.id}/priority`,
      payload, { headers: this.headers() }
    ).subscribe({
      next: () => {
        this.priorProcesandoId = null;
        this.priorMensaje      = `✅ Cita de ${cita.patientName} ya no es prioritaria.`;
        this.priorCitas        = this.priorCitas.filter(c => c.id !== cita.id);
        this.cdr.detectChanges();
        setTimeout(() => { this.priorMensaje = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.priorProcesandoId = null;
        this.priorMensaje      = '❌ ' + (err.error?.message || 'Error al quitar prioridad.');
        this.cdr.detectChanges();
        setTimeout(() => { this.priorMensaje = ''; this.cdr.detectChanges(); }, 5000);
      }
    });
  }

  badgeUrgencia(nivel: string): string {
    switch (nivel) {
      case 'HIGH':   return 'bg-danger';
      case 'MEDIUM': return 'bg-warning text-dark';
      case 'LOW':    return 'bg-info text-dark';
      default:       return 'bg-secondary';
    }
  }

  textoUrgencia(nivel: string): string {
    const mapa: Record<string, string> = { HIGH: 'Alta', MEDIUM: 'Media', LOW: 'Baja' };
    return mapa[nivel] || nivel || '—';
  }

  get priorCountHigh():   number { return this.priorCitas.filter(c => c.urgencyLevel === 'HIGH').length; }
  get priorCountMedium(): number { return this.priorCitas.filter(c => c.urgencyLevel === 'MEDIUM').length; }
  get priorCountLow():    number { return this.priorCitas.filter(c => c.urgencyLevel === 'LOW').length; }

  cargarPerfil() {
    if (this.perfilDatos) return;
    this.perfilCargando = true;
    this.perfilError    = '';
    this.cdr.detectChanges();

    this.http.get<any>(`${this.apiUrl}/schedulers/me`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          this.perfilDatos    = { ...data };
          this.perfilBackup   = { ...data };
          this.perfilCargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.perfilError    = err.error?.message || 'Error al cargar los datos del perfil.';
          this.perfilCargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  guardarPerfil() {
    if (this.perfilPasswordMismatch) return;
    this.perfilGuardando = true;
    this.perfilMensaje   = '';
    this.cdr.detectChanges();

    const payload: any = {
      firstName: this.perfilDatos.firstName,
      lastName:  this.perfilDatos.lastName,
      email:     this.perfilDatos.email,
      phone:     this.perfilDatos.phone
    };
    if (this.perfilNuevoPassword.trim()) {
      payload.password = this.perfilNuevoPassword.trim();
    }

    this.http.put(`${this.apiUrl}/schedulers/me`, payload, { headers: this.headers() }).subscribe({
      next: () => {
        this.perfilGuardando    = false;
        this.perfilEditando     = false;
        this.perfilMensaje      = '✅ Perfil actualizado correctamente.';
        this.agendadorNombre    = `${this.perfilDatos.firstName} ${this.perfilDatos.lastName}`;
        localStorage.setItem('nombreUsuario', this.agendadorNombre);
        this.perfilNuevoPassword  = '';
        this.perfilConfirmPassword = '';
        this.perfilBackup         = { ...this.perfilDatos };
        this.cdr.detectChanges();
        setTimeout(() => { this.perfilMensaje = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.perfilGuardando = false;
        this.perfilMensaje   = '❌ ' + (err.error?.message || 'Error al guardar los cambios.');
        this.cdr.detectChanges();
        setTimeout(() => { this.perfilMensaje = ''; this.cdr.detectChanges(); }, 5000);
      }
    });
  }

  cancelarEditarPerfil() {
    this.perfilDatos           = { ...this.perfilBackup };
    this.perfilEditando        = false;
    this.perfilNuevoPassword   = '';
    this.perfilConfirmPassword = '';
    this.perfilMensaje         = '';
    this.cdr.detectChanges();
  }
}