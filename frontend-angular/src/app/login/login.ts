import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {

  username = '';
  password = '';
  error    = '';
  cargando = false;

  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private router: Router) {}

  iniciarSesion() {
    if (!this.username || !this.password) {
      this.error = 'Complete todos los campos';
      return;
    }

    this.cargando = true;
    this.error    = '';

    this.http.post<any>(
      `${this.apiUrl}/users/login`,
      { username: this.username, password: this.password }
    ).subscribe({

      next: (res) => {
        localStorage.setItem('token',    res.token);
        localStorage.setItem('role',     res.role);
        localStorage.setItem('username', res.username ?? this.username);
        if (res.userId != null) {
          localStorage.setItem('userId', String(res.userId));
        }
        this.cargando = false;
        this.redirigirSegunRol(res.role);
      },

      error: (err) => {
        this.cargando = false;
        if      (err.status === 0)   this.error = 'No se puede conectar al servidor';
        else if (err.status === 401) this.error = 'Usuario o contraseña incorrectos';
        else if (err.status === 503) this.error = 'Keycloak no está disponible';
        else                         this.error = err.error?.error ?? 'Error al iniciar sesión';
      }
    });
  }

  private redirigirSegunRol(role: string) {
    switch (role) {
      case 'ADMIN':     this.router.navigate(['/admin']);     break;
      case 'SCHEDULER': this.router.navigate(['/agendador']); break;
      case 'DOCTOR':    this.router.navigate(['/medico']);    break;
      case 'PATIENT':   this.router.navigate(['/agendar']);  break;
      default:          this.router.navigate(['/agendar']);
    }
  }
}