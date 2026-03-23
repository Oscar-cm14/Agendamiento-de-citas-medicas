import { Component } from '@angular/core';

@Component({
  selector: 'app-medico',
  imports: [],
  templateUrl: './medico.html',
  styleUrl: './medico.css',
})
export class Medico {

 descripcion: string = "Seleccione una especialidad médica.";

  mostrarDescripcion(tipo: string) {

    if (tipo === 'interna') {
      this.descripcion = "Diagnóstico y tratamiento de enfermedades internas del adulto.";
    }

    else if (tipo === 'cardio') {
      this.descripcion = "Especialidad dedicada al corazón y sistema circulatorio.";
    }

    else if (tipo === 'pedia') {
      this.descripcion = "Atención médica especializada para niños.";
    }

    else if (tipo === 'gine') {
      this.descripcion = "Cuidado de la salud femenina y sistema reproductivo.";
    }

    else if (tipo === 'quiro') {
      this.descripcion = "Tratamientos para problemas musculares y de columna.";
    }

    else if (tipo === 'tera') {
      this.descripcion = "Terapia que estimula procesos naturales de curación.";
    }

  }


}
