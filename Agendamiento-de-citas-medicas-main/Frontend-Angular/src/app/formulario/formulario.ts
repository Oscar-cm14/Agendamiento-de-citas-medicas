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

  nombre = '';
  email = '';
  identificacion = '';
  contrasena = '';
  celular = '';
  genero = '';

  nombreError = '';
  emailError = '';
  identificacionError = '';
  contrasenaError = '';
  registroExitoso = false;
  cargando = false;
  errorServidor = '';

  constructor(private authService: AuthService) {}

  validarNombre(valor: string) {
    this.nombreError = valor.trim() === '' ? 'El nombre es obligatorio' : '';
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
    this.validarNombre(this.nombre);
    this.validarEmail(this.email);
    this.validarIdentificacion(this.identificacion);
    this.validarContrasena(this.contrasena);

    const hayErrores = this.nombreError || this.emailError
      || this.identificacionError || this.contrasenaError;

    if (hayErrores) return;

    this.cargando = true;
    this.errorServidor = '';

    const partes = this.nombre.trim().split(' ');
    const firstName = partes[0];
    const lastName = partes.slice(1).join(' ') || partes[0];

    const datos = {
      identification: this.identificacion,
      firstName: firstName,
      lastName: lastName,
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