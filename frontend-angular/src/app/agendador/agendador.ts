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

  pestanaActiva = 'listar';
  medicos: any[] = [];
  especialidades: string[] = [];

  // ── Nombre del agendador autenticado ──
  agendadorNombre = '';

  // ── Variables pestaña LISTAR ──
  especialidadBuscar = '';
  medicosFiltradosBuscar: any[] = [];
  doctorIdBuscar = 0;
  fechaBuscar = '';
  citas: any[] = [];
  buscando = false;
  errorBuscar = '';

  // ── RF5: Exportar CSV ──
  exportando = false;
  mensajeExport = '';

  // ── Cancelar cita ──
  cancelandoCitaId: number | null = null;
  mensajeCancelacion = '';

  // ── Variables pestaña REAGENDAR ────────────────────────────
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
    startTime: ''
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
  cargarNombreAgendador() {
    // Intentar leer el nombre guardado previamente
    const nombreGuardado = localStorage.getItem('nombreUsuario');
    if (nombreGuardado) {
      this.agendadorNombre = nombreGuardado;
      this.cdr.detectChanges();
      return;
    }

    // Si no hay nombre guardado, intentar parsear el token JWT
    try {
      const token = localStorage.getItem('token') || '';
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        // Keycloak suele incluir preferred_username, name, given_name
        this.agendadorNombre =
          payload.name ||
          payload.preferred_username ||
          payload.given_name ||
          payload.sub ||
          '';
        if (this.agendadorNombre) {
          localStorage.setItem('nombreUsuario', this.agendadorNombre);
        }
      }
    } catch (e) {
      // No se pudo parsear el token
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
    }
    this.cdr.detectChanges();
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

  // ── Cancelar cita ──
  cancelarCita(citaId: number) {
    if (!confirm('¿Está seguro de que desea cancelar esta cita?')) return;

    this.cancelandoCitaId = citaId;
    this.mensajeCancelacion = '';
    this.cdr.detectChanges();

    this.http.patch(
      `${this.apiUrl}/appointments/${citaId}/cancel`,
      {},
      { headers: this.headers() }
    ).subscribe({
      next: () => {
        this.cancelandoCitaId = null;
        this.mensajeCancelacion = '✅ Cita cancelada exitosamente.';
        this.cdr.detectChanges();
        // Refrescar la lista según los filtros actuales
        if (this.doctorIdBuscar && this.fechaBuscar) this.buscarCitas();
        setTimeout(() => { this.mensajeCancelacion = ''; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        this.cancelandoCitaId = null;
        this.mensajeCancelacion = '❌ ' + (err.error?.message || 'Error al cancelar la cita.');
        this.cdr.detectChanges();
        setTimeout(() => { this.mensajeCancelacion = ''; this.cdr.detectChanges(); }, 5000);
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
          this.nuevaCita.firstName = paciente.firstName || '';
          this.nuevaCita.lastName = paciente.lastName || '';
          this.nuevaCita.phone = paciente.phone || '';
          this.nuevaCita.email = paciente.email || '';
          this.nuevaCita.gender = paciente.gender || '';
          this.nuevaCita.birthDate = paciente.birthDate || '';
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
    this.reagNuevaFecha = '';
    this.reagFranjas = [];
    this.reagNuevaHora = '';
    this.reagExito = '';
    this.reagErrorGuardar = '';
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
      doctorId: this.reagCitaSeleccionada.doctorId,
      patientId: this.reagCitaSeleccionada.patientId,
      date: this.reagNuevaFecha,
      startTime: this.reagNuevaHora,
      notes: this.reagCitaSeleccionada.notes || ''
    };

    this.http.put(`${this.apiUrl}/appointments/${this.reagCitaSeleccionada.id}`,
      payload, { headers: this.headers() }).subscribe({
        next: () => {
          this.reagGuardando = false;
          this.reagExito = `✅ Cita de ${this.reagCitaSeleccionada.patientName} reagendada para el ${this.reagNuevaFecha} a las ${this.reagNuevaHora}`;
          this.reagCitaSeleccionada = null;
          this.reagFranjas = [];
          this.reagNuevaHora = '';
          this.reagBuscarCitas();
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
}