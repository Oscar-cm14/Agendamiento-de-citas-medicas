import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  // ─────────────────────────────────────────────
  // LOGIN
  // ─────────────────────────────────────────────
  login(username: string, password: string): Observable<any> {

    return this.http.post<any>(
      `${this.apiUrl}/auth/login`,
      {
        username,
        password
      }
    ).pipe(

      tap(response => {

        console.log('LOGIN RESPONSE:', response);

        // Guardar token
        if (response.token) {
          localStorage.setItem('token', response.token);
        }

        // Guardar rol
        if (response.role) {
          localStorage.setItem('role', response.role);
        }

        // Guardar userId
        if (response.userId) {
          localStorage.setItem(
            'userId',
            response.userId.toString()
          );
        }

        // Guardar username
        if (response.username) {
          localStorage.setItem(
            'username',
            response.username
          );
        }

      })

    );
  }

  // ─────────────────────────────────────────────
  // REGISTRO PACIENTE
  // ─────────────────────────────────────────────
  registrarPaciente(datos: any): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/patients/register`,
      datos
    );
  }

  // ─────────────────────────────────────────────
  // GUARDAR SESIÓN
  // ─────────────────────────────────────────────
  guardarSesion(
    token: string,
    role: string,
    userId?: number,
    username?: string
  ) {

    if (isPlatformBrowser(this.platformId)) {

      localStorage.setItem('token', token);

      localStorage.setItem('role', role);

      if (userId !== undefined && userId !== null) {
        localStorage.setItem(
          'userId',
          userId.toString()
        );
      }

      if (username) {
        localStorage.setItem(
          'username',
          username
        );
      }

    }
  }

  // ─────────────────────────────────────────────
  // TOKEN
  // ─────────────────────────────────────────────
  obtenerToken(): string | null {

    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('token');
    }

    return null;
  }

  // ─────────────────────────────────────────────
  // ROL
  // ─────────────────────────────────────────────
  obtenerRol(): string | null {

    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('role');
    }

    return null;
  }

  // ─────────────────────────────────────────────
  // USER ID
  // ─────────────────────────────────────────────
  obtenerUserId(): number | null {

    if (isPlatformBrowser(this.platformId)) {

      const id = localStorage.getItem('userId');

      console.log('USER ID LOCALSTORAGE:', id);

      return id ? Number(id) : null;
    }

    return null;
  }

  // ─────────────────────────────────────────────
  // USERNAME
  // ─────────────────────────────────────────────
  obtenerUsername(): string | null {

    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('username');
    }

    return null;
  }

  // ─────────────────────────────────────────────
  // AUTENTICADO
  // ─────────────────────────────────────────────
  estaAutenticado(): boolean {
    return this.obtenerToken() !== null;
  }

  // ─────────────────────────────────────────────
  // CERRAR SESIÓN
  // ─────────────────────────────────────────────
  cerrarSesion() {

    if (isPlatformBrowser(this.platformId)) {

      localStorage.removeItem('token');

      localStorage.removeItem('role');

      localStorage.removeItem('userId');

      localStorage.removeItem('username');

    }

  }
}