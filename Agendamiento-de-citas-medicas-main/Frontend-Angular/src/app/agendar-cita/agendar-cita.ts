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

  // Lista completa de médicos traída del backend
  medicos: any[] = [];

  // Franjas horarias disponibles del médico+fecha seleccionados
  franjas: any[] = [];

  // Lista de especialidades únicas (se construye a partir de medicos[])
  especialidades: string[] = [];

  // Especialidad que el paciente elige primero
  especialidadSeleccionada = '';

  // Médicos que pertenecen a la especialidad seleccionada
  medicosFiltrados: any[] = [];

  // ID del médico seleccionado (0 = ninguno)
  doctorId = 0;

  // Fecha elegida para la cita
  fecha = '';

  // Franja horaria elegida
  franjaSeleccionada: any = null;

  // ID del paciente autenticado (viene del token)
  patientId: number | null = null;

  // Flags de UI
  citaCreada = false;
  error = '';
  cargando = false;

  // ── Sección "Mis Citas" ──
  misCitas: any[] = [];
  cargandoCitas = false;
  errorCitas = '';           // mensaje de error en la pestaña de citas

  // Pestaña activa ('agendar' | 'mis-citas')
  pestanaActiva = 'agendar';

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
    // isPlatformBrowser evita errores de SSR (Angular Universal)
    if (isPlatformBrowser(this.platformId)) {
      this.patientId = this.authService.obtenerUserId();
      this.cargarMedicos(); // cargar médicos al iniciar (para tener las especialidades listas)
      // NO se llama cargarMisCitas() aquí; se llama cuando el usuario abre la pestaña
    }
  }

  // Construye cabecera con el JWT del paciente autenticado
  private headers(): HttpHeaders {
    const token = this.authService.obtenerToken() || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  // ── Trae todos los médicos y construye el listado de especialidades ──
  cargarMedicos() {
    this.appointmentService.listarMedicos().subscribe({
      next: (data) => {
        this.medicos = data;

        // filtrar valores nulos/vacíos antes de hacer el Set
        // Si un médico no tiene specialty asignada en la BD, se excluye
        const especialidadesRaw = data
          .map((m: any) => m.specialty)
          .filter((s: any) => s && s.trim() !== '');

        // Set elimina duplicados; sort() los ordena alfabéticamente
        this.especialidades = [...new Set<string>(especialidadesRaw)].sort();
      },
      error: () => this.error = 'Error al cargar médicos'
    });
  }

  // ── Se ejecuta cuando el paciente cambia la especialidad en el select ──
  onEspecialidadChange() {
    // Filtrar solo los médicos de la especialidad elegida
    this.medicosFiltrados = this.medicos.filter(
      m => m.specialty === this.especialidadSeleccionada
    );

    // Resetear los campos que dependen de la especialidad
    this.doctorId = 0;
    this.franjas = [];
    this.franjaSeleccionada = null;
    this.fecha = '';

    // si solo hay un médico en esa especialidad,
    // seleccionarlo automáticamente sin obligar al usuario a elegir
    if (this.medicosFiltrados.length === 1) {
      this.doctorId = this.medicosFiltrados[0].id;
    }
  }

  // ── Carga las citas del paciente autenticado ──
  cargarMisCitas() {
    // i patientId es null, parar YA y mostrar error
   
    if (!this.patientId) {
      this.cargandoCitas = false;
      this.errorCitas = 'No se pudo identificar al paciente. Intente cerrar sesión e ingresar nuevamente.';
      return;
    }

    // Activar spinner y limpiar estado anterior
    this.cargandoCitas = true;
    this.errorCitas = '';
    this.misCitas = [];

    const params = new HttpParams().set('patientId', this.patientId);

    this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params }).subscribe({
        next: (data) => {
          this.misCitas = data || []; // guardar lista (o array vacío si viene null)
          this.cargandoCitas = false;
          this.cdr.detectChanges();  // forzar refresco de la vista
        },
        // en caso de error apagar el spinner y mostrar mensaje
        error: (err) => {
          this.cargandoCitas = false;
          if (err.status === 401 || err.status === 403) {
            this.errorCitas = 'Sesión expirada. Por favor inicie sesión nuevamente.';
          } else {
            this.errorCitas = 'Error al cargar las citas. Intente de nuevo.';
          }
          this.cdr.detectChanges();
        }
      });
  }

  // ── Consulta las franjas disponibles para el médico y fecha elegidos ──
  buscarFranjas() {
    if (!this.doctorId || !this.fecha) return;
    this.franjas = [];
    this.franjaSeleccionada = null;
    this.appointmentService.obtenerFranjas(this.doctorId, this.fecha).subscribe({
      next: (data) => this.franjas = data.filter((f: any) => f.available),
      error: () => this.error = 'Error al cargar franjas'
    });
  }

  // ── El paciente hace clic en un botón de hora ──
  seleccionarFranja(franja: any) {
    if (!franja.available) return;
    this.franjaSeleccionada = franja;
  }

  // ── Cerrar sesión y volver al login ──
  cerrarSesion() {
    this.authService.cerrarSesion();
    this.router.navigate(['/login']);
  }

  // ── Envía la cita al backend ──
  confirmarCita() {
    // Validaciones antes de enviar
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
      doctorId:  this.doctorId,
      patientId: this.patientId,
      date:      this.fecha,
      startTime: this.franjaSeleccionada.startTime,
      notes:     ''
    };

    this.appointmentService.crearCita(datos).subscribe({
      next: () => {
        this.cargando = false;
        this.citaCreada = true;
        this.cdr.detectChanges();
        this.cargarMisCitas(); // refrescar la lista de citas

        // Limpiar el formulario automáticamente después de 2 segundos
        setTimeout(() => {
          this.limpiarFormulario();
          this.citaCreada = false;
        }, 2000);
      },
      error: (err) => {
        this.cargando = false;
        // Mensajes de error según el código HTTP
        if (err.status === 409)      this.error = 'La franja ya fue tomada.';
        else if (err.status === 400) this.error = 'Datos inválidos.';
        else if (err.status === 401 || err.status === 403) this.error = 'Sesión expirada.';
        else                         this.error = 'Error al agendar cita.';
      }
    });
  }

  // ── Traduce el estado de la cita al español ──
  etiquetaEstado(status: string): string {
    const map: any = {
      'SCHEDULED': 'Programada',
      'COMPLETED': 'Completada',
      'CANCELLED': 'Cancelada'
    };
    return map[status] || status;
  }

  // ── Resetea todos los campos del formulario ──
  limpiarFormulario() {
    this.doctorId              = 0;
    this.fecha                 = '';
    this.franjas               = [];
    this.franjaSeleccionada    = null;
    this.error                 = '';
    this.citaCreada            = false;
    this.especialidadSeleccionada = '';
    this.medicosFiltrados      = [];
  }
}