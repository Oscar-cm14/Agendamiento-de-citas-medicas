import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AppointmentService {

  private apiUrl = environment.apiUrl;

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

  // ================================================================
  // RF5 - NUEVO MÉTODO
  // Descarga el CSV de citas de un médico en una fecha.
  // Retorna Blob para que el navegador lo descargue como archivo.
  // observe: 'response' permite leer el header Content-Disposition
  // para obtener el nombre del archivo desde el servidor.
  // ================================================================
  exportarCsv(doctorId: number, date: string): Observable<Blob> {
    const params = new HttpParams()
      .set('doctorId', doctorId)
      .set('date', date);
    return this.http.get(`${this.apiUrl}/appointments/export`,
      {
        headers: this.headers(),
        params,
        responseType: 'blob'   // <-- indica que la respuesta es binaria (archivo)
      });
  }
}