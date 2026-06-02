import { Component, Inject, PLATFORM_ID, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';
import { environment } from '../../environments/environment';


@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './admin.html'
})
export class Admin implements OnInit {

  pestanaActiva = 'registrar-medico';

  // ── Registrar médico ──────────────────────────────────────────────────────
  medico = {
    identification: '', firstName: '', lastName: '',
    email: '', phone: '', specialty: '',
    licenseNumber: '', username: '', password: ''
  };
  medicoRegistrado = false;
  errorMedico = '';
  cargandoMedico = false;

  // ── Registrar agendador ───────────────────────────────────────────────────
  agendador = {
    identification: '', firstName: '', lastName: '',
    email: '', phone: '', username: '', password: ''
  };
  agendadorRegistrado = false;
  errorAgendador = '';
  cargandoAgendador = false;

  // ── Configuración ─────────────────────────────────────────────────────────
  configuracion = { windowWeeks: 4 };
  configGuardada = false;
  errorConfig = '';

  // ── Horarios ──────────────────────────────────────────────────────────────
  doctores: any[] = [];
  DIAS_SEMANA = [
    { value: 'MONDAY',    label: 'Lunes'     },
    { value: 'TUESDAY',   label: 'Martes'    },
    { value: 'WEDNESDAY', label: 'Miércoles' },
    { value: 'THURSDAY',  label: 'Jueves'    },
    { value: 'FRIDAY',    label: 'Viernes'   },
    { value: 'SATURDAY',  label: 'Sábado'    },
    { value: 'SUNDAY',    label: 'Domingo'   }
  ];
  horarioMedico = {
    doctorId: null as number | null,
    workingDays: [] as string[],
    startTime: '08:00',
    endTime: '17:00',
    intervalMinutes: 30
  };
  horarioGuardado = false;
  errorHorario = '';
  cargandoHorario = false;

  // ── Editar médico ─────────────────────────────────────────────────────────
  listaMedicos: any[] = [];
  medicoSeleccionado: any = null;
  medicoEditando: any = null;
  medicoActualizado = false;
  errorEditarMedico = '';
  cargandoEditarMedico = false;

  // ── Editar agendador ──────────────────────────────────────────────────────
  listaAgendadores: any[] = [];
  agendadorSeleccionado: any = null;
  agendadorEditando: any = null;
  agendadorActualizado = false;
  errorEditarAgendador = '';
  cargandoEditarAgendador = false;

  // ── Gestión de roles ──────────────────────────────────────────────────────
  ROLES_DISPONIBLES = ['ADMIN', 'DOCTOR', 'SCHEDULER', 'PATIENT'];
  listaUsuarios: any[] = [];
  usuarioSeleccionado: any = null;
  rolesUsuario: string[] = [];
  rolesActualizados = false;
  errorRoles = '';
  cargandoRoles = false;
  cargandoUsuarios = false;

  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit() {
    this.cargarDoctores();
  }

  private headers(): HttpHeaders {
    const token = isPlatformBrowser(this.platformId)
      ? localStorage.getItem('token') || '' : '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  cerrarSesion() {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('token');
      localStorage.removeItem('role');
    }
    this.router.navigate(['/']);
  }

  // ── Cargar datos base ─────────────────────────────────────────────────────

  cargarDoctores() {
    this.http.get<any[]>(`${this.apiUrl}/doctors`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          this.doctores     = data;
          this.listaMedicos = data;
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Error cargando doctores', err)
      });
  }

  cargarAgendadores() {
    this.http.get<any[]>(`${this.apiUrl}/schedulers`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          this.listaAgendadores = data;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando agendadores', err);
          this.http.get<any[]>(`${this.apiUrl}/schedulers/list`,
            { headers: this.headers() }).subscribe({
              next: (d) => { this.listaAgendadores = d; this.cdr.detectChanges(); },
              error: () => {}
            });
        }
      });
  }

  cargarUsuarios() {
    this.cargandoUsuarios = true;
    this.http.get<any[]>(`${this.apiUrl}/users`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          this.listaUsuarios    = data;
          this.cargandoUsuarios = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando usuarios', err);
          this.cargandoUsuarios = false;
          this.cdr.detectChanges();
        }
      });
  }

  // ── Cambio de pestaña ─────────────────────────────────────────────────────

  cambiarPestana(pestana: string) {
    this.pestanaActiva = pestana;
    if (pestana === 'editar-medico') {
      this.cargarDoctores();
      this.medicoSeleccionado  = null;
      this.medicoEditando      = null;
      this.medicoActualizado   = false;
      this.errorEditarMedico   = '';
    } else if (pestana === 'editar-agendador') {
      this.cargarAgendadores();
      this.agendadorSeleccionado = null;
      this.agendadorEditando     = null;
      this.agendadorActualizado  = false;
      this.errorEditarAgendador  = '';
    } else if (pestana === 'roles') {
      this.cargarUsuarios();
      this.usuarioSeleccionado = null;
      this.rolesUsuario        = [];
      this.rolesActualizados   = false;
      this.errorRoles          = '';
    }
  }

  // ── Registrar médico ──────────────────────────────────────────────────────

  registrarMedico() {
    this.cargandoMedico = true;
    this.errorMedico    = '';

    this.http.post(`${this.apiUrl}/doctors`, this.medico,
      { headers: this.headers(), observe: 'response' }).subscribe({
        next: (res) => {
          if (res.status === 200 || res.status === 201) {
            this.medicoRegistrado = true;
            this.cargarDoctores();
            this.medico = {
              identification: '', firstName: '', lastName: '',
              email: '', phone: '', specialty: '',
              licenseNumber: '', username: '', password: ''
            };
          }
          this.cargandoMedico = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.errorMedico    = err.error?.message || 'Error al registrar médico';
          this.cargandoMedico = false;
          this.cdr.detectChanges();
        }
      });
  }

  // ── Registrar agendador ───────────────────────────────────────────────────

  registrarAgendador() {
    this.cargandoAgendador = true;
    this.errorAgendador    = '';

    this.http.post(`${this.apiUrl}/schedulers/register`, this.agendador,
      { headers: this.headers(), observe: 'response' }).subscribe({
        next: (res) => {
          if (res.status === 200 || res.status === 201) {
            this.agendadorRegistrado = true;
            this.agendador = {
              identification: '', firstName: '', lastName: '',
              email: '', phone: '', username: '', password: ''
            };
          }
          this.cargandoAgendador = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.errorAgendador    = err.error?.message || 'Error al registrar agendador';
          this.cargandoAgendador = false;
          this.cdr.detectChanges();
        }
      });
  }

  // ── Configuración ─────────────────────────────────────────────────────────

  guardarConfiguracion() {
    this.configGuardada = false;
    this.errorConfig    = '';

    this.http.put(`${this.apiUrl}/configurations`, this.configuracion,
      { headers: this.headers(), observe: 'response' }).subscribe({
        next: () => {
          this.configGuardada = true;
          this.cdr.detectChanges();
          setTimeout(() => { this.configGuardada = false; this.cdr.detectChanges(); }, 3000);
        },
        error: (err) => {
          this.errorConfig = err.error?.message || 'Error al guardar configuración';
          this.cdr.detectChanges();
        }
      });
  }

  // ── Horarios ──────────────────────────────────────────────────────────────

  cargarHorarioMedico() {
    if (!this.horarioMedico.doctorId) return;

    this.http.get<any>(`${this.apiUrl}/doctors/schedules/${this.horarioMedico.doctorId}`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          if (data) {
            this.horarioMedico.workingDays     = data.workingDays || [];
            this.horarioMedico.startTime       = data.startTime ? data.startTime.substring(0, 5) : '08:00';
            this.horarioMedico.endTime         = data.endTime   ? data.endTime.substring(0, 5)   : '17:00';
            this.horarioMedico.intervalMinutes = data.intervalMinutes || 30;
          }
          this.cdr.detectChanges();
        },
        error: (err) => {
          if (err.status === 404) {
            this.horarioMedico.workingDays     = [];
            this.horarioMedico.startTime       = '08:00';
            this.horarioMedico.endTime         = '17:00';
            this.horarioMedico.intervalMinutes = 30;
          } else {
            console.error('Error cargando horario', err);
          }
          this.cdr.detectChanges();
        }
      });
  }

  onDiaCheckboxChange(event: any, diaValue: string) {
    if (event.target.checked) {
      if (!this.horarioMedico.workingDays.includes(diaValue)) {
        this.horarioMedico.workingDays.push(diaValue);
      }
    } else {
      this.horarioMedico.workingDays = this.horarioMedico.workingDays.filter(d => d !== diaValue);
    }
  }

  guardarHorarioMedico() {
    this.cargandoHorario = true;
    this.errorHorario    = '';
    this.horarioGuardado = false;

    const startTime = this.horarioMedico.startTime.length === 5
      ? this.horarioMedico.startTime + ':00' : this.horarioMedico.startTime;
    const endTime = this.horarioMedico.endTime.length === 5
      ? this.horarioMedico.endTime + ':00' : this.horarioMedico.endTime;

    const payload = {
      doctorId: this.horarioMedico.doctorId,
      workingDays: this.horarioMedico.workingDays,
      startTime, endTime,
      intervalMinutes: this.horarioMedico.intervalMinutes
    };

    this.http.put(`${this.apiUrl}/doctors/schedules`, payload,
      { headers: this.headers() }).subscribe({
        next: () => {
          this.horarioGuardado = true;
          this.cargandoHorario = false;
          this.cdr.detectChanges();
          setTimeout(() => { this.horarioGuardado = false; this.cdr.detectChanges(); }, 3000);
        },
        error: (err) => {
          this.errorHorario    = err.error?.message || 'Error al guardar horario';
          this.cargandoHorario = false;
          this.cdr.detectChanges();
        }
      });
  }

  // ── Editar médico ─────────────────────────────────────────────────────────

  seleccionarMedicoParaEditar(doc: any) {
    this.medicoSeleccionado  = doc;
    this.medicoActualizado   = false;
    this.errorEditarMedico   = '';

    // CORRECCIÓN: se agrega el campo skills al clonar el objeto
    this.medicoEditando = {
      id:             doc.id,
      identification: doc.identification || '',
      firstName:      doc.firstName       || doc.fullName?.split(' ')[0] || '',
      lastName:       doc.lastName        || doc.fullName?.split(' ').slice(1).join(' ') || '',
      email:          doc.email           || '',
      phone:          doc.phone           || '',
      specialty:      doc.specialty       || '',
      licenseNumber:  doc.licenseNumber   || '',
      skills:         doc.skills          || ''   // ← campo nuevo de habilidades
    };
    this.cdr.detectChanges();
  }

  guardarEdicionMedico() {
    if (!this.medicoEditando) return;
    this.cargandoEditarMedico = true;
    this.errorEditarMedico    = '';
    this.medicoActualizado    = false;

    // El payload incluye skills para que el backend lo reciba
    const payload = {
      identification: this.medicoEditando.identification,
      firstName:      this.medicoEditando.firstName,
      lastName:       this.medicoEditando.lastName,
      email:          this.medicoEditando.email,
      phone:          this.medicoEditando.phone,
      specialty:      this.medicoEditando.specialty,
      licenseNumber:  this.medicoEditando.licenseNumber,
      skills:         this.medicoEditando.skills   // ← se envía al backend
    };

    this.http.put(`${this.apiUrl}/doctors/${this.medicoEditando.id}`, payload,
      { headers: this.headers() }).subscribe({
        next: () => {
          this.medicoActualizado    = true;
          this.cargandoEditarMedico = false;
          this.cargarDoctores();
          this.medicoSeleccionado   = null;
          this.medicoEditando       = null;
          this.cdr.detectChanges();
          setTimeout(() => { this.medicoActualizado = false; this.cdr.detectChanges(); }, 4000);
        },
        error: (err) => {
          this.errorEditarMedico    = err.error?.message || 'Error al actualizar médico';
          this.cargandoEditarMedico = false;
          this.cdr.detectChanges();
        }
      });
  }

  cancelarEdicionMedico() {
    this.medicoSeleccionado = null;
    this.medicoEditando     = null;
    this.errorEditarMedico  = '';
  }

  // ── Editar agendador ──────────────────────────────────────────────────────

  seleccionarAgendadorParaEditar(ag: any) {
    this.agendadorSeleccionado = ag;
    this.agendadorActualizado  = false;
    this.errorEditarAgendador  = '';
    this.agendadorEditando = {
      id:             ag.id,
      identification: ag.identification || '',
      firstName:      ag.firstName       || ag.fullName?.split(' ')[0] || '',
      lastName:       ag.lastName        || ag.fullName?.split(' ').slice(1).join(' ') || '',
      email:          ag.email           || '',
      phone:          ag.phone           || ''
    };
    this.cdr.detectChanges();
  }

  guardarEdicionAgendador() {
    if (!this.agendadorEditando) return;
    this.cargandoEditarAgendador = true;
    this.errorEditarAgendador    = '';
    this.agendadorActualizado    = false;

    this.http.put(`${this.apiUrl}/schedulers/${this.agendadorEditando.id}`, this.agendadorEditando,
      { headers: this.headers() }).subscribe({
        next: () => {
          this.agendadorActualizado    = true;
          this.cargandoEditarAgendador = false;
          this.cargarAgendadores();
          this.agendadorSeleccionado   = null;
          this.agendadorEditando       = null;
          this.cdr.detectChanges();
          setTimeout(() => { this.agendadorActualizado = false; this.cdr.detectChanges(); }, 4000);
        },
        error: (err) => {
          this.errorEditarAgendador    = err.error?.message || 'Error al actualizar agendador';
          this.cargandoEditarAgendador = false;
          this.cdr.detectChanges();
        }
      });
  }

  cancelarEdicionAgendador() {
    this.agendadorSeleccionado = null;
    this.agendadorEditando     = null;
    this.errorEditarAgendador  = '';
  }

  // ── Gestión de roles ──────────────────────────────────────────────────────

  seleccionarUsuarioParaRoles(user: any) {
    this.usuarioSeleccionado = user;
    this.rolesActualizados   = false;
    this.errorRoles          = '';

    this.http.get<string[]>(`${this.apiUrl}/users/${user.id}/roles`,
      { headers: this.headers() }).subscribe({
        next: (roles) => {
          this.rolesUsuario = roles || [];
          this.cdr.detectChanges();
        },
        error: () => {
          this.rolesUsuario = user.roles ? [...user.roles] : [];
          this.cdr.detectChanges();
        }
      });
  }

  tieneRol(rol: string): boolean {
    return this.rolesUsuario.includes(rol);
  }

  toggleRol(event: any, rol: string) {
    if (event.target.checked) {
      if (!this.rolesUsuario.includes(rol)) {
        this.rolesUsuario = [...this.rolesUsuario, rol];
      }
    } else {
      this.rolesUsuario = this.rolesUsuario.filter(r => r !== rol);
    }
  }

  guardarRoles() {
    if (!this.usuarioSeleccionado) return;
    this.cargandoRoles     = true;
    this.errorRoles        = '';
    this.rolesActualizados = false;

    this.http.put(`${this.apiUrl}/users/${this.usuarioSeleccionado.id}/roles`,
      { roles: this.rolesUsuario },
      { headers: this.headers() }).subscribe({
        next: () => {
          this.rolesActualizados = true;
          this.cargandoRoles     = false;
          this.cargarUsuarios();
          this.cdr.detectChanges();
          setTimeout(() => { this.rolesActualizados = false; this.cdr.detectChanges(); }, 4000);
        },
        error: (err) => {
          this.errorRoles    = err.error?.message || 'Error al actualizar roles';
          this.cargandoRoles = false;
          this.cdr.detectChanges();
        }
      });
  }

  cancelarEdicionRoles() {
    this.usuarioSeleccionado = null;
    this.rolesUsuario        = [];
    this.errorRoles          = '';
  }

  getNombreUsuario(user: any): string {
    return user.fullName || user.username || user.name || `ID: ${user.id}`;
  }

  getRolBadgeClass(rol: string): string {
    const map: Record<string, string> = {
      ADMIN:     'bg-danger',
      DOCTOR:    'bg-primary',
      SCHEDULER: 'bg-success',
      PATIENT:   'bg-secondary'
    };
    return map[rol] || 'bg-dark';
  }
}