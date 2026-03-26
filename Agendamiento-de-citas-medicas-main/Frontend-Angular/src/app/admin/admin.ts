import { Component, Inject, PLATFORM_ID, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './admin.html'
})
export class Admin implements OnInit {

  pestanaActiva = 'registrar-medico';

  // Registrar médico
  medico = {
    identification: '', firstName: '', lastName: '',
    email: '', phone: '', specialty: '',
    licenseNumber: '', username: '', password: ''
  };
  medicoRegistrado = false;
  errorMedico = '';
  cargandoMedico = false;

  // Registrar agendador
  agendador = {
    identification: '', firstName: '', lastName: '',
    email: '', phone: '', username: '', password: ''
  };
  agendadorRegistrado = false;
  errorAgendador = '';
  cargandoAgendador = false;

  // Configuración
  configuracion = { windowWeeks: 4 };
  configGuardada = false;
  errorConfig = '';

  // Horarios Médicos
  doctores: any[] = [];
  DIAS_SEMANA = [
    { value: 'MONDAY', label: 'Lunes' },
    { value: 'TUESDAY', label: 'Martes' },
    { value: 'WEDNESDAY', label: 'Miércoles' },
    { value: 'THURSDAY', label: 'Jueves' },
    { value: 'FRIDAY', label: 'Viernes' },
    { value: 'SATURDAY', label: 'Sábado' },
    { value: 'SUNDAY', label: 'Domingo' }
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

  private apiUrl = 'http://localhost:8080/api/v1';

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

  cargarDoctores() {
    this.http.get<any[]>(`${this.apiUrl}/doctors`,
      { headers: this.headers() }).subscribe({
        next: (data) => this.doctores = data,
        error: (err) => console.error('Error cargando doctores', err)
      });
  }

  registrarMedico() {
    this.cargandoMedico = true;
    this.errorMedico = '';

    this.http.post(`${this.apiUrl}/doctors`, this.medico,
      { headers: this.headers(), observe: 'response' }).subscribe({
        next: (res) => {
          // Acepta 200 y 201
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
          this.errorMedico = err.error?.message || 'Error al registrar médico';
          this.cargandoMedico = false;
          this.cdr.detectChanges();
        }
      });
  }

  registrarAgendador() {
    this.cargandoAgendador = true;
    this.errorAgendador = '';

    this.http.post(`${this.apiUrl}/schedulers/register`, this.agendador,
      { headers: this.headers(), observe: 'response' }).subscribe({
        next: (res) => {
          // Acepta 200 y 201
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
          this.errorAgendador = err.error?.message || 'Error al registrar agendador';
          this.cargandoAgendador = false;
          this.cdr.detectChanges();
        }
      });
  }

  guardarConfiguracion() {
    this.configGuardada = false;
    this.errorConfig = '';

    this.http.put(`${this.apiUrl}/configurations`, this.configuracion,
      { headers: this.headers(), observe: 'response' }).subscribe({
        next: () => {
          this.configGuardada = true;
          this.errorConfig = '';
        },
        error: (err) => {
          this.errorConfig = err.error?.message || 'Error al guardar configuración';
        }
      });
  }

  cargarHorarioMedico() {
    if (!this.horarioMedico.doctorId) return;

    this.http.get<any>(`${this.apiUrl}/doctors/schedules/${this.horarioMedico.doctorId}`,
      { headers: this.headers() }).subscribe({
        next: (data) => {
          if (data) {
            this.horarioMedico.workingDays = data.workingDays || [];
            this.horarioMedico.startTime = data.startTime ? data.startTime.substring(0, 5) : '08:00';
            this.horarioMedico.endTime = data.endTime ? data.endTime.substring(0, 5) : '17:00';
            this.horarioMedico.intervalMinutes = data.intervalMinutes || 30;
          }
        },
        error: (err) => {
          if (err.status === 404) {
            this.horarioMedico.workingDays = [];
            this.horarioMedico.startTime = '08:00';
            this.horarioMedico.endTime = '17:00';
            this.horarioMedico.intervalMinutes = 30;
          } else {
            console.error('Error cargando horario', err);
          }
        }
      });
  }

  onDiaCheckboxChange(event: any, diaValue: string) {
    if (event.target.checked) {
      if (!this.horarioMedico.workingDays.includes(diaValue)) {
        this.horarioMedico.workingDays.push(diaValue);
      }
    } else {
      this.horarioMedico.workingDays =
        this.horarioMedico.workingDays.filter(d => d !== diaValue);
    }
  }

  guardarHorarioMedico() {
    this.cargandoHorario = true;
    this.errorHorario = '';

    const payload = {
      doctorId: this.horarioMedico.doctorId,
      workingDays: this.horarioMedico.workingDays,
      startTime: this.horarioMedico.startTime.length === 5
        ? this.horarioMedico.startTime + ':00'
        : this.horarioMedico.startTime,
      endTime: this.horarioMedico.endTime.length === 5
        ? this.horarioMedico.endTime + ':00'
        : this.horarioMedico.endTime,
      intervalMinutes: this.horarioMedico.intervalMinutes
    };

    this.http.put(`${this.apiUrl}/doctors/schedules`, payload,
      { headers: this.headers() }).subscribe({
        next: () => {
          this.horarioGuardado = true;
          this.cargandoHorario = false;
          setTimeout(() => this.horarioGuardado = false, 3000);
        },
        error: (err) => {
          this.errorHorario = err.error?.message || 'Error al guardar horario';
          this.cargandoHorario = false;
        }
      });
  }
}