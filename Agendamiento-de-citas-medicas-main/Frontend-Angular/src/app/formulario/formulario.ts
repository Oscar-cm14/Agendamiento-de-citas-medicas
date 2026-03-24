import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

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
  celular = '';
  genero = '';

  nombresError = '';
  apellidosError = '';
  emailError = '';
  identificacionError = '';
  contrasenaError = '';
  registroExitoso = false;
  cargando = false;
  errorServidor = '';

  constructor(private authService: AuthService) {}

  validarNombres(valor: string) {
    this.nombresError = valor.trim() === '' ? 'Los nombres son obligatorios' : '';
  }

  validarApellidos(valor: string) {
    this.apellidosError = valor.trim() === '' ? 'Los apellidos son obligatorios' : '';
  }

  validarEmail(valor: string) {
    if (valor.trim() === '') {
      this.emailError = 'El correo es obligatorio';
    } else if (!valor.includes('@') || !valor.includes('.')) {
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
  }

  registrar() {
    this.validarNombres(this.nombres);
    this.validarApellidos(this.apellidos);
    this.validarEmail(this.email);
    this.validarIdentificacion(this.identificacion);
    this.validarContrasena(this.contrasena);

    const hayErrores = this.nombresError || this.apellidosError || this.emailError
      || this.identificacionError || this.contrasenaError;

    if (hayErrores) return;

    this.cargando = true;
    this.errorServidor = '';

    const datos = {
      identification: this.identificacion,
      firstName: this.nombres.trim(),
      lastName: this.apellidos.trim(),
      phone: this.celular,
      gender: this.genero,
      email: this.email,
      username: this.email,
      password: this.contrasena
    };

    this.authService.registrarPaciente(datos).subscribe({
      next: () => {
        this.registroExitoso = true;
        this.cargando = false;
      },
      error: (err) => {
        this.errorServidor = err.error?.message
          || 'Error al registrar. Intente nuevamente.';
        this.cargando = false;
      }
    });
  }
}