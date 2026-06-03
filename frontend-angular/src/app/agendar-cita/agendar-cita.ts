// ============================================================
// agendar-cita.ts  –  Panel del Paciente 
// ============================================================

import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { AppointmentService } from '../services/appointment.service';
import { AuthService } from '../services/auth.service';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Router , RouterLink } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-agendar-cita',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './agendar-cita.html'
})
export class AgendarCita implements OnInit {

  medicos: any[] = [];
  franjas: any[] = [];
  especialidades: string[] = [];
  especialidadSeleccionada = '';
  medicosFiltrados: any[] = [];

  doctorId = 0;

  fecha: string = '';
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

  pacienteNombre = '';

  citaCancelando: any = null;

  motivoCancelacion = '';

  errorCancelacion = '';

  cancelando = false;

  tieneCitaPendiente = false;

  tieneConsultaGeneralCompletada = false;

  doctorWorkingDays: string[] = [];

  minDate = new Date();

  festivosColombiaStr: string[] = [];

  medicoDetalle: any = null;

  cargandoMedicoDetalle = false;

  private apiUrl = environment.apiUrl;

  constructor(
    private appointmentService: AppointmentService,
    private authService: AuthService,
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  // ============================================================
  // INIT CORREGIDO
  // ============================================================

  ngOnInit() {

    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    // Obtener patientId desde localStorage
    this.patientId = this.authService.obtenerUserId();

    // Si no existe → resolverlo
    if (!this.patientId) {

      this.resolverPatientId();

    } else {

      console.log('PATIENT ID:', this.patientId);

      this.cargarNombrePaciente();

      this.cargarMedicos();

      this.cargarMisCitas();

    }

  }

  // ============================================================
  // RESOLVER PATIENT ID
  // ============================================================

  resolverPatientId() {

    try {

      const username =
        localStorage.getItem('username') || '';

      if (!username) {

        this.error =
          'No se encontró el usuario autenticado.';

        return;
      }

      this.http.get<any>(
        `${this.apiUrl}/patients/by-username?username=${username}`,
        {
          headers: this.headers()
        }

      ).subscribe({

        next: (patient) => {

          console.log('PATIENT:', patient);

          if (patient?.id) {

            this.patientId = patient.id;

            localStorage.setItem(
              'userId',
              String(patient.id)
            );

            this.cargarNombrePaciente();

            this.cargarMedicos();

            this.cargarMisCitas();

            this.cdr.detectChanges();

          } else {

            this.error =
              'No se pudo identificar el paciente.';
          }

        },

        error: (err) => {

          console.error(err);

          if (err.status === 401 || err.status === 403) {
            this.authService.cerrarSesion();
            this.router.navigate(['/login']);
            return;
          }

          this.error = err.status === 404
            ? 'Paciente no encontrado. Por favor regístrese nuevamente.'
            : 'Error obteniendo información del paciente.';

          this.cdr.detectChanges();

        }

      });

    } catch (e) {

      console.error(e);

    }

  }

  // ============================================================
  // HEADERS JWT
  // ============================================================

  private headers(): HttpHeaders {

    const token =
      this.authService.obtenerToken() || '';

    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

  }

  // ============================================================
  // CARGAR NOMBRE
  // ============================================================

  cargarNombrePaciente() {

    try {

      const token =
        this.authService.obtenerToken() || '';

      if (token) {

        const payload =
          JSON.parse(atob(token.split('.')[1]));

        const givenName =
          payload.given_name || '';

        const familyName =
          payload.family_name || '';

        if (givenName || familyName) {

          this.pacienteNombre =
            (givenName + ' ' + familyName).trim();

        } else {

          this.pacienteNombre =
            payload.name ||
            payload.preferred_username ||
            '';

        }

      }

    } catch (e) {

      this.pacienteNombre = '';

    }

  }

  // ============================================================
  // CARGAR MÉDICOS
  // ============================================================

  cargarMedicos() {

    this.appointmentService.listarMedicos().subscribe({

      next: (data) => {

        this.medicos = data || [];

        const permitidas = [
          'Consulta General',
          'Terapia Neural',
          'Quiropraxia',
          'Fisioterapia'
        ];

        const raw =
          this.medicos
            .map((m: any) => m.specialty)
            .filter((s: any) => s && s.trim() !== '');

        this.especialidades =
          [...new Set<string>(raw)]
            .filter(s => permitidas.includes(s))
            .sort();

      },

      error: () => {

        this.error = 'Error al cargar médicos';

      }

    });

  }

  // ============================================================
  // MIS CITAS
  // ============================================================

  cargarMisCitas() {

    if (!this.patientId || this.patientId <= 0) {

      this.errorCitas =
        'No se pudo identificar al paciente.';

      return;
    }

    this.cargandoCitas = true;

    this.errorCitas = '';

    this.misCitas = [];

    const params =
      new HttpParams()
        .set('patientId', this.patientId.toString());

    this.http.get<any[]>(
      `${this.apiUrl}/appointments`,
      {
        headers: this.headers(),
        params
      }

    ).subscribe({

      next: (data) => {

        this.misCitas = data || [];

        this.tieneCitaPendiente =
          this.misCitas.some(
            c => c.status === 'SCHEDULED'
          );

        this.tieneConsultaGeneralCompletada =
          this.misCitas.some(
            c =>
              c.status === 'COMPLETED'
              &&
              c.specialty === 'Consulta General'
          );

        this.cargandoCitas = false;

        this.cdr.detectChanges();

      },

      error: (err) => {

        this.cargandoCitas = false;

        this.errorCitas =
          (err.status === 401 || err.status === 403)
            ? 'Sesión expirada.'
            : 'Error al cargar las citas.';

        this.cdr.detectChanges();

      }

    });

  }

  // ============================================================
  // BUSCAR FRANJAS
  // ============================================================

  buscarFranjas() {

    if (!this.doctorId || !this.fecha) {

      this.franjas = [];

      this.franjaSeleccionada = null;

      return;
    }

    this.fechaStr = this.fecha;

    this.franjas = [];

    this.franjaSeleccionada = null;

    this.error = '';

    this.cargandoFranjas = true;

    this.appointmentService
      .obtenerFranjas(this.doctorId, this.fechaStr)
      .subscribe({

        next: (data) => {

          this.franjas =
            (data || [])
              .filter((f: any) => f.available);

          this.cargandoFranjas = false;

          this.cdr.detectChanges();

        },

        error: (err) => {

          this.cargandoFranjas = false;

          this.error =
            err.status === 404
              ? 'El médico aún no tiene horario configurado.'
              : 'Error al cargar las franjas horarias.';

          this.cdr.detectChanges();

        }

      });

  }

  // ============================================================
  // CONFIRMAR CITA
  // ============================================================

  confirmarCita() {

    if (!this.patientId || this.patientId <= 0) {

      this.error =
        'No se ha detectado el ID del paciente.';

      return;
    }

    if (!this.doctorId || !this.fechaStr) {

      this.error =
        'Seleccione médico y fecha.';

      return;
    }

    if (!this.franjaSeleccionada) {

      this.error =
        'Seleccione una franja horaria.';

      return;
    }

    this.cargando = true;

    this.error = '';

    const datos = {

      doctorId: this.doctorId,

      patientId: this.patientId,

      date: this.fechaStr,

      startTime:
        this.franjaSeleccionada.startTime,

      notes: ''

    };

    this.appointmentService
      .crearCita(datos)
      .subscribe({

        next: () => {

          this.cargando = false;

          this.citaCreada = true;

          this.cargarMisCitas();

          this.cdr.detectChanges();

          setTimeout(() => {

            this.limpiarFormulario();

            this.citaCreada = false;

          }, 2000);

        },

        error: (err) => {

          this.cargando = false;

          if (err.status === 409 || err.status === 400) {

            this.error =
              err.error?.message
              ||
              'No se pudo agendar la cita.';

          } else if (
            err.status === 401
            ||
            err.status === 403
          ) {

            this.error =
              'Sesión expirada. Inicia sesión nuevamente.';

          } else {

            this.error =
              err.error?.message
              ||
              'Error al agendar cita.';
          }

          this.cdr.detectChanges();

        }

      });

  }

  // ============================================================
  // EDITAR PERFIL
  // ============================================================

  mostrarModalEditar = false;

  guardandoPerfil = false;

  mensajePerfil = '';

  errorPerfil = '';

  perfilPaciente: any = {

    firstName: '',

    lastName: '',

    email: '',

    phone: '',

    gender: '',

    birthDate: ''

  };

  abrirEditarPerfil() {

    if (!this.patientId || this.patientId <= 0) {

      this.errorPerfil =
        'No se pudo identificar el paciente.';

      return;
    }

    this.errorPerfil = '';

    this.mensajePerfil = '';

    this.http.get<any>(
      `${this.apiUrl}/patients/by-id/${this.patientId}`,
      {
        headers: this.headers()
      }

    ).subscribe({

      next: (resp) => {

        this.perfilPaciente = {

          firstName:
            resp.firstName || '',

          lastName:
            resp.lastName || '',

          email:
            resp.email || '',

          phone:
            resp.phone || '',

          gender:
            resp.gender || '',

          birthDate:
            resp.birthDate || ''

        };

        this.mostrarModalEditar = true;

        this.cdr.detectChanges();

      },

      error: (err) => {

        const status = err.status;

        if (status === 401 || status === 403) {

          this.errorPerfil =
            'Sesión expirada. Inicia sesión nuevamente.';

        } else if (status === 404) {

          this.errorPerfil =
            'No se encontró la información del paciente.';

        } else {

          this.errorPerfil =
            'No se pudo cargar la información del perfil.';
        }

        this.cdr.detectChanges();

      }

    });

  }

  actualizarPerfil() {

    if (!this.patientId || this.patientId <= 0) {
      return;
    }

    this.guardandoPerfil = true;

    this.errorPerfil = '';

    this.mensajePerfil = '';

    this.http.put<any>(
      `${this.apiUrl}/patients/${this.patientId}`,
      this.perfilPaciente,
      {
        headers: this.headers()
      }

    ).subscribe({

      next: () => {

        this.guardandoPerfil = false;

        this.mensajePerfil =
          '¡Información actualizada correctamente!';

        this.pacienteNombre =
          `${this.perfilPaciente.firstName} ${this.perfilPaciente.lastName}`;

        this.cdr.detectChanges();

        setTimeout(() => {

          this.mostrarModalEditar = false;

          this.mensajePerfil = '';

          this.cdr.detectChanges();

        }, 1500);

      },

      error: (err) => {

        this.guardandoPerfil = false;

        const status = err.status;

        if (status === 401 || status === 403) {

          this.errorPerfil =
            'Sesión expirada. Inicia sesión nuevamente.';

        } else if (status === 400) {

          this.errorPerfil =
            err.error?.message
            ||
            'Datos inválidos.';

        } else {

          this.errorPerfil =
            'No se pudo actualizar la información.';
        }

        this.cdr.detectChanges();

      }

    });

  }

  cerrarModalEditar() {

    this.mostrarModalEditar = false;

    this.errorPerfil = '';

    this.mensajePerfil = '';

  }

  // ============================================================
  // CERRAR SESIÓN
  // ============================================================

  cerrarSesion() {

    this.authService.cerrarSesion();

    this.router.navigate(['/login']);

  }

  // ============================================================
  // LIMPIAR
  // ============================================================

  limpiarFormulario() {

    this.doctorId = 0;

    this.fecha = '';

    this.fechaStr = '';

    this.franjas = [];

    this.franjaSeleccionada = null;

    this.error = '';

    this.citaCreada = false;

  }
// ============================================================
// MÉTODOS FALTANTES DEL TEMPLATE
// ============================================================

onEspecialidadChange() {

  this.medicosFiltrados =
    this.medicos.filter(
      m => m.specialty === this.especialidadSeleccionada
    );

  this.doctorId = 0;

  this.franjas = [];

  this.franjaSeleccionada = null;

  this.fecha = '';

  this.fechaStr = '';

  this.error = '';

  this.doctorWorkingDays = [];

  this.medicoDetalle = null;

  if (this.medicosFiltrados.length === 1) {

    this.doctorId =
      this.medicosFiltrados[0].id;

    this.cargarHorarioMedico();

    this.cargarDetalleMedico(this.doctorId);

    this.cdr.detectChanges();
  }
}

onDoctorChange() {

  this.franjas = [];

  this.franjaSeleccionada = null;

  this.fecha = '';

  this.fechaStr = '';

  this.error = '';

  this.medicoDetalle = null;

  if (this.doctorId && this.doctorId > 0) {

    this.cargarHorarioMedico();

    this.cargarDetalleMedico(this.doctorId);

  } else {

    this.doctorWorkingDays = [];
  }
}

seleccionarFranja(franja: any) {

  if (franja && franja.available) {

    this.franjaSeleccionada = franja;
  }
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

  if (!this.citaCancelando) {

    this.errorCancelacion =
      'No hay cita seleccionada para cancelar.';

    return;
  }

  if (!this.motivoCancelacion.trim()) {

    this.errorCancelacion =
      'Debe indicar el motivo de cancelación.';

    return;
  }

  this.cancelando = true;

  this.http.patch(
    `${this.apiUrl}/appointments/${this.citaCancelando.id}/cancel`,
    {
      reason: this.motivoCancelacion
    },
    {
      headers: this.headers()
    }

  ).subscribe({

    next: () => {

      this.cancelando = false;

      this.citaCancelando = null;

      this.motivoCancelacion = '';

      this.cargarMisCitas();

      this.cdr.detectChanges();

    },

    error: (err) => {

      this.cancelando = false;

      this.errorCancelacion =
        err.error?.message
        ||
        'Error al cancelar la cita.';

      this.cdr.detectChanges();

    }

  });

}

cargarHorarioMedico() {

  if (!this.doctorId || this.doctorId <= 0) {

    return;
  }

  this.appointmentService
    .obtenerHorario(this.doctorId)
    .subscribe({

      next: (horario: any) => {

        this.doctorWorkingDays =
          horario?.workingDays || [];

        this.cdr.detectChanges();

      },

      error: () => {

        this.doctorWorkingDays = [];

        this.cdr.detectChanges();

      }

    });

}

cargarDetalleMedico(doctorId: number) {

  if (!doctorId || doctorId <= 0) {

    return;
  }

  this.cargandoMedicoDetalle = true;

  this.http.get<any>(
    `${this.apiUrl}/doctors/${doctorId}`,
    {
      headers: this.headers()
    }

  ).subscribe({

    next: (data) => {

      this.medicoDetalle = data;

      this.cargandoMedicoDetalle = false;

      this.cdr.detectChanges();

    },

    error: () => {

      this.medicoDetalle = null;

      this.cargandoMedicoDetalle = false;

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

get especialidadesPermitidas() {

  if (!this.tieneConsultaGeneralCompletada) {

    return this.especialidades.filter(
      e => e === 'Consulta General'
    );
  }

  return this.especialidades;
}
}


