import { Routes } from '@angular/router';
import { Page } from './page/page';
import { Login } from './login/login';
import { Formulario } from './formulario/formulario';
import { AgendarCita } from './agendar-cita/agendar-cita';
import { Admin } from './admin/admin';
import { Agendador } from './agendador/agendador';
import { Medico } from './medico/medico';
import { Ayuda } from './ayuda/ayuda';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: Page },
  { path: 'login', component: Login },
  { path: 'registro', component: Formulario },
  { path: 'agendar',   component: AgendarCita, canActivate: [authGuard] },
  { path: 'admin',     component: Admin,       canActivate: [authGuard] },
  { path: 'agendador', component: Agendador,   canActivate: [authGuard] },
  { path: 'medico',    component: Medico,      canActivate: [authGuard] },
  { path: 'ayuda', component: Ayuda },
  { path: '**', redirectTo: '' }
];