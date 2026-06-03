// ============================================================
// agendador.ts  –  Panel del Agendador
// ============================================================

import { Component, Inject, PLATFORM_ID, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';

import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatNativeDateModule } from '@angular/material/core';

@Component({
  selector: 'app-agendador',
  standalone: true,
  imports: [FormsModule, CommonModule, MatDatepickerModule, MatInputModule, MatFormFieldModule, MatNativeDateModule],
  templateUrl: './agendador.html'
})
export class Agendador implements OnInit {

  pestanaActiva = 'listar';
  medicos: any[] = [];
  especialidades: string[] = [];

  // ── Nombre del agendador autenticado ──
  agendadorNombre = '';

  // ── Variables pestaña LISTAR ──
  especialidadBuscar = '';
  medicosFiltradosBuscar: any[] = [];
  doctorIdBuscar = 0;
  fechaBuscarObj: Date | null = null;
  fechaBuscar = '';
  citas: any[] = [];
  buscando = false;
  errorBuscar = '';

  // ── RF5: Exportar CSV ──
  exportando = false;
  mensajeExport = '';


  // ── Cancelar cita (modal igual al paciente) ──
  citaCancelando: any = null;
  motivoCancelacion = '';
  errorCancelacion = '';
  cancelando = false;
  mensajeCancelacion = '';

  // ── Variables pestaña REAGENDAR ────────────────────────────
  reagEspecialidad = '';
  reagMedicosFiltrados: any[] = [];
  reagDoctorId = 0;
  reagCitas: any[] = [];
  reagBuscando = false;
  reagError = '';
  reagCitaSeleccionada: any = null;
  reagNuevaFechaObj: Date | null = null;
  reagNuevaFecha = '';
  reagBuscarFechaObj: Date | null = null;
  reagBuscarFecha = '';
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
    startTime: ''
  };
  nuevaCitaFechaObj: Date | null = null;
  nuevaCitaNacimientoObj: Date | null = null;

  especialidadNuevaCita = '';
  medicosFiltradosNueva: any[] = [];
  franjas: any[] = [];
  citaCreada = false;
  errorCita = '';
  cargandoCita = false;

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
      this.cargarNombreAgendador();
      this.cargarMedicos();
    }
  }

  private headers(): HttpHeaders {
    const token = localStorage.getItem('token') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  // ── Carga el nombre del agendador desde el token JWT o localStorage ──
  // ── Carga el nombre real del agendador desde el JWT de Keycloak ──
  cargarNombreAgendador() {
    try {
      const token = localStorage.getItem('token') || '';
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        // Keycloak incluye given_name y family_name con el nombre real registrado
        const givenName = payload.given_name || '';
        const familyName = payload.family_name || '';
        if (givenName || familyName) {
          this.agendadorNombre = (givenName + ' ' + familyName).trim();
        } else {
          this.agendadorNombre = payload.name || payload.preferred_username || '';
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
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
    localStorage.removeItem('nombreUsuario');
    this.router.navigate(['/']);
  }

  cargarMedicos() {
    this.http.get<any[]>(`${this.apiUrl}/doctors`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          this.medicos = data;
          const raw = data
            .map((m: any) => m.specialty)
            .filter((s: any) => s && s.trim() !== '');
          this.especialidades = [...new Set<string>(raw)].sort();
          this.cdr.detectChanges();
        },
        error: () => { }
      });
  }

  onEspecialidadBuscarChange() {
    this.medicosFiltradosBuscar = this.medicos.filter(
      m => m.specialty === this.especialidadBuscar
    );
    this.doctorIdBuscar = 0;
    this.citas = [];
    this.mensajeExport = '';
  }

  onDoctorIdBuscarChange() {
    this.fechaBuscarObj = null;
    this.fechaBuscar = '';
    this.citas = [];
    this.cargarHorarioMedico(this.doctorIdBuscar);
  }

  onEspecialidadNuevaCitaChange() {
    this.medicosFiltradosNueva = this.medicos.filter(
      m => m.specialty === this.especialidadNuevaCita
    );
    this.nuevaCita.doctorId = 0;
    this.franjas = [];
    this.nuevaCita.startTime = '';
    this.nuevaCita.fecha = '';

    if (this.medicosFiltradosNueva.length === 1) {
      this.nuevaCita.doctorId = this.medicosFiltradosNueva[0].id;
      this.onDoctorChangeNuevaCita();
    }
    this.cdr.detectChanges();
  }

  onDoctorChangeNuevaCita() {
    this.nuevaCitaFechaObj = null;
    this.nuevaCita.fecha = '';
    this.franjas = [];
    this.nuevaCita.startTime = '';
    this.cargarHorarioMedico(this.nuevaCita.doctorId);
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

  onReagBuscarFechaChange() {
    this.reagBuscarFecha = this.reagBuscarFechaObj ? this.formatDateStr(this.reagBuscarFechaObj) : '';
  }

  onNuevaCitaNacimientoChange() {
    this.nuevaCita.birthDate = this.nuevaCitaNacimientoObj ? this.formatDateStr(this.nuevaCitaNacimientoObj) : '';
  }

  onNuevaCitaFechaChange() {
    this.nuevaCita.fecha = this.nuevaCitaFechaObj ? this.formatDateStr(this.nuevaCitaFechaObj) : '';
    if (this.nuevaCita.fecha) this.cargarFranjas();
  }

  onReagNuevaFechaChange() {
    this.reagNuevaFecha = this.reagNuevaFechaObj ? this.formatDateStr(this.reagNuevaFechaObj) : '';
    if (this.reagNuevaFecha) this.cargarFranjasReagendar();
  }

  // ── RF1: Buscar citas ──
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

  // ── Cancelar cita (modal) ──
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

  // ── RF5: Exportar CSV ──
  exportarCsv() {
    if (!this.doctorIdBuscar || !this.fechaBuscar) return;

    this.exportando = true;
    this.mensajeExport = '';
    this.cdr.detectChanges();

    const params = new HttpParams()
      .set('doctorId', this.doctorIdBuscar)
      .set('date', this.fechaBuscar);

    this.http.get(`${this.apiUrl}/appointments/export`,
      {
        headers: this.headers(),
        params,
        responseType: 'blob'
      }).subscribe({
        next: (blob: Blob) => {

          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `citas_${this.fechaBuscar}.csv`;
          link.click();
          window.URL.revokeObjectURL(url);


          // FIX: resetear el botón y mostrar mensaje de éxito

          this.exportando = false;
          this.mensajeExport = '✅ CSV descargado exitosamente';
          this.cdr.detectChanges();
          setTimeout(() => {
            this.mensajeExport = '';
            this.cdr.detectChanges();
          }, 4000);
        },
        error: () => {
          this.exportando = false;
          this.mensajeExport = '❌ Error al exportar el CSV';
          this.cdr.detectChanges();
        }
      });
  }

  // ── Autocompletado por cédula ──
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

          // FIX: asignar uno por uno + detectChanges al final

          this.nuevaCita.firstName = paciente.firstName || '';
          this.nuevaCita.lastName = paciente.lastName || '';
          this.nuevaCita.phone = paciente.phone || '';
          this.nuevaCita.email = paciente.email || '';
          this.nuevaCita.gender = paciente.gender || '';
          this.nuevaCita.birthDate = paciente.birthDate || '';
          this.nuevaCitaNacimientoObj = paciente.birthDate ? new Date(paciente.birthDate + 'T00:00:00') : null;
          this.pacienteId = paciente.id;
          this.pacienteEncontrado = true;
          this.buscandoPaciente = false;
          this.cdr.detectChanges();
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

  // ── Cargar franjas disponibles ──
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
          this.franjas = data.filter(f => f.available);
          this.cdr.detectChanges();
        },
        error: () => { }
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
        next: (paciente) => this.agendarCitaConPaciente(paciente.id),
        error: (err) => {
          this.errorCita = err.error?.message || 'Error al registrar paciente';
          this.cargandoCita = false;
          this.cdr.detectChanges();
        }
      });
  }

  private agendarCitaConPaciente(patientId: number) {
    const datosCita = {
      doctorId: this.nuevaCita.doctorId,
      patientId: patientId,
      date: this.nuevaCita.fecha,
      startTime: this.nuevaCita.startTime,
      notes: 'Cita agendada por WhatsApp'
    };

    this.http.post(`${this.apiUrl}/appointments`,
      datosCita, { headers: this.headers() }).subscribe({
        next: () => {
          this.cargandoCita = false;
          this.citaCreada = true;
          this.cdr.detectChanges();
          this.cargarFranjas();
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
    this.especialidadNuevaCita = '';
    this.medicosFiltradosNueva = [];
    this.nuevaCita = {
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
    this.nuevaCitaFechaObj = null;
    this.nuevaCitaNacimientoObj = null;
    this.franjas = [];
    this.cdr.detectChanges();
  }

  onReagEspecialidadChange() {
    this.reagMedicosFiltrados = this.medicos.filter(
      m => m.specialty === this.reagEspecialidad
    );
    this.reagDoctorId = 0;
    this.reagCitas = [];
    this.reagCitaSeleccionada = null;
  }

  reagBuscarCitas() {
    if (!this.reagDoctorId || !this.reagBuscarFecha) return;
    this.reagBuscando = true;
    this.reagError = '';
    this.reagCitas = [];
    this.reagCitaSeleccionada = null;

    const params = new HttpParams()
      .set('doctorId', this.reagDoctorId)
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
    this.cargarHorarioMedico(cita.doctorId);
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
          this.reagFranjas = data.filter(f => f.available);
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
      reason: this.reagCitaSeleccionada.notes || 'Reagendamiento solicitado'
    };

    this.http.put(`${this.apiUrl}/appointments/${this.reagCitaSeleccionada.id}/reschedule`,
      payload, { headers: this.headers() }).subscribe({
        next: () => {
          this.reagGuardando = false;
          this.reagExito = `✅ Cita de ${this.reagCitaSeleccionada.patientName} reagendada para el ${this.reagNuevaFecha} a las ${this.reagNuevaHora}`;
          this.reagCitaSeleccionada = null;
          this.reagFranjas = [];
          this.reagNuevaHora = '';
          this.reagBuscarCitas();
          this.reagFranjas = [];
          this.reagNuevaHora = '';
          this.reagBuscarCitas();   // refrescar la lista del día original
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

  etiquetaEstado(status: string): string {
    return { SCHEDULED: 'Programada', COMPLETED: 'Atendida', CANCELLED: 'Cancelada', NO_SHOW: 'No asistida' }
    [status] || status;
  }
}