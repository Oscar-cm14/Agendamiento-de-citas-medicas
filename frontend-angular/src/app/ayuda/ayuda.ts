import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-ayuda',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ayuda.html'
})
export class Ayuda {

  // Sección activa del manual: 'paciente' | 'medico' | 'agendador' | 'admin'
  // Se puede preseleccionar pasando el rol del usuario logueado
  seccionActiva = 'paciente';

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    // Detectar el rol del usuario para abrir el manual correcto automáticamente
    // Si está logueado, abre su manual directamente
    if (typeof localStorage !== 'undefined') {
      const rol = localStorage.getItem('role') || '';
      if (rol === 'DOCTOR')    this.seccionActiva = 'medico';
      if (rol === 'SCHEDULER') this.seccionActiva = 'agendador';
      if (rol === 'ADMIN')     this.seccionActiva = 'admin';
      // PATIENT y sin rol → queda en 'paciente'
    }
  }

  /** Regresa a la página anterior o al home */
  volverAtras() {
    window.history.back();
  }
}


