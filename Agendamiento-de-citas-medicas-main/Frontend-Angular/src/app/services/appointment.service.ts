import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class AppointmentService {

  private apiUrl = 'http://localhost:8080/api/v1';

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  private headers(): HttpHeaders {
    let token = '';
    if (isPlatformBrowser(this.platformId)) {
      token = localStorage.getItem('token') || '';
    }
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  listarCitas(doctorId: number, date: string): Observable<any[]> {
    const params = new HttpParams()
      .set('doctorId', doctorId)
      .set('date', date);
    return this.http.get<any[]>(`${this.apiUrl}/appointments`,
      { headers: this.headers(), params });
  }

  crearCita(datos: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/appointments`, datos,
      { headers: this.headers() });
  }

  obtenerFranjas(doctorId: number, date: string): Observable<any[]> {
    const params = new HttpParams()
      .set('doctorId', doctorId)
      .set('date', date);
    return this.http.get<any[]>(`${this.apiUrl}/appointments/slots`,
      { headers: this.headers(), params });
  }

  listarMedicos(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/doctors`,
      { headers: this.headers() });
  }

  obtenerHorario(doctorId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/doctors/schedules/${doctorId}`,
      { headers: this.headers() });
  }

  configurarHorario(datos: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/doctors/schedules`, datos,
      { headers: this.headers() });
  }
}