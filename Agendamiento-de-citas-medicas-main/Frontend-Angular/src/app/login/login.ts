import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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
  error = '';
  cargando = false;

  private apiUrl = 'http://localhost:8080/api/v1';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  iniciarSesion() {
    if (!this.username || !this.password) {
      this.error = 'Complete todos los campos';
      return;
    }

    this.cargando = true;
    this.error = '';

    this.http.post<any>(`${this.apiUrl}/auth/login`, {
      username: this.username,
      password: this.password
    }).subscribe({
      next: (res) => {
        // Guardar token directamente
        try {
          localStorage.setItem('token', res.token);
          localStorage.setItem('role', res.role);
          if (res.id) {
            localStorage.setItem('userId', res.id.toString());
          }
        } catch (e) {}

        this.cargando = false;

        // Redirigir según rol
        switch (res.role) {
          case 'PATIENT':
            this.router.navigate(['/agendar']);
            break;
          case 'ADMIN':
            this.router.navigate(['/admin']);
            break;
          case 'SCHEDULER':
            this.router.navigate(['/agendador']);
            break;
          case 'DOCTOR':
            this.router.navigate(['/agendador']);
            break;
          default:
            this.router.navigate(['/agendar']);
        }
      },
      error: (err) => {
        this.cargando = false;
        if (err.status === 0) {
          this.error = 'No se puede conectar al servidor';
        } else if (err.status === 401) {
          this.error = 'Usuario o contraseña incorrectos';
        } else if (err.status === 403) {
          this.error = 'Acceso denegado';
        } else {
          this.error = 'Error ' + err.status + ': ' + (err.error?.message || '');
        }
      }
    });
  }
}