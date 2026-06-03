import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-formulario',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css'
})
export class Formulario {

  nombres = '';
  apellidos = '';
  email = '';
  identificacion = '';
  contrasena = '';
  confirmarContrasena = '';
  mostrarContrasena = false;
  mostrarConfirmarContrasena = false;
  celular = '';
  codigoPais = '+57';
  genero = '';

  nombresError = '';
  apellidosError = '';
  emailError = '';
  identificacionError = '';
  contrasenaError = '';
  confirmarContrasenaError = '';
  celularError = '';
  generoError = '';
  registroExitoso = false;
  sincronizacionExitosa = false;  // true cuando el paciente ya existía y se re-sincronizó
  cargando = false;
  errorServidor = '';

  constructor(
    private authService: AuthService,
    private cdr: ChangeDetectorRef) { }

  limpiarTexto(valor: string): string {
    if (!valor) return '';
    // 1. Eliminar tildes y marcas diacríticas
    let limpio = valor.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
    // 2. Eliminar espacios múltiples y hacer trim
    limpio = limpio.trim().replace(/\s+/g, ' ');
    // 3. Capitalizar primera letra de cada palabra
    limpio = limpio.split(' ').map(word => 
      word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    ).join(' ');
    
    return limpio;
  }

  validarNombres(valor: string) {
    this.nombres = this.limpiarTexto(valor);
    if (this.nombres === '') {
      this.nombresError = 'Los nombres son obligatorios';
    } else {
      this.nombresError = '';
    }
  }

  validarApellidos(valor: string) {
    this.apellidos = this.limpiarTexto(valor);
    if (this.apellidos === '') {
      this.apellidosError = 'Los apellidos son obligatorios';
    } else {
      this.apellidosError = '';
    }
  }

  validarEmail(valor: string) {
    if (valor.trim() !== '' && (!valor.includes('@') || !valor.includes('.'))) {
      this.emailError = 'Ingrese un correo válido';
    } else {
      this.emailError = '';
    }
  }

  validarIdentificacion(valor: string) {
    this.identificacionError = valor.trim() === ''
      ? 'La identificación es obligatoria' : '';
  }

  validarContrasena(valor: string) {
    if (valor.trim() === '') {
      this.contrasenaError = 'La contraseña es obligatoria';
    } else if (valor.length < 6) {
      this.contrasenaError = 'Mínimo 6 caracteres';
    } else {
      this.contrasenaError = '';
    }
    // Si ya hay algo en confirmarContrasena, revalidarlo para que coincida
    if (this.confirmarContrasena) {
      this.validarConfirmarContrasena();
    }
  }

  validarConfirmarContrasena() {
    if (this.confirmarContrasena.trim() === '') {
      this.confirmarContrasenaError = 'Debe confirmar la contraseña';
    } else if (this.confirmarContrasena !== this.contrasena) {
      this.confirmarContrasenaError = 'Las contraseñas no coinciden';
    } else {
      this.confirmarContrasenaError = '';
    }
  }

  validarCelular(valor: string) {
    this.celularError = valor.trim() === '' ? 'El celular es obligatorio' : '';
  }

  validarGenero(valor: string) {
    this.generoError = valor.trim() === '' ? 'El género es obligatorio' : '';
  }

  registrar() {
    this.validarNombres(this.nombres);
    this.validarApellidos(this.apellidos);
    this.validarEmail(this.email);
    this.validarIdentificacion(this.identificacion);
    this.validarContrasena(this.contrasena);
    this.validarConfirmarContrasena();
    this.validarCelular(this.celular);
    this.validarGenero(this.genero);

    const hayErrores = this.nombresError || this.apellidosError || this.emailError
      || this.identificacionError || this.contrasenaError || this.confirmarContrasenaError
      || this.celularError || this.generoError;

    if (hayErrores) return;

    this.cargando = true;
    this.errorServidor = '';

    const datos = {
      identification: this.identificacion,
      firstName: this.nombres.trim(),
      lastName: this.apellidos.trim(),
      phone: `${this.codigoPais} ${this.celular.trim()}`,
      gender: this.genero,
      email: this.email,
      username: this.identificacion,
      password: this.contrasena
    };

    this.authService.registrarPaciente(datos).subscribe({
      next: (res: any) => {
        if (res?.id) {
          localStorage.setItem('userId', String(res.id));
        }
        // Si el paciente ya existía en H2, el backend lo re-sincroniza con
        // Keycloak y devuelve sus datos. Mostramos mensaje diferenciado.
        this.sincronizacionExitosa = res?._sincronizado === true;
        this.registroExitoso = true;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorServidor = err.error?.message
          || 'Error al registrar. Intente nuevamente.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }
}