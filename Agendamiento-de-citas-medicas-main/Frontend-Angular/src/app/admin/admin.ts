import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './admin.html'
})
export class Admin {

  pestanaActiva = 'registrar-medico';

  // Registrar médico
  medico = {
    identification: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    specialty: '',
    licenseNumber: '',
    username: '',
    password: ''
  };
  medicoRegistrado = false;
  errorMedico = '';
  cargandoMedico = false;

  // Registrar agendador
  agendador = {
    identification: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    username: '',
    password: ''
  };
  agendadorRegistrado = false;
  errorAgendador = '';
  cargandoAgendador = false;

  // Configuración
  configuracion = { windowWeeks: 4 };
  configGuardada = false;
  errorConfig = '';

  private apiUrl = 'http://localhost:8080/api/v1';

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

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

  registrarMedico() {
    this.cargandoMedico = true;
    this.errorMedico = '';
    this.http.post(`${this.apiUrl}/doctors`, this.medico,
      { headers: this.headers() }).subscribe({
      next: () => {
        this.medicoRegistrado = true;
        this.cargandoMedico = false;
        this.medico = {
          identification: '', firstName: '', lastName: '',
          email: '', phone: '', specialty: '',
          licenseNumber: '', username: '', password: ''
        };
      },
      error: (err) => {
        this.errorMedico = err.error?.message || 'Error al registrar médico';
        this.cargandoMedico = false;
      }
    });
  }

  registrarAgendador() {
    this.cargandoAgendador = true;
    this.errorAgendador = '';
    this.http.post(`${this.apiUrl}/schedulers/register`, this.agendador,
      { headers: this.headers() }).subscribe({
      next: () => {
        this.agendadorRegistrado = true;
        this.cargandoAgendador = false;
        this.agendador = {
          identification: '', firstName: '', lastName: '',
          email: '', phone: '', username: '', password: ''
        };
      },
      error: (err) => {
        this.errorAgendador = err.error?.message || 'Error al registrar agendador';
        this.cargandoAgendador = false;
      }
    });
  }

  guardarConfiguracion() {
    this.http.put(`${this.apiUrl}/configurations`, this.configuracion,
      { headers: this.headers() }).subscribe({
      next: () => {
        this.configGuardada = true;
        this.errorConfig = '';
      },
      error: (err) => {
        this.errorConfig = err.error?.message || 'Error al guardar configuración';
      }
    });
  }
}