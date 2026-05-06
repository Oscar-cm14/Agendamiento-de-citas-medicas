import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';

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

  private keycloakUrl = 'http://localhost:8081/realms/clinica-realm/protocol/openid-connect/token';
  private clientId = 'clinica-frontend';

  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  iniciarSesion() {
    if (!this.username || !this.password) {
      this.error = 'Complete todos los campos';
      return;
    }

    this.cargando = true;
    this.error = '';

    const body = new HttpParams()
      .set('client_id', this.clientId)
      .set('grant_type', 'password')
      .set('username', this.username)
      .set('password', this.password);

    const headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded'
    });

    this.http.post<any>(this.keycloakUrl, body.toString(), { headers }).subscribe({
      next: (res) => {
        try {
          const token = res.access_token;
          localStorage.setItem('token', token);
          
          // Parse JWT para extraer el rol principal
          const payload = JSON.parse(atob(token.split('.')[1]));
          const roles = payload.realm_access?.roles || [];
          
          let role = 'PATIENT'; // default
          if (roles.includes('ADMIN')) role = 'ADMIN';
          else if (roles.includes('DOCTOR')) role = 'DOCTOR';
          else if (roles.includes('SCHEDULER')) role = 'SCHEDULER';
          
          localStorage.setItem('role', role);

        } catch (e) {
          console.error("Error parseando token", e);
        }

        this.cargando = false;
        const savedRole = localStorage.getItem('role');

        switch (savedRole) {
          case 'ADMIN':
            this.router.navigate(['/admin']);
            break;
          case 'SCHEDULER':
            this.router.navigate(['/agendador']);
            break;
          case 'DOCTOR':
            this.router.navigate(['/medico']);
            break;
          default:
            this.router.navigate(['/agendar']);
        }
      },
      error: (err) => {
        this.cargando = false;
        if (err.status === 0) {
          this.error = 'No se puede conectar a Keycloak (Servidor caído o problema de CORS)';
        } else if (err.status === 401 || err.status === 400) {
          this.error = 'Usuario o contraseña incorrectos';
        } else {
          this.error = 'Error ' + err.status + ': ' + (err.error?.error_description || 'Acceso denegado');
        }
      }
    });
  }
}