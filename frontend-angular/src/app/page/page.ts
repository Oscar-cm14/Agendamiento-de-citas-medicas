import { Component } from '@angular/core';
import { Header } from '../header/header';
import { Menu } from '../menu/menu';
import { Carrusel } from '../carrusel/carrusel';

import { Farmacia } from '../farmacia/farmacia';
import { Footer } from '../footer/footer';

@Component({
  selector: 'app-page',
  imports: [Header, Menu, Carrusel, Farmacia, Footer],
  templateUrl: './page.html',
  styleUrl: './page.css'
})
export class Page { }
