import { Routes } from '@angular/router';
import { Page } from './page/page';
import { Login } from './login/login';
import { Formulario } from './formulario/formulario';
import { AgendarCita } from './agendar-cita/agendar-cita';
import { Admin } from './admin/admin';
import { Agendador } from './agendador/agendador';
import { Medico } from './medico/medico';

export const routes: Routes = [
  { path: '', component: Page },
  { path: 'login', component: Login },
  { path: 'registro', component: Formulario },
  { path: 'agendar', component: AgendarCita },
  { path: 'admin', component: Admin },
  { path: 'agendador', component: Agendador },
  { path: 'medico', component: Medico },
  { path: '**', redirectTo: '' }
];